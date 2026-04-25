# COMPLETE BEGINNER'S GUIDE — Building Aegis from Zero
## For Someone with No Android Development Experience

**Version:** 1.1 (updated with Neo4j Graph RAG)
**Date:** April 2026

---

## First, Read This

You are building something genuinely ambitious: a real-time intelligence app with AI conversations, a self-growing knowledge graph, voice interaction, push notifications, and podcast generation. This would be a medium-sized production project for an experienced engineer.

The good news: you have an AI coding agent (Claude Code, Cursor, etc.) that will write most of the actual code. Your job is to understand the structure, make decisions about trade-offs, test what was built, and not let complexity push you into shortcuts that create bigger problems later.

**What "minimal experience" means in practice:**
- The AI coding agent writes the code
- You describe what you want in plain English
- You test what was built and report what broke
- You read and understand what the agent writes, even if you couldn't write it yourself
- You make the decisions — the agent executes them

**What you absolutely must NOT do:**
- Let the agent "just make it work somehow" without explaining why
- Skip phases to get to the exciting features faster
- Ignore errors and hope they sort themselves out
- Put secrets in code files because it's "just temporary"

---

## Part 1: The Big Picture

### What You Are Actually Building

You are building **four separate things** that talk to each other:

**Thing 1: Crucix (the data engine)**
Not your code. An existing open-source project you fork. Add 3 small API endpoints to it, deploy it, and mostly leave it alone. It watches the world every 15 minutes and gives you structured data.

**Thing 2: Aegis Backend (your API server)**
A Node.js server that is the brain of the operation. It talks to Crucix for data, Gemini to generate articles and responses, Neo4j to store and query the knowledge graph, Supabase to store everything else, and Firebase to send push notifications. Your Android app ONLY talks to this server.

**Thing 3: Neo4j AuraDB (the knowledge graph)**
A dedicated graph database that stores entities (countries, commodities, indices, organisations) and the relationships between them that Gemini extracts from every article. Over time this becomes a persistent map of how world events connect to each other. The backend queries it to give Gemini historical context before answering you.

**Thing 4: Aegis Android (your app)**
What you see on your phone. Mostly a presentation layer — it shows what the backend sends, lets you type or speak, and renders everything in a readable format. The smart stuff happens on the backend.

### How the Knowledge Graph Works (Plain English)

Every time a new article is generated:

1. Gemini reads the Crucix sweep data and writes a 600-word analyst article
2. At the same time, Gemini also extracts up to 5 *relationships* from the data: e.g. "Russia escalated Ukraine conflict", "Ukraine conflict correlated_with European gas prices", "OPEC caused Oil price spike"
3. These relationships are stored as nodes and edges in Neo4j

Next time you ask a question — say, "why is European energy expensive?":

1. The backend extracts "European energy" as a key entity
2. It queries Neo4j: "show me everything connected to European energy within 2 hops"
3. Neo4j returns: Ukraine conflict → correlated_with → European gas prices, Russia → escalated → Ukraine conflict, OPEC → caused → Oil price spike
4. That chain gets prepended to the Gemini prompt BEFORE Gemini answers you
5. Gemini now gives you a historically grounded answer that traces the causal chain — not just what is happening now, but why it got here

**The graph gets smarter the longer the app runs.** After a week, it has 7 days of connections. After a month, it has a rich web of how things caused other things. This is what makes Aegis fundamentally different from just asking ChatGPT.

### Where Everything Lives

| What | Where | Cost |
|---|---|---|
| Crucix | Railway | ~$2–3/month |
| Aegis Backend | Railway | ~$2–3/month |
| Supabase | Supabase.com | Free tier |
| Neo4j | Neo4j AuraDB | Free tier (50k nodes / 175k edges) |
| Android App | Your physical phone | Free (sideloaded via ADB) |
| **Total** | | **~$4–6/month** |

---

## Part 2: Setting Up Your Development Environment

### Step 1: Android Studio
1. Download from `developer.android.com/studio`
2. On first launch, follow the setup wizard to install Android SDK
3. Create a virtual device: Tools → Device Manager → Create Device → Pixel 6 → API 33
4. This emulator is your development phone — you don't need to plug in your real phone until Phase 12

### Step 2: Node.js
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
# restart terminal, then:
nvm install 22 && nvm use 22
node --version  # should say v22.x.x
```

### Step 3: Neo4j AuraDB Account
1. Go to `console.neo4j.io`
2. Sign up (free)
3. Click "New Instance" → "AuraDB Free"
4. Wait ~2 minutes for provisioning
5. Download the credentials file when prompted — this contains your bolt URI, username, and password. **This is the only time you see the password.** Store it in your password manager immediately.
6. Your Neo4j Browser URL will be something like `https://browser.neo4j.io/?connectURL=neo4j+s://xxxxxxxx.databases.neo4j.io`

### Step 4: Git Setup
```bash
git --version           # installs if missing on macOS
git config --global user.name "Your Name"
git config --global user.email "you@email.com"
```

### Step 5: Your AI Coding Agent
Install Claude Code (runs in your terminal, reads/writes files directly):
```bash
npm install -g @anthropic-ai/claude-code
```

Alternative: Cursor (download at cursor.sh) — an AI-powered VS Code that works very well for this kind of project.

---

## Part 3: How to Work With Your AI Coding Agent

### The Right Way to Start Every Session

Before any code is written, say this to the agent:

> *"Read PLAN.md, ARCHITECTURE.md, and AI_RULES.md in this repo. Tell me which phase we are on, what the next unchecked task is, and confirm that you understand the Neo4j Graph RAG architecture before we start."*

This anchors the agent in the project context. Without this, it will drift and make decisions inconsistent with your architecture.

### The Right Way to Give Instructions

❌ **Too vague:** "Build the article generator"

✅ **Correct:** "Create `services/articleGenerator.js`. It should orchestrate the full pipeline from ARCHITECTURE.md Section 10: fetch sweep data via `crucixClient`, extract key entities, call `graphService.getGraphContext()`, call `geminiService.generateArticle()` with both sweep data and graph context, save to Supabase via `db/queries/articles.js`, then call `graphService.storeEdges()` with the returned edges. Follow AI_RULES.md — no raw Cypher outside graphService.js, close all Neo4j sessions in finally blocks."

The more specific you are, the less the agent hallucinates.

### The Right Way to Ask for a Plan First

Before every new phase, say:

> *"Before writing any code for Phase 3, show me your plan: which files you will create, what each function will do, and in what order. Do not write any code until I approve the plan."*

This prevents 300 lines of code going in the wrong direction.

### The Right Way to Handle Errors

1. Copy the FULL error message (not a summary — the full stack trace)
2. Paste it: *"This error appeared when I ran X. Here is the full error: [paste]. Explain what caused it, then fix it."*
3. For Neo4j errors specifically: first run the failing Cypher query manually in Neo4j Browser to see if the issue is in the query itself or in the JavaScript driver code

---

## Part 4: The Vercel Question

Crucix CANNOT run on Vercel. Vercel is serverless — it terminates functions after a few seconds and cannot run persistent background processes. Crucix needs a persistent Express server with a 15-minute `setInterval` sweep cycle. Use Railway for both Crucix and the Aegis backend. Railway runs your code in a persistent container, exactly like your own server but managed.

---

## Part 5: Understanding the open-notebooklm Situation

The open-notebooklm project you referenced uses Bark and MeloTTS for voices, which require GPU inference (~$0.50–2/hour). Aegis instead uses:
- Gemini to write the two-person dialogue script
- Google Cloud TTS for the voices (WaveNet quality, pay-per-character, nearly free at personal use scale)
- ffmpeg to concatenate the audio files

The result is equivalent quality without GPU costs. If you later want to experiment with Bark voices, you could call the Hugging Face Spaces hosted version — but Google TTS is the right call for v1.

---

## Part 6: Understanding Neo4j for a Beginner

### What is a Graph Database?

A regular database stores rows in tables. A graph database stores **nodes** (things) and **edges** (relationships between things). 

In Neo4j:
- `(Russia)` is a node
- `(European gas prices)` is a node
- `(Russia)-[:RELATES {type: "escalated"}]->(Ukraine conflict)` is an edge

The power is in traversal: "show me everything connected to Russia within 2 steps" is a single query that would require multiple joins in a regular database.

### Cypher: Neo4j's Query Language

Cypher is Neo4j's query language. It looks like ASCII art of the graph itself:

```cypher
-- Find all entities connected to Russia (1 hop)
MATCH (n:Entity {name: "Russia"})-[r]->(m)
RETURN n, r, m

-- Find causal chains (2 hops)
MATCH (n:Entity {name: "Oil prices"})-[r*1..2]-(m)
RETURN n, r, m LIMIT 20

-- Create a relationship
MERGE (a:Entity {name: "Russia"})
MERGE (b:Entity {name: "Ukraine conflict"})
MERGE (a)-[:RELATES {type: "escalated"}]->(b)
```

You don't need to become a Cypher expert. The agent will write all the queries. But understanding the basic syntax helps you debug when something goes wrong.

### Neo4j Browser

Neo4j Browser is a web interface where you can run Cypher queries and see the results as an interactive graph. You'll use it to:
- Verify that edges are being written correctly after article generation
- Explore the accumulated knowledge graph to see patterns
- Debug issues with entity normalisation (e.g. noticing "Russia" and "Russian Federation" are appearing as separate nodes)

Access it at: `https://browser.neo4j.io/?connectURL=YOUR_BOLT_URI`

### Entity Normalisation

The biggest practical challenge with the graph is that Gemini will sometimes call the same entity different names: "Russia", "the Kremlin", "Moscow", "Russian government". These will appear as separate nodes unless you normalise them.

`graphService.js` includes a `normaliseEntities()` function with a lookup map. When you notice duplicates in Neo4j Browser, add new entries to the map. This is an ongoing maintenance task, not a one-time fix.

---

## Part 7: Day-by-Day Suggested Schedule

Working ~2 hours per day:

**Week 1:** Phase 0 (environment + accounts) + Phase 1 (Crucix deployed)
**Week 2:** Phase 2 (backend core with Neo4j connected)
**Week 3:** Phase 3 (article generation + graph RAG write path)
**Week 4:** Phase 4 (conversations with graph RAG) + Phase 5 (notifications backend)
**Week 5:** Phase 6 (Android skeleton + auth)
**Week 6:** Phase 7 (dashboard + article screen)
**Week 7:** Phase 8 (podcast) + Phase 9 (voice)
**Week 8:** Phase 10 (recommendations) + Phase 11 (FCM notifications on Android)
**Week 9–10:** Phase 12 (polish, dark theme, release APK)
**After Week 10:** Phase 13 (graph visualisation, when the graph has real data)

---

## Part 8: Cost Summary

| Service | Free Tier | Personal Use Cost |
|---|---|---|
| Railway (2 services) | $5 credit/month | $0–5/month |
| Supabase | 500MB DB, 1GB storage | Free |
| Neo4j AuraDB | 50k nodes, 175k edges | Free (years of use) |
| Gemini API | 1M tokens/day | Free |
| Google Cloud TTS | 1M chars/month (WaveNet) | Free–$5/month |
| Firebase FCM | Unlimited | Free |
| **Total** | | **$0–10/month** |

---

## Part 9: Troubleshooting Reference

### Android blank screen
Open Android Studio → Logcat, filter by `com.aegis.app`, look for `E` level (red) errors.

### Backend 500 error
Check Railway logs for the backend service. The full stack trace will be there. Find the line number.

### Supabase 401/403
Either JWT is expired (app needs to refresh it) or RLS is blocking the query. Temporarily disable RLS on the affected table to confirm, then fix the policy.

### Neo4j connection error on backend startup
Check that `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` are set correctly in Railway environment variables. The URI format must be `neo4j+s://xxxxxxxx.databases.neo4j.io` (with the `+s` for SSL).

### Neo4j duplicate nodes (e.g. "Russia" and "Russian Federation" as separate nodes)
The `normaliseEntities()` function in `graphService.js` is missing an entry. Add the variant to the lookup map, then run a Cypher cleanup query in Neo4j Browser:
```cypher
MATCH (a:Entity {name: "Russian Federation"})
MATCH (b:Entity {name: "Russia"})
// Merge all relationships from a onto b, then delete a
CALL apoc.refactor.mergeNodes([a, b]) YIELD node RETURN node
```
Note: APOC is available on AuraDB free tier.

### Crucix not sweeping
Check Railway logs for the Crucix service. Look for sweep start/end log lines. If missing, check that `server.mjs` started without errors and that the `setInterval` is actually running.

### Podcast generation fails
Most likely `ffmpeg` is not installed on the Railway container. Check `nixpacks.toml` exists in `aegis-backend/` with the ffmpeg package listed. Redeploy after adding it.

### FCM notifications not arriving on device
Check: (1) `google-services.json` package name matches `AndroidManifest.xml` package name exactly, (2) FCM token was registered via `POST /notifications/register-token`, (3) notification channel ID in FCM payload matches channel created in `AegisApplication.kt`.

---

## Part 10: What Success Looks Like

You will know the app is working well when:

1. You open it in the morning and there are 3–5 articles you actually want to read
2. One of them connects a market event to a geopolitical development in a way you hadn't considered
3. You ask "why is oil expensive right now?" and get a traced causal chain from the knowledge graph — "this connects to the sanctions event two weeks ago, which..."
4. Two weeks in, the recommendations noticeably improve — fewer articles about modules you never read
5. You generate a podcast on the drive to work and both "voices" reference historical context the graph has accumulated
6. Neo4j Browser shows a rich, interconnected web of world events you built without manually curating a single node

That is the benchmark. When all six are true, the app is working.

---

## Appendix: Key Documentation Links

| Service | Documentation |
|---|---|
| Supabase JS SDK | docs.supabase.com/reference/javascript |
| Neo4j Driver (Node.js) | neo4j.com/docs/javascript-manual/current |
| Cypher Query Language | neo4j.com/docs/cypher-manual/current |
| Neo4j AuraDB | neo4j.com/docs/aura/auradb |
| Gemini API | ai.google.dev/api/generate-content |
| Google Cloud TTS | cloud.google.com/text-to-speech/docs |
| Firebase FCM Android | firebase.google.com/docs/cloud-messaging/android/client |
| Jetpack Compose | developer.android.com/jetpack/compose |
| Hilt | developer.android.com/training/dependency-injection/hilt-android |
| Retrofit | square.github.io/retrofit |
| Room | developer.android.com/training/data-storage/room |
| Railway | docs.railway.app |
| Express.js | expressjs.com |

---

*End of Beginner's Guide*
