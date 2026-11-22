package com.ytdlp.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private var currentBaseUrl: String = "https://web-production-19d55cca.up.railway.app"
    private var apiService: YtDlpApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun getService(baseUrl: String): YtDlpApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (apiService == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            apiService = retrofit.create(YtDlpApiService::class.java)
        }
        return apiService!!
    }

    fun getDownloadFileUrl(baseUrl: String, taskId: String): String {
        val base = baseUrl.trimEnd('/')
        return "$base/api/tasks/$taskId/file"
    }
}
