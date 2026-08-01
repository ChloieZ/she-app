package com.herspace.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** 用于获取投票类型（placeId → voteType） */
data class VoteTypeRow(
    val placeId: String,
    val voteType: String
)

@Dao
interface VoteDao {

    @Insert
    suspend fun insertVote(vote: VoteEntity)

    /** 更新已有投票类型（同 placeId），返回受影响行数 */
    @Query("UPDATE votes SET voteType = :voteType, timestamp = :ts WHERE placeId = :placeId")
    suspend fun updateVoteType(placeId: String, voteType: String, ts: Long): Int

    @Query("SELECT placeId, voteType FROM votes ORDER BY timestamp DESC")
    suspend fun getAllVoteTypes(): List<VoteTypeRow>

    @Query("""
        SELECT 
            placeId, placeName, placeLat, placeLng,
            COUNT(*) AS totalVotes,
            SUM(CASE WHEN voteType = 'friendly' THEN 1 ELSE 0 END) AS friendlyCount
        FROM votes
        WHERE placeId = :placeId
        GROUP BY placeId
    """)
    suspend fun getPlaceSummary(placeId: String): PlaceSummary?

    @Query("""
        SELECT 
            placeId, placeName, placeLat, placeLng,
            COUNT(*) AS totalVotes,
            SUM(CASE WHEN voteType = 'friendly' THEN 1 ELSE 0 END) AS friendlyCount
        FROM votes
        GROUP BY placeId
    """)
    suspend fun getAllPlaceSummaries(): List<PlaceSummary>

    @Query("SELECT * FROM votes WHERE placeId = :placeId ORDER BY timestamp DESC")
    suspend fun getVotesForPlace(placeId: String): List<VoteEntity>
}
