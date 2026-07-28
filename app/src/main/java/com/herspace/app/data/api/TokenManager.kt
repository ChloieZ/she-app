package com.herspace.app.data.api

import android.content.Context
import android.content.SharedPreferences

/** 本地 Token 管理 */
object TokenManager {
    private const val PREFS_NAME = "herspace_auth"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_NICKNAME = "nickname"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(token: String, email: String, nickname: String = "") {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_NICKNAME, nickname)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getNickname(): String? = prefs.getString(KEY_NICKNAME, null)
    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** 用于 Retrofit 的 Authorization header */
    fun authHeader(): String = "Bearer ${getToken() ?: ""}"
}
