package com.herspace.app.data.db

/**
 * 地点的投票汇总（非实体，用于查询统计）
 */
data class PlaceSummary(
    val placeId: String,
    val placeName: String,
    val placeLat: Double,
    val placeLng: Double,
    val totalVotes: Int,
    val friendlyCount: Int
)
