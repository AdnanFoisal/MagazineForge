package com.magazineforge.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import java.lang.reflect.Type
import com.magazineforge.app.models.*

class PageSchemaDeserializer : JsonDeserializer<PageSchema> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): PageSchema {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type")?.asString ?: "article"
        return when (type) {
            "ad" -> context.deserialize(json, AdSchema::class.java)
            "chart" -> context.deserialize(json, ChartSchema::class.java)
            "photo_essay" -> context.deserialize(json, PhotoEssaySchema::class.java)
            "qna" -> context.deserialize(json, QnASchema::class.java)
            else -> context.deserialize(json, ArticleSchema::class.java)
        }
    }
}

class PageSchemaSerializer : com.google.gson.JsonSerializer<PageSchema> {
    override fun serialize(src: PageSchema, typeOfSrc: Type, context: com.google.gson.JsonSerializationContext): JsonElement {
        return when (src) {
            is ArticleSchema -> context.serialize(src, ArticleSchema::class.java)
            is AdSchema -> context.serialize(src, AdSchema::class.java)
            is ChartSchema -> context.serialize(src, ChartSchema::class.java)
            is PhotoEssaySchema -> context.serialize(src, PhotoEssaySchema::class.java)
            is QnASchema -> context.serialize(src, QnASchema::class.java)
        }
    }
}

object ApiClient {

    // ════════════════════════════════════════════════════════════════════
    //  ⚠  AFTER DEPLOYING THE CLOUDFLARE WORKER, CHANGE THE NEXT LINE
    // ════════════════════════════════════════════════════════════════════
    //
    //  The line below still points at the Hugging Face Space directly, which
    //  means the HF token is compiled into this APK and is extractable from the
    //  binary with `strings`. The fix is the token proxy in /cloudflare-worker.
    //
    //  Do NOT change this until `wrangler deploy` has actually succeeded — the
    //  Worker URL 404s until it exists, and switching early breaks the live app.
    //
    //  STEP 1. Deploy the Worker:      see cloudflare-worker/README.md
    //  STEP 2. Put the app key in android-app/local.properties (gitignored):
    //
    //              APP_KEY=<the same value you gave `wrangler secret put APP_KEY`>
    //
    //  STEP 3. Replace the BASE_URL line below with exactly this one line,
    //          substituting your own workers.dev subdomain:
    //
    //     var BASE_URL = "https://magazineforge-proxy.<your-subdomain>.workers.dev/"
    //
    //          Keep the trailing slash — Retrofit requires baseUrl to end in "/".
    //
    //  STEP 4. Rebuild. Nothing else in this file needs to change: the
    //          X-App-Key header is already being sent on every request (see
    //          credentialsInterceptor below), and the Worker replaces it with
    //          the real Authorization header server-side.
    //
    //  STEP 5. Only once that build is verified working, set the Hugging Face
    //          Space to Private. Until then the Space is open to anyone.
    //
    //  Stored media paths are already host-less and resolved through
    //  resolveApiUrl() at display time, so template_config.json, the Firestore
    //  showcase records, and the backend's cover_url field all follow BASE_URL
    //  automatically. One thing does NOT: uploadImageWithRetry() below returns
    //  an absolute URL that gets persisted into run_snapshot.json, so a draft
    //  saved before the flip keeps pointing at the old host. See the note there.
    // ════════════════════════════════════════════════════════════════════
    var BASE_URL = "https://adnanfoisal-magazineforge.hf.space/"

    // HuggingFace Personal Access Token loaded from local.properties.
    // Empty string when local.properties has no HF_TOKEN entry.
    private val HF_TOKEN = com.magazineforge.app.BuildConfig.HF_TOKEN

    /**
     * Reverses the build-time XOR from app/build.gradle.kts.
     *
     * The app key is stored as an int[] XOR'd against a fixed pad rather than
     * as a String constant, so it does not appear in the DEX string pool and
     * cannot be recovered with `strings | grep`.
     *
     * This is obfuscation, not encryption — anyone running jadx on the APK can
     * read the pad and reverse this in minutes. It is deliberately only a speed
     * bump. The real protection is that this key is not the HuggingFace token:
     * it only unlocks the Worker proxy's 20 allowlisted routes, and it can be
     * rotated server-side without shipping a new APK.
     *
     * Returns "" when no APP_KEY was configured at build time, which keeps
     * builds working on machines that have no secrets set up.
     */
    private fun deobfuscateAppKey(): String {
        val data = com.magazineforge.app.BuildConfig.APP_KEY_X
        val pad = com.magazineforge.app.BuildConfig.APP_KEY_PAD
        if (data.isEmpty() || pad.isEmpty()) return ""
        val bytes = ByteArray(data.size) { i ->
            ((data[i] xor pad[i % pad.size]) and 0xFF).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private val APP_KEY: String by lazy { deobfuscateAppKey() }

    /**
     * True when [host] is the backend named by [BASE_URL].
     *
     * BASE_URL is a `var` and is rewritten at runtime, so the host is recomputed
     * per request rather than captured once. A malformed BASE_URL yields null
     * here, which denies the credentials — failing closed is the right direction
     * for a credential check.
     */
    private fun isBackendHost(host: String): Boolean {
        val backendHost = BASE_URL.toHttpUrlOrNull()?.host ?: return false
        return host.equals(backendHost, ignoreCase = true)
    }

    /**
     * Sends both credentials on every request TO OUR OWN BACKEND so that
     * flipping BASE_URL to the Worker is a genuine one-line change.
     *
     *  - While BASE_URL points at the Space: Authorization is what authenticates;
     *    the Space ignores the unknown X-App-Key header.
     *  - Once BASE_URL points at the Worker: X-App-Key is what authenticates;
     *    the Worker strips it and substitutes the real Authorization header from
     *    its encrypted secret store.
     *
     * The host check is load-bearing, not defensive tidiness. This client is
     * installed as Coil's GLOBAL image loader (MainActivity), so every image the
     * app renders passes through here — including the Unsplash template
     * thumbnails in template_config.json, which are third-party absolute URLs.
     * Without the check we attach `Authorization: Bearer <HF_TOKEN>` to requests
     * bound for images.unsplash.com and hand our backend credential to a CDN we
     * do not control. The same applies to showcase PDFs, whose URLs come from a
     * client-writable Firestore collection and are therefore attacker-influenced.
     *
     * Empty header values are skipped — OkHttp rejects some malformed values and
     * an empty Bearer token is worse than no header at all.
     */
    private val credentialsInterceptor = Interceptor { chain: Interceptor.Chain ->
        val request = chain.request()
        if (!isBackendHost(request.url.host)) {
            return@Interceptor chain.proceed(request)
        }
        val builder = request.newBuilder()
        if (HF_TOKEN.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $HF_TOKEN")
        }
        if (APP_KEY.isNotEmpty()) {
            builder.addHeader("X-App-Key", APP_KEY)
        }
        // The user's own stock-photo keys, when they saved any. Read into locals
        // first so a concurrent save cannot change the value between the emptiness
        // check and the header write. The host check above is what keeps these
        // personal keys off third-party image CDNs.
        val pixabay = headerSafe(userPixabayKey)
        if (pixabay.isNotEmpty()) {
            builder.addHeader("X-Pixabay-Key", pixabay)
        }
        val pexels = headerSafe(userPexelsKey)
        if (pexels.isNotEmpty()) {
            builder.addHeader("X-Pexels-Key", pexels)
        }
        chain.proceed(builder.build())
    }

    /**
     * User-supplied Pixabay/Pexels keys, primed from SecureStorage at startup and
     * refreshed whenever Settings saves.
     *
     * Cached here instead of read per request because the interceptor runs on
     * OkHttp's dispatcher threads, where a keystore-backed
     * EncryptedSharedPreferences read on every call would be slow and repeated
     * for no gain. `@Volatile` publishes a save made on the main thread to those
     * threads without needing a lock.
     */
    @Volatile
    var userPixabayKey: String = ""

    @Volatile
    var userPexelsKey: String = ""

    /**
     * Drops anything OkHttp would reject in a header value.
     *
     * OkHttp throws on non-ASCII and control characters, and this interceptor sits
     * in front of *every* backend call — so one stray newline in a pasted key would
     * take down the whole app rather than just image search. Real Pixabay and
     * Pexels keys are alphanumeric, so nothing legitimate is lost here.
     */
    private fun headerSafe(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.all { it.code in 0x20..0x7E }) trimmed else ""
    }

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(credentialsInterceptor)
        .build()

    val gson = GsonBuilder()
        .registerTypeAdapter(PageSchema::class.java, PageSchemaDeserializer())
        .registerTypeAdapter(PageSchema::class.java, PageSchemaSerializer())
        .create()

    val retrofitService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    // ── Fast upload client ────────────────────────────────────────────
    // Balanced timeouts: short enough to fail fast on genuine network errors,
    // long enough to survive an HF Space cold start (which can take 20-30s
    // to respond to the first request after sleep).
    private val uploadOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(credentialsInterceptor)
        .build()

    val uploadService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(uploadOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    /**
     * Upload an image with guaranteed delivery. Streams the RAW content:// URI
     * bytes directly to the server — ZERO client-side processing. The server
     * does all resize + compression with Pillow (100-300ms on the server CPU
     * vs 2-5s on a phone CPU).
     *
     * @param quality "cover" for cover/back cover images (2400px, 95% JPEG),
     *                "standard" for article images (1200px, 85% JPEG, default),
     *                "low" for thumbnails (800px, 75% JPEG)
     */
    suspend fun uploadImageWithRetry(
        context: android.content.Context,
        uri: android.net.Uri,
        quality: String = "standard",
        onProgress: ((String) -> Unit)? = null
    ): String {
        // Step 1: Wake the HF Space.
        onProgress?.invoke("Connecting...")
        ensureSpaceAwake()

        val mediaType = "image/*".toMediaTypeOrNull()

        try {
            // Step 2: Upload with retry. Each attempt re-opens the URI stream
            // (the stream is consumed by writeTo on each attempt).
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    onProgress?.invoke("Uploading (attempt $attempt)...")
                    val retryStream = context.contentResolver.openInputStream(uri)
                        ?: throw Exception("Could not open image")

                    // Streaming RequestBody: reads raw bytes from the URI
                    // and writes them directly to the HTTP body. No bitmap
                    // decode, no temp file, no memory spike.
                    val retryBody = object : okhttp3.RequestBody() {
                        override fun contentType(): okhttp3.MediaType? = mediaType
                        override fun writeTo(sink: okio.BufferedSink) {
                            retryStream.use { stream ->
                                val buffer = ByteArray(64 * 1024)  // 64 KB chunks
                                var bytesRead: Int
                                while (stream.read(buffer).also { bytesRead = it } != -1) {
                                    sink.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }
                    val body = okhttp3.MultipartBody.Part.createFormData("file", "upload.jpg", retryBody)
                    val response = uploadService.uploadAssetFast(body, quality)
                    if (response.isSuccessful) {
                        val path = response.body()?.url ?: ""
                        if (path.isNotEmpty()) {
                            // BASE_URL ends in "/" and the backend returns
                            // "/assets/<file>", so naive concatenation yields a
                            // double slash ("host//assets/..."). Trim one side.
                            //
                            // This returns an ABSOLUTE url, unlike the host-less
                            // paths stored everywhere else, and it gets persisted
                            // into run_snapshot.json via schema imageUrl fields.
                            // Compile is unaffected either way — the backend's
                            // resolve_local_asset() matches on the /assets/<file>
                            // segment and reads the file off local disk, ignoring
                            // the host. The only casualty is the in-editor Coil
                            // preview of a draft that was saved before BASE_URL
                            // moved and resumed after, which 404s against the old
                            // host. Left absolute deliberately: making it
                            // host-less would require every image call site to
                            // resolve it, and resolveApiUrl() already accepts
                            // both forms if that trade is ever worth making.
                            return "${BASE_URL.trimEnd('/')}/${path.trimStart('/')}"
                        }
                    }
                    val code = response.code()
                    if (code in 400..499 && code != 429) {
                        val errBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                        throw Exception("Upload rejected ($code): ${errBody ?: "no detail"}")
                    }
                    lastError = Exception("Upload failed ($code)")
                } catch (e: Exception) {
                    lastError = e
                }
                if (attempt < 3) {
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
            throw lastError ?: Exception("Upload failed after 3 attempts")
        } finally {
            // Nothing to clean up — we never created a temp file
        }
    }

    /**
     * [DEPRECATED] Client-side image compression. Kept for backwards
     * compatibility but no longer called. The server now does all image
     * processing via /upload-asset-fast, which is 5-10x faster because
     * the server CPU is much faster than a phone CPU at image work.
     */
    fun compressImage(
        context: android.content.Context,
        uri: android.net.Uri,
        maxDimension: Int = 1200,
        quality: Int = 80
    ): java.io.File {
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        val origW = opts.outWidth
        val origH = opts.outHeight

        var sampleSize = 1
        while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: throw Exception("Could not decode image")

        val scale = minOf(maxDimension.toFloat() / sampled.width, maxDimension.toFloat() / sampled.height, 1f)
        val finalW = (sampled.width * scale).toInt()
        val finalH = (sampled.height * scale).toInt()
        val scaled = if (scale < 1f) {
            val s = android.graphics.Bitmap.createScaledBitmap(sampled, finalW, finalH, true)
            if (s !== sampled) sampled.recycle()
            s
        } else sampled

        val outFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        outFile.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, it) }
        scaled.recycle()
        return outFile
    }

    suspend fun ensureSpaceAwake() {
        for (i in 1..3) {
            try {
                val response = retrofitService.checkHealth()
                if (response.isSuccessful) return
            } catch (e: Exception) {
                // Ignore and retry
            }
            kotlinx.coroutines.delay(2000)
        }
    }
}
