package com.thoughtflow.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thoughtflow.ai.data.model.*

object LocalCacheManager {
    private const val PREFS_NAME = "thoughtflow_offline_cache"
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    var isLastFetchOffline = false
        private set

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun markOnline() {
        isLastFetchOffline = false
    }

    fun markOffline() {
        isLastFetchOffline = true
    }

    fun getSavedHost(): String? {
        return prefs?.getString("saved_server_host", null)
    }

    fun saveServerHost(host: String) {
        prefs?.edit()?.putString("saved_server_host", host)?.apply()
    }

    // --- Dashboard Cache ---
    fun saveDashboard(dashboard: DashboardResponse) {
        prefs?.edit()?.putString("cache_dashboard", gson.toJson(dashboard))?.apply()
    }

    fun getDashboard(): DashboardResponse {
        val json = prefs?.getString("cache_dashboard", null)
        if (!json.isNullOrEmpty()) {
            try {
                val data = gson.fromJson(json, DashboardResponse::class.java)
                if (data != null) return data
            } catch (_: Exception) {}
        }
        return getFallbackDashboard()
    }

    // --- Thoughts Cache ---
    fun saveThoughts(thoughts: List<ThoughtResponse>) {
        if (thoughts.isNotEmpty()) {
            prefs?.edit()?.putString("cache_thoughts", gson.toJson(thoughts))?.apply()
        }
    }

    fun getThoughts(): List<ThoughtResponse> {
        val json = prefs?.getString("cache_thoughts", null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<ThoughtResponse>>() {}.type
                val list: List<ThoughtResponse>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) return list
            } catch (_: Exception) {}
        }
        return getFallbackThoughts()
    }

    // --- Videos Cache ---
    fun saveVideos(videos: List<VideoResponse>) {
        if (videos.isNotEmpty()) {
            prefs?.edit()?.putString("cache_videos", gson.toJson(videos))?.apply()
        }
    }

    fun getVideos(): List<VideoResponse> {
        val json = prefs?.getString("cache_videos", null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<VideoResponse>>() {}.type
                val list: List<VideoResponse>? = gson.fromJson(json, type)
                if (!list.isNullOrEmpty()) return list
            } catch (_: Exception) {}
        }
        return getFallbackVideos()
    }

    fun getCachedVideoCount(): Int = getVideos().size
    fun getCachedThoughtCount(): Int = getThoughts().size

    // --- Analytics Cache ---
    fun saveAnalytics(analytics: AnalyticsResponse) {
        prefs?.edit()?.putString("cache_analytics", gson.toJson(analytics))?.apply()
    }

    fun getAnalytics(): AnalyticsResponse? {
        val json = prefs?.getString("cache_analytics", null)
        if (!json.isNullOrEmpty()) {
            try {
                return gson.fromJson(json, AnalyticsResponse::class.java)
            } catch (_: Exception) {}
        }
        return null
    }

    // --- Built-in Fallbacks for Instant Offline Launch ---
    private fun getFallbackDashboard(): DashboardResponse {
        return DashboardResponse(
            todayGenerationStatus = "completed",
            todayPublishingStatus = "scheduled",
            nextScheduledExecution = "Tomorrow at 07:00 UTC",
            totalThoughts = 24,
            totalVideos = 8,
            totalPublished = 5,
            failedJobs = 0,
            youtubeConnected = true,
            upcomingSchedule = listOf(
                ScheduleItem(
                    date = "Tomorrow",
                    time = "07:00 UTC",
                    theme = "motivation",
                    status = "scheduled",
                    thoughtTitle = "Stop Waiting, Start Now"
                )
            ),
            recentActivity = emptyList(),
            latestVideos = getFallbackVideos(),
            channelStats = ChannelStats(
                channelTitle = "ThoughtFlow Motivation Official",
                subscriberCount = 1240,
                totalVideos = 18
            )
        )
    }

    private fun getFallbackThoughts(): List<ThoughtResponse> {
        return listOf(
            ThoughtResponse(
                id = "offline_01",
                text = "Great things never come from comfort zones. Take massive, decisive action today.",
                title = "Stop Waiting, Start Now",
                description = "Action creates the confidence that waiting destroys. Step forward with conviction.",
                theme = "motivation",
                tone = "inspiring",
                language = "en",
                hashtags = listOf("#shorts", "#motivation", "#mindset", "#discipline"),
                createdAt = "2026-10-21"
            ),
            ThoughtResponse(
                id = "offline_02",
                text = "Discipline is choosing between what you want now and what you want most in life.",
                title = "Discipline Over Motivation",
                description = "The champions build their empire on days they didn't feel like training.",
                theme = "discipline",
                tone = "bold",
                language = "en",
                hashtags = listOf("#shorts", "#discipline", "#focus", "#resilience"),
                createdAt = "2026-10-20"
            ),
            ThoughtResponse(
                id = "offline_03",
                text = "Your future is created by the small habits you protect today, not tomorrow.",
                title = "Raise Your Daily Standards",
                description = "Protect your daily non-negotiables with iron commitment and unwavering resolve.",
                theme = "productivity",
                tone = "mindful",
                language = "en",
                hashtags = listOf("#shorts", "#productivity", "#habits"),
                createdAt = "2026-10-19"
            ),
            ThoughtResponse(
                id = "offline_04",
                text = "Turn pain into fuel. Every challenge you endure builds the foundation of your success.",
                title = "Turn Pain Into Fuel",
                description = "You are stronger than the circumstances you are walking through right now.",
                theme = "resilience",
                tone = "empowering",
                language = "en",
                hashtags = listOf("#shorts", "#resilience", "#strength"),
                createdAt = "2026-10-18"
            )
        )
    }

    private fun getFallbackVideos(): List<VideoResponse> {
        return listOf(
            VideoResponse(
                id = "offline_vid_01",
                thoughtId = "offline_01",
                storageKey = "thought_f57c1822-274e-4430-9d0f-364975eea575.mp4",
                videoUrl = "/storage/videos/thought_f57c1822-274e-4430-9d0f-364975eea575.mp4",
                title = "Stop Waiting, Start Now #Shorts",
                description = "Action creates the confidence that waiting destroys. Step forward with conviction.",
                durationSeconds = 15,
                width = 1080,
                height = 1920,
                renderStatus = "completed",
                uploadStatus = "published",
                createdAt = "Today"
            ),
            VideoResponse(
                id = "offline_vid_02",
                thoughtId = "offline_02",
                storageKey = "thought_00e8495c-bcd6-4c13-a5a7-2b132dc1aa53.mp4",
                videoUrl = "/storage/videos/thought_00e8495c-bcd6-4c13-a5a7-2b132dc1aa53.mp4",
                title = "Turn Pain Into Fuel #Shorts",
                description = "You are stronger than the circumstances you are walking through right now.",
                durationSeconds = 10,
                width = 1080,
                height = 1920,
                renderStatus = "completed",
                uploadStatus = "ready",
                createdAt = "Yesterday"
            ),
            VideoResponse(
                id = "offline_vid_03",
                thoughtId = "offline_03",
                storageKey = "thought_9f68bd7e-8321-4161-992d-bbf957da6263.mp4",
                videoUrl = "/storage/videos/thought_9f68bd7e-8321-4161-992d-bbf957da6263.mp4",
                title = "Discipline Over Motivation #Shorts",
                description = "The champions build their empire on days they didn't feel like training.",
                durationSeconds = 20,
                width = 1080,
                height = 1920,
                renderStatus = "completed",
                uploadStatus = "ready",
                createdAt = "2 days ago"
            )
        )
    }

    // --- Auth Session & Token Storage ---
    fun getAuthToken(): String? {
        return prefs?.getString("auth_jwt_token", null)
    }

    fun setAuthToken(token: String?) {
        prefs?.edit()?.putString("auth_jwt_token", token)?.apply()
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    fun clearSession() {
        prefs?.edit()
            ?.remove("auth_jwt_token")
            ?.remove("profile_user_name")
            ?.remove("profile_user_email")
            ?.apply()
    }

    // --- User Profile Customization ---
    fun getUserName(): String {
        val name = prefs?.getString("profile_user_name", null)
        if (!name.isNullOrBlank()) return name
        val email = getUserEmail()
        if (email.isNotBlank() && email.contains("@")) {
            return email.substringBefore("@").replaceFirstChar { it.uppercase() }
        }
        return "Creator"
    }

    fun setUserName(name: String) {
        prefs?.edit()?.putString("profile_user_name", name.trim())?.apply()
    }

    fun getUserEmail(): String {
        return prefs?.getString("profile_user_email", "") ?: ""
    }

    fun setUserEmail(email: String) {
        prefs?.edit()?.putString("profile_user_email", email.trim())?.apply()
    }
}
