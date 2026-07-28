package com.herspace.app.util

import android.content.Context
import android.content.SharedPreferences

/** 管理搜索历史记录（最多 10 条） */
object SearchHistoryManager {
    private const val PREFS = "herspace_search"
    private const val KEY_HISTORY = "history"
    private const val MAX = 10

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun getHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun add(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val existing = getHistory().toMutableList()
        existing.remove(q)          // 去重
        existing.add(0, q)          // 最新在最前
        val trimmed = existing.take(MAX)
        prefs.edit().putString(KEY_HISTORY, trimmed.joinToString("\n")).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
