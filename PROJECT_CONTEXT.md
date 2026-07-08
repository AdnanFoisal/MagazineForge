# MagazineForge (MagBoy) Project Context

This document is designed to give a new coding agent complete context on the MagazineForge project in a single read. It covers the architecture, data flow, key files, and crucial gotchas required to contribute effectively without prior knowledge.

## 1. Project Overview
MagazineForge is an Android application that allows users to generate, customize, edit, and compile professional-grade magazines into PDF format directly from their mobile devices. It uses Google's Gemini AI to author content based on user prompts and a Python backend to compile the generated LaTeX code into finished PDFs.

## 2. Tech Stack & Architecture
- **Frontend (Android):** Kotlin, Jetpack Compose (UI), Coroutines/Flow (Async), Retrofit (Networking), Coil (Image Loading).
- **Backend (Python):** FastAPI (REST Framework), Uvicorn (Server), `google-generativeai` (Gemini SDK), Pydantic (Schema Validation).
- **Compilation Toolchain:** LuaLaTeX (PDF Generation), Ghostscript (Cover image extraction).
- **Hosting:** The backend is deployed as a private Hugging Face Space.

**Architecture Flow:**
`Android Client` → `HTTPS (REST)` → `FastAPI Backend (Hugging Face)` → `Gemini API (Content Gen)` → `LuaLaTeX (Compile)` → `Android Client (PDF View)`

## 3. Directory Structure
- `/android-app/`: Root of the Android project.
  - `build.gradle.kts` / `app/build.gradle.kts`: Gradle build configurations.
  - `app/src/main/assets/template_config.json`: Defines the available magazine templates shown in the UI gallery.
  - `app/src/main/java/com/magazineforge/app/`: 
    - `models/`: Kotlin data classes for API payloads and internal state. Must match backend schemas.
    - `network/`: Retrofit client setup and API interface definitions.
    - `ui/`: Jetpack Compose screens and the primary ViewModel.
- `/backend/`: Root of the Python backend.
  - `main.py`: FastAPI server entry point and endpoint routing.
  - `gemini_service.py`: Gemini AI prompt engineering and API interaction.
  - `schemas.py`: Pydantic models for strict data validation.
  - `templates/`: Raw `.tex` files used as starting points for generation.
  - `Dockerfile` & `requirements.txt`: Environment definitions for Hugging Face deployment.

## 4. Key Files, Deep Dive
- **`android-app/.../MainActivity.kt`**: The root UI orchestrator. It manages top-level navigation (`currentScreen`), observes state from the `EditorViewModel`, and handles the initial API key input (including a hardcoded bypass for keys starting with `AQ`).
- **`android-app/.../ui/EditorViewModel.kt`**: The core business logic coordinator. It manages Kotlin `StateFlow`s for schema generation, LaTeX generation, and compilation. It triggers network calls and handles the responses.
- **`android-app/.../network/ApiClient.kt` & `ApiService.kt`**: `ApiClient` configures OkHttp and importantly, injects the Hugging Face `Authorization` bearer token globally via an interceptor. `ApiService` defines the endpoints, passing the user's Gemini API key specifically via the `X-Gemini-Key` header to avoid interceptor conflicts.
- **`backend/main.py`**: The FastAPI server. It defines all endpoints, extracts the `X-Gemini-Key`, orchestrates asynchronous background tasks for LuaLaTeX compilation, and contains fallback mock logic for invalid/test API keys.
- **`backend/gemini_service.py`**: Handles all communication with Google Gemini. It contains the prompt engineering required to force Gemini to output strictly formatted JSON or raw LuaLaTeX code, and strips Markdown formatting from responses.
- **`backend/schemas.py`**: The source of truth for data structures. These Pydantic models validate incoming requests. If the Android app's `MagazineSchema` drifts from these definitions, the backend will reject requests with `422 Unprocessable Entity`.
- **`android-app/.../ui/LatexNotebookScreen.kt`**: The raw code editor UI. It implements custom text transformations for search highlighting, manages its own undo/redo stack, and allows users to trigger compilation or save the `.tex` file locally.
- **`android-app/.../ui/CoAuthorScreen.kt`**: The guided schema editor. It presents the AI-generated outline to the user, enforces validation (fields cannot be blank) before allowing the user to proceed to LaTeX generation.

## 5. Data Flow / Core Pipeline
**The "Assisted Mode" End-to-End Flow:**
1. **Selection:** User picks a template in `TemplateGalleryScreen` and clicks Next.
2. **Schema Gen:** `EditorScreen` calls `viewModel.generateSchema(apiKey, topic)`. Android sends `POST /generate-schema`.
3. **AI Outline:** Backend `main.py:generate_schema` calls `gemini_service.py:generate_full_magazine_schema`. Gemini returns a structured JSON outline. Backend returns it to Android.
4. **Customization:** Android UI transitions to `CoAuthorScreen`. User reviews/edits the `MagazineSchema` and clicks "Generate".
5. **LaTeX Gen:** `CoAuthorScreen` calls `viewModel.generateLatex(schema)`. Android sends `POST /generate-latex` with the customized schema.
6. **Template Fill:** Backend `main.py:generate_latex` loads local `.tex` templates and injects the schema strings (using `(((PLACEHOLDER)))` syntax). Returns the raw LaTeX string.
7. **Editing:** UI transitions to `LatexNotebookScreen`. User views/edits raw code, then clicks "Compile".
8. **Compilation:** `LatexNotebookScreen` calls `viewModel.compileRaw(code)`. Android sends `POST /compile-raw`.
9. **Background Job:** Backend starts `process_compile_raw_async`, writes code to disk, runs `lualatex` (and `gs` for the cover), and immediately returns a `jobId`.
10. **Polling:** Android polls `GET /job/{job_id}/status` until `COMPLETED`.
11. **Viewing:** Android fetches the PDF via `GET /job/{job_id}/download` and displays it in `PdfViewerScreen`.

## 6. API Contract
- `POST /verify-key`: Validates Gemini key. Req: `{"gemini_api_key": "str"}`. Res: `{"valid": bool, "models": list}`.
- `POST /generate-schema`: Creates magazine outline. Req: `GenerateSchemaRequest`. Headers: `X-Gemini-Key`. Res: `MagazineSchema` JSON.
- `POST /generate-latex`: Fills templates. Req: `GenerateLatexRequest`. Headers: `X-Gemini-Key`. Res: `{"latexCode": "str"}`.
- `POST /generate-raw-latex`: Direct AI-to-LaTeX. Req: `{"prompt": "str"}`. Headers: `X-Gemini-Key`. Res: `{"latexCode": "str"}`.
- `POST /compile-raw`: Starts LuaLaTeX compile. Req: `{"latexCode": "str"}`. Res: `{"jobId": "str"}`.
- `GET /job/{job_id}/status`: Poll status. Res: `{"status": "str", "progress": int, "error": "str", "cover_url": "str"}`.
- `GET /job/{job_id}/download`: Fetch PDF. Res: `application/pdf` stream. (Requires HF Authorization header).
- `GET /job/{job_id}/cover`: Fetch cover image. Res: `image/jpeg` stream. (Requires HF Authorization header).
- `POST /upload-asset`: Upload image to backend. Req: `multipart/form-data` file. Res: `{"url": "str"}`.

## 7. Environment & Config
- **Android `local.properties`**: Requires `HF_TOKEN=hf_...` (A Hugging Face Personal Access Token). This is injected into BuildConfig and used by Retrofit to authenticate against the private backend space.
- **Backend Environment Variables**: 
  - `MOCK_COMPILE`: If set to `"True"`, bypasses Gemini and LuaLaTeX, returning fast, hardcoded mock data and a dummy PDF. Useful for UI testing.
- **User Config**: The user's Google Gemini API Key is entered in the UI at runtime, stored in EncryptedSharedPreferences, and passed dynamically via the `X-Gemini-Key` header.

## 8. Build & Run Instructions
**To run the Backend locally:**
1. Install Python 3.11+, TeX Live (specifically `lualatex`), and Ghostscript (`gs`). Ensure they are in your PATH.
2. `cd backend`
3. `pip install -r requirements.txt`
4. `uvicorn main:app --reload --port 7860`

**To run the Android App locally:**
1. Create `android-app/local.properties` and add your Hugging Face token: `HF_TOKEN=hf_your_token_here`.
2. Open `/android-app` in Android Studio.
3. Sync Gradle and run on an emulator/device.

## 9. Known Issues & In-Progress Work
- **API Key Bypasses:** The codebase currently contains intentional hacks in `MainActivity.kt` and `main.py` that look for API keys starting with `AQ` or `AIzaSyMock`. If found, the app bypasses server-side key validation and the backend returns mocked schema/LaTeX data. This was added to unblock development when valid keys were failing. These should eventually be removed or moved behind a strict `DEBUG` build flag.
- Further architectural context can be found in legacy documentation files in the repository root (e.g., `architecture.md`, `DESIGN.md`, `MagazineApp_MasterBlueprint_v3.md`), but treat this `PROJECT_CONTEXT.md` as the most accurate reflection of the current implementation.

## 10. Conventions & Gotchas
- **Header Conflicts (CRITICAL):** Because the backend is a private Hugging Face space, the Android client's OkHttp interceptor globally injects `Authorization: Bearer <HF_TOKEN>`. Therefore, the user's Gemini API key MUST be passed via the `X-Gemini-Key` header. If you pass it via `Authorization`, the interceptor will duplicate or overwrite it, causing auth failures at either Hugging Face or Google.
- **Schema Synchronization:** The Kotlin data classes in `com.magazineforge.app.models` MUST exactly mirror the Pydantic classes in `backend/schemas.py`. Deserialization errors or 422 API errors will occur if a field is added to one but not the other.
- **Template Placeholders:** Backend LaTeX templates use `(((PLACEHOLDER)))` syntax for string replacement, not standard LaTeX or Jinja syntax.
- **Image Auth:** When loading generated images (Coil `AsyncImage`) or downloading PDFs (`PdfViewerScreen`), the Hugging Face `Authorization` header must be explicitly attached to the request, otherwise the private space will return a 401 Unauthorized.
