package com.herspace.app.data.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** API 客户端（支持调试/线上模式切换） */
object ApiClient {

    private const val PREFS = "herspace_config"
    private const val KEY_MODE = "api_mode"
    private const val DEBUG_URL = "http://localhost:5007/"
    private const val PROD_URL = "https://hzzgz.cn/she/"

    private var prefs: SharedPreferences? = null
    private var currentBaseUrl: String = DEBUG_URL

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var client: OkHttpClient = buildClient()
    private var retrofit: Retrofit = buildRetrofit()
    var api: HerSpaceApi = retrofit.create(HerSpaceApi::class.java)

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_MODE, "debug")
        switchMode(saved ?: "debug")
    }

    fun isDebugMode(): Boolean = currentBaseUrl == DEBUG_URL

    fun switchMode(mode: String) {
        currentBaseUrl = if (mode == "prod") PROD_URL else DEBUG_URL
        prefs?.edit()?.putString(KEY_MODE, mode)?.apply()
        client = buildClient()
        retrofit = buildRetrofit()
        api = retrofit.create(HerSpaceApi::class.java)
    }

    fun getMode(): String = if (currentBaseUrl == PROD_URL) "prod" else "debug"
    fun getBaseUrl(): String = currentBaseUrl

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
            .baseUrl(currentBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
