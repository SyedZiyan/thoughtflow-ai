package com.thoughtflow.ai.data.api

import com.thoughtflow.ai.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ThoughtFlowApi {

    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse

    @GET("thoughts")
    suspend fun getThoughts(
        @Query("theme") theme: String? = null,
        @Query("limit") limit: Int = 30
    ): List<ThoughtResponse>

    @POST("thoughts/generate")
    suspend fun generateThought(
        @Body request: GenerateThoughtRequest
    ): ThoughtResponse

    @GET("videos")
    suspend fun getVideos(): List<VideoResponse>

    @POST("videos/render")
    suspend fun renderVideo(
        @Body request: RenderVideoRequest
    ): VideoResponse

    @POST("youtube/upload")
    suspend fun publishVideo(@Body request: PublishRequest): Map<String, Any>

    @POST("automation/run")
    suspend fun runPipeline(): Map<String, Any>

    @GET("settings")
    suspend fun getSettings(): SettingsResponse

    @PATCH("settings")
    suspend fun updateSettings(@Body settings: Map<String, Any>): SettingsResponse
}
