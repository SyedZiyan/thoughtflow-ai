# ThoughtFlow AI — Autonomous AI Wisdom, Animated YouTube Shorts & Android App

ThoughtFlow AI is a production creator automation platform that generates original motivational wisdom, transforms thoughts into high-impact animated vertical videos (9:16 1080x1920 MP4), and publishes them automatically to YouTube Shorts.

It includes a native Android mobile application with an in-app 9:16 Shorts video player, local offline cache, and an In-App Over-The-Air (OTA) Auto-Updater.

---

## 🏛️ Architecture

```
thoughtflow-ai/
├── apps/
│   ├── android/        # Native Android App (Kotlin, Jetpack Compose, Material 3, In-App Shorts Player, OTA Updater)
│   └── web/            # Creator Web Dashboard (React 18, Vite, TypeScript, Tailwind CSS, TanStack Query)
├── backend/
│   ├── app/
│   │   ├── api/v1/     # REST Endpoints (Dashboard, Thoughts, Videos, YouTube Shorts, Settings, OTA Distribution)
│   │   ├── core/       # Pydantic v2 settings, Fernet token encryption & JWT auth
│   │   ├── db/         # SQLAlchemy database engine and session
│   │   ├── models/     # User, Thought, Video, Job, Settings, Integration, ActivityLog
│   │   ├── schemas/    # Typed Pydantic request/response schemas
│   │   ├── services/   # AI Provider, VideoRenderer (Pillow + FFmpeg), YouTube Data API v3, AudioService
│   │   └── prompts/    # Structured wisdom prompts & theme catalogs
│   ├── tests/          # Pytest unit & integration test suites
│   ├── requirements.txt
│   ├── Procfile
│   └── Dockerfile
├── worker/
│   ├── scheduler.py    # Standalone APScheduler daily automation worker (07:00 AM wisdom, 09:00 AM upload)
│   └── Dockerfile
├── storage/
│   └── videos/         # Rendered vertical 9:16 MP4 assets
├── docker-compose.yml  # Local multi-container development orchestration
├── render.yaml         # 24/7 Cloud deployment blueprint (Render / Railway / Fly.io)
├── publish_update.bat  # 1-Click Over-The-Air (OTA) APK release publisher
└── publish_update.ps1
```

---

## 🚀 Quickstart & Local Development

### 1. Environment Configuration
Copy the configuration template:
```powershell
cp .env.example .env
```
*(By default, `AI_PROVIDER=mock` and `YOUTUBE_UPLOAD_ENABLED=False` are configured, enabling 100% offline local testing without any external API keys).*

### 2. Backend Setup
```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --port 8000
```
- API Swagger Documentation: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`
- Mobile APK Download Landing Page: `http://localhost:8000/download`

### 3. Web Dashboard Setup
Open a new terminal:
```powershell
cd apps/web
npm install
npm run dev
```
Open `http://localhost:5173` to access the ThoughtFlow AI web dashboard.

### 4. Native Android Application
Open `apps/android` in **Android Studio** or compile directly via command line:
```powershell
cd apps/android
.\gradlew.bat assembleDebug
```
Deploy to a connected phone or emulator:
```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 In-App Over-The-Air (OTA) Updates

ThoughtFlow AI includes a seamless OTA update system:
- When a new APK is compiled, any installed phone will automatically display an **"Update Available"** banner.
- Tapping **"Update Now"** downloads and installs the new build in-place on the phone without needing USB cables or computer connection.

To publish an update:
```powershell
.\publish_update.bat
```

---

## ☁️ 24/7 Cloud Deployment

The backend is fully configured for zero-cost 24/7 hosting on Render or Railway using [`render.yaml`](./render.yaml) and [`backend/Procfile`](./backend/Procfile):
1. Push your repository to GitHub.
2. Link the repository to [Render](https://render.com) or [Railway](https://railway.app).
3. In the mobile app's **Profile & Settings**, tap the **"Cloud 24/7 Server"** chip and save.

Your scheduled morning Shorts will publish autonomously 365 days a year even while your laptop is turned off!

---

## 🧪 Running Automated Tests

```powershell
cd backend
.\.venv\Scripts\pytest.exe tests -v
```
