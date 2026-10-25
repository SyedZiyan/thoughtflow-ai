package com.thoughtflow.ai.data.model

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("today_generation_status") val todayGenerationStatus: String = "pending",
    @SerializedName("today_publishing_status") val todayPublishingStatus: String = "pending",
    @SerializedName("next_scheduled_execution") val nextScheduledExecution: String? = null,
    @SerializedName("total_generated_thoughts") val totalThoughts: Int = 0,
    @SerializedName("total_rendered_videos") val totalVideos: Int = 0,
    @SerializedName("total_published_videos") val totalPublished: Int = 0,
    @SerializedName("failed_jobs_count") val failedJobs: Int = 0,
    @SerializedName("youtube_connected") val youtubeConnected: Boolean = false,
    @SerializedName("upcoming_schedule") val upcomingSchedule: List<ScheduleItem> = emptyList(),
    @SerializedName("recent_activity") val recentActivity: List<ActivityItem> = emptyList(),
    @SerializedName("latest_videos") val latestVideos: List<VideoResponse> = emptyList(),
    @SerializedName("channel_stats") val channelStats: ChannelStats = ChannelStats()
)

data class ChannelStats(
    @SerializedName("channel_title") val channelTitle: String = "ThoughtFlow Motivation Official",
    @SerializedName("subscriber_count") val subscriberCount: Int = 1240,
    @SerializedName("total_videos") val totalVideos: Int = 18
)

data class ScheduleItem(
    val date: String,
    val time: String,
    val theme: String,
    val status: String,
    @SerializedName("thought_title") val thoughtTitle: String
)

data class ActivityItem(
    val id: String,
    @SerializedName("event_type") val eventType: String,
    val message: String,
    @SerializedName("created_at") val createdAt: String
)

data class ThoughtResponse(
    val id: String,
    val text: String,
    val title: String,
    val description: String,
    val theme: String,
    val tone: String,
    val language: String,
    val hashtags: List<String> = emptyList(),
    @SerializedName("is_duplicate") val isDuplicate: Boolean = false,
    @SerializedName("created_at") val createdAt: String
)

data class VideoResponse(
    val id: String,
    @SerializedName("thought_id") val thoughtId: String,
    @SerializedName("storage_key") val storageKey: String,
    @SerializedName("video_url") val videoUrl: String,
    val title: String,
    val description: String,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    val width: Int,
    val height: Int,
    @SerializedName("render_status") val renderStatus: String,
    @SerializedName("upload_status") val uploadStatus: String,
    @SerializedName("youtube_url") val youtubeUrl: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class GenerateThoughtRequest(
    val theme: String = "motivation",
    val tone: String = "inspiring",
    val language: String = "en",
    @SerializedName("custom_prompt") val customPrompt: String? = null,
    @SerializedName("auto_render") val autoRender: Boolean = false
)

data class RenderVideoRequest(
    @SerializedName("thought_id") val thoughtId: String,
    @SerializedName("duration_seconds") val durationSeconds: Int = 15,
    @SerializedName("visual_theme") val visualTheme: String = "cozy_balcony",
    @SerializedName("animation_style") val animationStyle: String = "smooth_zoom",
    val typography: String = "poetic_script",
    @SerializedName("text_placement") val textPlacement: String = "center",
    @SerializedName("narration_enabled") val narrationEnabled: Boolean = false,
    @SerializedName("ambient_enabled") val ambientEnabled: Boolean = true
)

data class PublishRequest(
    @SerializedName("video_id") val videoId: String,
    val title: String,
    val privacy: String = "public"
)

data class SettingsResponse(
    val timezone: String = "UTC",
    @SerializedName("generation_time") val generationTime: String = "07:00",
    @SerializedName("publishing_time") val publishingTime: String = "09:00",
    @SerializedName("video_duration") val videoDuration: Int = 15,
    @SerializedName("auto_publish_enabled") val autoPublishEnabled: Boolean = false,
    @SerializedName("daily_limit") val dailyLimit: Int = 5,
    @SerializedName("selected_themes") val selectedThemes: List<String> = emptyList()
)

data class YouTubeStatusResponse(
    val connected: Boolean = false,
    @SerializedName("channel_id") val channelId: String? = null,
    @SerializedName("channel_title") val channelTitle: String? = null,
    @SerializedName("channel_thumbnail") val channelThumbnail: String? = null,
    @SerializedName("subscriber_count") val subscriberCount: Int? = null,
    @SerializedName("video_count") val videoCount: Int? = null,
    @SerializedName("view_count") val viewCount: Int? = null,
    @SerializedName("upload_enabled") val uploadEnabled: Boolean = false,
    @SerializedName("dry_run_mode") val dryRunMode: Boolean = true
)

data class YouTubeConnectResponse(
    @SerializedName("auth_url") val authUrl: String? = null,
    val configured: Boolean = false,
    val message: String? = null
)

data class LinkChannelRequest(
    @SerializedName("channel_name") val channelName: String,
    @SerializedName("channel_id") val channelId: String? = null,
    @SerializedName("channel_handle") val channelHandle: String? = null,
    @SerializedName("subscriber_count") val subscriberCount: Int? = 0,
    @SerializedName("video_count") val videoCount: Int? = 0,
    @SerializedName("view_count") val viewCount: Int? = 0
)

data class AppVersionInfo(
    @SerializedName("version_name") val versionName: String = "1.0.0",
    @SerializedName("version_code") val versionCode: Int = 1,
    @SerializedName("release_notes") val releaseNotes: String = "",
    @SerializedName("download_url") val downloadUrl: String = "/api/v1/app/download",
    @SerializedName("file_size_mb") val fileSizeMb: Double = 0.0
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String,
    val role: String = "creator"
)

data class AnalyticsResponse(
    @SerializedName("total_published_videos") val totalPublishedVideos: Int = 0,
    @SerializedName("total_views") val totalViews: Int = 0,
    @SerializedName("total_likes") val totalLikes: Int = 0,
    @SerializedName("total_comments") val totalComments: Int = 0,
    @SerializedName("success_rate") val successRate: Double = 100.0,
    @SerializedName("performance_by_theme") val performanceByTheme: List<Map<String, Any>> = emptyList(),
    @SerializedName("recent_uploads") val recentUploads: List<Map<String, Any>> = emptyList()
)
