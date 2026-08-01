package com.herspace.app.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** API 客户端（支持调试/线上模式切换） */
object ApiClient {

    private const val BASE_URL = "https://hzzgz.cn/she/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var client: OkHttpClient = buildClient()
    private var retrofit: Retrofit = buildRetrofit()
    var api: HerSpaceApi = retrofit.create(HerSpaceApi::class.java)

    fun init(context: Context) {
        // 始终使用线上模式
    }

    fun getBaseUrl(): String = BASE_URL

    private fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
