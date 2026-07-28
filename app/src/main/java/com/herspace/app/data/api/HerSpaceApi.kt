package com.herspace.app.data.api

import retrofit2.http.*

/** 她空间后端 API 接口（注意：path 不以 / 开头，否则会替换 baseUrl 的 path） */
interface HerSpaceApi {

    @GET("api/places/search")
    suspend fun searchPlaces(
        @Query("keyword") keyword: String,
        @Query("city") city: String = "杭州",
        @Query("page_size") pageSize: Int = 25,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<PlaceResponse>

    @GET("api/places/nearby")
    suspend fun nearbyPlaces(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 1000,
        @Query("page_size") pageSize: Int = 20
    ): ApiResponse<PlaceResponse>

    @GET("api/places/category")
    suspend fun categoryPlaces(
        @Query("category") category: String,
        @Query("city") city: String = "杭州",
        @Query("page_size") pageSize: Int = 25,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResponse<PlaceResponse>

    @GET("api/places/{id}")
    suspend fun placeDetail(@Path("id") placeId: String): PlaceResponse

    @POST("api/vote")
    suspend fun submitVote(@Body request: VoteRequest): VoteResponse

    @GET("api/places/{id}/votes")
    suspend fun placeVotes(@Path("id") placeId: String): VoteListResponse

    // ── 认证 ──

    @POST("api/auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("api/auth/me")
    suspend fun me(@Header("Authorization") token: String): AuthUserResponse

    @GET("api/admin/users")
    suspend fun adminUsers(): UsersListResponse

    @GET("api/votes/today")
    suspend fun todayVotes(@Query("user_id") userId: String): TodayVotesResponse

    @GET("api/places/{id}/my-vote")
    suspend fun myVote(@Path("id") placeId: String, @Query("user_id") userId: String): MyVoteResponse
}
