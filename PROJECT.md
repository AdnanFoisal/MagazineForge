# Project: MagazineForge Enhancements

## Architecture
- **Backend (Python / FastAPI / Pydantic / Gemini)**: `backend/`
  - `schemas.py`: Pydantic data contracts (`ContractSchema`, `RefinePromptRequest`, `RefinePromptResponse`, `GenerateBriefRequest`, `GenerateBriefResponse`, etc.).
  - `gemini_service.py`: LLM calls, contract extraction, brief generation, dynamic page count synthesis, word budget logic, and prompt refinement.
  - `main.py`: FastAPI route handlers (`/extract-contract`, `/generate-brief`, `/generate-schema`, `/refine-prompt`, `/generate-latex`).
  - `templates/`: TeX template files for covers, articles, TOC, ads, charts, Q&A, back covers.
- **Android App (Kotlin / Jetpack Compose / Retrofit)**: `android-app/`
  - `models/`: DTO schemas (`MagazineSchema.kt`, etc.).
  - `network/`: Retrofit interface (`ApiService.kt`).
  - `ui/`: Compose UI screens (`EditorScreen.kt`, `CoAuthorScreen.kt`, `IntentCardScreen.kt`) & ViewModels (`EditorViewModel.kt`).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Dynamic Contract Extraction | Extract `cover_title`, `cover_subtitle`, `page_count`, `page_types` in `schemas.py` & `gemini_service.py` | M1 | R1 |
| 2 | Brief Title & Count Preservation | Preserve explicit cover titles (e.g. Title: "NIPPON") & page counts in `generate_brief()` | M1 | R1, AC-B2 |
| 3 | Dynamic Page Count & Layout Synthesis | Synthesize 5-6 pages for 6-page requests without 7-page hardcoded regex default; interleave requested page types | M1 | R1, AC-B1 |
| 4 | Backend `/refine-prompt` Endpoint | Implement `POST /refine-prompt` route, DTOs, and backing function in `gemini_service.py` | M1 | R1, R3, AC-B3 |
| 5 | LaTeX Cover Overlay & Callout Polish | Remove 10cm dark bottom rectangle in Cover C; replace solid gray boxes with floating callouts & TikZ drop shadows | M2 | R2, AC-L1 |
| 6 | LaTeX Article Spacing & Page Budget | Increase leading (13pt/21-22pt), image heights (9.5-10cm), parskip, & Python word budget (280-350) for 1.3-1.5 page fit | M2 | R2, AC-L2 |
| 7 | Brief UI Custom Title Selection | Add 4th "Custom Title" RadioButton + OutlinedTextField in `EditorScreen.kt` and thread to ViewModel | M3 | R3, AC-A2 |
| 8 | CoAuthorScreen Image Pickers & Slot Fix | Fix empty-list bug on `+ Add Image Slot` & add image upload/picker fields across all section types (`Ad`, `QnA`, `Chart`) | M3 | R3, AC-A1 |
| 9 | In-Place Prompt Refinement UI | Add Retrofit endpoint, DTOs, ViewModel method, and ✨ Refine Prompt button in `IntentCardScreen.kt` | M3 | R3, AC-A3 |
| 10| E2E Testing & Final Hardening | Verify all acceptance criteria with unit/integration tests and adversarial hardening | M4 | AC-All |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Dynamic Prompt Parsing & Page Type Synthesis | Backend schemas, Gemini prompt parsing, page synthesis, `/refine-prompt` endpoint | none | PLANNED |
| M2 | LaTeX Cover & Article Spacing Polish | LaTeX cover templates, drop shadows, article font leading, spacing, word budgets | M1 | PLANNED |
| M3 | Android UI Enhancements & Prompt Refinement | EditorScreen custom title, CoAuthorScreen image slots, IntentCardScreen prompt refinement | M1 | PLANNED |
| M4 | Final E2E Integration & Verification | Full test suite pass (Tiers 1-4) & adversarial coverage hardening (Tier 5) | M1, M2, M3 | PLANNED |

## Interface Contracts
### Client ↔ Backend (`/refine-prompt`)
- **Request**: `POST /refine-prompt` `{"prompt": String}`
- **Response**: `{"refined_prompt": String, "contract": ContractSchema}`

### Client ↔ Backend (`/generate-brief`)
- **Request**: `POST /generate-brief` `{"prompt": String, "articleCount": Optional[Int]}`
- **Response**: `{"titles": List[String], "category": String, "tone": String, "article_count": Int, "articles": List[ArticleTopic], "issue_bible": IssueBible}`

### Client ↔ Backend (`/generation-runs`)
- **Request**: `POST /generation-runs` `{"prompt": String, "coverTitle": String, ...}`

## Code Layout
- `backend/schemas.py` — Pydantic schemas & contracts
- `backend/gemini_service.py` — Gemini AI service logic
- `backend/main.py` — FastAPI routes
- `backend/templates/` — LaTeX `.tex` templates
- `android-app/app/src/main/java/com/magazineforge/app/models/` — Kotlin data models
- `android-app/app/src/main/java/com/magazineforge/app/network/ApiService.kt` — Retrofit API definitions
- `android-app/app/src/main/java/com/magazineforge/app/ui/` — Compose UI screens & ViewModels
