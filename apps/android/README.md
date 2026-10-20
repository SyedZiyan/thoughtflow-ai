# ThoughtFlow AI — Native Android Application (Kotlin & Jetpack Compose)

A modern, native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Retrofit**, crafted directly to match the design aesthetics provided in your reference screenshots.

---

## 🎨 Visual Design & Architectural Mapping

The user interface was reverse-engineered from your 3 reference design screenshots:

### 1. Signature Elevated Floating Dock & Cyan Hexagon FAB (Screenshots 1 & 2)
- **Floating Dock (`FloatingBottomDock.kt`)**: Floats above the screen with 32dp corner radius, soft elevation shadows (`#1A0F172A`), and clean navigation tabs (*Explore*, *Library*, *Notifications*, *Profile*).
- **Cyan Hexagon Action Button**: Custom mathematical polygon clipping (`HexagonShape.kt`) in vibrant cyan (`#06B6D4`) with elevation.
- **Animated Quick-Action Pill**: When the center hexagon is tapped, an animated dark purple pill (`#2D2545`) springs up right above the button with subtle bounce, offering one-tap access to:
  - 💡 **AI Thought Generator**
  - 🎬 **Photographic 9:16 Video Studio**
  - ▶️ **YouTube Shorts Studio**

### 2. Emerald Topographical Hero Card (Screenshot 3)
- **Topographical Contours (`HeroSmartCard.kt`)**: Smooth wavy contour curves drawn dynamically with Jetpack Compose `Canvas` and cubic Bézier paths over an emerald gradient (`#10B981` to `#059669`).
- **Schedule Countdown**: Real-time display of the next automated Shorts publication time (e.g. `Next: Tomorrow at 09:00 AM`).
- **Quick Action Pill**: A white pill button with emerald typography and arrow (`Run Pipeline Now >>>`) that immediately triggers quote curation, voiceover generation, and 9:16 video compilation.

### 3. Tactile AI Suggestion Cards (Screenshot 3)
- **Modern Item Cards (`AiSuggestionCard.kt`)**: High-contrast white cards on a soft background canvas (`#F4F6F9`) featuring emoji theme avatars, category pill tags (`Discipline`, `Motivation`, `Resilience`), and micro-action buttons.

---

## 📱 Screens Included

| Screen | File | Description |
|---|---|---|
| **Dashboard** | `ui/screens/DashboardScreen.kt` | Top bar with channel selector & alerts, Hero Smart Card, AI Suggestion feed. |
| **Thought Generator** | `ui/screens/GeneratorScreen.kt` | Theme & tone chips, prompt text box, and instant thought generation. |
| **Video Studio** | `ui/screens/StudioScreen.kt` | Photographic theme picker (*Cozy Balcony*, *Mountain Summit*, etc.), duration selector, audio toggles, and live compile. |
| **Content Library** | `ui/screens/LibraryScreen.kt` | Grid of rendered 9:16 video shorts with duration badges and playback preview. |
| **YouTube Studio** | `ui/screens/YouTubeScreen.kt` | YouTube channel subscriber count, publication status, and 1-tap Shorts upload. |
| **Settings & Network** | `ui/screens/SettingsScreen.kt` | Backend host IP config (`http://192.168.0.101:8000` / `http://10.0.2.2:8000`), connection testing, and automation preferences. |

---

## 🔌 Backend Connectivity

The Android app automatically communicates with the ThoughtFlow FastAPI backend:
- **Physical Phone (Wi-Fi)**: Automatically connects to `http://192.168.0.101:8000/api/v1/`
- **Android Emulator**: Automatically routes to `http://10.0.2.2:8000/api/v1/`
- **Custom IP**: Change the server endpoint anytime in the **Settings** tab.

---

## 🚀 How to Run in Android Studio

1. **Open Android Studio**.
2. Select **Open** and choose the folder:
   ```
   c:\Users\syedz\OneDrive\Desktop\thoughtflow-ai\apps\android
   ```
3. Android Studio will automatically sync the Gradle files using the bundled JDK 21.
4. Click the green **Run (▶)** button to launch the app on your Android Emulator or connected physical phone!
