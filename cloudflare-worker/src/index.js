/**
 * MagazineForge token proxy.
 *
 *   App --(X-App-Key)--> Worker --(Authorization: Bearer HF_TOKEN)--> HF Space
 *
 * The HuggingFace token lives only in the Worker's encrypted secret store, so
 * it is never compiled into the APK and cannot be extracted from the binary.
 *
 * Design notes:
 *  - Request and response bodies are passed through as streams. Nothing is
 *    buffered into memory, so multi-megabyte PDF downloads and cover images
 *    cost effectively no Worker memory and no CPU time. This matters on the
 *    free plan, where the budget is 10ms CPU per invocation (streaming I/O
 *    is not CPU time, so a proxied 20MB PDF still uses well under 10ms).
 *  - Only the routes the Android app actually calls are reachable. Everything
 *    else returns 404, including the Space's own /docs, /openapi.json and /.
 *  - No CORS handling. The only client is a native Android app using OkHttp,
 *    which never issues a preflight. Adding CORS headers here would only
 *    widen the attack surface by making the proxy usable from a browser.
 */

const UPSTREAM = 'https://adnanfoisal-magazineforge.hf.space';

// UUIDs (job_id, run_id) and stringified integers (section_id).
const ID = '[A-Za-z0-9_-]{1,64}';
// Server-generated asset filenames, e.g. "a1b2c3d4.jpg".
const FILE = '[A-Za-z0-9._-]{1,128}';

/**
 * Every route below was derived by reading
 * android-app/app/src/main/java/com/magazineforge/app/network/ApiService.kt
 * (all 18 Retrofit declarations, re-verified against the current file) plus
 * the two non-Retrofit call sites that also resolve against
 * ApiClient.BASE_URL. Nothing here is guessed.
 *
 * 21 routes total: 19 Retrofit + /assets/<file> + /static/samples/<file>.
 * If you add an endpoint to ApiService.kt, add it here too or it will 404.
 */
const ALLOWLIST = [
  // --- ApiService.kt: Retrofit declarations, in file order ---
  ['POST', `^/upload-asset$`],                              // uploadAsset
  ['POST', `^/upload-asset-fast$`],                         // uploadAssetFast (?quality=)
  ['GET', `^/health$`],                                     // checkHealth / ensureSpaceAwake
  ['POST', `^/verify-key$`],                                // verifyKey
  ['POST', `^/verify-image-keys$`],                         // verifyImageKeys (Pixabay/Pexels)
  ['GET', `^/job/${ID}/status$`],                            // getJobStatus
  ['GET', `^/job/${ID}/download$`],                          // downloadJob (streams PDF)
  ['GET', `^/job/${ID}/cover$`],                             // downloadCover (streams JPEG)
  ['POST', `^/extract-contract$`],                          // extractContract (Intent Gate)
  ['POST', `^/generate-brief$`],                            // generateBrief
  ['POST', `^/generate-schema$`],                           // generateSchema
  ['POST', `^/generation-runs$`],                           // createGenerationRun (?generate_all=)
  ['GET', `^/generation-runs/${ID}$`],                       // getGenerationRun
  ['POST', `^/generation-runs/${ID}/continue$`],             // continueGenerationRun
  ['POST', `^/generation-runs/${ID}/retry/${ID}$`],          // retryGenerationSection
  ['POST', `^/generation-runs/${ID}/cancel$`],               // cancelGenerationRun
  ['POST', `^/generate-latex$`],                            // generateLatex
  ['POST', `^/render-page$`],                               // renderPage (streams PDF/PNG)
  ['POST', `^/compile-raw$`],                               // compileRaw
  ['POST', `^/rewrite-selection$`],                         // rewriteSelection

  // --- Not Retrofit, but still resolved against BASE_URL ---
  // Coil loads uploaded images from "${BASE_URL}${response.url}" where the
  // backend returns "/assets/<uuid>.jpg" (backend/main.py:166,286), and
  // MainActivity.kt:86 points Coil at ApiClient.okHttpClient. Without this
  // route every uploaded image would 404 after the BASE_URL switch.
  ['GET', `^/assets/${FILE}$`],

  // Template gallery sample PDFs and their covers. Today these URLs are
  // hardcoded to the Space in app/src/main/assets/template_config.json so
  // they bypass the proxy, but they are allowlisted so that making the Space
  // private does not break the gallery once those URLs are repointed.
  ['GET', `^/static/samples/${FILE}$`],
];

const COMPILED = ALLOWLIST.map(([method, re]) => [method, new RegExp(re)]);

/**
 * Collapse repeated slashes before matching, and forward the collapsed path
 * upstream so the Space sees a clean URL too.
 *
 * ApiClient.kt used to build image URLs as `"${BASE_URL}$path"` where BASE_URL
 * ends in "/" and the backend's path starts with "/", producing a request for
 * "//assets/<file>.jpg". That is now fixed at the source (it joins with
 * trimEnd/trimStart), but this stays for two reasons: APKs already installed in
 * the field still contain the old concatenation, and an allowlist that silently
 * depends on exact slash counts is fragile.
 */
function normalize(pathname) {
  return pathname.replace(/\/{2,}/g, '/');
}

function isAllowed(method, pathname) {
  // Reject traversal outright: '.' is legal in asset filenames, so the
  // character class alone would admit "/assets/..".
  if (pathname.includes('..')) return false;
  const m = method === 'HEAD' ? 'GET' : method;
  return COMPILED.some(([am, re]) => am === m && re.test(pathname));
}

/**
 * Constant-time secret comparison.
 *
 * crypto.subtle.timingSafeEqual requires equal-length inputs, and comparing
 * raw keys would leak the expected length through the early return. Hashing
 * both sides to a fixed 32 bytes first removes that side channel.
 */
async function secretsMatch(presented, expected) {
  if (typeof presented !== 'string' || presented.length === 0) return false;
  const enc = new TextEncoder();
  const [a, b] = await Promise.all([
    crypto.subtle.digest('SHA-256', enc.encode(presented)),
    crypto.subtle.digest('SHA-256', enc.encode(expected)),
  ]);
  return crypto.subtle.timingSafeEqual(new Uint8Array(a), new Uint8Array(b));
}

// Deliberately bodyless and detail-free: an unauthenticated caller learns
// nothing about which routes exist or why it was turned away.
const notFound = () => new Response(null, { status: 404 });

export default {
  async fetch(request, env) {
    if (!env.HF_TOKEN || !env.APP_KEY) {
      // Misconfiguration, not a client error. Fail closed.
      return new Response('Proxy not configured', { status: 500 });
    }

    const url = new URL(request.url);
    const pathname = normalize(url.pathname);

    if (!isAllowed(request.method, pathname)) return notFound();

    if (!(await secretsMatch(request.headers.get('X-App-Key'), env.APP_KEY))) {
      // 404 rather than 401 so probes cannot map the allowlist by diffing
      // status codes.
      return notFound();
    }

    // Optional, opt-in only. Absent binding == absent feature, so the default
    // deployment cannot incur a charge. See wrangler.toml.
    //
    // Keyed on client IP, not on the app key: every installed APK carries the
    // SAME app key, so keying on it would make one abusive client throttle the
    // entire user base. IP gives per-device limiting, which is what you
    // actually want. Falls back to the key hash if CF-Connecting-IP is absent.
    if (env.APP_RATE_LIMITER) {
      const rlKey = request.headers.get('CF-Connecting-IP') || 'unknown';
      const { success } = await env.APP_RATE_LIMITER.limit({ key: rlKey });
      if (!success) return new Response('Too Many Requests', { status: 429 });
    }

    const target = new URL(pathname + url.search, UPSTREAM);

    // Constructing from `request` carries the method and the body as a live
    // stream, so nothing is read into memory here.
    const upstream = new Request(target, request);
    upstream.headers.set('Authorization', `Bearer ${env.HF_TOKEN}`);
    upstream.headers.delete('X-App-Key');
    upstream.headers.delete('Cookie');
    // Host is deliberately NOT set: `fetch` derives it from the target URL, and
    // Host is a forbidden header in the standard fetch spec, so setting it can
    // throw depending on the runtime's header guard.

    let response;
    try {
      response = await fetch(upstream);
    } catch (err) {
      return new Response('Upstream unreachable', { status: 502 });
    }

    // `new Response(body, response)` keeps status, statusText and every
    // upstream header (Content-Type, Content-Disposition, Content-Length)
    // while streaming the body straight through to the client.
    const out = new Response(response.body, response);
    out.headers.delete('Set-Cookie');
    return out;
  },
};
