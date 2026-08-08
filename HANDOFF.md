# MagazineForge — Handoff

Written for whoever picks this up next, human or agent. It states what is done,
what is broken, what I broke, and how to verify each remaining item.

Everything below reflects the repo as of commit `8b4c2a5` (main) and `8b74a5d`
(backend / HF Space).

---

## 0. Do these three things first — they are security, not features

### 0.1 Rotate the Hugging Face token (URGENT)

A token was pasted into a chat transcript to let me deploy. Treat it as public.

```
Rotate at: https://huggingface.co/settings/tokens
Revoke the old one. Do not reuse it.
```

The repo is clean — the token was passed via a credential helper and never
written to `.git/config`, a file, or a commit. Verify:

```bash
cd backend && git config --get remote.origin.url    # must NOT contain hf_
git log -p | grep -c 'hf_[A-Za-z0-9]\{20,\}'        # must print 0
```

### 0.2 Rotate the Pixabay and Pexels API keys (URGENT)

`backend/.env` was committed in the past and the keys are still recoverable from
the **public** HF Space history (commits `3694c7d` and `ad396ed`). Anyone can
clone that repo and read them.

I removed `.env` from tracking and `.gitignore` now covers it, but **deleting a
file does not purge history**. Rotate both keys. Then set them as HF Space
secrets (Settings → Variables and secrets), because the Space no longer gets
them from `.env`.

Verify: `curl https://huggingface.co/spaces/AdnanFoisal/MagazineForge/raw/3694c7d/.env`
should 404 only *after* you also purge history; otherwise rotation is the fix.

### 0.3 The release keystore is compromised

`android-app/app/release.keystore` has been tracked since commit `c78ca4d`, with
`storePassword`/`keyPassword` hardcoded as `"android"` in `build.gradle.kts:63-70`.
Its private key is permanently recoverable, so it **cannot sign anything you
publish to Play**. Fine if you only sideload.

Migration is written up in `BUILD_AND_INSTALL.md` under "Release signing". I did
not execute it because generating a new key destroys your current signing
identity — that is your call.

---

## 1. What I broke and reverted — read before touching the backend

I added a lualatex sandbox (`openin_any=p` / `openout_any=p` in `latex_env()`)
intending to stop user-submitted LaTeX from reading files outside its workspace.

**It took the entire Space down.** Paranoid mode forbids dotfiles, and
luaotfload keeps its font cache under `~/.texlive*/` — a dotfile path. The engine
could neither read nor write that cache, luaotfload's init aborted, and because
the LuaTeX format loads luaotfload *unconditionally*, **every compile failed**,
including plain documents that never load fontspec. That is why it looked like a
font problem for several deploy cycles.

I tried three narrower fixes (drop the build-time cache, drop `fonts-noto-cjk`,
relocate `TEXMFVAR`). None cleared it. Commit `8b74a5d` reverts the sandbox
entirely and restores `Dockerfile` + `packages.txt` to `0bfe991` verbatim.

**Backend is confirmed healthy at `8b74a5d`.**

### The security hole this leaves open (real, unfixed)

`/compile-raw` accepts arbitrary LaTeX from any caller. Without the sandbox, a
crafted document can `\input{/etc/passwd}` or similar and exfiltrate file
contents into the returned PDF.

**Do not re-add the sandbox directly to production.** The correct approach:

1. Duplicate the Space as a staging Space.
2. Set `TEXMFVAR` to a non-dotfile path **and** confirm luaotfload actually
   builds a usable cache there (`luaotfload-tool --update` exits non-zero in the
   Docker build sandbox because `TERM` is unset — that is a red herring, but the
   cache it produces as root was unreadable at runtime).
3. Success metric: a **plain** document (`\documentclass{article}` … no
   fontspec) compiles AND `\input{/etc/passwd}` fails. Both must hold.

---

## 2. Git state

| Repo | Commit | Pushed? |
|---|---|---|
| `MagazineForge` (GitHub) | `8b4c2a5` | **NO — local only.** Push when ready. |
| `backend/` (HF Space) | `8b74a5d` | Yes, live and healthy |

The main repo tracks `backend/` as an embedded git repo (gitlink), so
`git status` shows ` M backend` whenever the backend commit moves. That is
expected, not an error.

```bash
git push origin main          # publishes the Android work to GitHub
```

---

## 3. Work that is DONE and verified

Do not redo these.

### Android (commit `8b4c2a5`)
- **Two live credential leaks closed.** `ApiClient`'s interceptor attached
  `Authorization: Bearer <HF_TOKEN>` to *every* request with no host check, and
  `MainActivity` installs that client as Coil's **global** image loader — so
  every Unsplash template thumbnail handed your backend token to a third-party
  CDN. `PdfViewerScreen` separately sent the token to whatever URL it was given,
  including `pdfUrl` values from the client-writable `public_showcase` Firestore
  collection. Both now scope credentials to the `BASE_URL` host, failing closed.
- Library search / sort / rename, plus four defects found in review of that work
  (frozen rename field over 60 chars, failed cover move reported as success,
  delete leaking a cover, non-Latin titles rendering blank).
- Title parsing unified between `HomeScreen` and `MyMagazinesScreen`.
- Gradle heap raised to 3g — `assembleRelease` previously died in R8.
- **Both `assembleDebug` and `assembleRelease` build clean.** Release APK is
  11.5 MB (R8 shrinks it from 72 MB).

### Backend (commit `8b74a5d`)
- **`sanitize_latex` was corrupting every LaTeX command in body copy.** It
  swapped commands for `__LATEX_CMD_n__` placeholders, escaped, then restored —
  but `_` is in the escape set, so the placeholder was itself escaped and restore
  could never match. Output reached the page as `\_\_LATEX\_CMD\_0\_\_`.
  Rewritten to strip markup then escape; verified inert against 15 cases.
- **Chart fixes (three separate bugs):** tick labels were stripped not escaped
  (`R&D` → `RD`); variant C horizontal charts drew **no bars at all**
  (coordinate order was `(symbol, value)`, `xbar` needs `(value, symbol)`); and
  pgfplots symbolic coords are whitespace-sensitive, so `(40.5, c0)` collapsed
  all bars onto one row.
- Back-cover "black underline" removed — it was a duplicate tagline node
  rendering ~10pt below its intended 2pt offset.
- `find_ghostscript()` shared by the startup check and both call sites.
- `test_compile_raw.py` no longer catches the exceptions it raises (it could not
  fail, even with no network) and is gated behind `RUN_LIVE_TESTS=1`.
- **Backend suite: 13 passed, 1 skipped, 0 failed.**

---

## 4. REMAINING WORK

### Task A — Finish the 4-magazine quality run (main outstanding task)

Everything is staged; the run was interrupted by the outage.

**Assets already in place:**
- `C:\Users\adnan\.claude\jobs\286d5c8b\tmp\build_real.py` — complete build
  pipeline (resolves real photos → assembles LaTeX → compiles → saves PDF).
- `C:\Users\adnan\Downloads\magazineforge_real_pdfs\content\rewilding.json` —
  one of four content files, already written.

**What is missing:** three content JSON files — `deepsea.json`,
`fermentation.json`, `nighttrain.json`. They were generated but only
`rewilding.json` reached disk. Regenerate with four parallel agents using the
prompt template in §6 below (topics: Deep Sea Mining, The Fermentation Revival,
The Return of the Night Train).

**Then run:**
```bash
cd /c/Users/adnan/.claude/jobs/286d5c8b/tmp
python build_real.py
```

It prints per-magazine image resolution, assembly size, and compile result, and
writes PDFs + `results.json` to `magazineforge_real_pdfs/`.

**Success metrics — all four magazines must pass all four:**

| # | Metric | How to verify |
|---|---|---|
| 1 | **Compiles** | `results.json` shows `ok: true` for all 4 |
| 2 | **No gold placeholder** | `image_misses` is empty for every magazine. Visually: no solid gold/tan rectangle on any page. The gold block is `create_gold_placeholder()` firing because an image URL was empty or failed to download. |
| 3 | **Charts make sense** | Bars actually render (not an empty axis), bar heights match the `data_points` values, axis labels legible and not overlapping, special characters intact in tick labels. |
| 4 | **Back cover clean** | Full-bleed photo, white tagline, **no dark doubled/ghosted text under the tagline**. |

Plus content coherence: articles share one editorial thesis, bylines differ, no
two articles open the same way, TOC entries match article headlines.

### Task B — Cosmetic defects found but NOT fixed

All confirmed by visual inspection of rendered pages. None break compilation.

1. **`SecondaryColor` is `#FFFFFF`** → TOC subtitle and article subtitles render
   white-on-white and are invisible. Affects every magazine. Highest-value fix
   here. In `main.py`, the `secondary_hex` selection around the theme block.
2. **Empty tinted boxes** — sidebar/callout containers are still drawn when their
   content is empty, leaving coloured rectangles with nothing in them.
3. **Long strings truncate mid-word** with no ellipsis (TOC teasers, sidebar
   bullets, pull quotes).
4. **Ad-page contrast** — dark text on the olive/accent ad background, and page
   numbers over coloured bands, are close to illegible.
5. **TOC accent rule orphans a page** — the full-height rule in
   `toc_template_c.tex` can be pushed onto its own otherwise-empty page.
6. **Dangling `By`** when an article has no byline.
7. **Cover title bisected** by the colour band boundary on some covers.

### Task C — Fonts (deliberately deferred)

`lualatex` **silently drops** any codepoint the font lacks — no tofu box, no
warning, the text simply vanishes. Currently missing: **all CJK**, **Cyrillic**,
and **lowercase Greek**. A Japanese magazine compiles clean and is missing its
text.

Also: `fonts-texgyre` is listed in `packages.txt` but `\IfFontExistsTF{TeX Gyre
Heros}` fails on the Space, so every magazine renders in the Latin Modern
fallback rather than the intended typography.

**Do not fix this on production.** My attempts here are what caused the outage.
`fonts-noto-cjk` ships a `.ttc` collection that this TeX Live's luaotfload may
fail to parse. Use a staging Space and `.ttf`-based fonts (e.g.
`fonts-ipafont-gothic`, `fonts-nanum`).

Success metric: a probe document renders 日本語 and Привет visibly, **and** a
plain document still compiles.

### Task D — No Android test sources exist

`android-app/app/src/test` does not exist, so `testDebugUnitTest` passes
trivially and CI proves nothing about the Android side. Worth adding tests for
`sanitize`-style filename logic in `MyMagazinesScreen.kt` and `resolveApiUrl`.

---

## 5. Useful commands

```bash
# Backend health (must print COMPLETED)
cd backend && python - <<'EOF'
import time, requests
API="https://adnanfoisal-magazineforge.hf.space"
tex=r"\documentclass{article}\pagestyle{empty}\begin{document}OK\end{document}"
j=requests.post(f"{API}/compile-raw",json={"latexCode":tex},timeout=60).json()["jobId"]
for _ in range(40):
    time.sleep(4)
    s=requests.get(f"{API}/job/{j}/status",timeout=30).json()
    if str(s["status"]).upper() in ("COMPLETED","FAILED"): break
print(s["status"], str(s.get("error",""))[:200])
EOF

# Backend tests
cd backend && python -m pytest test_e2e.py test_generation_runs.py test_v2.py -q

# Android
cd android-app && ./gradlew assembleDebug && ./gradlew assembleRelease

# Deploy backend to the Space (token via helper, never in config)
cd backend
export HF_TOKEN='<new token>'
git -c 'credential.helper=!f() { echo username=hfuser; echo "password=$HF_TOKEN"; }; f' \
    push origin HEAD:main
```

**After any Space deploy, always verify a PLAIN document compiles** — not just a
magazine. That is the check that would have caught my outage immediately.

---

## 6. Content-generation prompt (for regenerating the 3 missing JSONs)

Run four agents in parallel, one topic each. Full working example already on disk
at `magazineforge_real_pdfs/content/rewilding.json` — match its shape exactly.

Key constraints that matter for the pipeline:
- Exactly 4 articles, 5 TOC entries.
- Chart: 5–6 data points, all positive, largest under 100, telling a clear story.
- `image_query` fields must be **literal and photographable** ("beaver swimming
  in water", not "hope for the future"). These hit Pixabay/Pexels; a query with
  no result produces the gold placeholder, which fails metric 2. Every query in
  a magazine must be different.
- **Never** use `% & $ # _ { } ~ ^` — write "40 percent", "and", "USD 3.50".
  The escaper handles them now, but avoiding them keeps output clean.
