# ARCHITECTURE.md — Technical Architecture
## Aegis: Personal World Intelligence App

**Version:** 1.1 (updated with Neo4j Graph RAG)
**Date:** April 2026

---

## 1. System Overview

Aegis is composed of four distinct layers that must never bleed into each other:

```
┌─────────────────────────────────┐
│         Android App             │  Kotlin + Jetpack Compose
│  (UI, local state, voice I/O)   │  Android Studio
└────────────┬────────────────────┘
             │ HTTPS / REST
             ▼
┌─────────────────────────────────┐
│       Aegis Backend API         │  Node.js + Express
│  (auth, AI, articles, graph)    │  Deployed on Railway
└────────┬──────────┬─────────────┘
         │          │
   ┌─────┘    ┌─────┴──────┐      ┌─────────────────┐
   ▼          ▼            ▼      ▼
┌──────────┐ ┌──────────┐ ┌──────────────┐
│ Supabase │ │  Neo4j   │ │ Crucix Server│  Railway (separate service)
│ Postgres │ │ AuraDB   │ │ (data feeds) │  Node.js, sweeps every 15 min
└──────────┘ └──────────┘ └──────────────┘
```

> ⚠️ **IMPORTANT — Vercel vs Railway:**
> Crucix CANNOT run on Vercel. Vercel is serverless — functions time out after 10–300 seconds. Crucix runs a persistent Express server with a background 15-minute sweep cycle and SSE connections. Both Crucix and the Aegis Backend must run on Railway, which provides always-on persistent containers.

---

## 2. Technology Choices

| Layer | Choice | Why |
|---|---|---|
| Android | Kotlin + Jetpack Compose | Modern, officially supported, best tooling |
| IDE | Android Studio | Only serious option for Android native |
| Backend API | Node.js + Express | Same language as Crucix, easy integration |
| Hosting | Railway | Persistent containers, free tier, simple deploys |
| Relational DB | Supabase (Postgres) | Auth, storage, RLS, free tier, JS SDK |
| Graph DB | Neo4j AuraDB | Graph RAG storage, Cypher queries, free tier (50k nodes / 175k edges), built-in browser visualisation |
| AI | Google Gemini API | Free tier generous, strong at long-context analysis |
| TTS (podcast) | Google Cloud Text-to-Speech | Multiple voice personas, high quality, pay-per-use |
| Push Notifications | Firebase Cloud Messaging | Free, official Android push infrastructure |
| Secrets (Android) | Android Keystore + EncryptedSharedPreferences | Never plain text, never in source code |
| Auth | Supabase Auth | JWT-based, handles sessions, free |

---

## 3. Repository Structure

Three separate Git repositories — never a monorepo:

```
aegis-crucix/          ← Fork of calesthio/Crucix with 3 added endpoints
aegis-backend/         ← The Aegis API server (Node.js)
aegis-android/         ← The Android app (Kotlin)
```

---

## 4. Crucix Repository (`aegis-crucix`)

Fork of the Crucix project. Only `server.mjs` is modified to add protected endpoints:

```
aegis-crucix/
├── .env.example               ← Never commit actual .env
├── .gitignore                 ← Includes .env, runs/, node_modules/
├── server.mjs                 ← MODIFIED: add /api/aegis/* endpoints
├── crucix.config.mjs
├── apis/
│   ├── briefing.mjs
│   └── sources/               ← 26 source modules — DO NOT TOUCH
├── lib/
│   ├── llm/
│   ├── delta/
│   └── alerts/
├── dashboard/
│   └── public/jarvis.html
└── runs/                      ← gitignored, runtime data
```

**Three endpoints added to `server.mjs`:**
```javascript
// All require: X-Aegis-Secret: <value from .env>
GET  /api/aegis/latest   → returns runs/latest.json
GET  /api/aegis/health   → returns { status, lastSweep, uptime }
GET  /api/aegis/delta    → returns what changed since last sweep
```

---

## 5. Backend API Repository (`aegis-backend`)

```
aegis-backend/
├── .env.example
├── .gitignore                 ← Includes .env, node_modules/
├── package.json
├── server.js                  ← Entry point, Express setup, route mounting
│
├── config/
│   └── index.js               ← Reads + validates all .env variables on startup
│
├── middleware/
│   ├── auth.js                ← Validates Supabase JWT on protected routes
│   ├── rateLimit.js           ← 60 req/min per IP (express-rate-limit)
│   └── errorHandler.js        ← Global error handler, structured logging
│
├── routes/
│   ├── auth.js                ← POST /auth/verify
│   ├── data.js                ← GET /data/latest, GET /data/health
│   ├── articles.js            ← GET /articles, GET /articles/:id, POST /articles/generate
│   ├── conversations.js       ← GET/POST /conversations, POST /conversations/:id/messages
│   ├── notifications.js       ← GET /notifications, PATCH /notifications/:id/read
│   ├── preferences.js         ← GET/PUT /preferences
│   ├── interactions.js        ← POST /interactions
│   └── podcast.js             ← POST /podcast/generate
│
├── services/
│   ├── crucixClient.js        ← Fetches data from Crucix /api/aegis/* endpoints
│   ├── geminiService.js       ← All calls to Gemini API (articles, chat, entity extraction)
│   ├── graphService.js        ← Neo4j read/write: entity extraction storage + Cypher RAG queries
│   ├── articleGenerator.js    ← Orchestrates: crucixClient → geminiService → graphService → save
│   ├── podcastGenerator.js    ← Dialogue script via Gemini + Google TTS + ffmpeg concat
│   ├── notificationService.js ← FCM push notifications
│   ├── recommendationEngine.js← Article scoring algorithm
│   └── sweepWatcher.js        ← Polls Crucix every 15 min, triggers article gen + notifications
│
├── db/
│   ├── supabaseClient.js      ← Supabase JS client singleton
│   ├── neo4jClient.js         ← Neo4j driver singleton (neo4j-driver package)
│   ├── migrations/            ← Sequential SQL migration files
│   │   ├── 001_initial_schema.sql
│   │   ├── 002_add_interactions.sql
│   │   └── 003_add_podcasts.sql
│   └── queries/
│       ├── articles.js
│       ├── conversations.js
│       ├── interactions.js
│       ├── notifications.js
│       └── preferences.js
│
└── utils/
    ├── logger.js              ← Structured logging (pino)
    └── validators.js          ← Input validation helpers
```

---

## 6. Graph RAG Architecture (`graphService.js`)

This is the core of the knowledge graph feature. It has two responsibilities:

### 6.1 Write Path — After Every Article

```javascript
// services/graphService.js

async function extractAndStoreEdges(articleId, articleText) {
  // Step 1: Ask Gemini to extract edges from the article
  const edges = await geminiService.extractEdges(articleText);
  // Returns: [{ source, target, relationship, confidence }, ...]
  // Max 5 edges per article. relationship is one of:
  // caused | correlated_with | escalated | preceded | triggered | de-escalated

  // Step 2: Normalise entity names to avoid duplicates
  // "Russia", "Russian Federation", "Russia govt" → all become "Russia"
  const normalisedEdges = normaliseEntities(edges);

  // Step 3: Write to Neo4j using MERGE (creates if not exists, no duplicates)
  const session = neo4jClient.session();
  for (const edge of normalisedEdges) {
    await session.run(`
      MERGE (a:Entity {name: $source})
      MERGE (b:Entity {name: $target})
      MERGE (a)-[r:RELATES {type: $relationship}]->(b)
      ON CREATE SET r.confidence = $confidence,
                    r.firstSeen = datetime(),
                    r.articleId = $articleId
      ON MATCH SET  r.lastSeen = datetime(),
                    r.count = coalesce(r.count, 1) + 1
    `, { ...edge, articleId });
  }
  await session.close();
}
```

### 6.2 Read Path — Before Every Gemini Call

```javascript
async function getGraphContext(entityNames) {
  // entityNames: array of key entities in the current question/topic
  // e.g. ["oil prices", "Saudi Arabia", "OPEC"]

  const session = neo4jClient.session();
  const result = await session.run(`
    MATCH (n:Entity)-[r*1..2]-(m:Entity)
    WHERE n.name IN $names
    RETURN n.name AS source,
           type(r[-1]) AS relationship,
           m.name AS target,
           r[-1].confidence AS confidence,
           r[-1].lastSeen AS lastSeen
    ORDER BY r[-1].lastSeen DESC
    LIMIT 20
  `, { names: entityNames });
  await session.close();

  // Format as a compact context string to prepend to the Gemini prompt
  return result.records.map(r =>
    `${r.get('source')} → [${r.get('relationship')}] → ${r.get('target')} (confidence: ${r.get('confidence')})`
  ).join('\n');
}
```

### 6.3 Gemini Entity Extraction Prompt

Added to the article generation prompt in `geminiService.js`:

```
After the article JSON, also return a "edges" array (max 5 items).
Each edge must have:
  - source: canonical entity name (country, organisation, commodity, index)
  - target: canonical entity name
  - relationship: one of [caused, correlated_with, escalated, preceded, triggered, de-escalated]
  - confidence: float 0.0–1.0

Only include edges where you have genuine evidence from the sweep data.
Do not speculate. If fewer than 5 clear edges exist, return fewer.
```

### 6.4 Entity Normalisation Rules

Common patterns to canonicalise in `normaliseEntities()`:

| Raw name | Canonical |
|---|---|
| "Russia", "Russian Federation", "Russian govt", "Moscow" | "Russia" |
| "US", "United States", "U.S.", "Washington" | "United States" |
| "oil", "crude", "WTI", "Brent" | "Oil prices" |
| "stock market", "equities", "S&P 500" | "US equities" |

Implement as a simple lookup map. Add to it over time as you notice duplicates in Neo4j Browser.

---

## 7. Supabase Database Schema

```sql
-- Users managed by Supabase Auth (auth.users table)

CREATE TABLE user_preferences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  modules TEXT[] NOT NULL DEFAULT '{}',
  notification_flash BOOLEAN DEFAULT TRUE,
  notification_priority BOOLEAN DEFAULT TRUE,
  notification_routine BOOLEAN DEFAULT FALSE,
  dnd_start TIME,
  dnd_end TIME,
  tts_speed FLOAT DEFAULT 1.0,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE articles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  summary TEXT NOT NULL,
  modules TEXT[] NOT NULL,
  sources JSONB NOT NULL DEFAULT '[]',
  sweep_id TEXT,
  graph_context_used BOOLEAN DEFAULT FALSE, -- was Neo4j context injected?
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE article_interactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  article_id UUID REFERENCES articles(id) ON DELETE CASCADE,
  read_duration_seconds INTEGER DEFAULT 0,
  scroll_depth_percent INTEGER DEFAULT 0,
  liked BOOLEAN,
  bookmarked BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  title TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
  content TEXT NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  tier TEXT NOT NULL CHECK (tier IN ('FLASH', 'PRIORITY', 'ROUTINE')),
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  article_id UUID REFERENCES articles(id),
  read BOOLEAN DEFAULT FALSE,
  sent_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE podcasts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  article_id UUID REFERENCES articles(id),
  audio_url TEXT NOT NULL,
  duration_seconds INTEGER,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE device_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  token TEXT NOT NULL UNIQUE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS on every table (see AI_RULES.md Section 7)
-- Index on frequently queried columns
CREATE INDEX idx_articles_created_at ON articles(created_at DESC);
CREATE INDEX idx_article_interactions_user ON article_interactions(user_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, read);
```

---

## 8. Neo4j Schema (AuraDB)

Neo4j is schemaless by default. The following describes the intended node/relationship structure:

```
// Node label: Entity
// Properties: name (string, unique), firstSeen (datetime), lastSeen (datetime)

// Relationship type: RELATES
// Properties: type (string), confidence (float), count (int),
//             firstSeen (datetime), lastSeen (datetime), articleId (string)

// Allowed relationship type values:
// caused | correlated_with | escalated | preceded | triggered | de-escalated

// Example graph:
(Russia)-[:RELATES {type:"escalated"}]->(Ukraine conflict)
(Ukraine conflict)-[:RELATES {type:"correlated_with"}]->(European gas prices)
(European gas prices)-[:RELATES {type:"preceded"}]->(VIX spike)
(OPEC)-[:RELATES {type:"caused"}]->(Oil prices)
```

**Useful Cypher queries for Neo4j Browser exploration:**

```cypher
// See everything connected to a specific entity (2 hops)
MATCH (n:Entity {name: "Russia"})-[r*1..2]-(m)
RETURN n, r, m LIMIT 50

// Most connected entities (hubs)
MATCH (n:Entity)-[r]-()
RETURN n.name, count(r) AS connections
ORDER BY connections DESC LIMIT 20

// Most recent edges
MATCH (a)-[r:RELATES]->(b)
RETURN a.name, r.type, b.name, r.lastSeen
ORDER BY r.lastSeen DESC LIMIT 30

// Relationship type distribution
MATCH ()-[r:RELATES]->()
RETURN r.type, count(r) AS total
ORDER BY total DESC

// Find causal chains (path between two entities)
MATCH path = shortestPath(
  (a:Entity {name:"Oil prices"})-[*..4]-(b:Entity {name:"VIX"})
)
RETURN path
```

---

## 9. Android App Repository (`aegis-android`)

```
aegis-android/
├── .gitignore                ← Includes local.properties, *.keystore, secrets.properties, google-services.json
├── gradle.properties
├── secrets.properties        ← gitignored, holds AEGIS_API_BASE_URL and SUPABASE_ANON_KEY
├── local.properties          ← gitignored (auto-generated by Android Studio)
│
├── app/
│   ├── build.gradle.kts      ← Kotlin DSL, references secrets via gradle-secrets-plugin
│   │
│   └── src/main/
│       ├── AndroidManifest.xml
│       │
│       ├── java/com/aegis/app/
│       │   ├── AegisApplication.kt
│       │   │
│       │   ├── data/
│       │   │   ├── remote/
│       │   │   │   ├── AegisApiService.kt
│       │   │   │   ├── dto/
│       │   │   │   └── ApiClient.kt
│       │   │   ├── local/
│       │   │   │   ├── AegisDatabase.kt
│       │   │   │   ├── dao/
│       │   │   │   └── entity/
│       │   │   └── repository/
│       │   │       ├── ArticleRepository.kt
│       │   │       ├── ConversationRepository.kt
│       │   │       ├── NotificationRepository.kt
│       │   │       └── PreferencesRepository.kt
│       │   │
│       │   ├── domain/
│       │   │   ├── model/
│       │   │   │   ├── Article.kt
│       │   │   │   ├── Message.kt
│       │   │   │   ├── Conversation.kt
│       │   │   │   └── Notification.kt
│       │   │   └── usecase/
│       │   │       ├── GetArticlesUseCase.kt
│       │   │       ├── SendMessageUseCase.kt
│       │   │       └── GeneratePodcastUseCase.kt
│       │   │
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Theme.kt
│       │   │   │   ├── Typography.kt
│       │   │   │   └── Color.kt
│       │   │   ├── navigation/
│       │   │   │   └── AegisNavGraph.kt
│       │   │   ├── screens/
│       │   │   │   ├── onboarding/
│       │   │   │   ├── auth/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── article/
│       │   │   │   ├── conversation/
│       │   │   │   ├── notifications/
│       │   │   │   └── settings/
│       │   │   └── components/
│       │   │       ├── ArticleCard.kt
│       │   │       ├── ModuleTile.kt
│       │   │       ├── MessageBubble.kt
│       │   │       ├── NotificationBadge.kt
│       │   │       ├── AudioPlayer.kt
│       │   │       └── LoadingIndicator.kt
│       │   │
│       │   ├── service/
│       │   │   └── AegisFcmService.kt
│       │   │
│       │   └── di/
│       │       ├── NetworkModule.kt
│       │       ├── DatabaseModule.kt
│       │       └── RepositoryModule.kt
│       │
│       └── res/
│           ├── values/
│           └── drawable/
```

---

## 10. Data Flow: Article Generation with Graph RAG

```
Every 15 min:
  Crucix sweeps 26 sources
       ↓
  sweepWatcher.js detects new sweep via /api/aegis/delta
       ↓
  articleGenerator.js:
    1. Fetches sweep data from Crucix
    2. Extracts key entities from sweep (simple regex/keyword scan)
    3. Calls graphService.getGraphContext(entities)
       → Neo4j 2-hop Cypher query returns historical connections
    4. Calls geminiService.generateArticle(sweepData, graphContext)
       → Gemini generates article AND returns up to 5 new edges
    5. Saves article to Supabase (articles table)
    6. Calls graphService.extractAndStoreEdges(articleId, edges)
       → New edges written to Neo4j with MERGE (no duplicates)
       ↓
  recommendationEngine.js scores article for user
       ↓
  notificationService.js evaluates tier, sends FCM push
       ↓
  Android app receives push, updates dashboard
```

---

## 11. Data Flow: Conversation with Graph RAG

```
User types or speaks a message
       ↓
  VoiceInputHandler (if voice): SpeechRecognizer → text
       ↓
  POST /conversations/:id/messages { role: "user", content: "..." }
       ↓
  Backend:
    1. Fetches last 10 messages (conversation history)
    2. Extracts key entities from user message
    3. Calls graphService.getGraphContext(entities)
       → Returns relevant historical chains from Neo4j
    4. Fetches latest Crucix sweep summary
    5. Builds Gemini prompt:
       [System: analyst persona]
       [Graph context: entity relationships from Neo4j]
       [Current data: latest sweep summary]
       [History: last 10 messages]
       [User: new message]
    6. Gemini responds
    7. Saves response to messages table
    8. Returns response to Android app
       ↓
  If voice mode: Android TextToSpeech reads response aloud
```

---

## 12. Security Architecture

### Android
- JWT token stored in EncryptedSharedPreferences (wraps Android Keystore)
- API base URL and Supabase anon key in `secrets.properties` (gitignored) → injected at build time via `gradle-secrets-plugin` into `BuildConfig`
- NEVER store secrets in `strings.xml`, `res/` files, or any committed file
- Certificate pinning on the API base URL (OkHttp CertificatePinner) in release builds
- No logging of sensitive data in release builds (`BuildConfig.DEBUG` check on every log call)

### Backend
- All routes except `/health` require `Authorization: Bearer <supabase_jwt>`
- Supabase JWT verified on every request using Supabase Admin SDK
- Crucix endpoints protected by `X-Aegis-Secret` header
- Neo4j credentials in `.env` only — never in source code
- Input validation on all request bodies (reject unexpected fields)
- Rate limiting: 60 requests/minute per IP
- CORS: only allow requests from the Android app's origin

### Supabase
- Row Level Security (RLS) enabled on ALL tables
- Policy on every table: `USING (auth.uid() = user_id)`
- Service role key used ONLY in backend `.env`, never in Android app
- Android app uses anon key only (which is scoped by RLS)

### Neo4j
- AuraDB connection string (bolt URL + username + password) in `.env` only
- Never exposed to the Android app — all Neo4j access is backend-only
- AuraDB free tier is managed/hosted by Neo4j — no server to secure yourself

---

## 13. Environment Variables

### `aegis-crucix/.env`
```
PORT=3117
FRED_API_KEY=
FIRMS_MAP_KEY=
EIA_API_KEY=
LLM_PROVIDER=gemini
LLM_API_KEY=
AEGIS_SECRET=<long_random_string>
```

### `aegis-backend/.env`
```
PORT=3000
NODE_ENV=production

# Supabase
SUPABASE_URL=
SUPABASE_SERVICE_KEY=

# Neo4j AuraDB
NEO4J_URI=neo4j+s://xxxxxxxx.databases.neo4j.io
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=

# AI & Media
GEMINI_API_KEY=
GOOGLE_TTS_API_KEY=

# Crucix
CRUCIX_BASE_URL=
CRUCIX_SECRET=

# Firebase
FCM_SERVICE_ACCOUNT_JSON=
```

### `aegis-android/secrets.properties` (gitignored)
```
AEGIS_API_BASE_URL=https://your-backend.railway.app
SUPABASE_ANON_KEY=eyJ...
```

---

## 14. Podcast Generation

Aegis uses Gemini for the dialogue script and Google Cloud TTS for voices, avoiding the GPU dependency of open-notebooklm:

```
POST /podcast/generate { articleId }
       ↓
  Fetch article from Supabase
  Fetch graph context for article's main entities from Neo4j
       ↓
  Send to Gemini:
  "Write a 5-minute podcast dialogue between Alex (curious questioner)
   and Jordan (analyst). Use this graph context to have them reference
   historical connections. Format: JSON array of {speaker, text}.
   Natural speech patterns. They may respectfully disagree."
       ↓
  For each line, call Google Cloud TTS:
    Alex  → WaveNet voice en-US-Neural2-A (male)
    Jordan → WaveNet voice en-US-Neural2-F (female)
       ↓
  Concatenate audio via ffmpeg (installed on Railway via nixpacks.toml)
       ↓
  Upload MP3 to Supabase Storage
       ↓
  Return { audioUrl, durationSeconds }
       ↓
  Android renders inline AudioPlayer component
```

---

*End of ARCHITECTURE.md*
