/**
 * Fails if src/index.js ALLOWLIST has drifted from ApiService.kt.
 *
 * The allowlist is a security control: an endpoint missing from it 404s at
 * runtime with no build-time signal. This already happened once (the
 * generation-runs/{id}/cancel route was added to ApiService.kt after the
 * Worker was written), so the check is automated.
 *
 * Run:  npm run check
 * No dependencies, no network. Exits 1 on drift.
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const API_SERVICE = resolve(
  here,
  '../../android-app/app/src/main/java/com/magazineforge/app/network/ApiService.kt',
);
const WORKER = resolve(here, '../src/index.js');

const kotlin = readFileSync(API_SERVICE, 'utf8');
const worker = readFileSync(WORKER, 'utf8');

// @POST("generation-runs/{run_id}/retry/{section_id}") -> POST /generation-runs/:/retry/:
const declared = [...kotlin.matchAll(/@(GET|POST|PUT|PATCH|DELETE)\("([^"]+)"\)/g)].map(
  ([, method, path]) => `${method} /${path.replace(/\{[^}]+\}/g, ':')}`,
);

// ['POST', `^/generation-runs/${ID}/retry/${ID}$`] -> POST /generation-runs/:/retry/:
const allowed = [...worker.matchAll(/\['(GET|POST|PUT|PATCH|DELETE)',\s*`\^([^`]+)\$`\]/g)].map(
  ([, method, re]) =>
    `${method} ${re.replace(/\$\{ID\}/g, ':').replace(/\$\{FILE\}/g, ':')}`,
);

const missing = declared.filter((r) => !allowed.includes(r));
const extra = allowed.filter(
  (r) => !declared.includes(r) && !/\/(assets|static\/samples)\//.test(r),
);

for (const r of missing) console.error(`MISSING from Worker ALLOWLIST: ${r}`);
for (const r of extra) console.error(`EXTRA in Worker ALLOWLIST (not in ApiService.kt): ${r}`);

if (missing.length || extra.length) {
  console.error(
    `\nDrift detected. ApiService.kt declares ${declared.length} endpoints; ` +
      `ALLOWLIST has ${allowed.length} routes.`,
  );
  process.exit(1);
}

console.log(
  `OK: all ${declared.length} ApiService.kt endpoints are allowlisted ` +
    `(${allowed.length} routes total, incl. /assets and /static/samples).`,
);
