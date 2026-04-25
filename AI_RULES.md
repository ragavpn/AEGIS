# AI_RULES.md — Engineering Rules & Standards
## Aegis Project — Non-Negotiables for the AI Coding Agent

**Read this before touching any file. These rules are not suggestions.**

---

## 1. Language & Framework Rules

### Android App
- **Language:** Kotlin ONLY. No Java. No Kotlin/Java mixed files.
- **UI:** Jetpack Compose ONLY. No XML layouts (`drawable/` XML for vector shapes is fine). No View-based UI.
- **Architecture:** MVVM + Repository pattern. ViewModel → Repository → DataSource. No business logic in Composables.
- **DI:** Hilt ONLY. No manual dependency injection. No Koin. Every ViewModel, Repository, and Service must be injected.
- **Navigation:** Single-Activity architecture with Jetpack Navigation Compose. No multiple Activities except the main one.
- **Async:** Kotlin Coroutines + Flow. No RxJava. No raw threads. No `AsyncTask` (deprecated).
- **HTTP:** Retrofit + OkHttp. No Volley. No raw HttpURLConnection.
- **Local DB:** Room. No raw SQLite. No SQLDelight.
- **Build system:** Gradle with Kotlin DSL (`.gradle.kts` files ONLY). No Groovy `.gradle` files.
- **Min SDK:** 26 (Android 8.0). Target SDK: latest stable.
- **IDE:** Android Studio. Project must open and build cleanly without errors.

### Backend
- **Language:** JavaScript (ES Modules). No TypeScript in the backend — keep the same style as Crucix.
- **Runtime:** Node.js 22+
- **Framework:** Express.js
- **Module system:** Pick CommonJS OR ESM and be consistent throughout the entire backend. Do not mix them.
- **No ORMs.** Use the Supabase JS SDK for Postgres queries. Raw SQL only in migration files.
- **Neo4j:** Use the official `neo4j-driver` npm package. All Neo4j access goes through `db/neo4jClient.js`. Never import the driver directly in a route or service — always use the singleton client.

---

## 2. Secrets Management — NON-NEGOTIABLE

**A leaked API key can cause massive billing charges. These rules are absolute and have no exceptions.**

### Android
- `secrets.properties` (gitignored) holds `AEGIS_API_BASE_URL` and `SUPABASE_ANON_KEY`
- Read via `gradle-secrets-plugin` into `BuildConfig` fields at compile time
- JWT tokens stored in `EncryptedSharedPreferences` backed by Android Keystore. NEVER plain `SharedPreferences`.
- NEVER put a secret in `strings.xml`, `res/values/`, or any other resource file
- NEVER hardcode a URL, key, or credential as a string literal anywhere in Kotlin source

### Backend
- ALL secrets in `.env` (gitignored). The `.env.example` lists every required variable with empty values and a comment.
- Use `process.env.VARIABLE_NAME` only. Validate all required variables exist at server startup — throw and refuse to start if any are missing.
- In Railway, all secrets go in the Railway dashboard environment variables panel, never in source.

### Neo4j
- `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` live in `.env` only
- These are never passed to the Android app under any circumstance
- All Neo4j access is backend-only, proxied through your own API

### Git
- `.gitignore` must include: `.env`, `*.keystore`, `local.properties`, `secrets.properties`, `google-services.json`, `service-account.json`, `node_modules/`, `runs/`, `build/`, `.gradle/`, `.skills/`
- Run `git status` before every commit — verify no secret files are staged
- If you accidentally commit a secret: **immediately rotate the key first**, then remove from git history with `git filter-branch` or BFG Repo Cleaner

---

## 3. Code Quality Rules

### General
- **No TODO comments in committed code.** Either implement it now or add it as a task in PLAN.md.
- **No commented-out code.** Delete it. Git history exists for a reason.
- **No `console.log` in production paths.** Use the logger (`logger.info`, `logger.error`). Guard debug logs with `BuildConfig.DEBUG` (Android) or `process.env.NODE_ENV !== 'production'` (backend).
- **Every function must have a single, clear purpose.** If it does two things, split it.
- **Maximum function length: 50 lines.** Split if longer.
- **Maximum file length: 300 lines.** Split into logical modules if longer.

### Android
- Every screen has exactly one ViewModel. ViewModels never call each other.
- ViewModels expose `StateFlow` with a sealed `UiState`: `Loading`, `Success(data)`, `Error(message)`.
- No network calls in Composables. All async work happens in ViewModels via repositories.
- All Composables that take parameters must have a `@Preview` annotation.
- Error states must always be surfaced to the user. No silent failures.
- Use `collectAsStateWithLifecycle` for Flow collection in Compose. Use `LaunchedEffect` for one-shot side effects.

### Backend
- Each route file handles exactly one resource. `articles.js` handles only article routes.
- Services contain all business logic. Routes only parse inputs, call one service, and return the result.
- All database access (Supabase AND Neo4j) goes through `db/queries/*.js` or `graphService.js`. No raw client calls in routes.
- Every API endpoint returns consistent JSON: `{ data: ... }` on success, `{ error: "message" }` on failure.
- HTTP status codes must be semantically correct: 200, 201, 400, 401, 403, 404, 500.
- Every async route handler must be wrapped in try/catch. Uncaught promise rejections crash the server.

### Neo4j / Graph Service Specific
- All Cypher queries live in `services/graphService.js`. No Cypher strings anywhere else.
- Always use parameterised queries (`$param` syntax). Never string-interpolate user input into Cypher — it is a injection vulnerability.
- Always close Neo4j sessions in a `finally` block to prevent connection leaks:
  ```javascript
  const session = neo4jClient.session();
  try {
    await session.run(query, params);
  } finally {
    await session.close();
  }
  ```
- Use `MERGE` not `CREATE` when writing nodes and relationships — `CREATE` will produce duplicates if called twice for the same entity.
- Cap all read queries with `LIMIT` — never run an unbounded Cypher query against the full graph.

---

## 4. Architecture Rules

- **Strict layering: UI → ViewModel → Repository → DataSource.** No layer skips.
- **DTOs are not domain models.** Map API responses to domain models before passing to the ViewModel.
- **Domain models are not database entities.** Room entities stay in `data/local/entity/`.
- **The Crucix project is read-only except for `server.mjs`** (adding `/api/aegis/*` endpoints). Never refactor Crucix's internal logic — it is an upstream dependency you do not own.
- **Neo4j is backend-only.** The Android app never connects directly to Neo4j. All graph queries are proxied through the Aegis backend API.
- **Graph context enriches prompts; it does not replace them.** The Gemini prompt must always include current sweep data. Graph context is additive, not a substitute.

---

## 5. Testing Rules

Pragmatic testing rules for a solo developer:

- Write at least **5 unit tests** for `recommendationEngine.js` — the scoring algorithm must be verifiable
- Write at least **3 unit tests** for `graphService.js` — test edge extraction normalisation and Cypher query output shape
- Write at least **1 unit test** for every ViewModel in Android
- Write at least **1 integration test** for every backend API route
- Test locations: `aegis-backend/__tests__/` and `aegis-android/app/src/test/`
- Backend test framework: **Jest**
- Android test framework: **JUnit4 + Turbine** (for Flow testing)
- Do NOT write UI/instrumentation tests in v1. They are slow and brittle. Focus on unit and integration tests.
- For Neo4j integration tests: use a separate AuraDB free instance labelled "test" — never run tests against your production graph database.

---

## 6. skills.sh — Install These Before Starting

Install these in each repository directory before writing a single line of code. Skills are markdown files that your AI coding agent reads automatically to inform its approach.

```bash
npx skills add supabase/agent-skills      # Correct Supabase patterns, RLS, auth
npx skills add wshobson/agents            # Node.js/Express backend patterns
npx skills add obra/superpowers           # Debugging, planning, verification, TDD
npx skills add better-auth/skills         # JWT and session management patterns
```

> **Note:** Do NOT install `sleekdotdesign/agent-skills`. UI design is not sourced from a generic skills file. Build the prototype with the minimal UI rules below, then use Stitch as the source of truth only during the dedicated design phase.

Add `.skills/` to `.gitignore` in each repo.

### 6.1 — Prototype UI Before Stitch

Until the base app works as a prototype, the UI must stay minimal and functional. The goal is to prove the product flow and content model before spending time on final visual design.

- Use a simple dark Compose theme with readable contrast.
- Use standard Material 3 components where they fit.
- Include only the controls needed for the current phase's acceptance criteria.
- Prefer clear hierarchy and spacing over custom visuals.
- Do not implement Stitch-specific glass cards, gradient orbs, visual effects, animation systems, or pixel-perfect styling before Phase 12.
- Do not block Phase 6 or Phase 7 work on Stitch availability.

### 6.2 — Stitch MCP: The Final Design Source of Truth

Stitch becomes the source of truth during Phase 12, after the working prototype exists and the content/details have been condensed into the app.

**When Phase 12 starts, connect the Stitch MCP and pull the design output before replacing prototype theme or Composable styling.**

Rules for working with Stitch MCP output in Phase 12:

- **Colours:** Export the full colour token set from Stitch. Place in `ui/theme/Color.kt`. Every colour reference in Compose code must use a named token (`AegisTheme.colors.flashAccent`), never a hardcoded hex literal.
- **Typography:** Export the type scale from Stitch. Place in `ui/theme/Typography.kt`. Helvetica Neue is the typeface. The agent must not substitute Inter, Roboto, or any other font.
- **Spacing & radii:** Export spacing and corner radius tokens. Place in `ui/theme/Tokens.kt`. Use token references, never magic numbers.
- **Components:** For each component in the Stitch frame (GlassCard, GradientOrb, LivePulse, MessageBubble, ModuleTile, NotificationBadge, AudioPlayer, ArticleCard), the agent must implement it as a standalone Composable in `ui/components/` matching the Stitch spec exactly.
- **Screens:** For each screen frame in Stitch (Splash, Auth, Onboarding, Dashboard, Article, Conversation, Voice Briefing, Notifications, Podcast, Settings), implement the corresponding screen Composable matching layout, spacing, and hierarchy from the Stitch frame.
- **The agent must never invent a design decision.** If a value (colour, size, spacing) exists in the Stitch output, use it. If it does not exist, ask the developer before proceeding.
- **Animations:** Implement the gradient orb pulse, live pulse ring, typing indicator, and voice waveform animations as specified in the Stitch prompt / MCP output. Use `infiniteTransition` in Compose for looping animations.

**How to use skills in your coding sessions:**
Start every session with: *"Read PLAN.md, ARCHITECTURE.md, and AI_RULES.md before we begin. Then tell me which phase we are on and what the next unchecked task is."*

The agent will orient itself from the documents before writing any code. If it suggests something that contradicts these rules, quote the rule back at it and ask for an alternative approach.

**When starting Phase 12 specifically**, also say: *"Connect the Stitch MCP. Pull all design tokens and frame specs before replacing prototype theme or Composable styling."*

---

## 7. Data Privacy & Security Checklist

Run this before every deployment to Railway:

**API & Secrets**
- [ ] `.env` is in `.gitignore` and not committed
- [ ] `secrets.properties` is in `.gitignore` and not committed
- [ ] `google-services.json` is in `.gitignore` and not committed
- [ ] No hardcoded strings resembling API keys in any source file
- [ ] `git log --all -p | grep -iE "api_key|secret|password|neo4j"` returns nothing alarming

**Supabase**
- [ ] RLS is enabled on all tables (`ALTER TABLE x ENABLE ROW LEVEL SECURITY`)
- [ ] Each table has a policy: `USING (auth.uid() = user_id)`
- [ ] Service role key is ONLY in backend `.env`, never in the Android app
- [ ] Android app uses the anon key only

**Neo4j**
- [ ] `NEO4J_URI`, `NEO4J_USERNAME`, `NEO4J_PASSWORD` are in backend `.env` only
- [ ] Neo4j credentials are set in Railway environment variables (not in source)
- [ ] AuraDB instance is not publicly browsable without credentials (it isn't by default)
- [ ] All Cypher queries use parameterised inputs (`$param`), not string concatenation

**Network**
- [ ] All backend traffic is HTTPS (Railway provides this automatically)
- [ ] Android manifest does NOT have `android:usesCleartextTraffic="true"`
- [ ] Certificate pinning active in release builds

**Android**
- [ ] JWT stored in `EncryptedSharedPreferences`
- [ ] All debug logging gated by `BuildConfig.DEBUG`
- [ ] Proguard/R8 minification enabled in release build config
- [ ] Release APK signed with a production keystore (not the debug keystore)

**Gemini / AI**
- [ ] No personally identifiable information sent to Gemini
- [ ] Gemini API key is backend-only, never in the Android app

---

## 8. Naming Conventions

### Android (Kotlin)
- Files and classes: `PascalCase` (e.g. `ArticleViewModel.kt`)
- Functions and variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Composables: `PascalCase` with `@Composable` annotation
- Test files: `ClassNameTest.kt`

### Backend (JavaScript)
- Files: `camelCase.js` (e.g. `graphService.js`, `articleGenerator.js`)
- Functions and variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Avoid classes; prefer modules with named exported functions

### Database — Supabase (Postgres)
- Table names: `snake_case`, plural (e.g. `article_interactions`)
- Column names: `snake_case`
- Indexes: `idx_<table>_<column>`

### Database — Neo4j
- Node labels: `PascalCase` singular (e.g. `Entity`)
- Relationship types: `SCREAMING_SNAKE_CASE` (e.g. `RELATES`)
- Property names: `camelCase` (e.g. `firstSeen`, `lastSeen`, `articleId`)

---

## 9. Git Workflow

- Commit message format: `type(scope): description`
  - Examples: `feat(graph): add entity extraction to article pipeline`, `fix(auth): handle expired token refresh`, `docs(rules): add Neo4j naming conventions`
  - Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- One commit per logical change. No "WIP" or "misc fixes" commits.
- Never force-push to `main`.
- Use feature branches per PLAN.md phase: `phase/1-crucix-setup`, `phase/2-backend-core`, `phase/3-articles-graph`, etc.

---

## 10. What to Do When Stuck

1. Read the full error message. The answer is almost always in it.
2. Check whether you violated a rule in this document.
3. Check ARCHITECTURE.md to confirm code is in the right layer.
4. For Neo4j issues: open Neo4j Browser and run the Cypher query manually to see if it works in isolation.
5. If the AI agent suggests a solution that violates these rules, reject it explicitly and ask for a compliant alternative.
6. If something has been broken for more than 30 minutes, stop coding and write a clear description of: what you expected, what actually happened, and what you have already tried. Then ask for help.

---

*End of AI_RULES.md*
