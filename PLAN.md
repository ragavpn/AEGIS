# PLAN.md — Step-by-Step Development Roadmap
## Aegis: Personal World Intelligence App

**Version:** 1.1 (updated with Neo4j Graph RAG)
**Date:** April 2026

**Rule: Complete one phase entirely before starting the next. Do not start Phase 3 if Phase 2 has any unchecked items.**

---

## How to Use This Document

- **Before starting any task:** Read the task, its acceptance criteria, and the files it touches.
- **While working:** Update the execution log at the bottom of each phase.
- **After completing a task:** Check the box and record changed files in the log.
- **If stuck:** Add a note under the task describing what you tried and what happened.

---

## Phase 0: Environment Setup
**Goal:** All tools installed, all accounts created, all repos initialised. Zero code written yet.
**Estimated time:** 2–3 hours

### Tasks

- [ ] **0.1 — Install Android Studio**
  - Download Android Studio (latest stable) from developer.android.com
  - Install Android SDK (API 34+), Kotlin plugin (bundled), and create an emulator: Pixel 6, API 33
  - Acceptance: Android Studio opens and the emulator boots to a home screen

- [ ] **0.2 — Install Node.js 22+**
  - Use nvm: `curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash`
  - Then: `nvm install 22 && nvm use 22`
  - Acceptance: `node --version` returns v22.x.x

- [ ] **0.3 — Create all accounts and collect credentials**
  - [ ] Railway account at railway.app (GitHub SSO)
  - [ ] Supabase project at supabase.com — note the Project URL, anon key, and service role key
  - [ ] Firebase project at console.firebase.google.com — enable Cloud Messaging, download `google-services.json`
  - [ ] Gemini API key from aistudio.google.com (free tier)
  - [ ] Google Cloud TTS API key from console.cloud.google.com — enable "Cloud Text-to-Speech API"
  - [ ] **Neo4j AuraDB free instance** at console.neo4j.io — create a new "AuraDB Free" instance, download the credentials file (contains URI, username, password). Note: the instance takes ~2 minutes to provision.
  - Acceptance: All 6 sets of credentials saved in a password manager. None in any file.

- [ ] **0.4 — Create three Git repositories**
  - Create on GitHub: `aegis-crucix`, `aegis-backend`, `aegis-android`
  - First commit in each must be a proper `.gitignore` and nothing else
  - `.gitignore` for Node repos: `.env`, `node_modules/`, `runs/`, `.skills/`
  - `.gitignore` for Android: `local.properties`, `secrets.properties`, `*.keystore`, `google-services.json`, `.gradle/`, `build/`, `.skills/`
  - Acceptance: Three repos on GitHub, each with `.gitignore` as first commit

- [ ] **0.5 — Install AI coding agent and skills**
  - Install Claude Code: `npm install -g @anthropic-ai/claude-code`
  - In each repo directory, run:
    ```bash
    npx skills add supabase/agent-skills
    npx skills add wshobson/agents
    npx skills add obra/superpowers
    npx skills add better-auth/skills
    ```
  - Acceptance: `.skills/` folder exists in each repo with markdown files inside

> Note: Do not install generic design skills for the Android app. Build the prototype with the minimal UI rules in `AI_RULES.md`; the final visual system comes from Stitch in Phase 12.

---

## Phase 1: Crucix Fork & Deployment
**Goal:** Crucix runs on Railway, health endpoint is publicly accessible.
**Estimated time:** 2–4 hours

### Tasks

- [ ] **1.1 — Fork and configure Crucix**
  - Fork `calesthio/Crucix` to your GitHub as `aegis-crucix`
  - Clone locally, copy `.env.example` to `.env`
  - Fill in: `FRED_API_KEY`, `FIRMS_MAP_KEY`, `GEMINI_API_KEY` (as `LLM_API_KEY` with `LLM_PROVIDER=gemini`), and a strong random `AEGIS_SECRET` (generate with `openssl rand -hex 32`)
  - Run `node server.mjs` locally. Wait 60 seconds. Open `http://localhost:3117`
  - Acceptance: Dashboard loads with data in at least some tiles

- [ ] **1.2 — Add Aegis API endpoints to Crucix**
  - In `server.mjs`, add three protected endpoints after existing routes:
    - `GET /api/aegis/latest` — reads and returns `runs/latest.json`
    - `GET /api/aegis/health` — returns `{ status: "ok", lastSweep, uptime: process.uptime() }`
    - `GET /api/aegis/delta` — returns diff since previous sweep
  - All three check `req.headers['x-aegis-secret'] === process.env.AEGIS_SECRET`, return 401 if wrong
  - Acceptance: `curl -H "X-Aegis-Secret: yoursecret" http://localhost:3117/api/aegis/health` returns `{ status: "ok" }`

- [ ] **1.3 — Deploy Crucix to Railway**
  - Railway: New Project → Deploy from GitHub → `aegis-crucix`
  - Set all `.env` values as Railway environment variables
  - Start command: `node server.mjs`
  - Assign a Railway public domain
  - Acceptance: `https://your-crucix.railway.app/api/aegis/health` (with correct secret header) returns `{ status: "ok" }`

---

## Phase 2: Backend API — Core
**Goal:** Node.js API on Railway with auth, Supabase schema, Neo4j connection, and core data endpoints.
**Estimated time:** 5–7 hours

### Tasks

- [ ] **2.1 — Initialise backend project**
  - `npm init -y` in `aegis-backend/`
  - Install: `express`, `@supabase/supabase-js`, `neo4j-driver`, `dotenv`, `express-rate-limit`, `cors`, `pino`
  - Create folder structure from ARCHITECTURE.md Section 5
  - Create `server.js` with Express boilerplate, `GET /health` endpoint
  - Add startup validation: throw if any required `.env` variable is missing
  - Acceptance: `node server.js` starts cleanly, `GET /health` returns `{ status: "ok" }`

- [ ] **2.2 — Supabase schema**
  - Open Supabase SQL Editor
  - Run all `CREATE TABLE` statements from ARCHITECTURE.md Section 7
  - Run `ALTER TABLE x ENABLE ROW LEVEL SECURITY` on every table
  - Create policy on every table: `CREATE POLICY "own_data" ON x USING (auth.uid() = user_id)`
  - Acceptance: All tables visible in Supabase Table Editor, RLS icon shown as enabled on each

- [ ] **2.3 — Neo4j client setup**
  - Create `db/neo4jClient.js` as a singleton using `neo4j-driver`
  - Reads `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` from `.env`
  - Exports `{ session }` function and a `verifyConnectivity()` function
  - Call `verifyConnectivity()` on server startup — log success or throw if connection fails
  - Acceptance: Server starts and logs "Neo4j connection verified" (or equivalent)

- [ ] **2.4 — Auth middleware**
  - Create `middleware/auth.js` — extracts `Authorization: Bearer <token>`, verifies with Supabase Admin SDK
  - Returns `401 { error: "Unauthorized" }` if invalid or missing
  - Attaches `req.user` (Supabase user object) on success
  - Acceptance: Protected route returns 401 without valid token, 200 with one

- [ ] **2.5 — Data routes (Crucix passthrough)**
  - Create `services/crucixClient.js` — calls Crucix `/api/aegis/latest` and `/api/aegis/health` with the shared secret header
  - Create `routes/data.js`: `GET /data/latest`, `GET /data/health`
  - Both routes require auth middleware
  - Acceptance: `GET /data/latest` with valid JWT returns Crucix sweep data

- [ ] **2.6 — Preferences routes**
  - `db/queries/preferences.js`: `getPreferences(userId)` and `upsertPreferences(userId, data)`
  - `routes/preferences.js`: `GET /preferences`, `PUT /preferences`
  - Acceptance: Can create and retrieve user preferences

- [ ] **2.7 — Deploy backend to Railway**
  - Railway: New Project → Deploy from GitHub → `aegis-backend`
  - Set all environment variables including `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD`
  - Start command: `node server.js`
  - Acceptance: `https://your-backend.railway.app/health` returns `{ status: "ok" }`. Railway logs show both Supabase and Neo4j connection confirmations.

---

## Phase 3: Article Generation + Graph RAG Write Path
**Goal:** Backend generates AI articles from Crucix data AND stores extracted entity relationships in Neo4j.
**Estimated time:** 5–7 hours

### Tasks

- [ ] **3.1 — Gemini service**
  - Create `services/geminiService.js`
  - Implement `generateArticle(sweepData, graphContext)`:
    - Prompt: write 500–700 word analyst briefing, include historical context, implications, cross-reference ≥2 data sources, note graph connections if graphContext is provided
    - Also prompt: return up to 5 `edges` as JSON — `{ source, target, relationship, confidence }`
    - Relationship must be one of: `caused | correlated_with | escalated | preceded | triggered | de-escalated`
    - Return: `{ title, body, summary, modules, sources, edges }`
  - Implement `extractEntities(text)`: given a short text, return an array of canonical entity names (used before graph queries to identify what to look up)
  - Acceptance: `generateArticle(mockSweepData, "")` returns a valid article object with an `edges` array

- [ ] **3.2 — Graph service (write path)**
  - Create `services/graphService.js`
  - Implement `storeEdges(articleId, edges)`:
    - Normalise entity names via a lookup map (see ARCHITECTURE.md Section 6.4)
    - Use `MERGE` Cypher to write nodes and relationships — no duplicates
    - Always close session in `finally` block
  - Implement `getGraphContext(entityNames)`:
    - Run 2-hop Cypher traversal from given entity names
    - Return a formatted string for injection into Gemini prompts
    - Cap with `LIMIT 20`, order by `lastSeen DESC`
  - Unit tests: write 3 tests — one for normalisation, one for Cypher output shape, one for empty entity list
  - Acceptance: After calling `storeEdges`, the edges are visible in Neo4j Browser. `getGraphContext(["Russia"])` returns a non-empty string after edges exist.

- [x] **3.3 — Article generator service**
  - Create `services/articleGenerator.js`
  - `generate()` function orchestrates the full pipeline:
    1. `crucixClient.getLatest()` → sweep data
    2. Extract key entities from sweep summary
    3. `graphService.getGraphContext(entities)` → graph context string
    4. `geminiService.generateArticle(sweepData, graphContext)` → article + edges
    5. Save article to Supabase (`db/queries/articles.js`)
    6. `graphService.storeEdges(articleId, edges)` → write to Neo4j
  - Acceptance: Calling `generate()` creates a new row in `articles` table AND new nodes/edges appear in Neo4j Browser

- [x] **3.4 — Sweep watcher**
  - Create `services/sweepWatcher.js`
  - Poll `GET /api/aegis/health` every 15 minutes via `setInterval`
  - When `lastSweep` timestamp changes, call `articleGenerator.generate()`
  - Also evaluate notification tier from sweep delta (FLASH/PRIORITY/ROUTINE logic)
  - Acceptance: After deployment, new articles appear in the database approximately every 15 minutes

- [x] **3.5 — Articles routes**
  - `GET /articles` — paginated, optionally filtered by `?module=geopolitics`, ordered by recommendation score for the authenticated user
  - `GET /articles/:id` — full article
  - `POST /articles/generate` — manually triggers generation (for "Refresh Now" button)
  - Acceptance: Can list and retrieve articles via API with valid auth

---

## Phase 4: Conversations with Graph RAG
**Goal:** Full conversation API where every Gemini response is enriched with current sweep data AND knowledge graph context.
**Estimated time:** 4–6 hours

### Tasks

- [x] **4.1 — Conversation queries**
  - `db/queries/conversations.js`:
    - `createConversation(userId)` — creates conversation, returns ID
    - `addMessage(conversationId, role, content)` — inserts message
    - `getHistory(conversationId, limit=10)` — returns last N messages in order
    - `listConversations(userId)` — returns all conversations with title and last message preview
  - Acceptance: All four functions work correctly against Supabase

- [x] **4.2 — Conversation AI service**
  - Extend `geminiService.js` with `chat(history, newMessage, sweepContext, graphContext)`:
    - System prompt: you are Aegis, a personal intelligence analyst. Be precise, cite signals, reference historical connections when available. Speak like a briefer, not a chatbot.
    - Context order: system → graph context → current sweep summary → conversation history → new message
    - Return: assistant response string
  - Acceptance: Given mock history, a new message, and context strings, returns a coherent, contextually grounded response

- [x] **4.3 — Conversations routes**
  - `GET /conversations` — list for authenticated user
  - `POST /conversations` — create new, return `{ conversationId }`
  - `GET /conversations/:id` — full conversation with all messages
  - `POST /conversations/:id/messages`:
    1. Save user message
    2. Extract entities from user message
    3. `graphService.getGraphContext(entities)`
    4. Fetch latest Crucix sweep summary
    5. Fetch last 10 messages
    6. `geminiService.chat(...)` → response
    7. Save assistant message
    8. Return `{ message: assistantResponse }`
  - Acceptance: Full round-trip — send message, get AI response, both saved, retrievable via GET

---

## Phase 5: Notifications Backend
**Goal:** Backend can classify events by tier and send push notifications via FCM.
**Estimated time:** 2–3 hours

### Tasks

- [x] **5.1 — FCM notification service**
  - Create `services/notificationService.js`
  - `sendNotification(userId, { tier, title, body, articleId })`:
    - Fetch device token from `device_tokens` table
    - Call FCM API with correct priority (FLASH = `high`, others = `normal`)
    - Save notification to `notifications` table
  - Acceptance: Calling the function sends a test push to an Android device registered via adb

- [x] **5.2 — Device token routes**
  - `POST /notifications/register-token { token }` — upsert token for user
  - `DELETE /notifications/register-token` — remove on logout
  - Acceptance: Token is saved and retrievable from `device_tokens`

- [x] **5.3 — Notification list routes**
  - `GET /notifications` — list with unread count
  - `PATCH /notifications/:id/read` — mark as read
  - Acceptance: Notifications list and marking read both work

---

## Phase 6: Android App — Skeleton & Auth
**Goal:** Android app builds, runs on emulator, and can authenticate via Supabase.
**Estimated time:** 4–6 hours

### Tasks

- [ ] **6.1 — Create Android project**
  - Android Studio: New Project → Empty Compose Activity
  - Name: "Aegis", Package: `com.aegis.app`, Min SDK: 26
  - Set up `secrets.properties` + `gradle-secrets-plugin` → injects `AEGIS_API_BASE_URL` and `SUPABASE_ANON_KEY` into `BuildConfig`
  - Add all dependencies to `build.gradle.kts`: Hilt, Retrofit + OkHttp, Room, Coil, Navigation Compose, Coroutines, Firebase Messaging
  - Add `google-services.json` to `app/` directory (gitignored)
  - Acceptance: Project builds with zero errors. Emulator launches the app.

- [ ] **6.2 — Hilt DI setup**
  - `AegisApplication.kt` with `@HiltAndroidApp` annotation
  - `di/NetworkModule.kt` providing Retrofit instance pointed at `BuildConfig.AEGIS_API_BASE_URL`
  - Create 3 Android notification channels in `AegisApplication.onCreate()`:
    - `aegis_flash` (IMPORTANCE_HIGH, red)
    - `aegis_priority` (IMPORTANCE_DEFAULT, amber)
    - `aegis_routine` (IMPORTANCE_LOW, blue)
  - Acceptance: App launches without crashing

- [ ] **6.3 — Auth screen**
  - `AuthScreen.kt` + `AuthViewModel.kt`
  - Email + password form with sign-in and sign-up tabs
  - On success: store JWT in `EncryptedSharedPreferences`, navigate to Dashboard (or Onboarding if first launch)
  - On error: show error message inline under the form
  - Acceptance: Can sign up, log in, and see JWT persists across app restarts

- [ ] **6.4 — Navigation graph**
  - `AegisNavGraph.kt` with all destinations: Auth, Onboarding, Dashboard, Article, Conversation, Notifications, Settings
  - On app start: check for valid JWT → if present, go to Dashboard; if absent, go to Auth
  - Acceptance: Navigation works between all screens, back stack is correct

---

## Phase 7: Android App — Onboarding & Dashboard
**Goal:** Module selection onboarding and a working dashboard showing live articles.
**Estimated time:** 4–6 hours

### Tasks

- [ ] **7.1 — Onboarding screen**
  - 7 module cards with icon, name, 1-line description, checkbox
  - "Continue" disabled until ≥1 selected
  - On submit: `PUT /preferences`, mark onboarding complete in local Room DB
  - Shown only on first launch (check Room DB flag on nav graph init)
  - Acceptance: Selections saved, onboarding never shown again after first completion

- [ ] **7.2 — Dashboard screen**
  - Top section "Curated For You": horizontal `LazyRow` of `ArticleCard` composables
  - `ArticleCard` shows: title, 2-line summary preview, time-ago, module badge chips
  - Below: vertical `LazyColumn` of module data tiles (one per selected module, showing latest signal value from Crucix data)
  - Bottom ticker: scrolling `LazyRow` of latest raw headlines from `data.latest`
  - Pull-to-refresh: triggers `POST /articles/generate`, then reloads article list
  - Loading state: skeleton placeholder cards (animated shimmer), NOT a full-screen spinner
  - Error state: message + retry button
  - Acceptance: Articles load and display. Pull-to-refresh triggers generation and refreshes the list.

  - Prototype UI rule: until Phase 12, keep this screen visually minimal and functional. Use a simple dark Compose theme, readable typography, clear spacing, and standard Material controls. Do not implement Stitch-specific glass cards, gradient orbs, animation systems, or pixel-perfect visual styling yet.

- [ ] **7.3 — Article screen**
  - Full article body rendered with markdown (use Compose `BasicText` with annotated strings, or a Compose markdown library)
  - Header: title, date, module badges, time-to-read estimate
  - Action bar: thumbs up / thumbs down, bookmark, share, "Generate Podcast" button (Phase 8)
  - Like/dislike calls `POST /interactions { articleId, liked: true/false }`
  - Read-time tracking: start `System.currentTimeMillis()` on screen enter, send to `POST /interactions` with `readDurationSeconds` on exit or background
  - Scroll depth tracking: observe `LazyColumn` scroll state, send `scrollDepthPercent` with interaction
  - Acceptance: Article displays correctly. Like/dislike and read-time interactions are saved in Supabase.

---

## Phase 8: Podcast Generation
**Goal:** Tap "Generate Podcast" on an article and hear a two-person AI dialogue about it.
**Estimated time:** 3–4 hours

### Tasks

- [ ] **8.1 — Podcast backend**
  - Create `services/podcastGenerator.js`:
    1. Fetch article from Supabase
    2. Get graph context for article's entities from Neo4j
    3. Gemini generates dialogue: JSON array of `{ speaker: "Alex"|"Jordan", text: "..." }` objects, ~5 minutes of dialogue
    4. For each line, call Google Cloud TTS:
       - Alex → `en-US-Neural2-A` (male)
       - Jordan → `en-US-Neural2-F` (female)
    5. Concatenate audio files via `ffmpeg`
    6. Upload MP3 to Supabase Storage (public bucket)
    7. Save to `podcasts` table, return `{ audioUrl, durationSeconds }`
  - Create `nixpacks.toml` in `aegis-backend/` to install ffmpeg on Railway:
    ```toml
    [phases.setup]
    nixPkgs = ["ffmpeg"]
    ```
  - Create `routes/podcast.js`: `POST /podcast/generate { articleId }`
  - Acceptance: Calling the route returns a public audio URL that plays correctly in a browser

- [ ] **8.2 — Audio player in Android**
  - Create `components/AudioPlayer.kt` composable
  - States: Loading, Ready (play/pause/seek), Error
  - Uses Android `MediaPlayer` API, streams from URL
  - Appears on the article screen when a podcast has been generated or while generating (loading indicator)
  - "Generate Podcast" button becomes disabled with a progress indicator while generating
  - Acceptance: Audio plays in the app with correct controls. Progress bar updates during playback.

---

## Phase 9: Voice Conversation Mode
**Goal:** Full two-way voice conversation — speak to Aegis and hear it speak back.
**Estimated time:** 3–4 hours

### Tasks

- [ ] **9.1 — Conversation screen**
  - `ConversationScreen.kt` + `ConversationViewModel.kt`
  - `LazyColumn` of `MessageBubble` composables (user right-aligned, assistant left-aligned)
  - Text input field + send button at bottom
  - Microphone button: hold to record, release to send
  - Voice mode toggle (speaker icon): when enabled, AI responses are read aloud via TTS
  - Acceptance: Can send a text message and see the AI response

- [ ] **9.2 — Voice input**
  - Create `VoiceInputHandler.kt` using Android `SpeechRecognizer`
  - Shows live partial transcription text while recording
  - On completion, sends transcribed text as a message
  - Requests `RECORD_AUDIO` permission at runtime (with rationale if denied)
  - Acceptance: Speech is transcribed and sent as a message accurately

- [ ] **9.3 — Voice output + briefing mode**
  - Android `TextToSpeech` reads AI responses aloud when voice mode is enabled
  - Tap anywhere on the screen to interrupt speech
  - "Daily briefing" button in dashboard: opens conversation screen in voice mode and sends a predefined "Give me today's briefing" message
  - AI delivers spoken briefing; user can tap to interrupt and ask questions; after the answer, the AI resumes
  - Acceptance: Briefing mode works end-to-end with interruption handling

---

## Phase 10: Recommendation Engine
**Goal:** Articles surface in order of your interests, improving over time.
**Estimated time:** 2–3 hours

### Tasks

- [x] **10.1 — Interaction recording**
  - `POST /interactions` accepts: `{ articleId, readDurationSeconds, scrollDepthPercent, liked, bookmarked }`
  - Upserts into `article_interactions` table (one row per user/article pair, updates if exists)
  - Acceptance: Interactions are saved correctly without duplicates

- [x] **10.2 — Scoring algorithm**
  - Create `services/recommendationEngine.js`
  - `scoreArticle(userId, article, interactions)`:
    - Inputs: article object, user's last 20 interactions
    - Score formula:
      - Module match: +3 per module the user has spent >30s reading before
      - Explicit like on same module: +5
      - Explicit dislike on same module: -10
      - Read duration on same module (normalised 0–1): ×2 multiplier
      - Discovery boost: if user has never interacted with this module, add +1 (ensures new topics surface)
    - Returns: numeric score
  - `GET /articles` uses this score to sort results for authenticated user
  - Unit tests: 5 test cases covering — fresh user (no interactions), heavy preference for one module, dislike penalty, discovery boost, mixed signals
  - Acceptance: After 10+ interactions, article order shifts measurably toward preferred modules

---

## Phase 11: Notifications on Android
**Goal:** Push notifications arrive correctly, open the right content, and are manageable.
**Estimated time:** 2–3 hours

### Tasks

- [ ] **11.1 — FCM integration**
  - Create `AegisFcmService.kt` extending `FirebaseMessagingService`
  - `onNewToken(token)`: call `POST /notifications/register-token` with the new token
  - `onMessageReceived(message)`: build and display Android notification on the correct channel based on `tier` in the FCM payload
  - Tapping notification: deep-link to `ArticleScreen` with the article ID from the payload (use Android `TaskStackBuilder`)
  - Acceptance: A notification sent via the backend arrives and tapping it opens the correct article

- [ ] **11.2 — Notifications screen**
  - List of received notifications grouped by tier (FLASH first)
  - Unread count badge on the bell icon in the app's top bar
  - Tap notification → mark as read via `PATCH /notifications/:id/read` → open article
  - Empty state when no notifications exist
  - Acceptance: Unread count reflects reality. Tapping a notification marks it read and navigates correctly.

---

## Phase 12: Polish, Dark Theme & Release
**Goal:** App is stable, looks great, and is ready for daily personal use on your phone.
**Estimated time:** 5–7 hours

### Tasks

- [ ] **12.1 — Settings screen**
  - Module toggles (update preferences live)
  - Notification tier toggles per module
  - Do Not Disturb hours picker (time range)
  - TTS playback speed slider (0.5× – 2×)
  - "Clear conversation history" button with confirmation dialog
  - Log out button: clears JWT, calls `DELETE /notifications/register-token`, navigates to Auth
  - Acceptance: All settings persist and take effect immediately

- [ ] **12.2 — Implement Stitch design system**
  - **Timing:** Start this only after the base Android prototype works end-to-end with real or mock content: onboarding, dashboard, article reading, conversation, and the main backend flows. Before this point, all UI should stay minimal and functional.
  - **Design source:** All colours, typography, spacing, component styles, and screen layouts come exclusively from the Stitch MCP output. Do not invent or approximate values. If a value is not in the Stitch output, ask before guessing.
  - **How to use Stitch MCP in the coding agent:**
    - Connect the Stitch MCP server in your coding agent before starting this task
    - Use the MCP to pull the exported design tokens (colours, radii, spacing, type scales) and place them in `ui/theme/Color.kt`, `ui/theme/Typography.kt`, and a new `ui/theme/Tokens.kt`
    - Use the MCP to pull each frame's component specs and implement them as Jetpack Compose composables in `ui/components/` and `ui/screens/`
    - Do not hardcode any value that exists in the Stitch token set — always reference the token name
  - **What to implement from the Stitch frames:**
    - `Color.kt` — full colour token set from Stitch (background, surface, accent, FLASH/PRIORITY/ROUTINE tiers, text primary/secondary)
    - `Typography.kt` — Helvetica Neue type scale at all specified weights and sizes from Stitch
    - `Tokens.kt` — spacing, corner radii, blur values, elevation/shadow values, animation durations
    - Gradient blob composable: `GradientOrb.kt` — the three-layer blurred shape orb used across Splash, Dashboard, Voice mode, and Podcast. Parameterised for position, scale, and intensity per screen.
    - Glass card composable: `GlassCard.kt` — the frosted glass surface used for all cards and sheets. Takes an optional `tierGlow` parameter (FLASH/PRIORITY/ROUTINE/none) that applies the tier-coloured border and glow
    - Live pulse indicator: `LivePulse.kt` — the animated ring that indicates live data
    - All screens must match the Stitch frames pixel-accurately for layout, spacing, and visual treatment
  - **Dark theme system:** Apply the Stitch dark theme globally via `AegisTheme` wrapper in `AegisApplication`. All screens, dialogs, bottom sheets, snackbars, and system bars (status bar, navigation bar) must use the Stitch dark theme tokens — no screen should ever flash white or light grey.
  - Acceptance: Screenshot each implemented screen side-by-side with its Stitch frame. All element positions, colours, type sizes, and spacing must match. Test in dark room conditions — no eye strain from overly bright surfaces.

- [ ] **12.3 — Error handling audit**
  - Walk through every screen with the network disabled and confirm:
    - No blank screens
    - No crash
    - Every error shows a message and a retry button
  - Confirm Crucix-down scenario: dashboard shows cached articles, not an empty screen
  - Confirm Neo4j-down scenario: articles still generate (just without graph context), conversation still works

- [ ] **12.4 — Security checklist**
  - Complete the full checklist from AI_RULES.md Section 7

- [ ] **12.5 — Build and install release APK**
  - Generate a release keystore: `keytool -genkey -v -keystore aegis-release.keystore -alias aegis -keyalg RSA -keysize 2048 -validity 10000`
  - Store keystore file and passwords in your password manager. NEVER commit the `.keystore` file.
  - Build signed APK: Build → Generate Signed Bundle/APK → APK → use the keystore above
  - Install on physical device: `adb install app-release.apk`
  - Test all flows on the real device (not just the emulator) for at least 24 hours
  - Acceptance: App is stable on your phone with no crashes for 48 hours of normal use

---

## Phase 13: Knowledge Graph Visualisation in Android (v2 Feature)
**Goal:** Explore the accumulated knowledge graph from within the app.
**Estimated time:** 4–5 hours (defer until after Phase 12 is complete)

### Tasks

- [ ] **13.1 — Contextual graph view on article screen**
  - Add "Show Connections" button to article screen
  - Opens a bottom sheet containing a WebView
  - WebView renders a force-directed graph using `react-force-graph-2d` (loaded from CDN)
  - Centred on the article's main entities, showing 1–2 hops of connections
  - Maximum 30 nodes visible at once
  - Backend endpoint: `GET /graph/context?entities=Russia,Oil+prices` returns nodes and edges as JSON
  - Acceptance: Tapping "Show Connections" on any article shows a navigable graph of related entities

- [ ] **13.2 — Global graph explorer screen**
  - New screen accessible from Settings or the main nav bar
  - WebView with full graph plus controls:
    - Date range slider (filter edges by `lastSeen`)
    - Minimum confidence threshold slider (0.5 – 1.0)
    - Module filter chips
    - Search box to highlight a specific entity
  - Backend endpoint: `GET /graph/global?minConfidence=0.7&since=2025-01-01` returns filtered graph
  - Acceptance: Can filter the graph to under 50 nodes and navigate it meaningfully

---

## Dependency Map

```
Phase 0 (Setup)            — no prerequisites
Phase 1 (Crucix)           — Phase 0 complete
Phase 2 (Backend core)     — Phase 1 complete (need Crucix URL + Neo4j credentials)
Phase 3 (Articles + Graph) — Phase 2 complete (need auth, Supabase, Neo4j connected)
Phase 4 (Conversations)    — Phase 3 complete (need graph service for RAG)
Phase 5 (Notifications BE) — Phase 3 complete (need article IDs)
Phase 6 (Android skeleton) — Phase 2 complete (need backend URL)
Phase 7 (Dashboard)        — Phase 6 + Phase 3 complete
Phase 8 (Podcast)          — Phase 7 complete (need article screen UI)
Phase 9 (Voice)            — Phase 7 complete (need conversation UI)
Phase 10 (Recommendations) — Phase 7 complete (need interactions)
Phase 11 (Notifications)   — Phase 5 + Phase 6 complete
Phase 12 (Polish)          — All previous phases complete
Phase 13 (Graph viz)       — Phase 12 complete, Neo4j has ≥2 weeks of data
```

---

## Execution Log

Update this table as you work. It prevents context loss when resuming after a break.

| Date | Phase | Task | Files Changed | Notes |
|---|---|---|---|---|
| — | — | Example | `.gitignore`, `package.json` | Created initial structure |
| 2026-04-26 | 10 | 10.1, 10.2 | `aegis-backend/routes/interactions.js`, `aegis-backend/db/queries/interactions.js`, `aegis-backend/services/recommendationEngine.js`, `__tests__/services/recommendationEngine.test.js` | Implemented and verified interaction tracking and recommendation engine with unit tests passing |
| 2026-04-26 | 10 | Testing | `__tests__/routes/interactions.test.js`, `__tests__/routes/articles.test.js`, `ArticleInteractionViewModelTest.kt`, `UserDataRepositoryInterface.kt`, `UserDataRepository.kt`, `RepositoryModule.kt`, `UserDataViewModel.kt`, `GatekeeperViewModel.kt`, `build.gradle.kts` | Added integration tests for all Phase 10 backend routes (18/18 passing). Extracted `UserDataRepositoryInterface` so ViewModels can be unit tested with a fake repo (no live Supabase). ViewModel unit tests cover toggleLike, toggleLike double-flip, recordReadDuration accumulation, recordScrollDepth max-tracking, and toggleBookmark. |

---

## Estimated Total Timeline

| Phase | Estimated Hours |
|---|---|
| 0 — Setup | 2–3 |
| 1 — Crucix | 2–4 |
| 2 — Backend core + Neo4j | 5–7 |
| 3 — Articles + Graph RAG | 5–7 |
| 4 — Conversations | 4–6 |
| 5 — Notifications (backend) | 2–3 |
| 6 — Android skeleton | 4–6 |
| 7 — Dashboard | 4–6 |
| 8 — Podcast | 3–4 |
| 9 — Voice | 3–4 |
| 10 — Recommendations | 2–3 |
| 11 — Notifications (Android) | 2–3 |
| 12 — Polish + Release | 5–7 |
| 13 — Graph visualisation (v2) | 4–5 |
| **Total (v1, Phases 0–12)** | **43–63 hours** |

At ~10 hours/week: **5–7 weeks to a working daily-use app.**

---

*End of PLAN.md*
