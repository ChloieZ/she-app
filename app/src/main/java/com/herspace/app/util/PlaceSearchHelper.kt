package com.herspace.app.util

import com.herspace.app.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/** 搜索结果包装 */
data class SearchResult(
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val type: String
) {
    fun distanceTo(targetLat: Double, targetLng: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat - targetLat)
        val dLng = Math.toRadians(lng - targetLng)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(targetLat)) * cos(Math.toRadians(lat)) * sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

/**
 * 搜索助手 — 通过后端服务代理高德 API
 */
class PlaceSearchHelper {

    private val api = ApiClient.api

    /** 关键词搜索 */
    suspend fun search(keyword: String, city: String = "杭州", pageSize: Int = 25, location: Pair<Double, Double>? = null): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.searchPlaces(keyword, city, pageSize, location?.first, location?.second)
                resp.results.map { it.toSearchResult() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** 周边搜索 */
    suspend fun searchAround(keyword: String = "", types: String = "", location: Pair<Double, Double>, radius: Int = 1000, pageSize: Int = 20): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val resp = api.nearbyPlaces(location.first, location.second, radius, pageSize)
                resp.results.map { it.toSearchResult() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

/** PlaceResponse → SearchResult */
private fun com.herspace.app.data.api.PlaceResponse.toSearchResult() = SearchResult(
    placeId = id,
    name = name,
    lat = lat,
    lng = lng,
    address = address,
    type = type
)
