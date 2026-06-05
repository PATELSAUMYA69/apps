package com.example.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

object RetrofitInstance {
    private const val DEFAULT_BASE_URL = "https://example.com/api/"

    private val authInterceptor = Interceptor { chain ->
        val baseUrl = BuildConfig.SYNC_API_BASE_URL
        val apiKey = BuildConfig.SYNC_API_KEY ?: ""
        
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer \$apiKey")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("Network", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: SyncApi by lazy {
        val baseUrl = BuildConfig.SYNC_API_BASE_URL.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SyncApi::class.java)
    }
}
