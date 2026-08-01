package com.herspace.app.util

import android.content.Context
import android.content.SharedPreferences
import com.herspace.app.data.api.TokenManager

/** 管理搜索历史记录（最多 10 条），按账号隔离 */
object SearchHistoryManager {
    private const val PREFS = "herspace_search"
    private const val MAX = 10

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** 按当前登录账号隔离（不同账号历史互不影响） */
    private fun key(): String {
        val email = TokenManager.getEmail()?.takeIf { it.isNotBlank() } ?: "default"
        return "history_$email"
    }

    fun getHistory(): List<String> {
        val raw = prefs.getString(key(), "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun add(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val existing = getHistory().toMutableList()
        existing.remove(q)          // 去重
        existing.add(0, q)          // 最新在最前
        val trimmed = existing.take(MAX)
        prefs.edit().putString(key(), trimmed.joinToString("\n")).apply()
    }

    fun clear() {
        prefs.edit().remove(key()).apply()
    }
}
