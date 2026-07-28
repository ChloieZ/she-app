package com.herspace.app.data.api

import com.google.gson.annotations.SerializedName

// ── 通用响应包装 ──

data class ApiResponse<T>(
    val count: Int = 0,
    val results: List<T> = emptyList()
)

// ── 地点 ──

data class PlaceResponse(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val address: String = "",
    val type: String = "",
    @SerializedName("total_votes")
    val totalVotes: Int = 0,
    @SerializedName("friendly_votes")
    val friendlyVotes: Int = 0,
    @SerializedName("not_friendly_votes")
    val notFriendlyVotes: Int = 0,
    @SerializedName("very_unfriendly_votes")
    val veryUnfriendlyVotes: Int = 0,
    val friendliness: String = "neutral",
    val distance: Double = 0.0
)

// ── 投票 ──

data class VoteRequest(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("vote_type")
    val voteType: String,
    @SerializedName("place_name")
    val placeName: String,
    val lat: Double,
    val lng: Double,
    @SerializedName("user_id")
    val userId: String = "anonymous"
)

data class VoteResponse(
    val success: Boolean = false,
    val place: PlaceResponse? = null,
    val vote: VoteItemResponse? = null
)

data class VoteItemResponse(
    val id: Int = 0,
    @SerializedName("place_id")
    val placeId: String = "",
    @SerializedName("vote_type")
    val voteType: String = "",
    @SerializedName("user_id")
    val userId: String = ""
)

data class VoteListResponse(
    val place: PlaceResponse? = null,
    val votes: List<VoteItemResponse> = emptyList()
)

data class TodayVotesResponse(
    val count: Int = 0,
    @SerializedName("place_ids")
    val placeIds: List<String> = emptyList()
)

data class MyVoteResponse(
    val voted: Boolean = false,
    val vote: VoteItemResponse? = null
)

// ── 健康检查 ──

data class HealthResponse(
    val status: String = "",
    val service: String = ""
)

// ── 认证 ──

data class AuthRequest(
    val email: String,
    val password: String,
    val nickname: String = ""
)

data class AuthResponse(
    val success: Boolean = false,
    val user: AuthUserResponse? = null,
    val error: String? = null
)

data class AuthUserResponse(
    val id: Int = 0,
    val email: String = "",
    val nickname: String = "",
    val token: String = "",
    @SerializedName("created_at")
    val createdAt: String = ""
)

data class UsersListResponse(
    val count: Int = 0,
    val users: List<AuthUserResponse> = emptyList()
)
