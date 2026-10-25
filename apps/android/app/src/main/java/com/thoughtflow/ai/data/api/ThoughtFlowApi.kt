package com.thoughtflow.ai.data.api

import com.thoughtflow.ai.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ThoughtFlowApi {

    @GET("app/version")
    suspend fun getAppVersion(): AppVersionInfo

    @Streaming
    @GET("app/download")
    suspend fun downloadApk(): ResponseBody

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

    @GET("youtube/status")
    suspend fun getYouTubeStatus(): YouTubeStatusResponse

    @POST("youtube/connect")
    suspend fun connectYouTube(): YouTubeConnectResponse

    @POST("youtube/link-channel")
    suspend fun linkYouTubeChannel(@Body request: LinkChannelRequest): YouTubeStatusResponse

    @POST("youtube/simulate-connect")
    suspend fun simulateYouTubeConnect(
        @Query("channel_name") channelName: String? = null
    ): YouTubeStatusResponse

    @POST("youtube/disconnect")
    suspend fun disconnectYouTube(): Map<String, Any>

    @POST("automation/run")
    suspend fun runPipeline(): Map<String, Any>

    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body credentials: LoginRequest): AuthResponse

    @GET("analytics")
    suspend fun getAnalytics(): AnalyticsResponse

    @GET("settings")
    suspend fun getSettings(): SettingsResponse

    @PATCH("settings")
    suspend fun updateSettings(@Body settings: Map<String, Any>): SettingsResponse
}
