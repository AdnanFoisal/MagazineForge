# MagazineForge — Architecture & Feature Map

> **Last Updated:** 2026-07-02 (Phase 6)

---

## Current Phase 6 Status

- Local backend split-flow tests pass against `MOCK_COMPILE=True`.
- All 18 template sample PDFs and 18 cover JPG thumbnails have been regenerated through the live HuggingFace `/compile-raw` endpoint and verified locally.
- Android schema models now use Gson `@SerializedName` mappings so snake_case backend JSON can round-trip through Kotlin camelCase models.
- HuggingFace static sample URLs remain pending live verification until the backend commit is pushed with a rotated HuggingFace token.
- Android APK compilation is delegated to GitHub Actions after the GitHub push.

---

## 1. Project Overview

MagazineForge is an AI-powered Android application that lets users create professional-quality digital magazines from simple text prompts. The app uses a Gemini AI backend to generate structured magazine schemas, converts them to LuaLaTeX code, and compiles them into beautifully typeset PDFs via a cloud LuaLaTeX engine on HuggingFace Spaces.

---

## 2. Repository Structure

```
MagBoy/
├── android-app/                    # Android Jetpack Compose Application
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── assets/
│   │   │   │   └── template_config.json    # 18 template definitions
│   │   │   ├── java/com/magazineforge/app/
│   │   │   │   ├── MainActivity.kt         # Entry point, Navigation, Scaffold
│   │   │   │   ├── models/
│   │   │   │   │   ├── CompileRequest.kt       # Compile request model
│   │   │   │   │   ├── JobModels.kt            # Job status/response models
│   │   │   │   │   ├── MagazineSchema.kt       # AI-generated schema model
│   │   │   │   │   ├── ShowcaseItem.kt         # Community feed item model
│   │   │   │   │   └── VerifyKeyModels.kt      # API key verification model
│   │   │   │   ├── network/
│   │   │   │   │   ├── ApiClient.kt            # Retrofit client (BASE_URL)
│   │   │   │   │   ├── ApiService.kt           # REST API interface
│   │   │   │   │   └── ShowcaseRepository.kt   # Firebase Firestore repository
│   │   │   │   ├── ui/
│   │   │   │   │   ├── CoAuthorScreen.kt           # AI Semi-Builder (granular controls)
│   │   │   │   │   ├── EditorScreen.kt             # LaTeX code editor + compile
│   │   │   │   │   ├── EditorViewModel.kt          # Compile job state management
│   │   │   │   │   ├── LatexNotebookScreen.kt      # Raw LaTeX notebook
│   │   │   │   │   ├── MyMagazinesScreen.kt        # User's saved magazines (Library)
│   │   │   │   │   ├── OnboardingScreen.kt         # First-run Gemini API key entry
│   │   │   │   │   ├── PdfViewerScreen.kt          # In-app PDF renderer
│   │   │   │   │   ├── ShowcaseScreen.kt            # Community feed (masonry grid)
│   │   │   │   │   ├── TemplateGalleryScreen.kt    # Template gallery + preview
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt                # Luxe Editorial Noir palette
│   │   │   │   │       ├── Theme.kt                # Material3 theme setup
│   │   │   │   │       └── Type.kt                 # Space Grotesk + Playfair Display
│   │   │   │   └── utils/                          # Utility classes
│   │   │   └── res/                                # Android resources
│   │   └── google-services.json                    # Firebase config (gitignored)
│   └── build.gradle.kts                            # Root Gradle config
│
├── backend/                        # FastAPI Backend (HuggingFace Spaces)
│   ├── main.py                     # FastAPI app (all API endpoints)
│   ├── gemini_service.py           # Gemini AI integration for schema gen
│   ├── schemas.py                  # Pydantic request/response models
│   ├── Dockerfile                  # Docker image (python + texlive)
│   ├── packages.txt                # System deps (texlive-luatex, ghostscript)
│   ├── requirements.txt            # Python deps
│   ├── templates/                  # LaTeX template files (.tex)
│   │   ├── cover_template_a.tex    # Layout A: Title top-center
│   │   ├── cover_template_b.tex    # Layout B: Title bottom-right
│   │   ├── cover_template_c.tex    # Layout C: Title dead-center
│   │   ├── article_template_*.tex  # Article page layouts
│   │   └── toc_template_*.tex      # Table of contents layouts
│   ├── static/samples/             # Generated sample PDFs + cover images
│   ├── generate_assets.py          # Asset generation script
│   └── test_compile_raw.py         # Backend test script
│
├── .github/workflows/
│   └── android-ci.yml              # GitHub Actions CI/CD pipeline
│
├── stitch_ui_export/               # Stitch UI design system export (HTML)
│
└── magazine-analysis/
    └── source-pdfs/                # Reference magazine PDFs for analysis
```

---

## 3. Features Implemented

### ✅ Phase 1 — Foundation & Onboarding
| Feature | File(s) | Description |
|---------|---------|-------------|
| Gemini API Key Onboarding | `OnboardingScreen.kt` | First-run screen to enter & verify Gemini API key |
| API Key Verification | `ApiService.kt` `/verify-key` | Backend validates key against Gemini API |
| Luxe Editorial Noir Theme | `Color.kt`, `Theme.kt`, `Type.kt` | Pitch black (#0A0A0A), Editorial Gold (#C5A059), Space Grotesk + Playfair Display |
| Navigation Scaffold | `MainActivity.kt` | Bottom nav: Showcase, Studio, Templates, Library |

### ✅ Phase 2 — Studio & AI Builder
| Feature | File(s) | Description |
|---------|---------|-------------|
| The Studio (Prompt Input) | `EditorScreen.kt` | User enters magazine description prompt |
| AI Schema Generation | `gemini_service.py`, `/generate-schema` | Gemini generates structured magazine JSON schema |
| AI LaTeX Generation | `main.py`, `/generate-latex` | Converts schema → full LuaLaTeX document |
| LaTeX Code Editor | `EditorScreen.kt` | Syntax-highlighted editor to review/edit LaTeX |
| Cloud Compilation | `main.py`, `/compile-raw` | Sends LaTeX to HF Space lualatex engine |
| Job Progress Tracking | `EditorViewModel.kt`, `/job/{id}/status` | Async polling with progress bar |
| PDF Download | `/job/{id}/download` | Download compiled PDF |
| In-App PDF Viewer | `PdfViewerScreen.kt` | Renders PDF natively in-app |

### ✅ Phase 3 — Template Gallery & Co-Author
| Feature | File(s) | Description |
|---------|---------|-------------|
| Template Gallery | `TemplateGalleryScreen.kt` | 18 templates across 6 categories |
| Category Filter Chips | `TemplateGalleryScreen.kt` | Horizontal scrollable category filters |
| Template Preview Cards | `TemplateGalleryScreen.kt` | Visual preview with AsyncImage + cover overlay |
| Preview Sample PDF Button | `TemplateGalleryScreen.kt` | Opens sample PDF in browser/viewer |
| AI Semi-Builder (Co-Author) | `CoAuthorScreen.kt` | 20-30 granular customization fields per page |
| Google Drive Image Import | `CoAuthorScreen.kt` | Per-field Google Drive image URL input |
| Gallery Image Import | `CoAuthorScreen.kt` | Per-field device gallery image picker |
| AI-Generated TOC | `CoAuthorScreen.kt` | AI auto-builds table of contents from content |

### ✅ Phase 4 — Community Showcase
| Feature | File(s) | Description |
|---------|---------|-------------|
| Showcase Feed | `ShowcaseScreen.kt` | Masonry grid of community magazines |
| Firebase Firestore Integration | `ShowcaseRepository.kt` | Real-time feed from Firestore `showcase` collection |
| Like/Share Interactions | `ShowcaseScreen.kt` | Social engagement on feed items |

### ✅ Phase 5 — Polish & Verification
| Feature | File(s) | Description |
|---------|---------|-------------|
| HF Compiler Verification | `test_compile_raw.py` | E2E test confirming cloud lualatex works |
| API Endpoint Fixes | `ApiService.kt` | Corrected endpoint paths to match backend |
| Preview Image Integration | `TemplateGalleryScreen.kt` | Dynamic AsyncImage on template cards |

### 🔄 Phase 6 — Asset Generation & CI (In Progress)
| Feature | File(s) | Description |
|---------|---------|-------------|
| CI/CD Fix | `android-ci.yml` | Mock google-services.json for GitHub Actions |
| Asset Generation | `generate_assets.py` | Compile unique sample PDFs for all 18 templates |
| Cover Image Extraction | `generate_assets.py` | Extract first page as JPEG thumbnail |
| Static Asset Serving | `main.py` `/static/` | Serve PDFs and images from HF Space |

---

## 4. Backend API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Health check |
| `GET` | `/health` | Health check |
| `POST` | `/verify-key` | Validates Gemini API key |
| `POST` | `/generate-schema` | AI generates magazine JSON schema from prompt |
| `POST` | `/generate-latex` | Converts schema → LaTeX code |
| `POST` | `/compile-raw` | Compiles raw LaTeX → PDF (async job) |
| `GET` | `/job/{id}/status` | Poll compilation job progress |
| `GET` | `/job/{id}/download` | Download completed PDF |
| `GET` | `/static/samples/*` | Serve template sample PDFs and cover images |

---

## 5. Design System — Luxe Editorial Noir

| Token | Value | Usage |
|-------|-------|-------|
| Background | `#0A0A0A` (Pitch Black) | App background |
| Surface | `#18181B` | Cards, sheets |
| Primary | `#C5A059` (Editorial Gold) | Accents, buttons, highlights |
| On-Surface | `#F5F5F7` | Primary text |
| Muted | `#A1A1AA` | Secondary text |
| Border | `#2E2A24` | Card borders |
| Heading Font | Playfair Display | Titles, headings |
| Body Font | Space Grotesk | Body text, UI labels |

---

## 6. Cloud Infrastructure

| Service | Purpose | URL |
|---------|---------|-----|
| HuggingFace Spaces | Backend API + LuaLaTeX compiler | `https://adnanfoisal-magazineforge.hf.space` |
| Firebase Firestore | Community Showcase feed data | `magazineforge-14d44` project |
| Firebase Storage | User-uploaded images (planned) | `magazineforge-14d44.firebasestorage.app` |
| GitHub Actions | CI/CD pipeline | `AdnanFoisal/MagazineForge` |

---

## 7. Template System

18 templates organized by category, each mapping to a LaTeX cover layout:

| Category | Templates | Layout |
|----------|-----------|--------|
| Food | Classic Dining, Modern Gastronomy, Rustic Kitchen | A, B, C |
| Travel | Expansive Horizon, The Voyager, City Guide | A, B, C |
| Tech | Cyber Edge, Clean Tech, The Spec Sheet | A, B, C |
| Lifestyle | Editorial Chic, Indie Culture, Mind & Body | A, B, C |
| Science | Deep Space, Macro World, The Academic | A, B, C |
| Custom | Boardroom, The Statement, Blank Canvas | A, B, C |

Each template has:
- `thumbnailUrl`: Cover image (first page of sample PDF)
- `samplePdfUrl`: Full compiled sample PDF
- `texTemplate`: Which LaTeX layout file to use
