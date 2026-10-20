# ThoughtFlow AI — Production Deployment Guide & Cost Analysis

## 1. Architecture Overview

ThoughtFlow AI is architected with complete separation of concerns:

- **Web Frontend**: React, Vite, Tailwind CSS static single-page application.
- **Android Mobile Application**: React Native and Expo app communicating via REST to FastAPI.
- **Backend API**: FastAPI with SQLAlchemy ORM, Pydantic schemas, and JWT/Fernet security.
- **Media Engine**: Pillow typography and background generation combined with FFmpeg hardware/software H.264 video encoding.
- **Autonomous Worker**: Standalone Python APScheduler worker ensuring scheduled daily video generation independent of server idle states.
- **Database**: PostgreSQL (production) or SQLite (zero-setup local development).

---

## 2. Near Zero-Cost Production Strategy

| Layer | Recommended Free / Low-Cost Provider | Cost per Month | Notes |
|---|---|---|---|
| **Web Frontend** | Cloudflare Pages / Vercel | ₹0 (Free) | Global edge CDN, automated Git deployments |
| **Backend & Worker** | Oracle Cloud Free Tier (Arm 4 OCPU / 24GB) or Hetzner CX22 VPS | ₹0 to ₹380 ($4.50) | Dedicated CPU & memory for FFmpeg video rendering |
| **Database** | Supabase / Neon PostgreSQL Free Tier | ₹0 (Free) | 500MB free PostgreSQL storage with connection pooling |
| **AI Thought Engine** | Google Gemini 1.5 Flash API (Google AI Studio) | ₹0 (Free) | 15 requests/minute free; 1 daily thought requires 1 request/day |
| **YouTube Uploads** | YouTube Data API v3 (Google Cloud Console) | ₹0 (Free) | 10,000 units daily quota; 1 video upload = 1,600 units |
| **Media Storage** | Local Docker volume / Cloudflare R2 | ₹0 (Free) | Cloudflare R2 includes 10GB free permanent storage |

### Total Estimated Monthly Cost
- **Pure Free Tier**: **₹0 / month** (0 INR)
- **Dedicated VPS Tier (optional for 24/7 rendering)**: **~₹350 – ₹450 / month**

---

## 3. Production Deployment with Docker Compose

### Prerequisites
1. Docker Engine 24+ and Docker Compose v2.
2. Domain name with DNS pointed to your server IP.

### Steps
1. Clone the repository on your server:
   ```bash
   git clone https://github.com/your-org/thoughtflow-ai.git
   cd thoughtflow-ai
   ```

2. Configure production environment:
   ```bash
   cp .env.example .env
   # Edit .env with your production secrets
   ```

3. Launch all services:
   ```bash
   cd deployment
   docker compose up -d --build
   ```

4. Verify service health:
   ```bash
   docker compose ps
   curl http://localhost:8000/health
   ```

---

## 4. Google OAuth 2.0 & YouTube API Configuration

1. Visit [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project named `ThoughtFlow AI`.
3. Enable **YouTube Data API v3** under **APIs & Services > Library**.
4. Go to **OAuth consent screen**:
   - User Type: External.
   - App Name: `ThoughtFlow AI`.
   - Scopes: Add `../auth/youtube.upload` and `../auth/youtube.readonly`.
   - Test Users: Add the Gmail address of your YouTube channel.
5. Go to **Credentials > Create Credentials > OAuth client ID**:
   - Application Type: Web application.
   - Authorized redirect URIs: `http://localhost:8000/api/v1/youtube/callback` (or your domain `https://api.yourdomain.com/api/v1/youtube/callback`).
6. Copy `Client ID` and `Client Secret` into `.env`.
