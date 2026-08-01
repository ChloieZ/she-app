package com.herspace.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 持久化用户投票过的地点信息（跨安装周期保留）
 */
object VotedPlacesStore {
    private const val PREFS_NAME = "voted_places"
    private const val KEY_PLACES = "places"

    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    data class VotedPlace(
        val placeId: String,
        val placeName: String,
        val lat: Double,
        val lng: Double
    )

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun addPlace(placeId: String, name: String, lat: Double, lng: Double) {
        val list = getAll().toMutableList()
        // 去重
        if (list.any { it.placeId == placeId }) return
        list.add(VotedPlace(placeId, name, lat, lng))
        save(list)
    }

    fun getAll(): List<VotedPlace> {
        val json = prefs?.getString(KEY_PLACES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VotedPlace>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun save(list: List<VotedPlace>) {
        prefs?.edit()?.putString(KEY_PLACES, gson.toJson(list))?.apply()
    }
}
