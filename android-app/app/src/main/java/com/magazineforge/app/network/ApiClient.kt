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
