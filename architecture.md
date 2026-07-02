# MagazineForge Architecture & Component Map

## Overview
MagazineForge is an AI-powered publishing tool that generates print-ready, professional-grade PDF magazines using Google's Gemini LLM and LuaLaTeX.

## Component Map

### 1. Android Frontend (`android-app/`)
**Stack:** Kotlin, Jetpack Compose, Retrofit2, Firebase BOM, Jetpack Security
**Design System:** Luxe Editorial Noir (PitchBlack, EditorialGold, Space Grotesk, Inter)

**Core UI Components:**
- **`app/src/main/java/com/magazineforge/app/MainActivity.kt`**: The root entry point. Handles navigation state and the new **Floating Progress Indicator** (planned).
- **`ui/theme/`**: Contains `Theme.kt`, `Color.kt`, and `Type.kt` establishing the Luxe Editorial Noir design language and Google Fonts.
- **`ui/OnboardingScreen.kt`**: "The Gateway" - Securely verifies and stores the user's Gemini API Key.
- **`ui/TemplateGalleryScreen.kt`**: The gallery where users select their magazine layout. **Feature:** Displays dynamic template preview images (planned).
- **`ui/EditorScreen.kt`**: "The Studio" - The prompt input interface where users define their magazine topic (planned).

**Infrastructure Components:**
- **`utils/SecureStorage.kt`**: Uses `EncryptedSharedPreferences` to securely persist the Gemini API key.
- **`network/ApiService.kt`**: Defines Retrofit routes (`/verify-key`, `/job`, `/job/{id}/status`).
- **Firebase:** Integrated via `google-services.json` in the `app/` directory. Primed for Cloud Storage.

### 2. Python Backend (`backend/`)
**Stack:** FastAPI, Uvicorn, LuaLaTeX, Python 3
**Current State:**
- Deployed to Hugging Face Spaces using Docker (`Dockerfile`).
- **`main.py`**: The core API router exposing:
  - `POST /verify-key`: Validates the Gemini key.
  - `POST /job`: Asynchronous job creation.
  - `GET /job/{id}/status`: Polling endpoint.
- **`gemini_service.py`**: Integrates with the Gemini API to act as the "Art Director", generating structured content.
- **`templates/`**: Contains the raw `.tex` files used by LuaLaTeX. Currently contains `cover_template_a.tex`.
- **Testing (`test_e2e.py`, `test_api.py`)**: Substantial test infrastructure is present.

### 3. CI/CD Pipeline (`.github/workflows/`)
**Stack:** GitHub Actions
- **`android-ci.yml`**: Compiles a signed Release APK using the local `release.keystore` and uploads it as a workflow artifact on push to `main`.

## Development Constraints & Rules
- **READ THIS FILE** before introducing new dependencies. The stack is strictly pinned (Kotlin 1.9.20, AGP 8.2.0, Gradle 8.4) to maintain compatibility with GitHub Actions.
- **Android UI Rules:** Never use default Material styles. All screens must be derived from the Luxe Editorial Noir design tokens defined in `Theme.kt`.
