package com.herspace.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VoteDao {

    @Insert
    suspend fun insertVote(vote: VoteEntity)

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
