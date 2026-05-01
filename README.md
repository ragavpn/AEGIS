# AEGIS — RAG-Powered Geopolitical Analyst

[![Build Status](https://github.com/ragavpn/AEGIS/actions/workflows/ci.yml/badge.svg)](https://github.com/ragavpn/AEGIS/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Android API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?logo=android)](https://android.com)
[![Node.js](https://img.shields.io/badge/Node.js-22-green.svg?logo=node.js)](https://nodejs.org)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg?logo=docker)](https://www.docker.com/)

Aegis is an intelligent, voice-enabled Android client and backend system that provides real-time geopolitical intelligence and financial signals using Retrieval-Augmented Generation (RAG).

## 🏗 Architecture

Aegis uses a modern, multi-tier architecture to securely process complex LLM workloads:

```text
📱 Android App (Jetpack Compose)
       │
       ▼ (REST API)
🌐 Node.js Backend (Express)
       │
       ├── 💾 Supabase (PostgreSQL + pgvector, Auth, Storage)
       ├── 🕸 Neo4j AuraDB (Knowledge Graph)
       ├── 🔔 Firebase Cloud Messaging (Push Notifications)
       ├── 🎙 ElevenLabs (Studio Podcast API)
       │
       ▼
🤖 Crucix LLM Gateway (Rate limiting, logging, token management)
       │
       ▼
🧠 Google Gemini API (LLM inference)
```

## 📂 Repository Structure

This monorepo contains the full stack:

- `/aegis-android/` — Native Android client built with Jetpack Compose & Hilt
- `/aegis-backend/` — Node.js Express server orchestrating data, vector search, and graph querying
- `/aegis-crucix/` — LLM gateway proxy (crucix) that securely bridges the backend to the Gemini API

> **Note on Deployment:** The previously separate repositories `aegis-backend`, `aegis-crucix`, and `aegis-android` are now deprecated. The AEGIS project is entirely maintained, built, and deployed as a unified monorepo via this repository.
> The CI/CD pipeline defined in `.github/workflows/ci.yml` handles building and deploying the services.

---

## 🚀 Quick Start (Docker)

To run the backend and crucix locally for development:

1. Clone the repository:
   ```bash
   git clone https://github.com/ragavpn/AEGIS.git
   cd AEGIS
   ```

2. Copy `.env.example` to `.env` in both `aegis-backend/` and `aegis-crucix/` and fill in your credentials.

3. Start the services:
   ```bash
   docker compose up --build
   ```
   The backend will be available at `http://localhost:3000` and Crucix at `http://localhost:3117`.

---

## 🛠 Complete Service Setup

### 1. Supabase
- Create a new Supabase project.
- Execute the SQL migrations located in `aegis-backend/db/schema.sql` (if available) or rely on auto-migration.
- Create a storage bucket named `podcasts` and set it to public.
- Copy your `Project URL` and `Service Role Key` into the backend `.env`.

### 2. Neo4j AuraDB
- Create a free instance on Neo4j AuraDB.
- Save the generated password.
- Copy the `neo4j+s://` Bolt URI, username (`neo4j`), and password to the backend `.env`.

### 3. ElevenLabs
- Get an API key from ElevenLabs.
- Copy it to `ELEVENLABS_API_KEY` in the backend `.env`.
- Note: The system uses the Studio Podcast API with specific voice IDs configured in `.env`.

### 4. Firebase (Push Notifications)
- Create a Firebase project.
- Register an Android app with the package name `com.aegis.app`.
- Download `google-services.json` and place it in `aegis-android/app/`.
- Generate a Service Account JSON key from Firebase Settings and base64-encode it into the `FIREBASE_SERVICE_ACCOUNT_BASE64` backend env var.

### 5. Android Build
1. Open `aegis-android/` in Android Studio.
2. Let Gradle sync.
3. To build a release APK, create a `keystore.properties` file in `aegis-android/` with your signing credentials and run `./gradlew assembleRelease`.

---

## 🌍 Environment Variables Reference

### Backend (`aegis-backend/.env`)
| Variable | Description |
|---|---|
| `PORT` | Server port (default 3000) |
| `SUPABASE_URL` | Your Supabase project URL |
| `SUPABASE_SERVICE_KEY` | Your Supabase Service Role Key |
| `NEO4J_URI` | Neo4j AuraDB connection URI |
| `NEO4J_USERNAME` | Neo4j username (default: neo4j) |
| `NEO4J_PASSWORD` | Neo4j instance password |
| `CRUCIX_BASE_URL` | URL where your Crucix instance is deployed |
| `CRUCIX_SECRET` | Secret key bridging Backend → Crucix |
| `FIREBASE_SERVICE_ACCOUNT_BASE64` | Base64 encoded Firebase service account JSON |
| `ELEVENLABS_API_KEY` | Your ElevenLabs API key |
| `ELEVENLABS_HOST_VOICE_ID` | ElevenLabs voice ID for Host |
| `ELEVENLABS_GUEST_VOICE_ID` | ElevenLabs voice ID for Guest |

### Crucix (`aegis-crucix/.env`)
| Variable | Description |
|---|---|
| `PORT` | Server port (default 3117) |
| `GEMINI_API_KEY` | Google Gemini API key |
| `CRUCIX_SECRET` | Secret key (must match backend) |

---

## ⚙️ CI/CD (GitHub Actions)

This repository includes a comprehensive CI/CD pipeline (`.github/workflows/ci.yml`) that runs on every push and PR to `main`:
1. **Validation & Android Build:** Runs unit tests, validates Supabase migrations, and builds an Android Debug APK.
2. **Docker Build & Deploy (main only):** Builds Docker images for `aegis-backend` and `aegis-crucix`, pushes to GHCR, and deploys them to Railway. Requires `DEPLOY=true` repository variable.
3. **Database Migrations (main only):** Runs `supabase db push` to keep the database schema in sync.
4. **Android Release Build:** Builds a signed Android release APK when appropriate Keystore secrets are provided.

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
