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
    @SerializedName("latest_videos") val latestVideos: List<VideoResponse> = emptyList()
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

data class SettingsResponse(
    val timezone: String = "UTC",
    @SerializedName("generation_time") val generationTime: String = "07:00",
    @SerializedName("publishing_time") val publishingTime: String = "09:00",
    @SerializedName("video_duration") val videoDuration: Int = 15,
    @SerializedName("auto_publish_enabled") val autoPublishEnabled: Boolean = false,
    @SerializedName("daily_limit") val dailyLimit: Int = 5,
    @SerializedName("selected_themes") val selectedThemes: List<String> = emptyList()
)
