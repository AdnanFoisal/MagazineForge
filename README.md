# 📖 MagazineForge

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![FastAPI](https://img.shields.io/badge/Backend-FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![LaTeX](https://img.shields.io/badge/Engine-LuaLaTeX-008080?style=for-the-badge&logo=latex&logoColor=white)

> **MagazineForge** (internally known as *MagBoy*) is a complete, cloud-compiled AI publishing platform that lives in your pocket. It orchestrates a sophisticated multi-stage AI pipeline to transform your simple prompts and device images into beautifully structured, production-ready magazine PDFs.

---

## ✨ Key Features

*   **📱 Native & Polished:** Built natively with Kotlin and Jetpack Compose. Features a custom dark/gold editorial design system, perfectly synced custom splash screens, buttery-smooth navigation, and elegant micro-animations.
*   **🧠 Intelligent Orchestration:** The app securely passes your LiteLLM credentials to a distributed backend, which orchestrates structured JSON data extraction from LLMs to author compelling articles, mastheads, and pull quotes.
*   **☁️ Cloud-Compiled PDFs:** Offloads heavy document processing to a HuggingFace Space. Native LaTeX (LuaLaTeX) and Ghostscript compile high-resolution, multi-page PDFs in seconds.
*   **🚀 Ultra-Fast Asset Streaming:** Bypasses mobile CPU bottlenecks by streaming raw image bytes directly to the server. Server-side Pillow processes high-res 2400px covers and article images in milliseconds.
*   **💾 Robust Persistence:** Features on-device encrypted `SecureStorage` for API keys and UI themes, a persistent SQLite `JobStore` for background tasks, and seamless local PDF management.

---

## 🛠 Developer & Architecture Guide

> **🤖 AI Agent Context:**
> - **Primary objective**: Generate high-quality magazine PDFs from user prompts using LiteLLM and LuaLaTeX.
> - **Single Source of Truth**: `backend/schemas.py`. Any schema changes here MUST be immediately mirrored in the Kotlin DTOs (`android-app/.../models/`).
> - **Design System**: Strict dark/gold editorial theme. Read `DESIGN.md` before changing any UI.
> - **Testing & Dev**: Set `MOCK_COMPILE=True` when running the backend locally to bypass expensive LLM/LaTeX calls.

### 1. Repository Layout

```mermaid
flowchart LR
    ROOT["MagazineForge/"]

    subgraph AND["android-app/  — Kotlin + Jetpack Compose"]
        A1["app/src/main/java/com/magazineforge/app/<br/>MainActivity.kt — nav state machine"]
        A2["ui/ — Compose screens + EditorViewModel"]
        A3["network/ — Retrofit ApiClient + ApiService"]
        A4["models/ — Kotlin DTOs (MUST mirror backend/schemas.py)"]
        A5["utils/SecureStorage.kt — encrypted key storage"]
        A6["ui/theme/ — editorial dark/gold design tokens"]
    end

    subgraph BE["backend/  — FastAPI (separate remote)"]
        B1["main.py — endpoints + compile orchestration"]
        B2["gemini_service.py — LLM prompts (OpenAI SDK → LiteLLM)"]
        B3["schemas.py — Pydantic models (SOURCE OF TRUTH)"]
        B4["image_service.py — Pixabay/Pexels search"]
        B5["templates/*.tex — LaTeX layout variants"]
        B6["jobs.db — SQLite JobStore"]
    end

    ROOT --> AND
    ROOT --> BE
```

### 2. System Architecture & Data Flow

```mermaid
flowchart LR
    USER([User])

    subgraph APP["Android app (Kotlin/Compose)"]
        UI["Compose Screens"]
        VM["EditorViewModel"]
        NET["ApiClient → ApiService"]
        SEC["SecureStorage"]
        STORE["Device Storage"]
    end

    subgraph BACK["FastAPI backend  (HuggingFace Space)"]
        ROUTER["main.py endpoints"]
        GS["gemini_service.py"]
        IS["image_service.py"]
        COMPILE["process_compile_raw_async"]
        JOBS["JobStore (SQLite)"]
        TEMPL["templates/*.tex"]
    end

    LLM["LiteLLM proxy → LLM"]
    IMGAPI["Pixabay / Pexels"]

    USER -->|topic + design| UI
    UI -->|UI Events| VM
    VM -->|Fetch Keys| SEC
    VM -->|"JSON Request<br/>X-LiteLLM-Url/Key headers"| NET
    NET -->|"JSON Request<br/>Authorization: Bearer HF_TOKEN"| ROUTER

    ROUTER -->|Generate Prompt| GS
    ROUTER -->|Image search| IS
    ROUTER -->|compile-raw| COMPILE
    ROUTER <-->|Read/Write Job| JOBS
    ROUTER -->|"Inject JSON into .tex"| TEMPL
    
    GS -->|API Call| LLM
    IS -->|API Call| IMGAPI
    COMPILE -->|PDF & JPEG| NET
    NET -->|Binary Streams| VM
    VM -->|Save .pdf| STORE
```

### 3. Critical Authentication Detail
The backend runs on a **private** Hugging Face Space. The Android `ApiClient` globally injects `Authorization: Bearer <HF_TOKEN>` via an OkHttp interceptor. 
User LLM credentials are provided **per-call** via custom `X-LiteLLM-Url` and `X-LiteLLM-Key` headers. **Never** mix LiteLLM credentials into the Authorization header, or the HF gateway will reject the request.

### 4. The Data Model Sync Rule
`backend/schemas.py` is the **single source of truth**. The Kotlin DTOs in `android-app/.../models/` must mirror it field-for-field. If you rename a field in `schemas.py`, update the Kotlin file in the same commit to avoid `422 Unprocessable Entity` crashes.

---

## 🚀 Build & Run Instructions

**Backend (Local Testing)**
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 7860     # Set MOCK_COMPILE=True to skip LaTeX
```
*(Requires `lualatex` and `gs` on your PATH for real PDF compilation).*

**Android App**
1. Create `android-app/local.properties` and add your HuggingFace token: `HF_TOKEN=hf_...`
2. Open `android-app/` in Android Studio.
3. Sync Gradle and hit Run.

---
*MagazineForge: Where editorial design meets artificial intelligence.*
