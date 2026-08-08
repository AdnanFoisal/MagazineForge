# MagazineForge token proxy (Cloudflare Worker)

## The problem this solves

Today the HuggingFace token is compiled into the APK:

```kotlin
// ApiClient.kt:49
private val HF_TOKEN = com.magazineforge.app.BuildConfig.HF_TOKEN
```

`BuildConfig` fields become `public static final String` in `BuildConfig.class`.
Anyone can run `unzip -p app.apk classes.dex | strings | grep hf_` and read the
token out of a shipped APK in about ten seconds. R8/obfuscation does not fix
this — it renames symbols, not string constants.

## The fix

```
App --(X-App-Key)--> Worker --(Authorization: Bearer HF_TOKEN)--> HF Space
```

The HF token lives only in Cloudflare's encrypted secret store. The APK carries
an app key instead, which you can rotate in about 20 seconds without shipping a
new APK (see "Rotating the app key").

## Everything here is free

| Thing | Free-plan allowance | This proxy's usage |
|---|---|---|
| Worker requests | 100,000/day (resets 00:00 UTC) | 1 per API call |
| CPU per invocation | 10 ms | Streaming a body is I/O, not CPU |
| Egress / bandwidth | No charge, ever | Large PDFs cost nothing |
| Encrypted secrets | Free | 2 secrets |

No KV, no R2, no D1, no Durable Objects, no Queues — `wrangler.toml` contains no
binding that can bill. Exceeding the free daily request cap causes requests to be
rejected, not billed; overage rates apply only to the Paid plan. You do not need
a card.

## Endpoint allowlist

Derived by reading `android-app/app/src/main/java/com/magazineforge/app/network/ApiService.kt`
(all 18 Retrofit declarations) plus the two non-Retrofit call sites that also
resolve against `ApiClient.BASE_URL` — 20 routes in total. Anything not on this
list gets a bodyless 404, including the Space's `/`, `/docs` and `/openapi.json`.

`npm run check` re-derives this list from `ApiService.kt` and fails on drift;
`npm run deploy` runs it automatically before deploying.

| Method | Path | Source |
|---|---|---|
| POST | `/upload-asset` | `uploadAsset` |
| POST | `/upload-asset-fast` | `uploadAssetFast` (`?quality=`) |
| GET | `/health` | `checkHealth` / `ensureSpaceAwake` |
| POST | `/verify-key` | `verifyKey` |
| GET | `/job/{id}/status` | `getJobStatus` |
| GET | `/job/{id}/download` | `downloadJob` — streams the PDF |
| GET | `/job/{id}/cover` | `downloadCover` — streams the JPEG |
| POST | `/generate-brief` | `generateBrief` |
| POST | `/generate-schema` | `generateSchema` |
| POST | `/generation-runs` | `createGenerationRun` (`?generate_all=`) |
| GET | `/generation-runs/{id}` | `getGenerationRun` |
| POST | `/generation-runs/{id}/continue` | `continueGenerationRun` |
| POST | `/generation-runs/{id}/retry/{sectionId}` | `retryGenerationSection` |
| POST | `/generation-runs/{id}/cancel` | `cancelGenerationRun` |
| POST | `/generate-latex` | `generateLatex` |
| POST | `/render-page` | `renderPage` |
| POST | `/compile-raw` | `compileRaw` |
| POST | `/rewrite-selection` | `rewriteSelection` |
| GET | `/assets/{file}` | Coil image loads — see below |
| GET | `/static/samples/{file}` | Template gallery samples |

`/assets/{file}` is not in `ApiService.kt` but is required: the backend returns
`{"url": "/assets/<uuid>.jpg"}` (`backend/main.py:166,286`), `ApiClient` joins
that onto `BASE_URL`, and `MainActivity.kt:86` points Coil at
`ApiClient.okHttpClient`. Omitting it would 404 every uploaded image.

Deliberately **not** allowlisted, because the app never calls them:
`/generate-raw-latex`, `/`, `/docs`, `/openapi.json`, `/redoc`.

## Design decisions

- **Streaming.** `new Request(target, request)` and `new Response(response.body, response)`
  pass bodies through as live streams. A 20 MB PDF is never buffered into Worker
  memory. Status, `Content-Type` and `Content-Disposition` are preserved because
  the second argument to `new Response` copies status and all headers.
- **Constant-time key check.** `crypto.subtle.timingSafeEqual` requires equal-length
  inputs, and comparing raw keys would leak the expected length via an early
  return. Both sides are SHA-256'd to a fixed 32 bytes first.
- **404, not 401.** Auth failures and disallowed routes return an identical
  bodyless 404, so a prober cannot map the allowlist by diffing status codes.
- **No CORS.** The only client is a native Android app using OkHttp, which never
  sends a preflight. Adding CORS headers would only make the proxy usable from a
  browser. `OPTIONS` is not on the allowlist, so preflights get 404.
- **Header hygiene.** The inbound `X-App-Key` and `Cookie` are stripped before
  the upstream call; `Set-Cookie` is stripped from the response.

## Deploy — copy-paste

You need Node.js 18+. Nothing below requires a payment method.

If you have no Cloudflare account yet, sign up free at
<https://dash.cloudflare.com/sign-up> first. No card is requested for the free
plan.

This directory is already a complete Worker (`src/index.js` + `wrangler.toml`),
so there is no scaffold to generate — you do **not** need `npm create cloudflare`.
Just install Wrangler and log in:

```bash
# 1. From the repo root.
cd cloudflare-worker
npm install
npx wrangler login          # opens a browser, authorizes your Cloudflare account
```

<details>
<summary>If you would rather start from Cloudflare's scaffold instead</summary>

```bash
npm create cloudflare@latest -- magazineforge-proxy
# choose: "Hello World" Worker  ->  JavaScript  ->  no to deploying now
```

Then copy this directory's `src/index.js` and `wrangler.toml` over the generated
ones and continue from step 2. The scaffold adds nothing else this proxy needs.
</details>

```bash
# 2. Generate a strong app key and keep the printed value — you will paste it
#    into local.properties in step 4.
node -e "console.log(require('crypto').randomBytes(32).toString('base64url'))"
```

```bash
# 3. Store both secrets, encrypted. Each command prompts for the value;
#    paste it and press Enter. The value is never echoed and never written
#    to disk in this repo.
npx wrangler secret put HF_TOKEN     # paste your hf_... token
npx wrangler secret put APP_KEY      # paste the key from step 2
```

```bash
# 4. Deploy.
npx wrangler deploy
```

Wrangler prints the URL, e.g.
`https://magazineforge-proxy.<your-subdomain>.workers.dev`.

```bash
# 5. Smoke-test it. Replace URL and KEY with your values.
curl -i -H "X-App-Key: <your app key>" https://magazineforge-proxy.<sub>.workers.dev/health
#   -> 200 {"status":"awake"}

curl -i https://magazineforge-proxy.<sub>.workers.dev/health
#   -> 404   (no key)

curl -i -H "X-App-Key: <your app key>" https://magazineforge-proxy.<sub>.workers.dev/docs
#   -> 404   (not on the allowlist)
```

### 6. Point the app at the Worker

Two edits, both in files that already have the plumbing:

**a.** `android-app/local.properties` (gitignored, never committed) — add:

```properties
APP_KEY=<the key from step 2>
```

**b.** `android-app/app/src/main/java/com/magazineforge/app/network/ApiClient.kt`
line 46 — change exactly one line. The comment block directly above it in the
source spells this out:

```kotlin
var BASE_URL = "https://magazineforge-proxy.<your-subdomain>.workers.dev/"
```

Keep the trailing slash — Retrofit requires it.

Then rebuild the APK. The `X-App-Key` header is already being sent today (it is
simply ignored by the Space while `BASE_URL` still points there), so no other
Kotlin change is required.

### 7. Make the Space private

Only after step 6 is verified working. HuggingFace → your Space → Settings →
Change visibility → Private. Until you do this, the proxy protects the token but
the Space itself is still open to anyone who knows the URL.

## Rotating the app key without republishing the APK

You cannot — not with a single key. Rotating the Worker's `APP_KEY` invalidates
every installed APK immediately. To rotate without breaking installed apps,
overlap two keys:

1. Add a second secret alongside the current one:
   ```bash
   npx wrangler secret put APP_KEY_NEXT     # paste the new key
   ```
2. In `src/index.js`, accept either during the overlap window. Replace the auth
   check with:
   ```js
   const ok = (await secretsMatch(request.headers.get('X-App-Key'), env.APP_KEY))
     || (env.APP_KEY_NEXT && await secretsMatch(request.headers.get('X-App-Key'), env.APP_KEY_NEXT));
   if (!ok) return notFound();
   ```
   then `npx wrangler deploy`.
3. Ship an APK built with `APP_KEY=<the new key>` in `local.properties`.
4. Once users have updated, promote and clean up:
   ```bash
   npx wrangler secret put APP_KEY          # paste the new key
   npx wrangler secret delete APP_KEY_NEXT
   ```
   Revert the `src/index.js` change and `npx wrangler deploy`.

**What rotation always buys you, even with one key:** if the HF token itself
leaks or you want to invalidate it, you rotate `HF_TOKEN` on the Worker only —
`npx wrangler secret put HF_TOKEN && npx wrangler deploy`. Installed APKs keep
working, because they never knew the HF token. That is the whole point of the
proxy, and it is the rotation that actually matters.

## Rate limiting

Disabled by default. Cloudflare's Rate Limiting binding carries no separate
charge — you pay only for Workers requests and CPU, both already free at this
volume — but it needs Wrangler >= 4.36.0, and a first deploy should not be able
to fail on an optional feature. To enable it, uncomment the `[[ratelimits]]`
block in `wrangler.toml` and redeploy; `src/index.js` already checks
`if (env.APP_RATE_LIMITER)` and needs no change.

Caveat: counters are per Cloudflare location, not global, so it is a loose flood
guard rather than an exact quota. Global-accurate counting needs Durable Objects
(free-plan-compatible only with `new_sqlite_classes`), which is more machinery
than this proxy warrants.

## What this does NOT protect against

The app key is extractable from the APK too — it is XOR-chunked rather than a
plain string constant, but a determined attacker with `jadx` will recover it.
What the proxy buys you is:

- The **HF token** is no longer extractable at all. That is the credential that
  matters, because it is account-scoped and grants far more than this Space.
- The blast radius of a leaked app key is one revocable string, rotatable
  server-side.
- Only 19 specific routes are reachable, so a stolen key cannot be used to poke
  at the Space's admin surface.
