package com.herspace.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 投票记录
 */
@Entity(tableName = "votes")
data class VoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placeId: String,          // 地点的唯一标识（使用 OSM node ID 或自定义）
    val placeName: String,        // 店铺名称
    val placeLat: Double,         // 纬度
    val placeLng: Double,         // 经度
    val voteType: String,         // "friendly", "not_friendly", "very_unfriendly"
    val timestamp: Long = System.currentTimeMillis()
)
