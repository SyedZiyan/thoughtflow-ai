# ThoughtFlow AI — AI-Powered Daily Thought Generation, Animated YouTube Shorts Automation & Mobile App

ThoughtFlow AI is a production-minded creator automation platform that generates original motivational wisdom every day, transforms thoughts into high-impact animated vertical videos (9:16 1080x1920 MP4), and publishes them automatically to YouTube Shorts.

---

## Architecture

```
thoughtflow-ai/
├── apps/
│   ├── web/               # React 18, Vite, TypeScript, Tailwind CSS, TanStack Query
│   └── mobile/            # React Native, Expo 51, TypeScript, Expo Router
├── backend/
│   ├── app/
│   │   ├── api/v1/        # Versioned REST endpoints (Dashboard, Thoughts, Videos, YouTube, Automation)
│   │   ├── core/          # Pydantic v2 settings, Fernet token encryption & JWT auth
│   │   ├── db/            # SQLAlchemy database engine and session
│   │   ├── models/        # User, Thought, Video, Job, Settings, Integration, ActivityLog
│   │   ├── schemas/       # Typed Pydantic request/response schemas
│   │   ├── services/      # AI generator, VideoRenderer (Pillow+FFmpeg), YouTube Data API v3
│   │   └── prompts/       # Structured system prompts and theme catalogs
│   ├── tests/             # Pytest unit & integration test suites
│   ├── requirements.txt
│   └── Dockerfile
├── worker/
│   ├── scheduler.py       # Standalone APScheduler daily automation worker
│   └── Dockerfile
├── shared/
│   └── api-contracts/     # TypeScript contracts shared across Web and Mobile
├── deployment/
│   ├── docker-compose.yml # PostgreSQL, FastAPI, Worker, and Nginx Web services
│   └── deployment.md      # Deployment guide & zero-cost INR breakdown
└── storage/
    └── videos/            # Rendered vertical 9:16 MP4 assets
```

---

## Prerequisites

- **Python**: 3.11+ (Tested on Python 3.14)
- **Node.js**: v20+ (Tested on Node.js v24.20 LTS with npm 11.19)
- **FFmpeg**: v6.0+ (Tested on FFmpeg v9.0.1 Essentials with `ffmpeg` and `ffprobe` in PATH)
- **Operating System**: Windows (PowerShell commands included), macOS, or Linux

---

## Quickstart & Local Development

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
```

Run database initialization and start the FastAPI server:
```powershell
uvicorn app.main:app --reload --port 8000
```
- API Swagger Documentation: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

### 3. Web Dashboard Setup
Open a new terminal:
```powershell
cd apps/web
npm install
npm run dev
```
Open `http://localhost:5173` to access the ThoughtFlow AI creator dashboard.

### 4. Standalone Automation Worker
Open a new terminal to start the background daily worker:
```powershell
cd worker
..\backend\.venv\Scripts\python.exe scheduler.py
```

### 5. Android Mobile Application
```powershell
cd apps/mobile
npm install
npx expo start
```
- **Android Emulator**: Press `a` in the Expo terminal.
- **Physical Device**: Install **Expo Go** from Google Play Store and scan the QR code.
- Ensure your phone and PC are connected to the same Wi-Fi network and set the server IP in the app Settings tab.

---

## Running Automated Tests

### Backend Unit & Integration Tests (12 Test Cases)
```powershell
cd backend
.\.venv\Scripts\pytest.exe tests -v
```

### Rendering a Real Test Video Locally
```powershell
cd backend
.\.venv\Scripts\python.exe tests/test_render_manual.py
```
This generates an animated 1080x1920 MP4 in `storage/videos/thought_test_sample_01.mp4`.

### Building Web Production Bundle
```powershell
cd apps/web
npm run build
```

---

## YouTube Data API v3 & Google OAuth 2.0 Setup

1. Open [Google Cloud Console](https://console.cloud.google.com).
2. Enable **YouTube Data API v3**.
3. Create an **OAuth 2.0 Client ID (Web Application)**.
4. Set Authorized Redirect URI: `http://localhost:8000/api/v1/youtube/callback`.
5. Add your credentials to `.env`:
   ```env
   GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
   GOOGLE_CLIENT_SECRET=your_client_secret
   GOOGLE_REDIRECT_URI=http://localhost:8000/api/v1/youtube/callback
   YOUTUBE_UPLOAD_ENABLED=True
   ```
6. Navigate to **YouTube Channel** in the dashboard and click **Connect via Google OAuth**.

---

## Production Deployment with Docker Compose

Launch PostgreSQL, FastAPI, the autonomous worker, and the Nginx web app:
```powershell
cd deployment
docker compose up -d --build
```
Verify running containers:
```powershell
docker compose ps
```
See `deployment/deployment.md` for zero-cost hosting configurations and monthly cost calculations in INR.
