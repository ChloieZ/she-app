package com.herspace.app.data.repository

import com.herspace.app.data.api.ApiClient
import com.herspace.app.data.api.PlaceResponse
import com.herspace.app.data.api.TokenManager
import com.herspace.app.data.api.VoteRequest
import com.herspace.app.data.db.AppDatabase
import com.herspace.app.data.db.PlaceSummary
import com.herspace.app.data.db.VoteEntity
import com.herspace.app.data.model.FriendlinessLevel
import com.herspace.app.util.SearchResult
import com.herspace.app.util.VotedPlacesStore

class PlaceRepository(private val db: AppDatabase) {

    private val voteDao = db.voteDao()
    private val api = ApiClient.api

    // ── 投票（同时提交到服务端和本地缓存） ──

    suspend fun submitVote(
        placeName: String,
        placeId: String,
        lat: Double,
        lng: Double,
        voteType: String,
        typeName: String = ""
    ) {
        val userId = TokenManager.getEmail() ?: "anonymous"
        try {
            val resp = api.submitVote(VoteRequest(placeId = placeId, voteType = voteType, placeName = placeName, lat = lat, lng = lng, userId = userId, typeName = typeName))
        } catch (e: Exception) {
        }
        // 本地缓存（保持原始类型）——同地点更新投票类型，避免重复记录导致统计错乱
        val vote = VoteEntity(placeId = placeId, placeName = placeName, placeLat = lat, placeLng = lng, voteType = voteType)
        val updated = voteDao.updateVoteType(placeId, voteType, System.currentTimeMillis())
        if (updated == 0) voteDao.insertVote(vote)
        // 记录到持久化存储（跨安装周期保留）
        VotedPlacesStore.addPlace(placeId, placeName, lat, lng)
    }

    // ── 搜索（走服务端代理的高德 API） ──

    /** 关键词搜索 → 返回 SearchResult */
    suspend fun searchPlaces(keyword: String, city: String = "杭州", pageSize: Int = 25, lat: Double? = null, lng: Double? = null): List<SearchResult> {
        return try {
            val resp = api.searchPlaces(keyword, city, pageSize, lat, lng)
            resp.results.map { it.toSearchResult() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 周边搜索 */
    suspend fun nearbyPlaces(lat: Double, lng: Double, radius: Int = 1000, pageSize: Int = 20): List<SearchResult> {
        return try {
            val resp = api.nearbyPlaces(lat, lng, radius, pageSize)
            resp.results.map { it.toSearchResult() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 分类搜索 */
    suspend fun categoryPlaces(category: String, city: String = "杭州", pageSize: Int = 25, lat: Double? = null, lng: Double? = null): List<SearchResult> {
        return try {
            val resp = api.categoryPlaces(category, city, pageSize, lat, lng)
            resp.results.map { it.toSearchResult() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── 本地数据（投票记录） ──

    /** 从服务端获取地点详情（含投票统计） */
    suspend fun getPlaceDetail(placeId: String): PlaceResponse? {
        return try {
            api.placeDetail(placeId)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getPlaceSummary(placeId: String): PlaceSummary? {
        return voteDao.getPlaceSummary(placeId)
    }

    suspend fun getAllPlaceSummaries(): List<PlaceSummary> {
        return voteDao.getAllPlaceSummaries()
    }

    /** 获取所有投票的 placeId→最新voteType（用于颜色纠正） */
    suspend fun getAllVoteTypes(): Map<String, String> {
        val rows = voteDao.getAllVoteTypes() // 最新在前
        val map = mutableMapOf<String, String>()
        for (row in rows) {
            if (row.placeId !in map) map[row.placeId] = row.voteType
        }
        return map
    }

    // ── 友好度计算 ──

    fun calculateFriendliness(summary: PlaceSummary): FriendlinessLevel {
        if (summary.totalVotes == 0) return FriendlinessLevel.NEUTRAL
        val ratio = summary.friendlyCount.toFloat() / summary.totalVotes
        return FriendlinessLevel.fromRatio(ratio)
    }
}

/** 将 PlaceResponse 转为 SearchResult */
fun PlaceResponse.toSearchResult() = SearchResult(
    placeId = id,
    name = name,
    lat = lat,
    lng = lng,
    address = address,
    type = type
)
