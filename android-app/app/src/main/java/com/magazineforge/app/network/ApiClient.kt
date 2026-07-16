package com.magazineforge.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor

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
    // Short timeouts so uploads fail fast instead of hanging for 10 min
    // when the HF Space is sleeping.
    private val uploadOkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
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
     * Compress an image to max [maxDimension] px on its longest side
     * at [quality]% JPEG quality.  Typical 10-20 MB phone photo → ~150-300 KB.
     */
    fun compressImage(
        context: android.content.Context,
        uri: android.net.Uri,
        maxDimension: Int = 1200,
        quality: Int = 80
    ): java.io.File {
        // Decode bounds only
        val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
        val origW = opts.outWidth
        val origH = opts.outHeight

        // Calculate sample size for fast downscaling
        var sampleSize = 1
        while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }

        // Decode with sample size
        val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampled = context.contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: throw Exception("Could not decode image")

        // Scale to exact max dimension
        val scale = minOf(maxDimension.toFloat() / sampled.width, maxDimension.toFloat() / sampled.height, 1f)
        val finalW = (sampled.width * scale).toInt()
        val finalH = (sampled.height * scale).toInt()
        val scaled = if (scale < 1f) {
            val s = android.graphics.Bitmap.createScaledBitmap(sampled, finalW, finalH, true)
            if (s !== sampled) sampled.recycle()
            s
        } else sampled

        // Write compressed JPEG
        val outFile = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        outFile.outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, it) }
        scaled.recycle()
        return outFile
    }

    suspend fun ensureSpaceAwake() {
        // Ping health endpoint up to 3 times to wake up the space
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
