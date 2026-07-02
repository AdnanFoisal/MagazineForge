# MagazineForge — Master Blueprint v3 (Antigravity 2.0 + Stitch MCP Build Edition)
### Complete Engineering Blueprint, Bug Registry & Feature Spec — Reviewed & Updated June 2026
*Nothing from v1 or v2 has been deleted. Everything below is preserved in full. New material is clearly marked **[v3 NEW]** or **[v3 CLARIFICATION]**. Where v3 corrects a fact in v2, the old text stays and a note is added directly beneath it — exactly the same convention v2 used on v1.*

---

## 📌 WHAT CHANGED IN v3 — READ THIS FIRST

v3 was commissioned to answer seven specific asks. Here's where each one landed in the document:

1. **Bring-your-own Gemini API key, with verification.** Users now type in their *own* Gemini API key instead of you supplying one. New **Feature 11** (Part 3) covers the input flow and the verify-before-use check. This intersects with a real, time-sensitive Google policy change (standard keys vs. "auth" keys — see **Part 6, A10**) that you need to know about *before* you build this, not after.
2. **World-class design via Google Stitch MCP, not default agent UI.** New **Part 0B** explains what Stitch is, how its MCP server plugs into Antigravity, and — critically — gives you the exact prompting pattern so the agent designs every screen in Stitch first and *then* implements it, instead of generating generic Material Design boilerplate on its own. This is the single highest-leverage change in v3 for "looking professional."
3. **Maximum modularity, no matter how many micro-steps.** New **Part 9** lays out the actual rule for how small a "step" should be, why, and how the agent should checkpoint after each one. New **Part 12** re-expresses the entire Part 5 build order as hyper-granular numbered micro-steps so nothing is ever bundled.
4. **GitHub Actions instead of local APK compilation.** New **Part 7** is a full CI/CD setup — debug builds on every push, signed release builds on tags, artifact retrieval — so you never install Android Studio or the SDK locally.
5. **Agent self-verification + the "why software breaks" briefing.** New **Part 8** is the verification protocol: what "done" means for every single step, a checklist of the specific failure modes that sink projects like this one, and an explicit, standing instruction that every tool/library/model version must be searched and confirmed current — not assumed — before it's used.
6. **The complete magazine design, and bulletproof Gemini system prompting.** New **Part 10** walks through what a *full* magazine actually is (not just a cover) and how to write a Gemini system prompt detailed enough that generations don't break your pipeline.
7. **Ready-to-use, expert-level magazine analysis prompts.** New **Part 11** is the deliverable you asked for directly: a set of large, professional-grade prompts you copy-paste into Gemini Vision (or any multimodal model) alongside real magazine PDFs, to extract structured "Design Persona" JSON you'll later feed back to the agent.

Everything from v1 and v2 — the bug registry, the feature refinements, the architecture table, the build order — is still here, unedited, below.

---

## PART 0 — Using Google Antigravity 2.0 to Build This App

### What Antigravity 2.0 actually is
It's Google's agent-first development platform — think "Cursor/Windsurf, but Google's version, now with a standalone orchestrator app on top of the IDE." As of mid-2026 it includes:
- **Antigravity IDE** — a VS Code–style editor (built largely by the former Windsurf team) with inline AI completions and an agent panel.
- **Antigravity 2.0 (the app)** — a separate desktop "command center" for running multiple agents in parallel, scheduling background tasks, and managing them across projects. You can run this alongside any editor.
- **Antigravity CLI** — terminal-native agent runner, replacing Gemini CLI.
- **Antigravity SDK** — for embedding the same agent primitives into your own tools (not something you need for this project).
- A **Browser Subagent** that actually opens a real Chrome instance, clicks through your running app, and verifies it works — this is genuinely useful for this project (testing your FastAPI Swagger UI, and later a web preview, without you doing it by hand).
- It's free for individual use, with a compute-based quota that refreshes every 5 hours — plenty for a solo personal project.

### The one thing to get right: Antigravity is the workshop, not a part of the car
This is the most important correction in this whole document. Antigravity 2.0 is **the tool you use to write and run the code** — the same role Android Studio, VS Code, or a human contractor would play. It is **not** a library, SDK, or server your finished app calls at runtime. Nothing in Part 4's architecture table changes because of it. When you're done, MagazineForge still runs as:
- An Android app (or, see Part 6's optional alternative, a simple web app)
- A FastAPI backend on Hugging Face Spaces
- Calling the Gemini API directly

Antigravity just generates, tests, and debugs all of that *for* you. Don't let it (or any AI) talk you into adding an "Antigravity API call" inside the shipped app — that's not what it's for.

### Suggested Antigravity project setup
Create one Antigravity **Project** with two folders so the agent has clean boundaries and the right context for each half of the stack:
```
magazineforge/
├── backend/        # FastAPI + LuaLaTeX + templates — Python
└── android-app/    # Jetpack Compose — Kotlin
```
Give the agent the relevant SKILL/README-style context up front (you can literally paste in this whole blueprint as project context) so it doesn't re-derive the architecture or reintroduce the bugs already solved below.

### Phase-by-phase prompts (maps to Part 5's Build Order)
Feed these to Antigravity roughly one phase at a time — don't ask for the whole app in one prompt, multi-step agent builds are far more reliable when scoped:
1. *"Set up a FastAPI app in /backend with a Dockerfile and packages.txt that installs TeX Live (luatex + latex-extra + fonts-extra) and ghostscript on Hugging Face Spaces. Add a /health endpoint. Then write one cover_template_a.tex with placeholders, plus a Python function that injects values and runs lualatex twice with a 90s timeout, then returns the PDF. Test it via the Swagger UI."*
2. *"Add a /generate endpoint that calls the Gemini API (gemini-2.5-flash) with this Design DNA system prompt [paste from Part 3 / Feature 08], strips markdown code fences, and validates the result with this Pydantic model [paste CoverPage]. Also add the LaTeX sanitizer from Bug 04 — run it on every string before template injection."*
3. *"Scaffold a single-screen Jetpack Compose Android app with Retrofit2 pointed at my Space URL, a topic text field, an image URL field, and a Compile button that's disabled while a request is in flight. Show the returned PDF with PdfRenderer."*
4. Continue through Phases 4–6 the same way, one bug-fix or feature section of this document per prompt, asking the agent to use the Browser Subagent to hit your Swagger UI and confirm each endpoint before moving on.

> **[v3 CLARIFICATION]** Step 3 of this prompt sequence is exactly the point where v3 changes things. Before you ask the agent to scaffold *any* screen from scratch, send it through the Stitch MCP workflow in **Part 0B** first. The prompts above are kept here unedited (so you can see what v2 originally suggested), but treat them as superseded by Part 0B + Part 12 for anything UI-related. The backend prompts (1, 2) are unaffected — Stitch is a UI design tool, it has nothing to do with FastAPI or LaTeX.

### Guardrails when vibe-coding with real secrets
An agent that can run shell commands and write files can also accidentally commit or print your API keys. Before you start:
- Put `GEMINI_API_KEY`, `APP_SECRET_KEY`, and any Firebase credentials in a `.env` file (backend) and `local.properties` (Android) — **never** type the actual key value into the chat prompt itself.
- Add `.env`, `local.properties`, and `*serviceAccount*.json` to `.gitignore` *before* the first commit, and ask the agent to confirm it did so.
- On Hugging Face, set secrets via the Space's **Settings → Variables and Secrets** UI, not in committed files. HF does scan for accidentally-committed secrets and will warn you, but don't rely on that as your only safeguard.
- Periodically ask the agent to `grep -r` the repo for anything that looks like a key before you push, especially after a long autonomous session.

> **[v3 NEW]** In v3, *users* type their own Gemini key into the Android app at runtime (Feature 11). That key never touches your repo, your `.env`, or Antigravity at all — it lives only on the user's device and travels straight from the Android app to Google's API. Keep this distinction crystal clear for the agent: developer secrets (yours) go in `.env`/HF Secrets as above; end-user keys (theirs) are runtime input, encrypted on-device, never logged, never sent to your own backend in plaintext if you can avoid it. See Feature 11 for exactly how this works with the FastAPI proxy pattern.

---

## PART 0B — Design Workflow: Google Stitch MCP **[v3 NEW]**

### Why this section exists
Left to its own devices, a coding agent (Antigravity, Claude Code, Cursor — any of them) defaults to whatever its training data calls "a clean UI": centered cards, a generic blue accent, system-default Material 3 components, predictable spacing. It's not *bad*, but it's the same look every AI-scaffolded app has right now. You explicitly asked for the result to look like the work of a professional design studio, not a hackathon demo — and the tool to get there already exists and is sitting right next to Antigravity: **Google Stitch**.

### What Stitch actually is
Stitch (`stitch.withgoogle.com`) is Google's AI-native UI design canvas, powered by Gemini. You describe a screen — or a whole app — in natural language (or upload a reference image, or just talk to it via voice), and it produces a high-fidelity, production-quality visual design: real typography, real spacing, a coherent color system, actual layout structure — not a wireframe. Every design produces clean, responsive HTML and Tailwind CSS code, and every design also exports to Figma with proper Auto Layout, named layers, and editable text. It's free, with a generous monthly generation quota on the standard model.

### The MCP bridge — how it connects to Antigravity
Google Stitch can be connected to the Antigravity IDE via the Model Context Protocol (MCP), letting an autonomous agent fetch the "Design DNA" of a Stitch project and implement a pixel-perfect application from it. Concretely, once Stitch is added as an MCP server inside Antigravity:
- You invoke it from inside any Antigravity prompt — e.g. "Use the Stitch MCP server to design a visual interface for this app" — and Antigravity generates a context-aware prompt for Stitch automatically, because it already knows what your app does and what its functionality is.
- Stitch returns a design, and Antigravity pulls the HTML/CSS files directly into its workspace, incorporating them into the implementation without needing to re-imagine how to convert the design into code.
- For native targets, Antigravity can consume Stitch's design context through MCP and generate implementation code in the target framework — the documented pattern today covers React/Tailwind and Flutter most directly.

> **[v3 CLARIFICATION — important, read before you build]** Stitch does **not** export Jetpack Compose code directly today. There isn't a native one-click integration into a tool like Jetpack Compose yet — the most practical path is to export the design as an image or HTML/CSS and use that as the visual ground truth for building in another tool. That's fine for this project — it's exactly the workflow below — but don't let the agent (or anyone explaining this to you) imply Stitch will hand you finished `.kt` Composables. It hands you the *design truth*; Antigravity still writes the actual Compose code, just against a precise visual reference instead of guessing.

### Setting up the Stitch MCP server in Antigravity
In the Antigravity agent window, click the three-dot menu and select MCP Servers, then search for "stitch" and install the Stitch MCP server. You'll need an API key from the Stitch app: go to stitch.withgoogle.com, click your profile picture, open Stitch Settings, and create an API key there, then paste it into the MCP server configuration screen in Antigravity. The raw config (accessible via the Agent Panel's three-dot menu → MCP Servers → Manage MCP Servers → View Raw Config) looks like:
```json
{
  "mcpServers": {
    "stitch": {
      "serverUrl": "https://stitch.googleapis.com/mcp",
      "headers": { "X-Goog-Api-Key": "YOUR-API-KEY" }
    }
  }
}
```
> **[v3 NEW]** Treat the Stitch API key exactly like the Gemini key in Part 0's guardrails — it's a developer-side secret for *your* design workflow, unrelated to the end-user BYOK key in Feature 11. Don't paste the raw key value into a chat prompt; paste it into the MCP config screen/file directly.

### The actual MagazineForge design workflow (do this before any Compose code is written)
This replaces step 3 of Part 0's phase-by-phase prompts. Run it once per major screen, before Phase 3 of the build order (Part 5/Part 12):

**Step 1 — Design each screen in Stitch directly, in natural language, one at a time.** Don't ask for "the whole app" in one Stitch prompt — same modularity principle as Part 9. Suggested screen list, matched to MagazineForge's actual feature set (Part 3):
- Template Gallery (categorized tabs + 2-column grid — Feature 01)
- Live Preview / Compile screen (canvas + bottom sheet form fields — Feature 03)
- Font Pair picker (Feature 04)
- Draft vs. Final compile toggle (Feature 05)
- API Key entry / verification screen (Feature 11 — new in v3)
- Project Library ("My Magazines" — Feature 07)
- PDF Viewer screen

Example Stitch prompt for one screen: *"Design a mobile app screen for an editorial-grade magazine creation tool. It's a template gallery: a categorized tab row at the top (Travel, Food, Tech, Lifestyle, Science, Custom), a 2-column grid of magazine cover thumbnails below, a search bar, and a 'Surprise Me' button. Aesthetic: feels like Condé Nast Traveller meets a premium editorial app — confident serif display type for headers, generous white space, a single restrained accent color, no generic Material Design defaults."* Being explicit about the *mood* (which real publications it should evoke) is what keeps the output from looking generic — Stitch responds well to concrete visual references, the same way it does to an uploaded screenshot.

**Step 2 — Extract the Design DNA into a DESIGN.md.** Once you're happy with a screen (or a whole linked flow) in Stitch, tell the Antigravity agent directly: "Use the Stitch MCP to fetch the project. Extract the color palette and typography, then generate a DESIGN.md file in my root directory." This works because Stitch's design system toolkit lets you extract a design system from any project into DESIGN.md — an agent-friendly markdown file built specifically to export or import design rules to and from other design and coding tools. Review the generated file — it should contain real hex codes, font family names, spacing scale, and component rules, not vague descriptions.

**Step 3 — Implement against the DESIGN.md, not against memory or vibes.** Prompt pattern: *"Read DESIGN.md. Implement the Template Gallery screen in Jetpack Compose using these exact colors, font families, and spacing values. Do not substitute Material 3 defaults for any value that DESIGN.md specifies. If a font from DESIGN.md isn't available as a Compose-bundled font, tell me before substituting anything."* This last clause matters: the entire point of the workflow is to "Vibe Check" the implemented code against the original design using the agent's integrated browser, then prompt fixes like "the button padding is slightly off" when something doesn't match. For native Android (no integrated browser preview), substitute the Vibe Check step with: build a debug APK (Part 7), install it on your device or an emulator, take a screenshot, and visually diff it against the Stitch export side-by-side — either by eye or by feeding both images back to the agent and asking it to spot mismatches.

**Step 4 — One shared design language, enforced.** After the first 2–3 screens are built this way, have the agent extract a single Compose `Theme.kt`/`Color.kt`/`Type.kt` from the cumulative DESIGN.md rules so every later screen pulls from the same source of truth instead of re-deriving colors per-screen. This is also where Font Pair System (Feature 04) and the Stitch-derived brand palette should converge into one design system file.

**Step 5 — Reuse the design system for new screens, don't redesign from scratch each time.** When you need to add a new screen to an existing app, first extract the design system of the existing screens, then generate the new screen based on that system, to keep visual style unified. In Stitch terms: open your existing exported project, ask Stitch to "match this project's existing style" for the new screen, rather than starting a fresh, unrelated generation.

### What "good" looks like at the end of this workflow
Every screen in the final Android app should be traceable back to a specific Stitch export and a specific DESIGN.md commit. If the agent ever proposes a new screen and starts writing Compose code with inline hardcoded colors/fonts that don't appear anywhere in DESIGN.md, stop it — that's the "generic agent design" failure mode this whole section exists to prevent.

### Limitations to plan around
- Stitch doesn't manage shared component libraries or design tokens the way a mature design system tool like Figma does — DESIGN.md plus your own Compose theme file is what fills that gap for this project.
- Stitch's direct code export is HTML/Tailwind only; there is no native React, Vue, or SwiftUI export, and (per the clarification above) no native Jetpack Compose export either — Antigravity is doing real translation work here, not a mechanical conversion, so always run the Vibe Check / screenshot-diff step.
- The free tier has a monthly generation cap (on the order of a few hundred standard-mode generations) — plenty for a 6–7 screen personal app, but don't burn it iterating endlessly on minor tweaks once a screen is "good enough."


---

## PART 1 — CORE IDEA, REFINED

### What This App Actually Is

A mobile-first AI publishing tool where any person — zero design skills, zero technical knowledge — opens an app, picks a magazine style, types a topic, drops in a few photo links, and gets back a print-ready, professional-grade PDF magazine in under two minutes. The output quality matches what a professional graphic designer with InDesign would produce.

**What makes it genuinely novel:** Every existing tool (Canva, Adobe Express, FlipHTML5) outputs screen-quality web layouts. This app outputs *typographically correct, print-ready* PDFs via LuaLaTeX — the same engine professional typesetters use. Nothing on the market combines this with mobile + AI + zero skills.

> **[v3 NEW]** "Professional-grade" now applies to two separate layers of the product, and it's worth being explicit about the distinction so nothing gets confused while building: (1) the **output** — the generated magazine PDF — gets its quality from LuaLaTeX typesetting + the Design DNA system prompt (Part 10), and (2) the **app itself** — the screens the user taps through to get there — gets its quality from the Stitch MCP workflow (Part 0B). Both matter for "world-class," but they're solved by completely different tools and should never be conflated when prompting the agent. Don't ask Stitch to design the magazine; don't ask the Gemini Design DNA prompt to design the app UI.

---

### Corrected Architecture

> **[2026 UPDATE]** The original note here flagged "Antigravity 2.0" as a hallucinated, non-existent SDK and said using it would make the project unbuildable. That assessment was incorrect — Antigravity 2.0 is a real Google product (see Part 0). The correction that *does* still hold is the important one: nothing below changes because of it. Antigravity is the development tool you'll use to write this exact stack; it is not itself a layer in the diagram.

```
┌─────────────────────────────────────────────────────────────┐
│                    USER'S ANDROID PHONE                      │
│  Jetpack Compose UI (Kotlin)                                 │
│  • Template Gallery  • Live Preview Canvas  • Form Fields   │
│  • PDF Viewer        • Project Library      • Share Sheet   │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTPS/REST (Retrofit2)
                           ▼
┌─────────────────────────────────────────────────────────────┐
│               HUGGING FACE SPACES BACKEND                    │
│  FastAPI (Python)                                            │
│  ├── /generate  → calls Gemini API → returns JSON           │
│  ├── /compile   → sanitize → inject → lualatex → PDF        │
│  ├── /health    → wake-up ping endpoint                      │
│  └── /preview   → returns draft wireframe PDF (fast)        │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
   ┌─────────────────┐      ┌─────────────────────┐
   │   Gemini API    │      │  Firebase (optional) │
   │  (AI Studio     │      │  • Auth              │
   │   free tier)    │      │  • Cloud Storage     │
   └─────────────────┘      │  • Firestore         │
                            └─────────────────────┘
```

**Why this stack is better:**
- FastAPI is simple Python — you can write and debug it with AI help (Antigravity 2.0, in your case)
- No imaginary SDKs, no complex agent frameworks for MVP
- Every piece is free at your scale
- HuggingFace Spaces runs Docker containers, LuaLaTeX installs cleanly

> **[v3 NEW]** With Feature 11 (BYOK), the `/generate` arrow changes slightly in practice: the Gemini API key used for that call now comes from the user's request payload (or a short-lived session token derived from it) instead of a key baked into your HF Space's environment variables. The box diagram doesn't need a new shape for this — it's the same arrow, just carrying a different credential per user instead of one shared developer credential. Full detail in Feature 11.

---

## PART 2 — COMPLETE BUG REGISTRY

Every bug is rated: 🔴 App-Breaking | 🟠 Serious | 🟡 Moderate | 🟢 Minor

---

### SECTION A — Backend / Compilation Bugs

**Bug 01 🔴 — HF Cold Start Kills Compilation**
The free HF Space sleeps after inactivity. When a user hits Compile, the container takes 30-60 seconds to wake up. Android's HTTP client will timeout at ~30s and throw a `SocketTimeoutException`, showing a crash screen.

> **[2026 UPDATE]** The original text said this sleep happens after "~15 minutes" of inactivity. The actual current behavior for free `cpu-basic` Spaces is **48 hours** of inactivity before sleep — you cannot configure a shorter or custom sleep time on the free tier (that option only exists on paid hardware). For a personal app you check in on every day or two, this means cold starts will be rarer than the original draft assumed, but they're still guaranteed the first time you open the app after a few days away — so the fix below is still worth keeping.

*Fix:* The moment the user taps any template in the gallery (before they even type anything), the Android app fires a silent background GET to `/health`. By the time they've filled in their topic and image URL (~60 seconds), the server is awake. Also set Retrofit timeout to 180 seconds for the `/compile` endpoint specifically.

```python
# HF Backend — app.py
@app.get("/health")
async def health():
    return {"status": "awake"}
```

```kotlin
// Android — fire on template tap, not on compile
LaunchedEffect(selectedTemplate) {
    viewModel.pingBackend()  // silent, no UI feedback needed
}
```

---

**Bug 02 🔴 — LuaLaTeX Cannot Fetch Remote Images**
When your `.tex` template tries to `\includegraphics{https://...}`, LuaLaTeX does not fetch URLs. It expects a local file path. If you use `shell_escape` to `curl` the image at compile time, that works locally but HF's security sandbox often blocks outbound shell commands.

*Fix:* Pre-download ALL images in your Python FastAPI code **before** calling LuaLaTeX. Save them to `/tmp/` on the HF container, update the `.tex` file to use local paths.

```python
import requests, os, uuid
from pathlib import Path

def download_image(url: str) -> str:
    """Downloads image to /tmp, returns local path"""
    ext = ".jpg"  # always force JPG output
    local_path = f"/tmp/{uuid.uuid4()}{ext}"
    response = requests.get(url, timeout=15, 
                           headers={"User-Agent": "Mozilla/5.0"})
    response.raise_for_status()
    with open(local_path, 'wb') as f:
        f.write(response.content)
    return local_path
```

---

**Bug 03 🔴 — WebP and HEIC Images Crash LuaLaTeX**
LuaLaTeX's graphics engine only natively handles JPG, PNG, and PDF. Google Drive often serves WebP. iPhone users uploading images get HEIC. Both will cause an immediate compilation failure with cryptic errors.

*Fix:* After downloading, always convert to JPG with Pillow regardless of format.

```python
from PIL import Image

def convert_to_jpg(input_path: str) -> str:
    output_path = input_path.rsplit('.', 1)[0] + "_converted.jpg"
    with Image.open(input_path) as img:
        # Handle transparency (PNG with alpha → white background)
        if img.mode in ('RGBA', 'LA', 'P'):
            background = Image.new('RGB', img.size, (255, 255, 255))
            if img.mode == 'P':
                img = img.convert('RGBA')
            background.paste(img, mask=img.split()[-1])
            img = background
        else:
            img = img.convert('RGB')
        # Downsample to 150 DPI — sufficient for screen, reduces memory
        img.save(output_path, 'JPEG', quality=85, optimize=True)
    return output_path
```

---

**Bug 04 🔴 — LaTeX Reserved Characters Break Compilation**
A user types: `"Burgers & 100% Beef — The $5 Guide"`. The characters `& % $ # _ ^ { } ~ \` are all reserved LaTeX operators. This will cause an immediate, silent crash. Gemini will NOT reliably escape these even if you tell it to.

*Fix:* Never trust the AI or the user. Run ALL user-provided strings through a sanitizer on the backend **before** template injection.

```python
def sanitize_latex(text: str) -> str:
    """Escapes all LaTeX reserved characters in user text"""
    replacements = [
        ('\\', '\\textbackslash{}'),  # MUST be first
        ('&', '\\&'),
        ('%', '\\%'),
        ('$', '\\$'),
        ('#', '\\#'),
        ('_', '\\_'),
        ('^', '\\^{}'),
        ('{', '\\{'),
        ('}', '\\}'),
        ('~', '\\textasciitilde{}'),
    ]
    for char, replacement in replacements:
        text = text.replace(char, replacement)
    return text
```

---

**Bug 05 🔴 — Memory Exhaustion on 25-Page Magazines**
Compiling a full magazine with ~25 pages and 30+ images will exhaust HF's free tier RAM (16GB shared). LuaLaTeX loads all images into memory simultaneously. You'll get an OOM kill with no error message returned to the user.

*Fix A:* Aggressive image compression before compile (already done in Bug 03 fix — 150 DPI, quality=85 is the right target).

*Fix B:* Chunk compilation. Split a 25-page magazine into 5-page batches, compile each separately, merge with Ghostscript.

```python
import subprocess

def merge_pdfs(pdf_paths: list, output_path: str):
    cmd = ["gs", "-dBATCH", "-dNOPAUSE", "-q", "-sDEVICE=pdfwrite",
           f"-sOutputFile={output_path}"] + pdf_paths
    subprocess.run(cmd, check=True, timeout=60)
```

---

**Bug 06 🔴 — Google Drive Links Don't Work Directly**
Users will copy the standard "Share" link from Google Drive:
`https://drive.google.com/file/d/FILE_ID/view?usp=sharing`

This opens a **viewer page**, not the raw image. LuaLaTeX (and even your Python downloader) will download an HTML page instead of the image, causing a silent crash.

*Fix:* Detect Drive URLs and transform them before downloading.

```python
import re

def normalize_image_url(url: str) -> str:
    """Converts Google Drive viewer links to direct download links"""
    drive_match = re.search(r'/file/d/([a-zA-Z0-9_-]+)', url)
    if drive_match:
        file_id = drive_match.group(1)
        return f"https://drive.google.com/uc?export=download&id={file_id}"
    return url
```

Also warn users: large Drive files trigger a virus-scan warning page instead of the file. Recommend using Imgur or Firebase Storage links instead.

---

**Bug 07 🔴 — HF Container Has No LuaLaTeX Installed By Default**
HuggingFace Spaces run a base Python Docker image. LuaLaTeX is not pre-installed. Without it, your entire backend is non-functional.

*Fix:* Create a `Dockerfile` or `packages.txt` in your HF Space repo to install TeX Live.

```
# packages.txt (place in HF Space root — HF reads this automatically)
texlive-full
ghostscript
```

Warning: `texlive-full` is 4GB. Use `texlive-latex-extra` + specific packages instead to keep the container lean:

```
# packages.txt
texlive-luatex
texlive-latex-extra
texlive-fonts-extra
texlive-font-utils
ghostscript
python3-pillow
```

> **[2026 UPDATE]** Package names occasionally shift between Debian releases. Ask Antigravity to verify these exact names against the current HF Spaces base image during Phase 1 (e.g. `apt-cache search texlive-luatex`) rather than trusting any static list, this one included.

---

**Bug 08 🔴 — LuaLaTeX Compilation Has No Timeout**
If a `.tex` file has an infinite loop (which malformed TikZ code can cause), LuaLaTeX will hang forever. Your HF Space will freeze, every subsequent user request will pile up, and HF will eventually kill the whole container.

*Fix:* Always wrap lualatex calls in a subprocess with a hard timeout.

```python
import subprocess, os

def compile_latex(tex_path: str, output_dir: str) -> str:
    try:
        result = subprocess.run(
            ["lualatex", "--interaction=nonstopmode",
             "--output-directory", output_dir, tex_path],
            capture_output=True, text=True,
            timeout=90  # hard kill after 90 seconds
        )
        if result.returncode != 0:
            raise RuntimeError(f"LaTeX error:\n{result.stdout[-2000:]}")
        return os.path.join(output_dir, 
                           os.path.basename(tex_path).replace('.tex', '.pdf'))
    except subprocess.TimeoutExpired:
        raise RuntimeError("Compilation timed out — magazine may be too large")
```

---

**Bug 09 🟠 — Concurrent Users Share One HF Worker**
HF Free Spaces run a single-threaded FastAPI instance by default. If two users hit Compile simultaneously, the second request blocks until the first finishes (up to 90 seconds). The second user gets no feedback and assumes the app crashed.

*Fix A (MVP):* Add a simple request queue with feedback.
```python
from asyncio import Semaphore
compile_lock = Semaphore(1)  # only 1 compile at a time

@app.post("/compile")
async def compile_endpoint(payload: MagazinePayload):
    async with compile_lock:
        return await compile_magazine(payload)
```

*Fix B (better):* Run FastAPI with multiple Uvicorn workers. Add `--workers 2` to HF startup command. This still shares 16GB RAM so stay cautious with image sizes.

> Note: for a personal, single-user app, Fix A alone is fine — you'll basically never have two simultaneous compiles. Don't over-build this one.

---

**Bug 10 🟠 — Gemini JSON Schema Drift**
Gemini will occasionally return JSON with a missing key, an extra key, a value that's the wrong type, or (rarely) valid JSON with a markdown code block wrapper around it like:
```
```json
{ ... }
```
```

Your backend will crash trying to inject `None` into a template placeholder.

*Fix:* Dual-layer validation. Strip code fences before parsing, then validate with Pydantic.

```python
from pydantic import BaseModel
from typing import List, Optional
import json, re

class CoverPage(BaseModel):
    main_title: str
    subtitle: Optional[str] = "Untitled"
    accent_hex: str = "#D97757"
    cover_pattern: str = "clean_title_dominant"
    callouts: List[str] = []
    image_url: str

def parse_gemini_response(raw: str) -> CoverPage:
    # Strip markdown code fences if present
    cleaned = re.sub(r'```(?:json)?\n?', '', raw).strip()
    data = json.loads(cleaned)
    return CoverPage(**data)  # Pydantic validates and fills defaults
```

> **[v3 CLARIFICATION]** Part 10 expands this same dual-layer pattern to the *full-issue* JSON schema (cover + TOC + every article), not just the cover. The validation principle is identical — just more Pydantic models, one per content block — see Part 10 for the complete set.

---

**Bug 11 🟠 — TikZ Coordinate Overflow on Long Titles**
Even with an Android-side character limit of 12, certain font+size combinations can cause text to overflow its TikZ bounding box. On some languages, 12 characters can be physically wider than 12 English characters. This doesn't crash compilation — it silently bleeds text off the page.

*Fix:* Use `\resizebox` or `\adjustbox` in the template instead of fixed font sizes.

```latex
% Instead of:
\fontsize{72}{80}\selectfont \MAINTITLE

% Use:
\resizebox{0.85\paperwidth}{!}{\MAINTITLE}
% This auto-scales the text to always fit within 85% of page width
```

---

**Bug 12 🟠 — PDF Too Large to Transfer Over Mobile Network**
A 25-page magazine with full-resolution images can be 40-80MB. Sending this over mobile data will be extremely slow and may timeout. On a slow 4G connection, 80MB takes over a minute just to transfer.

*Fix:* Post-process the PDF with Ghostscript to compress it for screen delivery, then offer a separate "High Resolution" download.

```python
def compress_pdf(input_path: str, output_path: str, quality="screen"):
    # quality options: screen(72dpi), ebook(150dpi), printer(300dpi)
    subprocess.run([
        "gs", "-dBATCH", "-dNOPAUSE", "-q",
        f"-dPDFSETTINGS=/{quality}",
        "-sDEVICE=pdfwrite",
        f"-sOutputFile={output_path}", input_path
    ], check=True, timeout=60)
```

Screen quality: typically reduces 80MB → 8-12MB. Still looks excellent on phone screens.

---

**Bug 13 🟠 — HF Ephemeral Storage Loses PDFs**
HF Free Spaces have no persistent storage. Any PDF saved to the container's local filesystem disappears when the container restarts (which happens after inactivity). Users who try to re-download their magazine the next day will get a 404.

*Fix:* Either (A) always recompile on re-download request, or (B) upload the compiled PDF to Firebase Cloud Storage and give the user a permanent Firebase URL.

```python
import firebase_admin
from firebase_admin import storage

def upload_pdf_to_firebase(local_path: str, user_id: str, 
                            project_id: str) -> str:
    bucket = storage.bucket()
    blob_name = f"magazines/{user_id}/{project_id}.pdf"
    blob = bucket.blob(blob_name)
    blob.upload_from_filename(local_path, content_type='application/pdf')
    blob.make_public()
    return blob.public_url
```

---

**Bug 14 🟠 — Font Downloads Fail at Compile Time**
If your templates reference Google Fonts by download URL, and Google's font servers are slow or down during compilation, LuaLaTeX will wait for the font download and eventually timeout or produce output with missing fonts (defaulting to Computer Modern, which looks terrible).

*Fix:* Bundle the 5-6 font families you've curated directly into the HF Space repository. Pre-install them into the TeX font tree at container startup. Never rely on runtime font downloads.

```
# Your HF Space directory structure:
/fonts/
  LibreBaskerville-Regular.ttf
  LibreBaskerville-Bold.ttf
  Inter-Regular.ttf
  Inter-Bold.ttf
  Playfair-Display-Bold.ttf
  ...
```

```latex
% In your .tex template:
\setmainfont{Inter}[
    Path = /app/fonts/,
    Extension = .ttf,
    UprightFont = Inter-Regular,
    BoldFont = Inter-Bold,
]
```

---

**Bug 15 🟠 — Android Network Security Config Blocks HTTP**
Android 9+ (API 28+) blocks all cleartext HTTP connections by default. If your HF Space URL ever serves HTTP instead of HTTPS (rare but possible during HF maintenance), all requests will silently fail with a security exception that doesn't surface as a user-visible error.

*Fix:* Always use HTTPS. Also add `network_security_config.xml` to explicitly state your app only trusts HTTPS.

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">your-space.hf.space</domain>
    </domain-config>
</network-security-config>
```

---

**Bug 16 🟠 — Scoped Storage Breaks PDF Download on Android 10+**
Android 10+ (API 29+) introduced Scoped Storage. You cannot write files to arbitrary locations on the device. If you try to save the PDF to `/sdcard/Download/` using old methods, it silently fails on modern devices.

*Fix:* Use the `MediaStore` API or `DownloadManager` for saving files.

```kotlin
fun savePdfToDownloads(context: Context, pdfBytes: ByteArray, 
                        filename: String): Uri {
    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, filename)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, 
                              contentValues)!!
    resolver.openOutputStream(uri)?.use { it.write(pdfBytes) }
    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
    resolver.update(uri, contentValues, null, null)
    return uri
}
```

---

**Bug 17 🟠 — Duplicate Compile Requests on Double-Tap**
If a user taps the Compile button twice quickly (very common on mobile), two simultaneous requests fire to the backend. The user gets two PDFs (or two errors), the server handles double the load, and the UI state becomes confused.

*Fix:* Disable the Compile button immediately on first tap, re-enable only after a response or timeout.

```kotlin
var isCompiling by remember { mutableStateOf(false) }

Button(
    onClick = {
        if (!isCompiling) {
            isCompiling = true
            viewModel.compile { isCompiling = false }
        }
    },
    enabled = !isCompiling
) {
    if (isCompiling) CircularProgressIndicator(Modifier.size(16.dp))
    else Text("Generate Magazine")
}
```

---

**Bug 18 🟡 — Image Aspect Ratio Mismatch on Cover**
Magazine covers are portrait (e.g., 210×280mm — roughly 3:4 ratio). A user uploading a landscape photo (16:9) from their phone will get a cover where the image is either distorted (stretched) or shows white bars on the sides.

*Fix:* In the template, use `keepaspectratio=false` and `clip` together to always fill the full page, center-cropping the image.

```latex
\node[anchor=center] at (current page.center) {
    \includegraphics[
        width=\paperwidth,
        height=\paperheight,
        keepaspectratio=false  % stretch to fill — intentional
    ]{\HEROIMAGE}
};
```

Also add a pre-check in Python: if image is landscape, auto-rotate or center-crop it to 3:4 before passing to LuaLaTeX.

---

**Bug 19 🟡 — Gemini Free Tier Rate Limits**

> **[2026 UPDATE]** `Gemini 2.0 Flash` (named in the original text) was retired on March 3, 2026 and no longer exists as an API target. Use `gemini-2.5-flash` (or `gemini-3-flash` once it's out of preview) instead. Current free-tier behavior as of mid-2026: Flash and Flash-Lite models remain free with roughly 10-15 requests/minute and ~1,000-1,500 requests/day (figures move around, check Google AI Studio's live quota panel for your project rather than trusting any fixed number — it changes). Pro models were moved behind a paywall in April 2026, so don't reach for Pro on the free plan. None of this changes the fix below — it's the same pattern, just point it at a current model and the correct exception import for your SDK version.

> **[v3 CLARIFICATION]** As of the v3 revision date, the model lineup has moved again — `gemini-3.5-flash`, `gemini-3-flash-preview`, and `gemini-3.1-flash-lite` are now in active rotation alongside `gemini-2.5-flash`. This is precisely why Part 6's A2 entry and Part 8's version-verification protocol exist: don't hardcode a model name from this document (or any document) without checking Google AI Studio's current model list first. With Feature 11 (BYOK) shipping in v3, this also matters more than before — *each user's own key* determines which models they can actually call, so the backend should request model availability dynamically rather than assuming every user has access to the exact same model your dev key does.

Gemini's free tier has rate limits. For a personal app this is usually fine. But if you share the app with friends, a burst of simultaneous users hitting Generate will hit the limit and return 429 errors with no useful message to the user.

*Fix:* Catch 429 errors specifically and show a friendly message.

```python
import google.generativeai as genai
from google.api_core.exceptions import ResourceExhausted

try:
    response = model.generate_content(prompt)
except ResourceExhausted:
    raise HTTPException(429, "AI is busy right now — please wait 30 seconds and try again")
```

---

**Bug 20 🟡 — LuaLaTeX Runs Twice By Default**
LuaLaTeX needs to run twice to resolve cross-references (table of contents page numbers, etc.). If you only run it once, the TOC will show `??` instead of actual page numbers. Many backend implementations miss this.

*Fix:* Always run lualatex twice in sequence.

```python
for _ in range(2):
    subprocess.run(["lualatex", "--interaction=nonstopmode",
                   "--output-directory", output_dir, tex_path],
                  timeout=90, check=True)
```

---

**Bug 21 🟡 — Push Notifications Require Permission on Android 13+**
Android 13 (API 33+) requires explicit runtime permission for push notifications. If you add async compilation with push notification delivery, the notification will silently fail on modern devices unless you request `POST_NOTIFICATIONS` permission first.

*Fix:*
```kotlin
// In your Activity or first-launch flow
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        NOTIFICATION_PERMISSION_CODE
    )
}
```

---

**Bug 22 🟡 — Non-English Characters Break LaTeX Input Encoding**
A user types a title in Arabic, Chinese, or Bengali. Standard LaTeX encoding will either produce garbage characters or crash. LuaLaTeX supports Unicode natively, but only if the font being used contains those characters. Your curated fonts (Libre Baskerville, Inter, etc.) don't have Arabic or Bengali glyphs.

*Fix:* Detect non-Latin scripts on the Android side and auto-switch to a font that supports that script.

```kotlin
fun detectScript(text: String): String {
    return when {
        text.any { it.code in 0x0600..0x06FF } -> "arabic"    // Arabic
        text.any { it.code in 0x0980..0x09FF } -> "bengali"   // Bengali
        text.any { it.code in 0x4E00..0x9FFF } -> "cjk"      // Chinese
        else -> "latin"
    }
}
```

Then pass `script` in the JSON payload and your template switches fonts accordingly.

---

**Bug 23 🟡 — Template Thumbnails Are Static Images That Go Stale**
Your 15 template carousel shows thumbnail images of each template style. These are pre-made screenshots bundled into the APK. If you update a template's design, the thumbnails go stale and no longer match what the user actually gets.

*Fix:* Generate thumbnails programmatically. When you first deploy a template, have your HF backend compile a sample page with dummy content and store the first page as a PNG thumbnail in Firebase Storage. Pull thumbnails from Firebase, not from the APK bundle.

---

**Bug 24 🟡 — Android Back Button Loses Unsaved Work**
User fills in all their magazine details, taps the back button accidentally, and loses everything. Jetpack Compose's default back behavior pops the screen without confirmation.

*Fix:* Intercept the back button when there's unsaved work.

```kotlin
BackHandler(enabled = hasUnsavedChanges) {
    showDiscardDialog = true  // "Discard changes?" dialog
}
```

---

**Bug 25 🟡 — HF Space URL Exposes API Endpoint Publicly**
Your HF Space URL is public. Anyone who finds it can call `/compile` with unlimited requests, exhausting your free tier quota, running up HF compute, and draining your Gemini API quota.

*Fix:* Add a secret API key header that only your Android app knows.

```python
# Backend
from fastapi.security import APIKeyHeader
from fastapi import Security, HTTPException

API_KEY = os.environ["APP_SECRET_KEY"]  # set in HF Space secrets
api_key_header = APIKeyHeader(name="X-App-Key")

@app.post("/compile")
async def compile_endpoint(payload: MagazinePayload,
                           key: str = Security(api_key_header)):
    if key != API_KEY:
        raise HTTPException(403, "Unauthorized")
    ...
```

```kotlin
// Android — add to every request
.addHeader("X-App-Key", BuildConfig.APP_SECRET_KEY)
```

Store the key in `local.properties` → inject into `BuildConfig` at build time. Never hardcode in source. See Part 0's "guardrails" note — this matters even more when an AI agent is the one writing the code.

> **[v3 CLARIFICATION]** This `X-App-Key` header authenticates *your app* to *your backend* — it has nothing to do with the per-user Gemini key from Feature 11. Once Feature 11 ships, a single `/compile` or `/generate` request will typically carry **two** distinct credentials: the `X-App-Key` header (proves the request came from your real app, not a scraped copy of your API) and the user's own Gemini key (proves they're allowed to call Gemini, and pays for their own usage). Keep them in separate headers and never let one substitute for the other.

---

**Bug 26 🟢 — PDF Viewer Opens in External App**
When the user gets their PDF, opening it via `ACTION_VIEW` intent launches whatever PDF viewer the user has installed (which may not exist on older devices, causing a crash).

*Fix:* Bundle an in-app PDF renderer using the Android `PdfRenderer` API (built-in, no dependency needed).

---

**Bug 27 🟢 — Compilation Log Is Invisible to User**
A 60-second wait with a spinner is unbearable on mobile. Users will tap Back, thinking it crashed.

*Fix:* Stream friendly status messages from the backend using Server-Sent Events (SSE).

```python
# Backend — stream progress to Android
from fastapi.responses import StreamingResponse
import asyncio

async def compile_with_progress(payload):
    yield "data: Waking up the layout engine...\n\n"
    await asyncio.sleep(1)
    yield "data: Downloading your images...\n\n"
    # ... actual download ...
    yield "data: Setting typography...\n\n"
    # ... actual compile ...
    yield "data: Polishing final PDF...\n\n"
    yield f"data: DONE:{pdf_url}\n\n"
```

---

### SECTION C — New Bugs Introduced By v3 Features **[v3 NEW]**

These are net-new failure modes that didn't exist in v1/v2 because the features that cause them (BYOK, Stitch MCP, GitHub Actions CI/CD) are new in v3. Same severity scale as above.

---

**Bug 28 🔴 — Unverified Gemini Key Reaches `/generate` and Fails Mid-Compile**
If the Android app lets a user paste a key and immediately starts a 60-90 second compile job without checking the key first, an invalid/revoked/wrong-scope key fails *deep inside* the pipeline — after images have already been downloaded, after the user has waited 40+ seconds — and the resulting error is confusing ("Compilation failed" tells them nothing about their key being wrong).

*Fix:* Never let an unverified key reach `/generate` or `/compile`. Verify synchronously the moment the user finishes typing/pasting it (see Feature 11's `/verify-key` endpoint), before it's ever stored or used for a real generation. This is the literal feature you asked for in v3 — full implementation in Feature 11.

---

**Bug 29 🟠 — Standard (Unrestricted) Gemini Keys Get Silently Rejected**
Google is actively migrating the Gemini API away from old-style "Standard" API keys toward "Auth" keys. Starting June 19, 2026, the Gemini API rejects requests from unrestricted Standard keys — only Standard keys with explicit restrictions, or newly-issued Auth keys, continue to work; by September 2026, all Standard keys are rejected outright. A user who copies an old key they made a year ago (common, since people don't usually regenerate keys) can get a hard rejection on a key that "looks" syntactically valid and even passes a naive format check.

*Fix:* Your `/verify-key` endpoint (Feature 11) must do a real network call against the Gemini API, not just check the key's string shape — that's the only way to catch this. Surface Google's actual error text ("standard key" rejections come back with a distinct error message) and tell the user directly: *"This looks like an older API key type Google is retiring. Open AI Studio → API Keys, and create a new key — new keys are automatically the supported type."* Don't try to silently work around this; direct the user to generate a current key. See Part 6, A10 for the full timeline and why this matters specifically for a BYOK app.

---

**Bug 30 🟠 — Stitch-Derived Design Tokens Drift From the Implemented App**
The DESIGN.md the agent extracts from Stitch (Part 0B) is a snapshot. If you go back into Stitch later and tweak a screen — change an accent color, adjust spacing — and forget to re-extract DESIGN.md and re-prompt the agent, your Compose app and your "source of truth" design silently diverge. Six months in, nobody can tell which one is actually correct anymore.

*Fix:* Treat DESIGN.md as a versioned file in git, same as code. Every time you meaningfully change a screen in Stitch, immediately re-run the extraction step and commit the new DESIGN.md in the same commit as the resulting Compose changes, with a commit message that says what changed and why. Never let DESIGN.md go more than one work session out of sync with what Stitch currently shows.

---

**Bug 31 🟠 — GitHub Actions Build Succeeds But the APK Was Never Actually Installed/Run**
A CI workflow that ends at "Build APK" → green checkmark gives a false sense of "it works." A build can succeed (compiles, produces a valid APK) while the app still crashes on launch, can't reach the backend, or has a broken screen — because compiling Kotlin and *running* the app are different kinds of correctness, and GitHub Actions' default Android workflow only proves the first one.

*Fix:* Don't treat "build succeeded" as "done." Part 8's verification protocol and Part 7's CI workflow both require an explicit install-and-launch check (either on a real device you own, or via an Android emulator step in the CI job for automated smoke tests) before any phase is marked complete — see Part 7 for the emulator-based smoke-test job and Part 8 for the Definition of Done that makes this non-optional.

---

**Bug 32 🟡 — Mismatched Dependency Versions Compile Individually, Fail Together**
Each library in this stack (AGP, Gradle, Kotlin, Compose BOM, Retrofit, Coil, Room, Firebase BoM) has its own minimum-compatible-version requirements against the others. An agent that adds "the latest Retrofit" today and "the latest Compose BOM" next week, without checking they're still mutually compatible, can produce a project where every individual `./gradlew` task looks fine in isolation but the full assemble fails with a deep, unhelpful Gradle resolution error, or — worse — builds successfully but crashes at runtime with a `NoSuchMethodError` from a binary-incompatible transitive dependency.

*Fix:* This is exactly what Part 8's version-compatibility protocol exists to prevent — never add or bump a single dependency without checking it against the current versions of everything else in the same build (the Gradle/Kotlin/AGP/Compose BOM constellation, specifically, is the highest-risk combination in this project). Use a single Compose BOM and a single Gradle version catalog (`libs.versions.toml`) so versions are declared once, in one place, and checked together — not scattered across a dozen `build.gradle.kts` lines that each get bumped independently over time.


---

## PART 3 — FEATURE REFINEMENTS

### Feature 01 — Template Gallery (Refined)
**Current plan:** Horizontal scrollable row of 15-20 thumbnails.

**Problems:** Thumbnails are too small to see detail on mobile. No way to search or filter. 15 items in a row is overwhelming.

**Refined:**
- Categorized tab row at top: Travel | Food | Tech | Lifestyle | Science | Custom
- 2-column grid below, not horizontal scroll — shows more per screen
- Tapping a template opens a full-screen "Preview Mode" showing 3 sample pages
- Search bar at top: "travel coastal" finds Travel + Coastal persona
- "Surprise Me" button picks randomly from highest-rated templates

> **[v3 NEW]** This is the first screen on the recommended Stitch design list in Part 0B — design it there before any Compose code is written for it.

---

### Feature 02 — Image Input (Completely Replaced)
**Current plan:** User pastes a Google Drive URL into a text field.

**Problems:** Drive links require format transformation. Users don't know how to get the right link. It's fragile, error-prone, and feels technical.

**Refined:**
- Primary: Photo picker opens the phone's gallery directly
- App uploads to Firebase Storage (or Imgur API — free, requires a one-time registered Client-ID for anonymous uploads) automatically
- Returns a clean, stable direct URL automatically
- Secondary option: Paste any URL (with smart Drive link detection + transformation)
- Image is shown as thumbnail immediately so user knows it uploaded

> **[2026 UPDATE]** The original text called Imgur's anonymous upload "no auth required" — it isn't, quite. You still register once for a free `Client-ID` at api.imgur.com and send it as an `Authorization: Client-ID ...` header; you just don't need a full OAuth login flow. Given you already need Firebase Storage for Bug 13 (permanent PDF storage), it's simplest to skip Imgur entirely and use Firebase Storage for images too — one less moving part. See Part 6, A7.

```kotlin
// Firebase Storage auto-upload
val storageRef = Firebase.storage.reference
    .child("user_images/${UUID.randomUUID()}.jpg")
storageRef.putFile(imageUri)
    .addOnSuccessListener { 
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            viewModel.setImageUrl(uri.toString())
        }
    }
```

---

### Feature 03 — Live Preview Canvas (Refined)
**Current plan:** Jetpack Compose canvas that mirrors template coordinates.

**Refined design:**
- Canvas takes up 60% of screen height, form fields in bottom sheet that slides up
- Real-time font size counter: "12/12 characters" below the title field
- If user exceeds character limit, the input field flashes red and refuses more input — no silent cropping
- Color picker shows a mini preview of the cover recoloring live
- Swipe left/right on the preview to see Page 1 (cover), Page 2 (TOC placeholder), Page 3 (article placeholder)

---

### Feature 04 — Font Pair System (New)
**Current plan:** Not explicitly designed.

**Refined:**
Offer exactly 5 named curated pairings. Names describe the feel, not the font names (users don't know font names):

| Name | Display Font | Body Font | Mood |
|------|-------------|-----------|------|
| Classic Luxury | Playfair Display Bold | EB Garamond | Timeless, editorial |
| Modern Editorial | Libre Baskerville | Inter | Clean, professional |
| Bold Statement | Bebas Neue | Lato | Energetic, impactful |
| Soft Lifestyle | Cormorant Garamond | Nunito | Warm, approachable |
| Tech Precision | Space Grotesk Bold | IBM Plex Sans | Sharp, technical |

Show a preview of each pairing using the user's actual title text, not dummy text.

> **[v3 CLARIFICATION]** This table is also exactly the kind of "design token" content that belongs in DESIGN.md (Part 0B). When the agent extracts the Stitch design for the Font Pair picker screen, cross-check that the font *names* DESIGN.md lists match this table — if Stitch's visual mockup used a placeholder web font that isn't actually one of these five families, that's the agent's cue to substitute the correct one rather than silently bundling a sixth, uncurated font into the APK.

---

### Feature 05 — Draft Mode vs Final Mode (New)
**Current plan:** One compile button.

**Refined:**
Two buttons side by side:
- **Quick Preview** — compiles in ~4 seconds with image placeholders (wireframes), no real images downloaded. Used for checking text flow and layout.
- **Generate Final** — full quality, downloads images, full typography. Takes 30-90 seconds.

This is implemented in LuaLaTeX using the `draft` option:
```latex
\documentclass[draft]{article}
% In draft mode, \includegraphics shows a box with filename, doesn't load image
```

---

### Feature 06 — Smart Error Messages (New)
**Current plan:** Show a generic "Compilation failed" error.

**Refined:** Parse the LuaLaTeX error log and translate it to human language.

```python
def parse_latex_error(log: str) -> str:
    if "File" in log and "not found" in log:
        return "One of your images couldn't be loaded. Check that your image links are publicly accessible."
    if "Undefined control sequence" in log:
        return "A special character in your text caused an issue. We've fixed it — tap Generate again."
    if "LaTeX Error: Environment" in log:
        return "A layout configuration issue occurred. Please try a different template style."
    if "TeX capacity exceeded" in log:
        return "Your magazine is too large for a single compile. Try reducing the number of pages."
    return "Something unexpected went wrong. Your content has been saved — tap Generate to retry."
```

> **[v3 NEW]** Add one more branch here for the BYOK era: if the upstream Gemini call itself failed (not the LaTeX compile), don't route that error through `parse_latex_error` at all — it'll never match any of these patterns and will fall through to the unhelpful generic message. Catch Gemini/key errors at the `/generate` layer (Feature 11) and translate *those* separately, before the request ever reaches the LaTeX compile step.

---

### Feature 07 — Project Library (New)
**Current plan:** Local JSON files stored on device.

**Refined:**
A "My Magazines" tab showing:
- Cover thumbnail (first page rendered as PNG)
- Title and date created
- Page count and template used
- Status: Draft / Generating / Complete
- Swipe left to delete, swipe right to duplicate

Storage: local Room database for metadata + SQLite. PDF files in app-private storage or Firebase Storage for persistence across device reinstalls.

---

### Feature 08 — Persona Extraction Workflow (Clarified)
This is not a user-facing feature — it's your one-time setup work as the developer.

**How to do it:**

1. Download 20-25 professional magazine PDFs (National Geographic, Condé Nast Traveller, Wired, etc.)
2. Convert each page to a PNG image: `pdftoppm -r 150 magazine.pdf page`
3. Feed page images to Gemini Vision with this prompt:

```
Analyze this magazine page and output a JSON Design Persona:
{
  "layout_pattern": "clean_title_dominant|callout_heavy|typographic_led",
  "typography": {
    "headline_weight": "ultra_bold|bold|medium",
    "headline_style": "serif|sans_serif|display",
    "body_style": "serif|sans_serif",
    "size_contrast": "extreme|high|moderate"
  },
  "color": {
    "accent_count": 1-3,
    "accent_tone": "warm|cool|neutral|vibrant",
    "background_treatment": "full_bleed|framed|bordered"
  },
  "text_placement": "sky_zone|overlay_bar|bottom_strip|border_frame",
  "mood": ["elegant","bold","minimal","editorial","energetic"]
}
Output JSON only.
```

4. Collect these JSONs → distill patterns → write your Design DNA system prompt
5. This entire workflow is done by you offline, once, before launch

> **[NEW NOTE]** This is also a perfect Antigravity task — give it the folder of page PNGs and the prompt above, and have it batch-call the Gemini API, collect the JSON outputs into one file, and summarize the patterns it sees. Saves you doing this by hand 20+ times.

> **[v3 CLARIFICATION — this feature is now massively expanded]** The single generic prompt above was always meant as a sketch, not a finished tool. v3 delivers the actual thing: **Part 11** contains five full, expert-level analysis prompts (cover, table of contents, interior article spread, typography/color system, image treatment) plus a meta-synthesis prompt that merges everything into one final Design DNA — ready to copy-paste today, no further drafting needed on your end. Part 10 explains the theory connecting this extraction step to the live `/generate` system prompt the running app actually uses. Treat the prompt in this section as superseded by Part 11; it's kept here only so you can see what it grew out of.

---

### Feature 09 — Async Compilation with Notifications (New)
For the final high-quality compile (which can take 60-90 seconds), don't make the user stare at a loading screen.

1. Android sends compile request → server returns a `job_id` immediately (< 1 second)
2. Android dismisses loading screen, shows "Your magazine is being crafted..."
3. Server compiles in background, uploads PDF to Firebase
4. Server calls Firebase Cloud Messaging to push a notification
5. User sees: "Your Tokyo Food Guide is ready! 🎉" as a push notification
6. Tapping opens the app directly to the PDF viewer

---

### Feature 10 — Content Generation (What Goes INSIDE the Magazine)
**This was missing from your architecture discussions entirely and it's critical.**

A magazine isn't just a cover. It has articles, body text, captions, pull quotes, a table of contents. Where does all that text come from?

**Proposed flow:**
1. User sets page count (4, 8, 12, 16, or 24 pages — only multiples of 4, because magazines work in signatures)
2. Gemini generates a full editorial plan: article titles, section names, page assignments
3. Gemini then generates body copy for each article (~300-500 words per article)
4. User can edit any text in the human-readable form fields before compile
5. User can also provide their own text by pasting into the fields

This is a significant addition to the Gemini system prompt — it needs to output:
- Cover JSON (already planned)
- Table of Contents JSON (page titles + page numbers)
- Per-article JSON (headline, subheadline, body text, pull quote, image URL)

> **[v3 CLARIFICATION]** Part 10 is the full expansion of this feature: what a complete magazine issue actually contains end to end, the full multi-block JSON schema behind it, and the exact system-prompt engineering needed so Gemini's output never breaks the pipeline described above. Read Feature 10 as the original problem statement and Part 10 as its complete solution.

---

### Feature 11 — Bring-Your-Own Gemini API Key, With Verification **[v3 NEW]**

**The ask:** instead of you paying for/supplying one shared Gemini API key, each user enters their *own* key, and the app must confirm the key actually works before letting them generate anything.

**Why this is the right call for this project:** it removes you as a single point of quota exhaustion or billing exposure if you ever share the app beyond yourself, and it sidesteps Bug 19/Bug 09 almost entirely — each user is bound by their own rate limits, not a shared one.

**The flow:**
1. First-launch (or Settings) screen: a single text field, "Paste your Gemini API key," with a link/button "Don't have one? Get a free key" that opens `https://aistudio.google.com/apikey` in the browser.
2. The moment the user taps a "Verify" button (don't auto-verify on every keystroke — that burns quota and looks broken), the app sends the key to your backend's new `/verify-key` endpoint.
3. `/verify-key` makes one cheap, real call to Google — **not** a content-generation call (that costs tokens and is overkill for a yes/no check). The correct lightweight check is a `GET` to the `models.list` endpoint, which returns the list of models available to that key (and a clean error if the key is bad), at no generation cost:

```python
import httpx
from fastapi import HTTPException

GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"

async def verify_gemini_key(user_key: str) -> dict:
    """
    Calls Gemini's models.list endpoint as a free, lightweight key check.
    Returns which models the key can actually use — needed anyway, since
    not every user's key/project has access to the same model set (Bug 19 note).
    """
    async with httpx.AsyncClient(timeout=10) as client:
        resp = await client.get(
            GEMINI_MODELS_URL,
            headers={"x-goog-api-key": user_key},
        )
    if resp.status_code == 200:
        models = [m["name"] for m in resp.json().get("models", [])
                  if "generateContent" in m.get("supportedGenerationMethods", [])]
        flash_models = [m for m in models if "flash" in m.lower()]
        if not flash_models:
            return {"valid": True, "warning": "Key works, but no Flash model is available on it."}
        return {"valid": True, "models": flash_models}
    if resp.status_code == 403:
        raise HTTPException(400, "That key was rejected by Google. It may be invalid, revoked, or restricted to a different API.")
    if resp.status_code == 400:
        raise HTTPException(400, "That doesn't look like a valid Gemini API key. Double check you copied the whole thing.")
    raise HTTPException(502, "Couldn't reach Google to verify the key — check your connection and try again.")

@app.post("/verify-key")
async def verify_key_endpoint(payload: dict):
    return await verify_gemini_key(payload["gemini_api_key"])
```

```kotlin
// Android — verify before saving, never save an unverified key
suspend fun onVerifyTapped(rawKey: String) {
    isVerifying = true
    try {
        val result = api.verifyKey(VerifyKeyRequest(rawKey))
        if (result.valid) {
            secureStorage.saveGeminiKey(rawKey)   // see storage note below
            verifyState = VerifyState.Success(result.models)
        }
    } catch (e: HttpException) {
        verifyState = VerifyState.Failure(e.parseFriendlyMessage())
    } finally {
        isVerifying = false
    }
}
```

4. Only once `/verify-key` returns success does the app store the key locally (Android `EncryptedSharedPreferences` / Jetpack Security, never plain `SharedPreferences`) and unlock the Compile flow. Per Bug 28, a key must never reach `/generate` or `/compile` unverified.
5. Every subsequent `/generate` call sends this key as a header (e.g. `X-User-Gemini-Key`), and your backend uses *that* key for the Gemini call — never your own developer key — for that user's request.

**Why a backend proxy at all, instead of calling Gemini directly from the Android app?** Two reasons: (a) you still want the LaTeX sanitizer (Bug 04) and Pydantic validation (Bug 10) to run server-side regardless of whose key generated the content, and (b) it keeps the Gemini key off the open internet in transit to a third party — it goes Android → your HTTPS backend → Google, the same trusted hop your own dev key already takes today, just parameterized per-user instead of fixed.

**Critical real-world constraint you must design around — read this before building:** Google is in the middle of retiring an entire class of Gemini API keys. See **Part 6, A10** for the full timeline; the short version folds directly into the verification logic above: a key can be syntactically well-formed and still get rejected for reasons that have nothing to do with the user mistyping it. `/verify-key`'s job is to catch this *before* generation, with a message that tells the user what to actually do about it (go make a new key), not just "invalid key."

---

### Feature 12 — Stitch-Driven UI Generation, Enforced **[v3 NEW]**

**The ask:** instead of the coding agent designing generic default screens inside the app, it should use the Stitch MCP server to actually generate every visual part of the app, and implement that.

**This feature is really a process rule, not a runtime feature** — there's nothing in the shipped APK that "is" Feature 12. It's the enforcement mechanism for Part 0B, expressed as a feature so it has a checkbox in your tracking the same as everything else. The rule, stated plainly: **no Compose screen gets written from a blank prompt.** Every screen's first implementation must cite a specific Stitch export / DESIGN.md commit it's implementing. If you (or the agent) catch yourselves about to write `Color(0xFF6750A4)` (Material 3's stock purple — the textbook "didn't actually design anything" tell) inline in a Composable, stop and go back to Part 0B.

**Acceptance check for this feature, per screen:**
- [ ] Screen has a corresponding Stitch project/export you can point to
- [ ] DESIGN.md was extracted (or re-extracted, if the Stitch design changed) immediately before implementation
- [ ] The implemented Compose screen's colors, fonts, and spacing trace back to DESIGN.md values, not Compose/Material defaults
- [ ] A side-by-side screenshot comparison (Stitch export vs. running app) was done and any drift was deliberately fixed, not left

---

### Feature 13 — The "World-Class" Bar, Defined **[v3 NEW]**

"World-class software" is subjective unless you pin it to something concrete the agent can actually check against. Use this as the working definition for both halves of the product:

**For the app UI (judged against Part 0B's Stitch workflow):**
- No two screens use a color that doesn't appear in the shared `Color.kt` derived from DESIGN.md
- Typography uses exactly the curated font pairings (Feature 04) — never a system/default font appearing unintentionally
- Every interactive element has a deliberate pressed/disabled/loading state (not just the Compose default ripple) — verify this explicitly, it's the detail that most separates "an AI scaffolded this" from "a designer touched this"
- Empty states, error states, and loading states were each designed once in Stitch, not improvised in code when the agent realizes mid-implementation that it forgot one exists

**For the output magazine (judged against Part 10's Design DNA system):**
- A non-designer should not be able to tell the output PDF was AI-generated, beyond it being personalized to their topic
- Typographic hierarchy (title vs. subhead vs. body vs. caption vs. pull quote) is always visually distinct and consistent within one issue
- No orphaned single words on their own line at a column break, no widow/orphan paragraph lines, no images bleeding incorrectly off a trim edge unless deliberately full-bleed

Re-run this checklist explicitly at the end of Phase 4 (templates) and Phase 6 (polish) in the build order — see Part 8 for where this slots into the verification protocol.


---

## PART 4 — FINAL REFINED ARCHITECTURE STACK

| Layer | Technology | Why |
|-------|-----------|-----|
| Android UI | Jetpack Compose (Kotlin) | Modern, declarative, animation-friendly |
| Android state | ViewModel + StateFlow | Clean reactive state management |
| Android HTTP | Retrofit2 + OkHttp | Mature, well-documented, handles timeouts |
| Android images | Coil 3 | Kotlin-first async image loading |
| Android local DB | Room (SQLite) | Project history, preferences |
| Android PDF view | PdfRenderer (built-in) | No extra dependency |
| Android secure storage | EncryptedSharedPreferences (Jetpack Security) | **[v3 NEW]** Stores the user's own Gemini key (Feature 11) — never plain SharedPreferences |
| Backend | FastAPI (Python) | Simple, fast, great for async work |
| AI | **Gemini 2.5 Flash** *(2026 update — was "2.0 Flash," retired March 2026)* | Best free-tier reasoning for code+content |
| Typesetting | LuaLaTeX (TeX Live) | Professional print quality |
| Backend host | HuggingFace Spaces | Free, Docker-based, easy deploy |
| Image CDN | Firebase Storage *(2026 update — see Feature 02; drop Imgur to simplify)* | Already in stack for PDFs; one less service to manage |
| Cloud storage | Firebase Storage (free tier) | Stores compiled PDFs permanently |
| Auth (optional) | Firebase Auth | Google Sign-In, one-tap setup |
| PDF merge | Ghostscript | Bundled with TeX Live, no extra install |
| Notifications | Firebase Cloud Messaging | Free, reliable push delivery |
| **Dev tool** | **Google Antigravity 2.0** | The agent you vibe-code all of the above with — not a runtime dependency, see Part 0 |
| **Design tool** *(new row)* | **Google Stitch + Stitch MCP server** | Generates the actual UI design for every screen; Antigravity implements against its DESIGN.md export — not a runtime dependency either, see Part 0B |
| **CI/CD** *(new row)* | **GitHub Actions** | Builds (and optionally signs) the Android APK/AAB in the cloud on every push/tag — no local Android Studio/SDK install required, see Part 7 |

---

## PART 5 — BUILD ORDER (Realistic Roadmap)

> **[v3 CLARIFICATION]** This Part is preserved exactly as v2 wrote it — it's still correct as a *high-level* roadmap. For v3's modularity requirement (every step broken down as far as it needs to go, however many micro-steps that takes), use **Part 12**, which re-expresses every phase below as a fully numbered, hyper-granular checklist with an explicit verification action attached to each micro-step. Read this Part first for the big picture, then build from Part 12 day-to-day.

### Phase 1 — Backend First (Weeks 1-2)
Get LuaLaTeX compiling a single hard-coded cover page on HF Spaces. No AI, no Android yet. Just prove the compilation pipeline works end-to-end.

1. Set up HF Space with packages.txt
2. Write one `cover_template_a.tex` with `{{PLACEHOLDERS}}`
3. Write the Python injection + compile + return PDF endpoint
4. Test by calling the API from your browser (Swagger UI) — or have Antigravity's Browser Subagent do this for you and report back
5. Verify PDF output opens correctly

**Success criteria:** You can POST a JSON to your HF endpoint and get a real PDF back.

---

### Phase 2 — Add Gemini (Week 3)
Connect Gemini to the pipeline. The AI generates the JSON that fills the template.

1. Write the Design DNA system prompt
2. Add the `/generate` endpoint that calls Gemini (use `gemini-2.5-flash`) and returns design JSON
3. Add Pydantic validation + LaTeX sanitizer
4. Test: send a topic → get JSON → compile → get PDF

**Success criteria:** "A moody travel guide to Iceland" produces a real magazine cover PDF.

---

### Phase 3 — Android MVP (Weeks 4-5)
Build the minimum viable Android app. One screen, no frills.

1. Single screen: text field for topic, image URL input, Compile button
2. Retrofit integration to your HF backend
3. Show the returned PDF using PdfRenderer
4. Basic error handling

**Success criteria:** The app works on your phone, end-to-end.

---

### Phase 4 — Template System (Weeks 6-7)
Add the 15 templates and the gallery UI.

1. Build 3 cover templates (Pattern A, B, C) as `.tex` files
2. Build the template_config.json system
3. Add the template gallery carousel in Android
4. Add the live Compose preview canvas
5. Add human-readable form fields

---

### Phase 5 — Interior Pages (Weeks 8-10)
This is the largest phase. Add table of contents, articles, and multi-page compilation.

1. Build `toc_template.tex`, `article_two_column.tex`, `article_three_column.tex`
2. Extend Gemini prompt to generate full editorial content
3. Add chunked compilation + Ghostscript merge
4. Add Draft Mode (wireframe preview)

---

### Phase 6 — Polish (Weeks 11-12)
1. Smart error messages
2. Push notifications for async compile
3. Project library screen
4. Firebase integration for PDF storage
5. Image picker (gallery upload instead of URL)
6. API key security

> **[v3 CLARIFICATION]** "API key security" in step 6 originally meant just Bug 25's `X-App-Key` header. In v3 it also means everything in Feature 11 (BYOK + verification) and Bug 28/29 (never let an unverified or soon-to-be-rejected key type reach generation). Treat this checklist item as expanded to cover both.

---

## PART 6 — ADDITIONAL GAPS FILLED

**A1 — Antigravity 2.0 reality check.** Covered fully in Part 0. Short version: it's real, it's free for individuals, and it's the IDE/agent you build with — it doesn't appear anywhere in your shipped app's code or architecture.

**A2 — Gemini 2.0 Flash no longer exists.** Retired March 3, 2026. Every code sample in this document that calls the API should target `gemini-2.5-flash` (or `gemini-3-flash` if you want to try the newer preview model — check current availability in AI Studio, preview models can change). This is a one-line swap but it matters: code generated by an AI agent (or by an older AI conversation, like the one that produced your original draft) may default to outdated model names from its training data.

**A3 — HF free-tier sleep is 48 hours, not 15 minutes.** Corrected in Bug 01. Practical effect: if you use the app most days, you'll rarely see a cold start. If you want zero cold starts ever, HF's paid "always-on" hardware starts around $0.03-0.05/hr for the smallest CPU tier — almost certainly not worth it for a personal project, but worth knowing it exists if it ever bothers you.

**A4 — EU/UK/Switzerland data rule.** If you (or anyone you share this with) are using the Gemini API from the EEA, Switzerland, or the UK, Google's terms require the **paid** tier rather than the free AI Studio tier for that usage. If this is purely personal use from outside those regions, the free tier is fine — just flagging it since the architecture leans on "free tier" throughout.

> **[v3 CLARIFICATION]** This is now the user's own concern as much as yours, since each user supplies their own key (Feature 11). Consider a one-line note on the key-entry screen mentioning this rule, so a user in the EEA isn't surprised when their free-tier key behaves unexpectedly for in-region usage.

**A5 — Optional simpler architecture, if you want to cut real build time.** A native Android app (Jetpack Compose, Gradle, Retrofit, Room, signing, eventually a Play Store listing if you ever want to install it without a USB cable) is a lot of moving parts for a personal project, even with an agent writing the code. Since this is solo/personal/free, consider:
- Build the frontend as a small responsive **web app** (plain HTML/JS or a lightweight framework) served from the *same* FastAPI backend on Hugging Face Spaces.
- Open it on your phone's browser and "Add to Home Screen" — it behaves like an app icon, no Play Store, no Gradle, no APK signing.
- This collapses the whole project into one codebase Antigravity can build in a single project folder, and removes Bugs 15, 16, 21, and 26 entirely (those are all Android-platform-specific).
- Trade-off: you lose true offline use and native push notifications (Feature 09 would become a simple "check back in a minute" instead). For a personal tool, that's a reasonable trade.
This is optional — the original native Android plan is sound and everything in Parts 1-5 is written for it. Mentioning this because "personal use, built solo with an AI agent" is exactly the situation where the lighter-weight option tends to be worth it.

> **[v3 CLARIFICATION]** If you do take this web-app path, Part 0B's Stitch workflow actually gets *easier*, not harder — Stitch's primary, most mature export format is HTML/Tailwind, and Antigravity's browser-based "Vibe Check" (literally opening the running app in a real browser and comparing it pixel-for-pixel to the Stitch design) was built with exactly this kind of target in mind. The native Compose path in Part 0B still works (that's what's written there in full), it just requires the extra translation step; a web app skips it.

**A6 — Secrets hygiene with a coding agent.** Expanded in Part 0. The short version: an agent that can read/write files and run shell commands will, if you're not careful, paste your real API key into a commit, a log, or its own chat context. Use `.env` / `local.properties` / HF Secrets UI for every credential, gitignore them before the first commit, and never type a real key value directly into a prompt.

**A7 — Drop Imgur, use Firebase Storage for images too.** You already need Firebase Storage for PDFs (Bug 13) and pushes (Feature 09). Routing image uploads through it as well (instead of Imgur) means one Google account, one SDK, one set of credentials to manage — simpler for a solo build, and avoids the Imgur Client-ID registration step and its upload rate limits entirely.

---

### v3 Additional Gaps **[v3 NEW]**

**A8 — Stitch's real limits, stated plainly.** Stitch is a genuinely capable design tool, but two things about it matter for this project specifically: (1) there's no native Jetpack Compose export — the agent is translating a design reference into Compose code, not pasting in generated code, so the screenshot/Vibe-Check verification step in Part 0B is not optional, it's the only thing actually proving the translation was faithful; (2) the free tier has a monthly generation cap, so don't treat Stitch as infinitely iterable — settle a screen within a handful of generations, extract DESIGN.md, move on.

**A9 — Why GitHub Actions instead of a local Android Studio install.** Beyond just "saves disk space," there's a real reliability argument: a local Android SDK/emulator setup accumulates machine-specific state (cached SDK components, local `local.properties` paths, IDE config) that can silently differ between your machine and any other environment — including the one Antigravity itself might be running shell commands in. A GitHub Actions workflow runs on a clean, identical, disposable VM every single time, which means "it built on Actions" is a much stronger and more reproducible signal than "it built on my laptop." Part 7 has the actual workflows.

**A10 — Gemini's Standard-key-to-Auth-key migration, and why it matters specifically for v3's BYOK feature.** This is a real, currently-active Google policy change, not a hypothetical: Google is moving all Gemini API keys from "Standard" keys to "Auth" keys. **Starting June 19, 2026, unrestricted Standard keys are rejected outright** — only Standard keys with explicit API restrictions applied, or newly-created Auth keys (the default for any key created in AI Studio today), continue to work. **By September 2026, the Gemini API stops accepting Standard keys entirely**, restricted or not. Practically, for this app: a meaningful fraction of users who already have an "old" Gemini key sitting around from earlier in 2025/2026 may bring a key that gets rejected — not because they typed it wrong, but because of this migration. Feature 11's `/verify-key` endpoint must surface this distinctly (Bug 29) rather than lump it in with "wrong key." Tell the user to go generate a brand-new key in AI Studio if this happens; don't try to detect/patch around key types programmatically, since Google's own behavior here is what's authoritative and it can keep shifting. Re-check ai.google.dev's API key documentation periodically, since this kind of platform-side migration is exactly the sort of thing that goes stale fastest in any blueprint, this one included.

**A11 — The agent must verify its own work, every step, not just at the end.** This sounds obvious but is the single most common way solo AI-agent-built projects quietly rot: a step that "looks" finished (file created, code compiles, no red squiggles) is treated as done, and three phases later something breaks in a way that's now tangled up with everything built on top of it. Part 8 is the full protocol; the one-sentence version is: no step is complete until its own specific success criterion has been *actually run and observed*, not assumed from the absence of an error.

**A12 — Every tool/library/model version must be searched and confirmed current before use — explicitly, every time.** This document itself is proof of why: v1 had a stale model name, v2 had a wrong sleep-timer figure, and by the time you're reading this, some fact in v3 is probably stale too (Gemini's model lineup alone has changed three times across this document's revisions). The instruction to the agent isn't "use good versions" — it's "before adding or bumping any dependency, library, SDK, model name, or external API version, run an actual search for its current state, and treat training-data knowledge as a starting guess to verify, never as the answer." Part 8 operationalizes this into a concrete pre-flight check.


---

## PART 7 — CI/CD VIA GITHUB ACTIONS (No Local Android Studio Required) **[v3 NEW]**

### Why this Part exists
You asked specifically not to rely on downloading large Android APK compilers locally. The Android SDK + build tools + an emulator image easily total several GB, and a local Android Studio install is yet more on top of that. GitHub Actions gives you the same Gradle build, running on a disposable cloud Linux VM, triggered automatically by a `git push` — you never install the SDK on your own machine at all. Antigravity (running locally or in its own sandbox) only ever needs to write code and run `./gradlew` commands for quick local compile checks if it has Java/Gradle available; the *actual* release-quality build, the one you trust, happens in Actions.

### What you do need locally (much smaller footprint)
- Git, and a way to push to GitHub (Antigravity can do this for you)
- A JDK (17, matching the AGP requirement below) if you want the agent to run `./gradlew test` or `./gradlew assembleDebug` locally for fast iteration — this is optional, since Actions will catch anything that slips through, but it speeds up the inner dev loop
- You do **not** need Android Studio, the Android SDK, or an emulator installed locally for any of this to work

### Workflow 1 — Debug build on every push (fast feedback loop)
Place this at `android-app/.github/workflows/android-debug.yml` (adjust the path prefix if your repo root is the `android-app/` folder itself rather than a monorepo):

```yaml
name: Android Debug Build
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:   # lets you trigger it manually from the Actions tab

jobs:
  build:
    name: Build & Unit Test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Run unit tests
        run: ./gradlew test --stacktrace

      - name: Build debug APK
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: magazineforge-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 14
```

> **[v3 — version note, verify before use]** `actions/checkout@v4`, `actions/setup-java@v4`, and `actions/upload-artifact@v4` are correct as of this revision — older tutorials online still show `@v1`/`@v2`/`@v3` for these, and `upload-artifact@v3` specifically was deprecated and will hard-fail. This is exactly the kind of fact that goes stale; per Part 8, have the agent check the Marketplace page for each action for its current major version before trusting any pinned version number, including the ones just listed.

Download the built APK from the workflow run's "Artifacts" section in the GitHub Actions UI, then sideload it onto your phone (`adb install app-debug.apk`, or just download it directly on the phone and tap to install — debug APKs don't need Play Protect bypass the way unsigned release builds sometimes do) to do the install-and-launch check from Bug 31 / Part 8.

### Workflow 2 — Signed release build on a version tag
For a build you'd actually keep and reinstall over time (rather than a disposable debug build), sign it with a real keystore so Android treats every version as updates from the same app rather than a new, unrelated one.

**One-time setup:**
1. Generate a keystore once (any machine with a JDK, this is a one-time local step, not a recurring build dependency): `keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 9125 -alias magazineforge`
2. Base64-encode it: `base64 -i release.jks -o release.jks.b64` (macOS/Linux) or `certutil -encode release.jks release.jks.b64` (Windows)
3. In your GitHub repo, go to **Settings → Secrets and variables → Actions** and add: `ANDROID_KEYSTORE_BASE64` (contents of the .b64 file), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
4. Delete the local `.jks` and `.b64` files from your working directory once the secrets are saved — don't leave the real keystore sitting in a folder Antigravity might accidentally `git add`.

```yaml
name: Android Signed Release Build
on:
  push:
    tags:
      - 'v*'   # e.g. pushing tag v1.0.0 triggers this

jobs:
  build-release:
    name: Build & Sign Release APK
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      - name: Decode keystore
        run: echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 -d > release.jks

      - name: Build signed release APK
        run: |
          ./gradlew assembleRelease \
            -Pandroid.injected.signing.store.file=$(pwd)/release.jks \
            -Pandroid.injected.signing.store.password=${{ secrets.KEYSTORE_PASSWORD }} \
            -Pandroid.injected.signing.key.alias=${{ secrets.KEY_ALIAS }} \
            -Pandroid.injected.signing.key.password=${{ secrets.KEY_PASSWORD }}

      - name: Clean up keystore from runner
        if: always()
        run: rm -f release.jks

      - name: Upload signed APK
        uses: actions/upload-artifact@v4
        with:
          name: magazineforge-release-${{ github.ref_name }}
          path: app/build/outputs/apk/release/app-release.apk

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: app/build/outputs/apk/release/app-release.apk
          generate_release_notes: true
```

> **[v3 NEW]** The `rm -f release.jks` cleanup step matters more than it looks — GitHub Actions runners are ephemeral and torn down after the job, but it's still good hygiene to never leave a decoded secret sitting on disk longer than the step that needs it, the same philosophy as Part 0's secrets guardrails applied to CI instead of local dev.

### Workflow 3 (optional) — Automated install-and-launch smoke test via emulator
This directly answers Bug 31 ("build succeeded ≠ app actually runs") with an automated check, instead of relying only on you manually installing the APK on your own phone every time.

```yaml
name: Android Emulator Smoke Test
on:
  workflow_dispatch:   # run this on demand — full emulator boots are slow, don't run on every push
  push:
    branches: [ main ]

jobs:
  smoke-test:
    name: Install & Launch on Emulator
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'
      - run: chmod +x ./gradlew
      - run: ./gradlew assembleDebug

      - name: Run on emulator and check it launches without crashing
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          target: google_apis
          arch: x86_64
          script: |
            adb install app/build/outputs/apk/debug/app-debug.apk
            adb shell am start -n com.yourpackage.magazineforge/.MainActivity
            sleep 5
            # Fails the job if the app crashed within 5 seconds of launch
            adb shell pidof com.yourpackage.magazineforge || exit 1
```

> **[v3 — version note, verify before use]** `reactivecircus/android-emulator-runner@v2` and the specific `api-level`/`target` combination are correct as of this revision but, again, exactly the kind of thing to re-verify (Part 8) — emulator runner actions and supported API levels both update periodically. This workflow is intentionally `workflow_dispatch`-gated rather than running on every push, because booting a full Android emulator in CI is slow (often 5-10+ minutes) and burns your Actions minutes quota faster than the debug-build workflow — use it as a periodic checkpoint (e.g. end of each Phase in Part 12), not on every single commit.

### What this Part deliberately does not cover
Play Store publishing automation (e.g. `r0adkll/upload-google-play`) is out of scope here — this is a personal/solo project per Part 1, and you said you want to avoid heavy tooling, not add more of it. If you ever do want to publish to the Play Store, that's a clean, separate addition to Workflow 2 above, not a prerequisite for anything in this document.


---

## PART 8 — VERIFICATION & QUALITY ASSURANCE PROTOCOL **[v3 NEW]**

### Why this Part exists
You asked for two related things: (1) the agent should verify every step is correct, and verify the finished app actually runs, and (2) you want explicit guidance on the specific ways software projects like this one tend to fail, plus a standing rule that every tool/library/model version gets searched and confirmed, not assumed. This Part is both of those, made concrete enough that "verify your work" stops being a vague aspiration and becomes a checklist the agent actually runs.

### 8.1 — The core rule: a step is not done until its success criterion was *observed*, not assumed
"No compile errors" is not a success criterion. "No compile errors" is the *absence* of one specific failure mode — it tells you nothing about whether the code does what it's supposed to. Every micro-step in Part 12 has an explicit, observable success criterion attached (a specific HTTP response, a specific file's contents, a specific screen rendering a specific way). The agent should not move to the next step until that specific thing was checked — by running the Swagger UI call, by using the Browser Subagent, by installing the actual APK, by reading the actual response body — not by inference from "the command didn't print an error."

### 8.2 — Per-layer Definition of Done

**Backend endpoint (e.g. `/generate`, `/compile`, `/verify-key`):** Done means you (or the agent, via Browser Subagent/curl/Swagger) actually sent a real request and read a real response body that matches what was expected — including at least one deliberately *bad* input (malformed JSON, missing field, invalid key) to confirm the error path also behaves, not just the happy path.

**LaTeX template change:** Done means a real PDF was compiled from it (not just that the `.tex` file has no obvious syntax error) and opened/viewed — page boundaries, font rendering, and image placement checked visually, not just "lualatex exited 0."

**Android screen:** Done means the APK was actually built (Part 7), installed on a device or emulator, and the screen was actually navigated to and interacted with — tapped buttons, typed into fields, rotated the device if relevant — not just "the Composable preview in the IDE looks right." Compose Previews render in isolation and can look fine while the screen crashes or misbehaves once wired into real navigation/state.

**Stitch-derived UI implementation:** Done means the screenshot-diff / Vibe Check from Part 0B / Feature 12 was actually performed and any mismatch was either fixed or consciously accepted, not skipped because "it's probably close enough."

**A full phase (Part 5/Part 12):** Done means every micro-step's own Definition of Done above was met, *and* the phase's own stated "Success criteria" (already present in Part 5 for each phase) was independently re-checked at the end, end-to-end, as a fresh run-through — not inferred from the individual steps having passed.

### 8.3 — The version-and-compatibility verification protocol (operationalizing A12)
Before adding or upgrading **any** of the following, run an actual search and confirm the current state rather than relying on training data or this document's own version numbers:
- Any Gradle/AGP/Kotlin/Compose BOM version (Bug 32) — these four move together; check them as a set, not individually. As of this revision: AGP 9.x requires Gradle 9.1.0+ and JDK 17, with AGP 10.0 (mid-2026) removing several legacy APIs outright — if the agent is reading this months later, that "10.0" line may already be the present, not the future, so check the actual current AGP release notes rather than trusting this sentence's tense.
- Any Gemini model name (Bug 19, A2, A10) — the lineup has changed repeatedly across this document's own revisions; always check Google AI Studio's live model list.
- Any GitHub Action's pinned version (Part 7's version notes) — Marketplace pages show current major versions; deprecated majors (e.g. `upload-artifact@v3`) hard-fail rather than just warning.
- Any Debian/HF package name in `packages.txt` (Bug 07) — verify against the current HF Spaces base image.
- The Stitch MCP server's connection method/config schema (Part 0B) — MCP server configs for third-party tools change as the protocol and the specific server mature; if the documented config in this blueprint doesn't match what Antigravity's MCP server browser shows you, trust what's in front of you over this document.

**The standing instruction to give the agent, verbatim, at the start of any session:** *"Before you add, install, or bump any dependency, library, SDK, API model name, or external tool version, search for its current state first. Don't use a version number or model name from your training data, or from this document, without confirming it's still accurate today. If you find a discrepancy between what this document says and what you find, tell me, and prefer what you found."*

### 8.4 — The failure-mode checklist (what actually sinks projects like this one)
This list exists so the agent (and you) have names for the specific things to watch for, tied to where each one already shows up in this document:

- **Dependency/version drift** — covered above and in Bug 32. The single highest-risk failure mode in any Gradle-based Android project that's been touched by multiple sessions of an AI agent over weeks.
- **Silent failures / swallowed exceptions** — a `try/except: pass` (or Kotlin's equivalent) anywhere in this stack turns a real, debuggable error into a mystery three steps later. Every catch block in this project should either re-raise with context, log with enough detail to diagnose later, or surface a Smart Error Message (Feature 06) — never silently continue.
- **Partial success treated as full success** — e.g. a chunked PDF compile (Bug 05) where 4 of 5 chunks succeed and the 5th silently fails; the merge step must check that every expected chunk actually exists before calling Ghostscript, not just merge whatever's present.
- **Environment parity gaps** — "works on my machine"/"works in Antigravity's sandbox" but not on the actual HF Space or the actual GitHub Actions runner. This is exactly why Part 7 exists — Actions' clean VM is the tiebreaker for what's actually true, not your local environment.
- **Race conditions / concurrency** — Bug 09's concurrent-compile issue is the obvious one here, but also: two near-simultaneous `/verify-key` calls for the same user, or a double-tap (Bug 17) racing two `/generate` calls that both try to write the same temp file path.
- **Encoding/locale/script issues** — Bug 22's non-Latin script handling is a specific instance of a general category; also watch for it in date formatting, currency-like symbols a user might type, and any text that round-trips through JSON (Bug 10) and back.
- **Secrets leakage** — Part 0's guardrails, applied continuously, not just at project setup. Re-run a `grep -r` for key-shaped strings before any push, especially after a long autonomous agent session (A6).
- **Idempotency gaps** — if `/compile` is ever retried (timeout, user re-tap, network blip), does retrying it twice cause a duplicate Firebase upload, a duplicate push notification, or a corrupted merge? Check this explicitly wherever a request might plausibly be retried.
- **Schema drift between Gemini's output and what the backend expects** — Bug 10, now multiplied across every content block once Part 10's full-issue schema is in play. Every block needs its own Pydantic model and its own "what do we do if this field is missing/wrong-typed" decision, not one shared assumption.
- **"It built" mistaken for "it works"** — Bug 31, directly. A green checkmark on a GitHub Actions build job proves compilation succeeded; it proves nothing about runtime behavior unless the workflow includes an actual install-and-launch step (Workflow 3 in Part 7).

### 8.5 — Final, whole-app verification checklist (run this before calling *any* version "done," not just at the very end of the whole project)
- [ ] Fresh install on a device that has never run the app before (clears any "worked because of leftover local state" false positives)
- [ ] Paste a brand-new, just-created Gemini API key and confirm `/verify-key` succeeds
- [ ] Paste a deliberately invalid/garbage string into the key field and confirm the error message is specific and actionable, not generic
- [ ] Generate at least one magazine using each currently-built template
- [ ] Generate a magazine with a title containing `& % $ # _ ^ { } ~ \` (Bug 04) and confirm it compiles cleanly
- [ ] Generate a magazine with a non-Latin-script title (Bug 22) and confirm the correct font/script switch happened
- [ ] Generate the largest currently-supported page count (Bug 05) and confirm it completes within a reasonable time without an OOM kill
- [ ] Kill network connectivity mid-compile and confirm the app shows a recoverable error, not a crash
- [ ] Double-tap Compile rapidly (Bug 17) and confirm only one job actually runs
- [ ] Force the app into the background for several minutes during an async compile (Feature 09) and confirm the push notification still arrives
- [ ] Re-open the app a day later and confirm a previously generated magazine is still retrievable (Bug 13)

---

## PART 9 — THE MODULARITY PRINCIPLE **[v3 NEW]**

### What "modular, no matter how many micro-steps" actually means in practice
You asked for every step — however many it takes — to stay modular and as precise as possible. Here is the concrete rule, not just the sentiment:

**A single step is too big if it changes more than one independently-verifiable thing.** If a prompt to the agent would require more than one Definition-of-Done check (Part 8.2) to confirm it worked, it should be split into the steps that each have exactly one. This is the actual test — not a step count, not a time estimate, but: *can I verify this individually, in isolation, before anything is built on top of it?*

**Worked example — what NOT to do:**
> "Build the template gallery screen with the categorized tabs, the 2-column grid, the search bar, and the Surprise Me button, wire it to the backend, and add the preview mode."

This bundles at least five independently-verifiable things into one step. If something's wrong, you don't know which of the five caused it, and you've likely built the next phase on top of a partially-broken foundation without realizing it.

**Worked example — the same work, modular:**
1. Design the screen in Stitch (Part 0B), extract DESIGN.md. *Verify: DESIGN.md contains real values, reviewed by eye.*
2. Implement the static tab row only, with hardcoded category labels, no data wiring. *Verify: tabs render and switch visually on a built, installed APK.*
3. Implement the 2-column grid with hardcoded placeholder thumbnails. *Verify: grid renders correctly at 2, 3, and 7 items (edge cases) without layout breakage.*
4. Wire the grid to real template data from the backend (or a local mock if the backend endpoint isn't built yet). *Verify: a real network call populates real thumbnails.*
5. Add the search bar, with a hardcoded local filter only. *Verify: typing "travel" actually narrows the visible grid.*
6. Add the "Surprise Me" button. *Verify: tapping it actually navigates/selects, doesn't just log a Toast.*
7. Add full-screen Preview Mode on tap. *Verify: tapping a real template actually opens it, shows 3 real sample pages, and the back button returns to the gallery without losing scroll position.*

Seven steps instead of one — each individually small, each individually checkable, each one safe to build on top of because it was actually confirmed working before step *n+1* started.

### How granular is "too granular"?
There's no fixed floor — the rule above (one verifiable thing per step) is the only constraint, and it can legitimately produce a lot of steps for a complex screen. That's fine. A long, precise list of small steps is strictly better for this project than a short list of large ones, because every individual step is a checkpoint where a problem gets caught while it's still cheap to fix, instead of three layers later when it's tangled into everything built since.

### Checkpointing
After each verified micro-step, the agent should make a small, scoped commit (one logical change per commit, matching the one-verifiable-thing-per-step rule above) with a message describing what was verified, not just what was written — e.g. `"Add tab row to template gallery — verified switching between 6 tabs renders correct labels on-device"` rather than just `"add tabs"`. This gives you (and future sessions of the agent, which have no memory of past sessions per the standard model limitation) a trustworthy trail of what was actually confirmed working versus what was merely written.

### How this interacts with Part 12
Part 12 takes every phase from Part 5 and pre-decomposes it into this level of granularity for you, so you're not starting from a blank page on "how small should this be" for the whole build — but treat Part 12's breakdown as a floor, not a ceiling. If the agent (or you) finds a listed micro-step in Part 12 still bundles more than one verifiable thing once you're actually building it, split it further. The principle in this Part always wins over the specific list in Part 12 if they ever disagree.


---

## PART 10 — THE COMPLETE MAGAZINE DESIGN & BULLETPROOF GEMINI SYSTEM PROMPTING **[v3 NEW]**

### 10.1 — What a "complete magazine" actually contains
Feature 10 flagged that a magazine is more than a cover, but didn't spell out the full anatomy. Here it is, mapped to what Gemini needs to generate and what LaTeX templates need to exist for each:

| Section | Appears | What it needs from Gemini | Template needed |
|---|---|---|---|
| **Front cover** | Page 1 | Main title, subtitle, 2-4 cover line callouts, accent color, mood | `cover_template_[a/b/c].tex` |
| **Inside front cover / masthead** | Page 2 | Issue tagline, a short "from the editor" blurb (2-3 sentences), fake masthead credits list (Editor-in-Chief, Art Director, etc. — generated personas, clearly part of the fictional publication) | `masthead_template.tex` |
| **Table of contents** | Page 3 | Section titles + page numbers + 1-line teaser per article | `toc_template.tex` |
| **Feature articles** (2-5 of them, scaling with page count) | Pages 4 through N-2 | Headline, subheadline, byline (fictional author name fitting the persona), 300-500 word body copy, 1-2 pull quotes, 1-3 image placements with captions | `article_two_column.tex`, `article_three_column.tex`, `article_photo_essay.tex` |
| **Sidebars / "5 things to know" boxes** | Embedded within feature articles | A short title + 3-5 bullet items, visually distinct from the main column flow | Variant of the article templates, boxed-out region |
| **Back cover** | Last page | Either a closing full-bleed image + a single short tagline, or a stylized "next issue" teaser | `back_cover_template.tex` |

This table is the backbone of the full-issue JSON schema below — every row needs its own Pydantic model on the backend (per Part 8.4's schema-drift warning) and its own block in the Gemini system prompt's output schema.

### 10.2 — The full-issue JSON schema
This expands Bug 10's `CoverPage` model to the whole issue. Each top-level key corresponds to one row in the table above:

```python
from pydantic import BaseModel, Field
from typing import List, Optional, Literal

class CoverPage(BaseModel):
    main_title: str
    subtitle: Optional[str] = "Untitled"
    accent_hex: str = "#D97757"
    cover_pattern: Literal["clean_title_dominant", "callout_heavy", "typographic_led"] = "clean_title_dominant"
    callouts: List[str] = Field(default_factory=list, max_items=4)
    image_url: str

class MastheadBlock(BaseModel):
    issue_tagline: str
    editors_note: str  # 2-3 sentences, plain prose
    credits: List[str] = Field(default_factory=list)  # e.g. ["Editor-in-Chief: Mara Voss", ...]

class TocEntry(BaseModel):
    section_title: str
    page_number: int
    teaser: str  # one sentence

class PullQuote(BaseModel):
    quote_text: str
    attributed_to: Optional[str] = None

class ImagePlacement(BaseModel):
    image_url: str
    caption: Optional[str] = None
    placement: Literal["full_bleed", "half_page", "inset", "photo_essay_grid"] = "half_page"

class SidebarBox(BaseModel):
    box_title: str
    bullet_items: List[str] = Field(default_factory=list, max_items=5)

class FeatureArticle(BaseModel):
    headline: str
    subheadline: Optional[str] = None
    byline: str  # fictional author name, in-persona for the publication
    body_copy: str  # 300-500 words, plain prose, no markdown
    pull_quotes: List[PullQuote] = Field(default_factory=list, max_items=2)
    images: List[ImagePlacement] = Field(default_factory=list, max_items=3)
    sidebar: Optional[SidebarBox] = None
    layout: Literal["two_column", "three_column", "photo_essay"] = "two_column"

class BackCover(BaseModel):
    style: Literal["closing_image", "next_issue_teaser"]
    tagline: str
    image_url: Optional[str] = None

class FullIssue(BaseModel):
    cover: CoverPage
    masthead: MastheadBlock
    toc: List[TocEntry]
    articles: List[FeatureArticle]
    back_cover: BackCover
```

Validate each top-level block independently when parsing Gemini's response (same dual-layer pattern as Bug 10: strip code fences, then `FullIssue(**data)`), and — critically, per Part 8.4 — decide explicitly what happens if one block is malformed while the rest is fine. The right default for this app: if `articles[2]` fails validation but `cover`, `masthead`, `toc`, and `articles[0]`/`articles[1]` are all fine, compile the issue with the articles that did validate and silently drop the one that didn't, rather than failing the entire 90-second job over one bad block. Surface this to the user as "Your magazine is ready, though one article section needed to be skipped" — not as a hard failure.

### 10.3 — Why a "good enough" system prompt isn't good enough here
The stakes for prompt precision are higher in this app than in a typical chatbot use case, because a malformed Gemini response doesn't just produce an awkward sentence — it can break the LaTeX compile (Bug 04, Bug 10, Bug 11) and waste the user's 30-90 second wait entirely. The system prompt is doing real engineering work, not just style guidance. Five specific techniques matter most for this use case:

1. **Role-prime with real editorial expertise, not a generic "you are a helpful assistant."** Gemini writes meaningfully better, more specific magazine copy when primed as a specific kind of professional (see the exact framing used in Part 11's prompts) than when asked generically to "write a magazine article."
2. **Put the literal Pydantic-equivalent schema in the prompt, not a vague description of it.** Show Gemini the exact JSON shape, field names, and allowed enum values it must produce — copy the actual schema, don't paraphrase it.
3. **Give one full worked example (few-shot), not just the schema.** Models are dramatically more reliable at hitting an exact structure when shown one complete, correctly-formatted example of it, even a short one, in addition to the abstract schema.
4. **State forbidden patterns explicitly, even though the backend sanitizes anyway.** Telling Gemini directly not to use `&`, `%`, `$`, `#`, `_`, `^`, `{`, `}`, `~`, or `\` in any text field — and to write around them naturally ("and" instead of "&", "percent" instead of "%") — measurably reduces how often the sanitizer (Bug 04) has to mangle a sentence to make it safe, which keeps the *prose* reading naturally even after sanitization, not just keeps it from crashing.
5. **Instruct an explicit self-check before responding.** Adding a closing instruction like "before you respond, verify your output is valid JSON, matches the schema exactly, contains no markdown code fences, and contains none of the forbidden characters listed above" measurably reduces malformed-output rates — it's a cheap, free addition that costs nothing but a sentence and catches a meaningful fraction of schema drift before it ever reaches your Pydantic layer.

### 10.4 — A worked master system prompt for `/generate`
This is a complete, usable starting point for the system prompt your `/generate` endpoint sends to Gemini alongside the user's topic, page count, and template choice. Treat it as a strong first draft to refine once you've run Part 11's persona-extraction workflow and have real Design DNA patterns to fold in — the placeholder `{{DESIGN_DNA_PATTERNS}}` section is exactly where Part 11's output goes.

```
SYSTEM PROMPT — MagazineForge Editorial Generation Engine

You are the senior editorial AI for a professional magazine publishing platform.
You have the combined expertise of a magazine editor-in-chief, a creative director,
and a staff writer at a top-tier publication (think Condé Nast Traveller, Wired,
or National Geographic in tone and quality bar). You are generating a complete,
fictional magazine issue based on a user-supplied topic. Your output will be fed
directly into an automated print-typesetting pipeline — there is no human editor
between your output and the final printed page, so precision matters as much as
quality.

== YOUR TASK ==
Given a TOPIC, a PAGE_COUNT (always a multiple of 4), and a TEMPLATE_STYLE
(one of: clean_title_dominant, callout_heavy, typographic_led), generate a
complete magazine issue as a single JSON object matching the schema below exactly.

== DESIGN DNA TO FOLLOW FOR THIS TEMPLATE_STYLE ==
{{DESIGN_DNA_PATTERNS}}
(This section is populated per-template from the synthesized persona patterns
produced by the Part 11 analysis workflow — typography mood, color tendencies,
text placement conventions, and overall editorial voice characteristic of this
template style, distilled from real professional magazines.)

== OUTPUT SCHEMA — MATCH THIS EXACTLY, NO ADDITIONAL OR MISSING KEYS ==
{
  "cover": {
    "main_title": "string, max 6 words, the magazine issue's headline title",
    "subtitle": "string, max 12 words",
    "accent_hex": "string, a single hex color code fitting the topic's mood",
    "cover_pattern": "clean_title_dominant | callout_heavy | typographic_led",
    "callouts": ["string", "... up to 4 short cover lines, each under 6 words"],
    "image_url": "leave this as the literal string USER_PROVIDED_IMAGE"
  },
  "masthead": {
    "issue_tagline": "string, one short evocative phrase for this issue",
    "editors_note": "string, 2-3 sentences, warm and editorial in voice",
    "credits": ["string, e.g. 'Editor-in-Chief: [invented name]'", "... 3-5 entries"]
  },
  "toc": [
    { "section_title": "string", "page_number": integer, "teaser": "string, one sentence" }
  ],
  "articles": [
    {
      "headline": "string",
      "subheadline": "string or null",
      "byline": "string, an invented author name fitting the publication's voice",
      "body_copy": "string, 300-500 words, plain prose, no markdown formatting at all",
      "pull_quotes": [ { "quote_text": "string", "attributed_to": "string or null" } ],
      "images": [ { "image_url": "USER_PROVIDED_IMAGE", "caption": "string or null", "placement": "full_bleed | half_page | inset | photo_essay_grid" } ],
      "sidebar": { "box_title": "string", "bullet_items": ["string", "... up to 5"] } ,
      "layout": "two_column | three_column | photo_essay"
    }
  ],
  "back_cover": {
    "style": "closing_image | next_issue_teaser",
    "tagline": "string",
    "image_url": "USER_PROVIDED_IMAGE or null"
  }
}

== WORKED EXAMPLE (for structure only — generate fresh content for the real topic) ==
{
  "cover": {
    "main_title": "Coastal Iceland",
    "subtitle": "Six days chasing fire and ice",
    "accent_hex": "#2E4A5C",
    "cover_pattern": "clean_title_dominant",
    "callouts": ["Glacier lagoons at dawn", "Where to eat in Reykjavik", "Pack light, pack smart"],
    "image_url": "USER_PROVIDED_IMAGE"
  },
  "masthead": {
    "issue_tagline": "Slow travel for restless minds",
    "editors_note": "This issue, we trade itineraries for instinct. Iceland rewards the traveler willing to get a little lost.",
    "credits": ["Editor-in-Chief: Lena Ashworth", "Art Director: Mateo Silva", "Staff Writer: Priya Ramanathan"]
  },
  "toc": [
    { "section_title": "The Long Way to Vik", "page_number": 4, "teaser": "A detour that became the whole trip." }
  ],
  "articles": [
    {
      "headline": "The Long Way to Vik",
      "subheadline": "Why the slow road is the only road worth taking",
      "byline": "Priya Ramanathan",
      "body_copy": "[300-500 words of original prose would go here]",
      "pull_quotes": [ { "quote_text": "The black sand doesn't photograph the way it feels underfoot.", "attributed_to": null } ],
      "images": [ { "image_url": "USER_PROVIDED_IMAGE", "caption": "Vik's basalt cliffs at low tide.", "placement": "half_page" } ],
      "sidebar": { "box_title": "Five things to pack", "bullet_items": ["Wool base layer", "Waterproof boots", "A real camera, not just your phone", "Cash for tiny villages", "Patience for the weather"] },
      "layout": "two_column"
    }
  ],
  "back_cover": { "style": "closing_image", "tagline": "Next issue: the long way to anywhere.", "image_url": "USER_PROVIDED_IMAGE" }
}

== HARD RULES ==
1. Output raw JSON only. No markdown code fences, no commentary before or after
   the JSON, no "Here is the JSON:" preamble.
2. Never use these characters anywhere in any text field, including inside
   body_copy: & % $ # _ ^ { } ~ \ — write around them naturally in normal
   English instead (write "and" instead of "&", "percent" instead of "%").
3. Generate exactly enough articles to fill PAGE_COUNT at roughly 2 pages per
   article after the cover (1 page), masthead (1 page), TOC (1 page), and back
   cover (1 page) are accounted for.
4. Every invented name (authors, editors) must read as a plausible real name —
   no joke names, no placeholder text like "Author Name."
5. body_copy must be original prose written specifically for this topic — never
   lorem ipsum, never a generic template paragraph with the topic swapped in.
6. Before responding, verify: your output is valid JSON, it matches the schema
   above exactly (no extra or missing keys), it contains zero of the forbidden
   characters from rule 2, and it is not wrapped in any markdown formatting.

== INPUT ==
TOPIC: {{user_topic}}
PAGE_COUNT: {{user_page_count}}
TEMPLATE_STYLE: {{user_template_style}}
```

> **[v3 NEW]** Note the `image_url` fields are deliberately set to the literal placeholder string `USER_PROVIDED_IMAGE` rather than asking Gemini to invent a URL. Never let Gemini generate or guess an image URL — it has no way to know what images the user actually uploaded (Feature 02), and a hallucinated URL will fail Bug 02's download step. Your backend should substitute the user's real uploaded image URLs into these fields after parsing, matching them to placements in order, not trust whatever Gemini puts there.

### 10.5 — Handling long magazines without truncation
For larger page counts (16-24 pages, per Feature 10), a single Gemini call generating the entire `FullIssue` JSON in one response risks hitting output-length limits or degrading in quality toward the end of a very long generation. The more reliable pattern, once you're past the Phase 2 MVP:
1. First call: generate just `cover`, `masthead`, and `toc` (small, fast, cheap).
2. One call per article (or per 2-3 articles batched), each given the topic, the issue's established tone from the first call's `masthead.editors_note`, and which `toc` entry it corresponds to — so each article call has full context without needing to regenerate the whole issue.
3. Final call: generate `back_cover`, with the article headlines as context so the "next issue" teaser (if used) feels connected to what came before.

This also maps cleanly onto Bug 05's chunked-compilation pattern — you're already compiling in chunks for memory reasons, so generating in matching chunks means each compile chunk only depends on a Gemini call that's already complete, rather than waiting on one giant generation before any compilation can start.


---

## PART 11 — READY-TO-USE MAGAZINE PERSONA EXTRACTION & ANALYSIS PROMPTS **[v3 NEW]**

### How to use this Part
This is the deliverable for Feature 08's expanded workflow. Each prompt below is complete and ready to paste into Gemini Vision (or any multimodal model — GPT-4o/GPT-5-class vision, Claude with vision, etc.) **alongside page images** from a real, professionally-designed magazine (convert PDF pages to PNG at 150-300 DPI first, per Feature 08's `pdftoppm` step). Run prompts A through E against each of your 20-25 source magazines, save every JSON response to its own file (suggested naming: `persona_[publication]_[issue]_[prompt-letter].json`), then run the **Meta-Synthesis Prompt** at the end over your whole collected set. The Meta-Synthesis output is what you hand back to the Antigravity agent to embed as the real `{{DESIGN_DNA_PATTERNS}}` content in Part 10.4's system prompt.

Run each prompt against **one page (or one clearly-bounded spread) at a time** — don't batch multiple unrelated pages into one call expecting one JSON back; that's the same "bundle multiple unverifiable things into one step" mistake Part 9 warns against, just applied to data collection instead of code.

---

### Prompt A — Cover Page Deep Analysis

```
You are a senior magazine art director with over twenty years of experience at
major consumer publishing houses, specializing in cover design strategy across
travel, lifestyle, technology, food, and science titles. You have personally
overseen newsstand cover decisions for hundreds of issues and can identify the
specific design decisions — not just the vibe — behind any cover at a glance:
grid structure, type hierarchy, color strategy, and how text and image negotiate
for attention on the page.

I am showing you the cover of a single magazine issue. Analyze it with the
rigor of an internal design-system audit — the kind of breakdown you'd produce
if a junior designer asked you to explain exactly why this cover works, in
terms specific and structured enough that someone could rebuild an equivalent
cover from your answer alone, without seeing the original.

Examine, specifically:
- The underlying grid/layout structure: is the title dominant and centered, is
  it off-axis, is the page built around a strong vertical or horizontal axis,
  is there a visible "safe zone" the masthead/barcode area respects?
- Typographic hierarchy: how many distinct type sizes are visible, and what is
  the approximate size RATIO between the largest and smallest (e.g. "the main
  title is roughly 6-8x the size of the smallest cover line")?
- Type style per hierarchy level: is the main title serif, sans-serif, or
  display/script? Is it ultra-bold, bold, or a lighter weight used at large
  size for contrast? Same question for the secondary cover lines and any
  smaller text.
- Color strategy: how many distinct accent colors are used (not counting the
  photo itself)? Are they warm, cool, neutral, or saturated/vibrant? Is there
  a single dominant accent or a coordinated small palette?
- Background/image treatment: full-bleed photographic, a framed/bordered
  image, a flat color field, or a hybrid? If photographic, is the image cropped
  tight on a single subject, a wide environmental shot, or a graphic/illustrated
  treatment?
- Text placement relative to the image: does text sit in open negative space
  within the photo (a "sky zone"), on a solid overlay bar/panel, in a bottom
  strip, or in a bordered frame around the image rather than over it?
- Overall mood, described with 3-5 adjectives a design team would actually use
  internally (not generic marketing language) — e.g. "confident," "restrained,"
  "maximalist," "editorial," "playful," "austere."

Output ONLY a single JSON object in exactly this shape, with no additional
commentary before or after it, and no markdown code fence around it:

{
  "publication_context": "string — your best guess at the genre/category this looks like (e.g. 'outdoor travel', 'tech/business', 'food/lifestyle'), based purely on visual design language, not any text you can read naming the actual publication",
  "layout_pattern": "clean_title_dominant | callout_heavy | typographic_led | image_dominant_minimal_text",
  "typography": {
    "headline_weight": "ultra_bold | bold | medium | light_at_large_size",
    "headline_style": "serif | sans_serif | display | script",
    "size_contrast_ratio_estimate": "a short string like '6-8x' or '3-4x' describing largest:smallest type size ratio",
    "secondary_text_style": "serif | sans_serif",
    "distinct_size_levels_visible": integer
  },
  "color": {
    "accent_count": integer (1-3),
    "accent_tone": "warm | cool | neutral | vibrant",
    "dominant_accent_hex_estimate": "your best-guess hex code for the single most prominent non-photographic accent color",
    "background_treatment": "full_bleed_photo | framed_photo | flat_color | hybrid_illustration"
  },
  "text_placement": "sky_zone | overlay_bar | bottom_strip | border_frame | scattered_callouts",
  "callout_count_visible": integer,
  "mood": ["adjective1", "adjective2", "adjective3"],
  "notable_technique": "one sentence describing the single most distinctive design choice on this specific cover that you would point out to a junior designer studying it"
}
```

---

### Prompt B — Table of Contents Analysis

```
You are the same senior magazine art director as before, now specifically
reviewing table-of-contents (TOC) page design — a notoriously underrated page
that most readers skim for under ten seconds, which means every design choice
on it has to earn attention fast or get skipped entirely.

I am showing you a table of contents page (or spread) from a magazine issue.
Analyze its structure precisely enough that someone could rebuild an equivalent
TOC page from your description alone.

Examine, specifically:
- Overall structure: is it a single list-style column, a multi-column grid
  with thumbnail images per entry, a hierarchical structure (Features vs.
  Departments, clearly visually separated), or a hybrid?
- How page numbers are presented: aligned right in a dedicated column, set
  inline immediately after the title, or set in a distinct visual treatment
  (boxed, colored, oversized)?
- Whether thumbnail images accompany entries, and if so, how many, and at
  roughly what size relative to the page
- Typographic treatment of section titles vs. one-line teaser/description
  text per entry — same family as the cover, or a deliberately different,
  more utilitarian typeface for this more functional page?
- Use of any rule lines, dividers, or background tints to separate sections
  (e.g. Features from shorter Departments/columns)
- Where the masthead credits block (editor, art director, etc.), if visible
  on this page at all, sits relative to the actual contents list
- Any decorative or branding element (an issue number, a recurring motif, a
  small illustration) that isn't purely functional

Output ONLY a single JSON object in exactly this shape, no commentary, no
markdown code fence:

{
  "structure": "single_column_list | multi_column_grid_with_thumbnails | hierarchical_features_vs_departments | hybrid",
  "page_number_treatment": "right_aligned_column | inline_after_title | boxed_distinct_treatment | oversized_decorative",
  "thumbnails_present": true or false,
  "thumbnail_count_estimate": integer or null,
  "thumbnail_size_relative_to_page": "small_icon | medium_quarter_page | large_feature_sized | null",
  "typography": {
    "section_title_style": "matches_cover_typeface | distinct_utilitarian_typeface",
    "teaser_text_style": "serif | sans_serif",
    "uses_all_caps_for_section_titles": true or false
  },
  "dividers": {
    "uses_rule_lines": true or false,
    "uses_background_tints": true or false,
    "section_separation_clarity": "very_clear | moderate | minimal"
  },
  "masthead_credits_visible_here": true or false,
  "decorative_branding_element": "string describing it, or null if none",
  "overall_density": "spacious_airy | balanced | dense_information_rich"
}
```

---

### Prompt C — Interior Article Spread Analysis

```
You are the same senior magazine art director, now reviewing an interior
feature-article spread (a two-page layout, or a single representative page
if that's what's shown) — the core editorial real estate of the entire issue,
where the publication's actual reading experience and typographic personality
live.

I am showing you one interior article page or spread. Analyze its construction
the way you would walk a layout artist through rebuilding it from scratch.

Examine, specifically:
- Column structure: how many text columns, and are they equal width or is
  one wider (a common pattern where one column carries the lead paragraph
  in a larger size before the rest drops into a tighter multi-column grid)?
- Headline treatment: size relative to body text (give a rough ratio), and
  whether it spans the full width, sits within one column, or breaks across
  an image
- Use of a large lead/drop capital letter at the article's opening paragraph
- Pull quote treatment: how it's set apart visually (size jump, color, a rule
  line above/below, a distinct typeface), and roughly how much of the page
  width it occupies
- Image-to-text ratio on this specific page/spread, described as a rough
  percentage split
- Caption styling: typeface, size relative to body text, and position
  (directly beneath the image vs. to the side)
- Whether there's a sidebar/boxed-out callout (a "5 things to know"-style
  insert) and if so, how it's visually distinguished from the main article
  flow (background tint, border, different column width)
- White space usage: generous margins and breathing room, or a denser,
  more packed layout typical of a content-heavy publication
- Any running elements (a folio/footer with the publication name and page
  number, a section label/kicker above the headline)

Output ONLY a single JSON object in exactly this shape, no commentary, no
markdown code fence:

{
  "column_structure": {
    "column_count": integer,
    "equal_width_columns": true or false,
    "lead_paragraph_treatment": "larger_single_column_lead | standard_matching_columns | drop_cap_only"
  },
  "headline": {
    "size_ratio_to_body_estimate": "string like '5-7x'",
    "width_treatment": "full_width | single_column | breaks_across_image",
    "uses_kicker_label_above": true or false
  },
  "drop_cap_used": true or false,
  "pull_quote": {
    "present": true or false,
    "visual_separation": "size_jump_only | color_change | rule_lines | distinct_typeface | combination",
    "width_relative_to_column": "matches_one_column | spans_multiple_columns | full_page_width | null"
  },
  "image_to_text_ratio_estimate": "string like '40% image / 60% text'",
  "caption": {
    "position": "directly_beneath_image | adjacent_side_column | overlaid_on_image",
    "size_relative_to_body": "smaller | same | null_no_caption"
  },
  "sidebar_present": true or false,
  "sidebar_visual_distinction": "background_tint | border_outline | narrower_column_only | none_blends_in",
  "white_space_density": "generous_airy | balanced | dense_packed",
  "running_footer_elements": ["string", "... e.g. 'page number', 'publication name', 'section label'"]
}
```

---

### Prompt D — Typography & Color System Synthesis (Full Issue)

```
You are the same senior magazine art director, now performing a full-issue
design-system audit — the kind of internal document a publication's design
team maintains to ensure every page, regardless of which staff designer built
it, feels like it belongs to the same magazine.

I am showing you several pages from across a single magazine issue (cover,
table of contents, and one or more interior spreads together). Your job is
to look ACROSS all of them and identify the consistent system underneath —
not to re-describe any single page, but to extract what stays constant from
page to page, which is the actual reusable "Design DNA" of this publication.

Examine, specifically:
- The core type pairing: what serif/sans-serif (or serif/serif, or
  display/sans) combination recurs across cover, headlines, and body text?
  Describe each role (display, headline, body, caption) and its likely
  general category (e.g. "a humanist sans for body text, a high-contrast
  serif for display") even if you can't identify the exact named typeface.
- The color system: is there one true signature accent color used
  consistently across the issue, or does the accent shift per article/section
  while a neutral palette (blacks, whites, a warm or cool gray) stays
  constant underneath? Estimate the consistent neutral tone (warm gray vs.
  cool gray vs. true black/white) as well as the accent behavior.
- Spacing/rhythm: does the issue favor generous margins and white space
  throughout, or a denser, more utilitarian use of space? Is this consistent
  cover-to-back, or does it shift between cover (often more spacious) and
  interior editorial pages (often denser)?
- Photographic style consistency: consistent color grading/treatment across
  different photographers' work (suggesting a strong post-production house
  style), or more variation page to page?
- The publication's overall "voice" in design terms — if you had to describe
  this publication's design philosophy in one paragraph to a new staff
  designer joining the team, what would you tell them to always preserve,
  and what would you tell them they have freedom to vary?

Output ONLY a single JSON object in exactly this shape, no commentary, no
markdown code fence:

{
  "type_pairing": {
    "display_role": "string describing category, e.g. 'high-contrast serif display'",
    "headline_role": "string",
    "body_role": "string, e.g. 'humanist sans-serif, moderate x-height'",
    "caption_role": "string"
  },
  "color_system": {
    "signature_accent_consistent_across_issue": true or false,
    "accent_behavior": "single_fixed_accent | shifts_per_section_with_constant_neutral_base",
    "neutral_tone": "warm_gray | cool_gray | true_black_and_white",
    "estimated_signature_hex": "string or null if accent shifts per section"
  },
  "spacing_rhythm": {
    "overall_density": "generous_airy | balanced | dense_utilitarian",
    "cover_vs_interior_consistency": "matches | cover_more_spacious_than_interior | interior_more_spacious_than_cover"
  },
  "photographic_style_consistency": "strong_unified_grading | moderate_consistency | high_variation_per_photographer",
  "design_philosophy_summary": "a 2-4 sentence paragraph in your own words describing what this publication's design system always preserves vs. where it allows variation, written as if briefing a new staff designer",
  "mood_keywords": ["adjective1", "adjective2", "adjective3", "adjective4"]
}
```

---

### Prompt E — Image Treatment & Photography Style Analysis

```
You are now a photo director and visual editor — the role responsible for
how a publication selects, crops, color-grades, and places photography
distinctly from how the layout designer handles type and grid. Many
publications are visually distinguishable from their photo treatment alone,
even with the masthead and all type removed.

I am showing you one or more pages from a magazine issue, focusing your
analysis specifically on the photography/imagery, not the typography or
layout grid (those are covered by other audits).

Examine, specifically:
- Color grading tendency: warm and golden, cool and desaturated, high-contrast
  and punchy, naturalistic/true-to-life, or a deliberate filmic/grain
  treatment?
- Cropping philosophy: tight on subjects/details, wide environmental/
  establishing shots, a deliberate mix used intentionally for pacing, or
  consistently one or the other throughout?
- Subject framing conventions: are human subjects typically shown candid/
  documentary-style, posed/editorial, or largely absent in favor of
  place/object/food photography?
- Use of negative space within photographs specifically to accommodate text
  overlays (as opposed to text sitting on a separate solid panel) — is this
  a frequent technique in this publication or rare?
- Full-bleed vs. framed/bordered image usage as a general tendency across
  the pages shown
- Whether a recognizable "look" would let a reader identify this publication
  from an unlabeled photo alone, and if so, what specifically creates that
  recognizability

Output ONLY a single JSON object in exactly this shape, no commentary, no
markdown code fence:

{
  "color_grading": "warm_golden | cool_desaturated | high_contrast_punchy | naturalistic_true_to_life | filmic_grain_treatment",
  "cropping_philosophy": "tight_on_subject | wide_environmental | intentional_mix_for_pacing | consistently_one_style",
  "subject_framing": "candid_documentary | posed_editorial | place_object_food_focused | mixed",
  "negative_space_for_text_overlay": "frequent_technique | occasional | rare_text_kept_separate",
  "bleed_tendency": "mostly_full_bleed | mostly_framed_bordered | even_mix",
  "recognizable_signature_look": true or false,
  "signature_look_description": "one sentence on what specifically creates the recognizable look, or null if not strongly recognizable"
}
```

---

### Meta-Synthesis Prompt — Merging All Personas Into One Design DNA

Run this last, once you've collected Prompt A-E outputs across all 20-25 source magazines (or, more usefully, grouped by template style — run one synthesis per Feature 01 category like Travel, Food, Tech, so each `TEMPLATE_STYLE` in Part 10.4 gets its own distilled Design DNA rather than one blended-together average that fits nothing well). Paste in the full collected set of JSON outputs as the input.

```
You are a design systems strategist who specializes in distilling many
individual design analyses into one clear, actionable creative brief that a
generative AI system can reliably follow. You will be given a large set of
structured JSON design-persona analyses, each one describing a single page,
spread, or full issue from a real professional magazine, all belonging to the
same broad editorial category (e.g. "travel" or "tech" or "food").

Your job is NOT to summarize every individual entry. Your job is to find the
genuine PATTERNS that recur across most or all of them — the things that
several different real, independently-designed publications in this category
converge on — and discard the one-off idiosyncrasies that only show up once
or twice. A pattern that appears in 15 of 20 source analyses is signal; a
striking but unique choice that appears in 1 of 20 is noise for this purpose,
even if it was the single most visually memorable thing in that one analysis.

Produce a Design DNA brief structured as follows, written as direct,
actionable instructions to a generative system (not as descriptive
observations about the past) — this exact text will be inserted into a
production system prompt, so phrase every line as a rule to follow, not a
fact about what you observed:

1. TYPOGRAPHY RULES — which headline weight/style and body style to default
   to, and the approximate size-contrast ratio to aim for, stated as
   instructions ("Use an ultra-bold serif or display headline at roughly
   6-8x the body text size...").
2. COLOR RULES — how many accent colors to use, what tone family (warm/cool/
   neutral/vibrant) is most consistent with this category, and whether the
   accent should stay fixed across an issue or vary by section.
3. LAYOUT/TEXT-PLACEMENT RULES — which text-placement pattern(s) recur most
   (sky_zone, overlay_bar, bottom_strip, etc.) and which layout_pattern
   value(s) (clean_title_dominant, callout_heavy, typographic_led) best fit
   this category.
4. PHOTOGRAPHY RULES — the recurring color-grading tendency, cropping
   philosophy, and bleed tendency for this category.
5. VOICE/MOOD RULES — the 4-6 mood adjectives that recur most often across
   the source set, stated as a target tone for both visual design AND for
   how body copy should read.
6. WHAT TO AVOID — explicitly call out any pattern that appeared in your
   source set but only rarely or inconsistently, so the generative system is
   told NOT to default to it just because it showed up once.

End with a single dense paragraph, 4-6 sentences, written exactly as it
should appear verbatim inside a production AI system prompt under a heading
like "DESIGN DNA TO FOLLOW FOR THIS TEMPLATE STYLE" — this final paragraph is
the literal text that will be pasted into the {{DESIGN_DNA_PATTERNS}}
placeholder in the MagazineForge generation system prompt, so write it as a
finished, ready-to-use creative brief, not as a description of your analysis
process.

Output your full reasoning as plain text (not JSON this time — this output
is meant to be read and lightly edited by a human, then pasted into a system
prompt, not parsed programmatically).

Here is the collected set of design persona analyses to synthesize:

[PASTE ALL COLLECTED PROMPT A-E JSON OUTPUTS FOR THIS CATEGORY HERE]
```

### What you do with the output
The Meta-Synthesis Prompt's final paragraph is what goes directly into Part 10.4's `{{DESIGN_DNA_PATTERNS}}` placeholder for that `TEMPLATE_STYLE`. Hand the full Meta-Synthesis output (not just the final paragraph) to the Antigravity agent along with an instruction like: *"Here's the synthesized Design DNA brief for the Travel template category. Insert the closing paragraph into the system prompt at backend/prompts/design_dna_travel.txt, and use the numbered rules above it as a comment block above that file explaining the reasoning, so future edits to the prompt stay consistent with where it came from."* This keeps the provenance of your system prompt traceable back to real analysis, rather than becoming an opaque block of text nobody remembers the reasoning behind six months from now.


---

## PART 12 — HYPER-MODULAR BUILD ORDER (Part 5, Fully Decomposed) **[v3 NEW]**

This re-expresses every phase from Part 5 at the granularity Part 9 describes — one verifiable thing per step, with its specific Definition of Done attached. Numbering follows `Phase.Step` so you can always map back to Part 5. Where a step still feels bundled once you're actually executing it, split it further per Part 9 — this list is a floor, not a ceiling. Every step implicitly carries Part 8.3's standing instruction: verify current versions before adding anything new.

### Phase 1 — Backend First

- **1.1** Create the HF Space (Docker SDK type) and push an empty FastAPI app with just a root `/` route returning `{"status": "alive"}`. *Verify: visiting the Space URL in a browser returns that JSON.*
- **1.2** Add `packages.txt` with the LuaLaTeX/Ghostscript packages from Bug 07, after searching HF's current base image for correct package names (Bug 07/Part 8.3). *Verify: the Space rebuilds successfully and the build log shows the packages installed without error.*
- **1.3** Add a `/health` endpoint (Bug 01). *Verify: `GET /health` returns 200 with the expected body, checked via Swagger UI or the Browser Subagent.*
- **1.4** Write `cover_template_a.tex` with hardcoded placeholder text (no `{{PLACEHOLDERS}}` yet) and confirm it's valid LaTeX on its own. *Verify: manually run `lualatex` against it once (locally in the Antigravity sandbox or via a one-off HF Space shell) and confirm a PDF is produced.*
- **1.5** Add `{{PLACEHOLDERS}}` syntax to the template for title/subtitle/image path only (not the full schema yet). *Verify: a hardcoded Python script with hardcoded test values, run once, produces a correctly-filled PDF.*
- **1.6** Write the `download_image` function from Bug 02, called with one hardcoded test image URL only. *Verify: the function returns a local file path, and that file is confirmably a real image (open it).*
- **1.7** Add the `convert_to_jpg` function from Bug 03, chained after 1.6. *Verify: feed it a WebP test file specifically — not just a JPG — and confirm it returns a valid JPG.*
- **1.8** Add the `normalize_image_url` function from Bug 06. *Verify: feed it a real Google Drive share-link format URL and confirm it returns the corrected direct-download form, as a unit test, not just by reading the code.*
- **1.9** Wire 1.5 through 1.8 together into one `POST /compile` endpoint accepting a hardcoded-shape JSON body. *Verify: a real POST via Swagger UI, with a real image URL in the body, returns a real downloadable PDF.*
- **1.10** Add the 90-second subprocess timeout from Bug 08. *Verify: deliberately feed it a `.tex` file you know will hang (an intentionally broken TikZ loop) and confirm the endpoint returns a timeout error within ~90 seconds rather than hanging indefinitely.*
- **1.11** Add the LaTeX sanitizer from Bug 04, applied to the title/subtitle fields only at this point. *Verify: send the literal test string from Part 8.5's checklist (`& % $ # _ ^ { } ~ \`) through `/compile` and confirm a clean PDF, not a crash.*

**Phase 1 complete when:** every step above is individually verified AND a fresh end-to-end run (new image URL, new title text, from a cold Space) produces a correct PDF, matching Part 5's original Phase 1 success criteria.

### Phase 2 — Add Gemini

- **2.1** Write a first-draft Design DNA system prompt for the cover only (a simpler precursor to Part 10.4's full version — don't jump straight to the full-issue schema yet). *Verify: by eye, read it back and confirm it actually contains the schema, an example, and the hard rules, not just vague style guidance.*
- **2.2** Add a `/generate` endpoint that calls Gemini with 2.1's prompt and returns the raw text response, unparsed. *Verify: one real call with a real topic string, confirm you get text back at all (not yet checking its structure).*
- **2.3** Add the code-fence-stripping step from Bug 10. *Verify: deliberately get Gemini to wrap its output in a \`\`\`json fence (it will, sometimes, even when told not to) and confirm the stripping logic actually removes it — test against a captured real example, not a synthetic one.*
- **2.4** Add the `CoverPage` Pydantic model and validation from Bug 10. *Verify: feed it both a valid parsed response and a deliberately broken one (missing a required field) and confirm the broken one raises a clear, catchable error rather than crashing the process.*
- **2.5** Connect `/generate`'s validated output into `/compile` from Phase 1, as one combined flow. *Verify: one real end-to-end call — topic in, real Gemini call, real validation, real LaTeX compile, real PDF out.*
- **2.6** Add the 429 rate-limit handling from Bug 19, using a currently-verified model name (Part 8.3). *Verify: this is hard to trigger deliberately on a personal account, so verify by code review plus a deliberately-malformed API key test instead, confirming the error path at least returns the friendly message rather than an unhandled exception.*

**Phase 2 complete when:** "a moody travel guide to Iceland" (Part 5's literal original success criterion) produces a real, correctly-typeset cover PDF, run fresh, not reused from an earlier successful run.

### Phase 3 — Android MVP

> Before 3.1, complete the Stitch design step for this screen per Part 0B (design in Stitch, extract DESIGN.md) — this isn't optional, per Feature 12.

- **3.1** Design the single MVP screen in Stitch: topic field, image URL field, Compile button, nothing else. *Verify: you're satisfied with the Stitch export visually before any code is written.*
- **3.2** Extract DESIGN.md from that Stitch export. *Verify: open the file, confirm it contains real hex values and font names, not placeholders.*
- **3.3** Scaffold a blank Jetpack Compose project with the Compose BOM/AGP/Gradle versions checked current (Part 8.3), no screens yet. *Verify: `./gradlew assembleDebug` succeeds on a totally empty app, locally or via the Part 7 Workflow 1 CI run.*
- **3.4** Build the static screen layout (text fields, button) using DESIGN.md's values, with no networking wired up yet — button does nothing on tap. *Verify: build, install, and visually inspect the running app against the Stitch export side by side (the Vibe Check from Feature 12).*
- **3.5** Add Retrofit2 + OkHttp, pointed at your HF Space URL, with a single method that calls `/health` only. *Verify: tap a temporary debug button and confirm a Toast/log shows a real successful response from your real backend.*
- **3.6** Wire the Compile button to call `/compile` (not yet `/generate`+`/compile` chained) with the two text field values. *Verify: real tap, real network call, confirm via logging that the request body matches what you typed.*
- **3.7** Add `PdfRenderer`-based PDF display for the response. *Verify: a real returned PDF actually renders visibly on-screen, not just "the bytes were received."*
- **3.8** Add the Bug 17 double-tap guard and the Bug 01 background health-ping. *Verify: deliberately double-tap rapidly and confirm only one request fires; verify the health ping fires on screen-entry via logging.*
- **3.9** Add basic error handling (a generic but non-crashing message on any failure). *Verify: turn off WiFi/data deliberately mid-request and confirm the app shows an error state instead of crashing or hanging forever.*

**Phase 3 complete when:** the full Part 5 Phase 3 success criterion is met — the app works on your actual phone, end-to-end, freshly installed (per Part 8.5's fresh-install check).

### Phase 4 — Template System

- **4.1** Design the Template Gallery screen in Stitch (categorized tabs, 2-column grid, search, Surprise Me) per the worked example in Part 9. *Verify: Stitch export reviewed and approved before coding.*
- **4.2** Build the static tab row only (Part 9's step 2 from the worked example). *Verify: on-device, tabs render and switch.*
- **4.3** Build the 2-column grid with placeholder thumbnails (Part 9's step 3). *Verify: on-device at 2, 3, and 7 items.*
- **4.4** Build `template_config.json` describing the 3 initial cover templates (Pattern A/B/C), each with its `.tex` filename and metadata. *Verify: the file parses correctly in a quick script/test, matches the actual `.tex` files that exist.*
- **4.5** Build cover templates B and C (A already exists from Phase 1), each independently. *Verify: each compiles to a correct PDF on its own, with its own test data, before moving to the next.*
- **4.6** Wire the grid to real `template_config.json` data instead of placeholders (Part 9's step 4). *Verify: real thumbnails (even if just solid-color placeholders at this point, per Bug 23 — real generated thumbnails come later) populate from real data.*
- **4.7** Add the search bar with local filtering only (Part 9's step 5). *Verify: typing actually narrows results on-device.*
- **4.8** Add the "Surprise Me" button (Part 9's step 6). *Verify: tapping it navigates/selects a real template, not just a log line.*
- **4.9** Add full-screen Preview Mode (Part 9's step 7). *Verify: tap-through, 3 sample pages shown, back button returns correctly with scroll position preserved.*
- **4.10** Add the live Compose preview canvas (Feature 03) as its own screen, built and Stitch-designed the same way as 4.1-4.2. *Verify: typing in the form fields visibly updates the canvas in real time, on-device.*
- **4.11** Add the human-readable form fields with the character-limit flash-red behavior from Feature 03. *Verify: deliberately exceed the limit and confirm the field refuses further input and visually flashes, rather than silently truncating.*
- **4.12** Generate real template thumbnails per Bug 23 (compile each template once with dummy content, store the PNG) instead of the Phase 4.6 placeholders. *Verify: the gallery now shows real, accurate thumbnails matching what each template actually produces.*

### Phase 5 — Interior Pages

- **5.1** Expand the Gemini system prompt from Phase 2's cover-only version to the full Part 10.4 schema, one new top-level block at a time (masthead, then toc, then articles, then back_cover) rather than all at once. *Verify each block independently: one real call per added block, confirming that specific block's JSON validates, before adding the next.*
- **5.2** Build `toc_template.tex` with hardcoded test data. *Verify: compiles to a correct PDF independently of the rest of the pipeline.*
- **5.3** Build `article_two_column.tex` with hardcoded test data. *Verify: same, independently.*
- **5.4** Build `article_three_column.tex` with hardcoded test data. *Verify: same, independently.*
- **5.5** Add the full `FullIssue` Pydantic model from Part 10.2 and the partial-failure handling described there (drop a malformed article block rather than failing the whole issue). *Verify: deliberately corrupt one article block in a test payload and confirm the issue still compiles with the remaining valid blocks.*
- **5.6** Add the chunked compilation + Ghostscript merge from Bug 05, tested first with a small (8-page) issue. *Verify: the merged PDF has the correct total page count and pages appear in the correct order.*
- **5.7** Re-test chunked compilation at the largest supported page count (24 pages, per Feature 10). *Verify: completes without an OOM kill (Bug 05) and within a reasonable time (track actual seconds, don't just confirm "it finished eventually").*
- **5.8** Add Draft Mode (Feature 05) using the `draft` LaTeX option. *Verify: a draft-mode compile is meaningfully faster (track actual seconds) and shows placeholder boxes instead of downloaded images.*
- **5.9** Add the long-magazine chunked-generation pattern from Part 10.5 (separate Gemini calls per article instead of one giant call) once page counts exceed roughly 12 pages. *Verify: a 24-page generation completes without truncated/cut-off article content.*

### Phase 6 — Polish

- **6.1** Build and wire Feature 06's Smart Error Messages, including the Part 10.4 note about routing Gemini/key errors separately from LaTeX errors. *Verify: deliberately trigger at least 3 distinct error types (bad image URL, bad LaTeX character that somehow slipped past the sanitizer, bad API key) and confirm each shows a distinct, specific message.*
- **6.2** Add the async `/compile` job-ID pattern from Feature 09 (server returns a job_id immediately). *Verify: confirm the Android app's UI actually dismisses the loading state and shows the "being crafted" message within under a second of tapping Compile, not after waiting for the full compile.*
- **6.3** Add Firebase Cloud Messaging push delivery for completed async jobs, including the Android 13+ permission request from Bug 21. *Verify: on a real Android 13+ device, confirm the permission prompt actually appears and a real push notification arrives after a real background compile.*
- **6.4** Build the Project Library screen (Feature 07), Stitch-designed per Part 0B first. *Verify: a freshly generated magazine actually appears in the library with correct thumbnail, title, date, and status.*
- **6.5** Wire Firebase Storage for permanent PDF persistence (Bug 13). *Verify: generate a magazine, force-quit the HF Space (or just wait for a natural cold-start cycle), and confirm the PDF is still retrievable the next day.*
- **6.6** Replace the URL-paste image input with the gallery photo picker + auto-upload from Feature 02. *Verify: pick a real photo from the device gallery and confirm it uploads and a usable direct URL comes back, on-device.*
- **6.7** Implement Feature 11 in full: the key entry/verification screen (Stitch-designed first), the `/verify-key` endpoint, and `EncryptedSharedPreferences` storage. *Verify against Part 8.5's specific checklist items for this feature — a valid new key, an invalid key, and a known-rejected old-style key if you can get hold of one for testing.*
- **6.8** Add the `X-App-Key` header from Bug 25, now alongside the per-user Gemini key header from Feature 11 (Bug 25's clarification note) — confirm both are present and distinct on every request. *Verify by inspecting actual outgoing request headers, not just by reading the Kotlin code.*
- **6.9** Run the full Part 8.5 final verification checklist, top to bottom, as its own dedicated step — not assumed from the individual feature verifications above. *Verify: every checkbox in Part 8.5, checked fresh, in one sitting.*
- **6.10** Run the Feature 13 "World-Class Bar" checklist for both the app UI and a sample of output magazines. *Verify: walk through every bullet in Feature 13 explicitly and fix anything that doesn't pass, rather than treating it as aspirational.*


---

## UPDATED ONE-LINE SUMMARY FOR EVERY DECISION

- Never ask Gemini to write LaTeX. It writes content. Your templates write LaTeX.
- Never trust user input. Sanitize every string before template injection.
- Never compile synchronously if the magazine is > 8 pages. Use async + push.
- Never show raw errors. Translate every error to plain English with a fix suggestion.
- Never bundle template thumbnails in APK. Generate and cache them from real compiles.
- Always pre-download and convert images before calling LuaLaTeX.
- Always run LuaLaTeX twice. Always set a 90-second timeout. Always chunk large jobs.
- Never type a real API key into an agent prompt. Use env files / secret managers and gitignore them first.
- Antigravity 2.0 builds the app; it is never part of the app. Keep that boundary clean.
- Check model names (Gemini, in particular) against Google's current docs before trusting any AI-written code sample, including this one — these names retire faster than you'd expect.
- **[v3 NEW]** Never let an unverified Gemini key reach `/generate` or `/compile`. Verify it with a real, free `models.list` call first, every time.
- **[v3 NEW]** Never let the agent design a screen from a blank prompt. Design it in Stitch, extract DESIGN.md, implement against that — every screen, no exceptions.
- **[v3 NEW]** Never treat a build that compiles as a build that works. Install it, launch it, look at it — every phase, not just at the end.
- **[v3 NEW]** Never bundle more than one independently-verifiable change into a single step, no matter how many steps that produces.
- **[v3 NEW]** Never assume a dependency, model name, or tool version is current because it worked last time, or because this document says so. Search and confirm, every time, before adding or bumping anything.
- **[v3 NEW]** Never build the Android APK locally as the build of record. GitHub Actions' clean, disposable VM is the build you trust.
- **[v3 NEW]** Never let Gemini invent an image URL. It only ever sees the literal placeholder `USER_PROVIDED_IMAGE`; your backend substitutes the real ones.
- **[v3 NEW]** Never fail an entire magazine compile over one malformed content block. Validate every block independently and drop only what's broken.

---

*This document represents the complete engineering foundation for MagazineForge, reviewed and updated for 2026, now through its third revision.
Build Phase 1 first. Prove the pipeline. Then grow — one verified micro-step at a time.*
