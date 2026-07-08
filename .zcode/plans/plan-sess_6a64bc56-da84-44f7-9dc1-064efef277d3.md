## Packet 2 — Backend Reliability Hardening (9 tasks, backend-only)

Files in scope: `backend/main.py`, `backend/gemini_service.py`, `backend/requirements.txt`. No UI/Android changes.

---

### Task 1 — Gemini structured output at API level
**File:** `gemini_service.py`, `generate_full_magazine_schema()`.
- SDK 0.8.6: pass `response_mime_type="application/json"` inside `generation_config` (confirmed it's NOT a direct `GenerativeModel.__init__` arg).
- Change `model = genai.GenerativeModel('gemini-3.5-flash')` to include `generation_config={"response_mime_type": "application/json"}`.
- Keep the existing `clean_json_text()` as a safety net (it's idempotent on already-clean JSON).
- Do NOT add a `response_schema` (MagazineSchema's nested Optionals + lists-of-objects often trigger Gemini API schema-rejection; `response_mime_type` alone forces valid JSON and satisfies the acceptance check).
- **Acceptance:** `inspect`/grep confirms an explicit `response_mime_type` parameter is now passed, not just prompt text. Verified by a live call returning valid JSON.

### Task 2 — SQLite persistence for jobs (replaces `JOBS` dict)
**File:** `backend/main.py`.
- Add a `JobStore` class wrapping `sqlite3` (`jobs.db` in backend dir). Columns: `job_id` (PK), `status`, `progress`, `pdf_path`, `error`, `cover_url`, `created_at`.
- Methods: `create(job_id)`, `update(job_id, **fields)`, `get(job_id) -> dict|None`.
- Replace all `JOBS[...]` usages: creation (line 181), field mutations (progress/cover_url/status/pdf_path/error), `.get()` reads in `/job/{id}/status`, `/download`, `/cover`, and the cleanup `del JOBS[job_id]` (line 175).
- Keep on-disk PDFs in `workspace/<job_id>/` as-is — they already persist; only metadata was lost.
- Mirror the existing workspace-cleanup cap (15 jobs) by pruning old SQLite rows alongside `cleanup_old_workspaces`.
- **Acceptance:** create a job, "restart" by re-instantiating the app/JobStore, confirm `/job/{id}/status` + `/download` still resolve (unit-tested with TestClient by creating a new process / re-import). Note: full HF Space *rebuild* wipes the filesystem — out of scope for SQLite; flagged as a known limitation.

### Task 3 — Retry with exponential backoff on transient failures
**File:** `gemini_service.py`.
- Add `call_gemini_with_retry(fn, *args, **kwargs)` helper: retries on transient errors (`google.api_core.exceptions.DeadlineExceeded`, `ServiceUnavailable`, `InternalServerError`, `RetryError`, timeouts/`ConnectionError`) with exponential backoff (e.g. 1s, 2s, 4s; max 3 attempts). Fails immediately (no retry) on 4xx: `Unauthenticated`/`PermissionDenied` (401/403), `ResourceExhausted` (429), and `InvalidArgument`.
- Wrap the `model.generate_content()` calls in both `generate_full_magazine_schema()` and `generate_raw_latex()` (inside its model-fallback loop).
- **Acceptance:** unit test with a mock that raises a transient error once → at least one retry before failing; a mock raising a 401/429-type error → zero retries, immediate clear error.

### Task 4 — Startup self-check
**File:** `backend/main.py`.
- Add `run_self_check()`: verify `templates/` dir + expected files (`cover/toc/article_template_{a,b,c}.tex`) exist, and `shutil.which("lualatex")` / `shutil.which("gs")` resolve. Log clear `WARNING`/`ERROR` for anything missing.
- Run it inside the existing `startup_event()` (after the cleanup task). Keep it fast (file-stat + which are cheap) so it doesn't block `/health`.
- Log only — don't crash the Space (better to boot and report than to die).
- **Acceptance:** temporarily rename a template → startup logs a clear "missing template" message. (Tested locally by pointing the check at a missing file.)

### Task 5 — Confirm `/health` is a lightweight pre-warm ping
**File:** `backend/main.py`.
- Already returns `{"status": "awake"}` with no slow init. Confirm the Task 4 self-check doesn't block it (fast + non-network). No code change expected beyond confirming.
- **Acceptance:** note that `/health` is dependency-free and fast on cold start; flag the Android-side pre-warm ping as a note for Packet 3 (which owns Android changes).

### Task 6 — Hard timeout on LuaLaTeX compile
**File:** `backend/main.py`, `process_compile_raw_async()`.
- Define `COMPILE_TIMEOUT = 90` module constant (gs stays 15s).
- Wrap the `subprocess.run` calls to catch `subprocess.TimeoutExpired` explicitly and raise a clean `Exception(f"Compilation timed out after {COMPILE_TIMEOUT}s")` → sets job FAILED with a clear message (rather than a generic traceback).
- Keep the 2-run lualatex pass (needed for TOC/refs to settle) but ensure a timeout surfaces cleanly.
- **Acceptance:** unit test feeding a pathological LaTeX (e.g. infinite-loop `\loop`) → FAILED status with a "timed out" message within the window (tested in mock-off mode locally if lualatex available; otherwise via a simulated timeout).

### Task 7 — Duplicate compile guard (optional, server-side)
**File:** `backend/main.py`.
- Packet marks this optional; primary fix is client-side in Packet 3. I'll add a documented note (comment + flag in summary) rather than a heavy idempotency scheme, to stay within scope. No behavioral change this packet.
- **Acceptance:** explicitly documented as deferred to Packet 3 per the packet's own instruction.

### Task 8 — Resize/compress images before compile
**File:** `backend/main.py`, `download_and_convert_image()`.
- After opening the image (and after mode normalization), before save: if `max(width, height) > 2000`, call `img.thumbnail((2000, 2000))` (in-place, preserves aspect ratio, downsamples). Keep existing `quality=90` JPEG save.
- Applies to both downloaded images and base64 images. (Gold placeholder is already 800×1000 — no change needed.)
- **Acceptance:** pass a 12MP+ test image through `download_and_convert_image` → output is ≤2000px on the long edge; verify file size dropped vs. original.

### Task 9 — Add `python-magic` to requirements.txt
**File:** `backend/requirements.txt`.
- Add `python-magic>=0.4.27` (for Packet 4 upload content-type validation). Do NOT add opencv.
- **Acceptance:** `pip install -r requirements.txt` succeeds. Note: `python-magic` on Windows dev may need `python-magic-bin` at runtime (Linux/HF Space is fine) — flagged for Packet 4.

---

### Order of execution
1 → 3 (both edit `gemini_service.py`, land together), then 2 → 4 → 5 → 6 → 8 (all `main.py`), then 9, then 7 (note-only). Show real diff + run each acceptance check after each. Run the full backend test suite at the end and re-run the Packet 1 live Gemini test to confirm no regression. Stop and summarize.

### Notes for later packets (flagged, not done)
- **Packet 3:** Android-side pre-warm ping on `/health` (Task 5) + client-side button-disable audit (Task 7).
- **Packet 4:** `python-magic` runtime use; possible opencv addition.
- **Known limitation (Task 2):** SQLite survives Space sleep/restart; a full rebuild still wipes the HF ephemeral filesystem (would need `/data` persistent volume — out of scope here).