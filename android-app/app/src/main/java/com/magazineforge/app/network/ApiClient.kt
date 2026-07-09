package com.magazineforge.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import okhttp3.OkHttpClient
import okhttp3.Interceptor

object ApiClient {
    // Live Hugging Face Space deployment
    var BASE_URL = "https://adnanfoisal-magazineforge.hf.space/"
    
    // HuggingFace Personal Access Token loaded from local.properties
    private val HF_TOKEN = com.magazineforge.app.BuildConfig.HF_TOKEN

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain: Interceptor.Chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $HF_TOKEN")
                .build()
            chain.proceed(request)
        }
        .build()

    val retrofitService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
