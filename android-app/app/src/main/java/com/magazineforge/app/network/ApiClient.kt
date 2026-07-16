package com.magazineforge.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor
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
    // Live Hugging Face Space deployment
    var BASE_URL = "https://adnanfoisal-magazineforge.hf.space/"
    
    // HuggingFace Personal Access Token loaded from local.properties
    private val HF_TOKEN = com.magazineforge.app.BuildConfig.HF_TOKEN

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(600, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $HF_TOKEN")
                .build()
            chain.proceed(request)
        }
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
        .addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $HF_TOKEN")
                .build()
            chain.proceed(request)
        }
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
     * Upload an image with guaranteed delivery. Wakes the HF Space first,
     * then retries the upload up to 3 times with exponential backoff.
     * Returns the full URL on success, throws on permanent failure.
     *
     * This is the ONLY upload method the UI should call — it handles all
     * the edge cases (cold Space, transient network errors, compression)
     * so the UI code stays simple.
     */
    suspend fun uploadImageWithRetry(
        context: android.content.Context,
        uri: android.net.Uri,
        onProgress: ((String) -> Unit)? = null
    ): String {
        onProgress?.invoke("Waking server...")
        ensureSpaceAwake()

        onProgress?.invoke("Compressing...")
        val compressed = compressImage(context, uri)

        try {
            var lastError: Exception? = null
            for (attempt in 1..3) {
                try {
                    onProgress?.invoke("Uploading (attempt $attempt)...")
                    val requestFile = compressed.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val body = okhttp3.MultipartBody.Part.createFormData("file", compressed.name, requestFile)
                    val response = uploadService.uploadAsset(body)
                    if (response.isSuccessful) {
                        val path = response.body()?.url ?: ""
                        if (path.isNotEmpty()) {
                            return "${BASE_URL}$path"
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
            compressed.delete()
        }
    }

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
