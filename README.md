# 📖 MagazineForge

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![FastAPI](https://img.shields.io/badge/Backend-FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![LaTeX](https://img.shields.io/badge/Engine-XeLaTeX-008080?style=for-the-badge&logo=latex&logoColor=white)
![License](https://img.shields.io/badge/Copyright-Adnan_Foisal--All_Rights_Reserved-gold?style=for-the-badge)

> **MagazineForge** is an enterprise-grade, AI-native magazine publishing platform built for Android and Python. It combines multi-stage LLM intent expansion, dynamic layout compilation, dual-provider stock photo aggregation, and high-precision LaTeX typesetting to transform single prompts into luxury, print-ready 16-page magazine PDFs.

---

## 🌟 Premium Features

*   **📱 Native Jetpack Compose UX:** Built with a custom luxury dark/gold editorial design system (`LuxeTypography`, tokenized styling, glassmorphism, and reactive state management).
*   **✨ In-Place Prompt Refinement:** Instant LLM prompt expansion right on the Intent Card screen without losing context or resetting user input.
*   **🎨 Interactive Color Swatch Selector:** 7 curated luxury color swatches (Pure White, Classic Gold, Crimson Red, Electric Cyan, Royal Purple, Warm Amber, Dark Charcoal) with real-time accent hex binding.
*   **🔍 Dual-Provider Image Aggregation:** Searches Pixabay (12 results) and Pexels (10 results) in parallel, delivering 20+ candidate stock photos with live image thumbnail previews inside the schema editor.
*   **📰 High-Fashion Table of Contents:** Vogue/GQ-style structured TOC spread with double-digit page badges (`01`, `02`), section rules, and column layout synthesis.
*   **🏷️ 4th Custom Title Selection:** Choose from 3 AI-suggested titles or type in your own custom magazine title with direct schema binding.
*   **☁️ Serverless TeX Document Compiler:** Offloads complex PDF generation to a Hugging Face Space running XeLaTeX and Ghostscript for instant, high-res PDF downloads.

---

## 🛠 Architectural Overview

```mermaid
flowchart LR
    ROOT["MagazineForge/"]

    subgraph AND["android-app/ — Kotlin + Jetpack Compose"]
        A1["MainActivity.kt — Nav State Machine"]
        A2["ui/ — Intent, Editor & CoAuthor Screens"]
        A3["network/ — Retrofit + OkHttp Interceptors"]
        A4["models/ — Kotlin DTOs (Mirrors Backend)"]
        A5["utils/SecureStorage.kt — Encrypted Key Storage"]
        A6["ui/theme/ — Editorial Tokenized Theme System"]
    end

    subgraph BE["backend/ — FastAPI (HuggingFace Space)"]
        B1["main.py — Endpoints & Compilation Engine"]
        B2["gemini_service.py — Multi-Stage LLM Synthesis"]
        B3["schemas.py — Pydantic Schemas (Single Source of Truth)"]
        B4["image_service.py — Pixabay & Pexels Dual Engine"]
        B5["templates/*.tex — XeLaTeX Magazine Layout Spreads"]
        B6["jobs.db — Async SQLite JobStore"]
    end

    ROOT --> AND
    ROOT --> BE
```

---

## ⚡ System Data Flow & Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor User as Android App User
    participant App as Android Client (Compose)
    participant HF as FastAPI Backend (Hugging Face)
    participant LLM as Gemini / LiteLLM Engine
    participant Stock as Pixabay & Pexels APIs
    participant TeX as XeLaTeX Engine

    User->>App: Input Topic Prompt
    App->>HF: POST /refine-prompt
    HF->>LLM: Multi-Stage Prompt Expansion
    LLM-->>App: Return Refined Intent Contract
    User->>App: Customize Color Swatches & Select Stock Photos
    App->>HF: POST /preview-images (Parallel Query)
    Stock-->>App: 20+ Stock Image Candidates + Live Previews
    User->>App: Tap "Compile Magazine"
    App->>HF: POST /compile-raw
    HF->>TeX: Inject JSON Schema into .tex Layout Spreads
    TeX-->>HF: Output High-Res PDF Document
    HF-->>App: Stream PDF Binary
    App-->>User: Open Interactive PDF Reader
```

---

## 🔒 Security & Data Model Synchronization

1. **Strict Data Contract Sync**: `backend/schemas.py` is the single source of truth. All Pydantic schema updates are mirrored field-for-field in Kotlin DTOs (`MagazineSchema.kt`).
2. **Encrypted Key Storage**: User API keys are stored on-device using Android `EncryptedSharedPreferences` (`SecureStorage.kt`).
3. **Isolated Header Authentication**: The backend utilizes custom `X-LiteLLM-Url` and `X-LiteLLM-Key` headers alongside Hugging Face Bearer tokens.

---

## 🚀 Local Development Setup

### Backend
```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 7860
```
*(Requires `xelatex` and `ghostscript` installed on your path for PDF compilation)*

### Android App
1. Add your Hugging Face bearer token to `android-app/local.properties`: `HF_TOKEN=hf_...`
2. Open `android-app/` in Android Studio.
3. Sync Gradle and run on device or emulator.

---

*MagazineForge: Where editorial design meets artificial intelligence.*
