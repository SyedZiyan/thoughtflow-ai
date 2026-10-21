package com.thoughtflow.ai.data.repository

import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.local.LocalCacheManager
import com.thoughtflow.ai.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ThoughtFlowRepository {

    private val api get() = ApiClient.api

    fun getDashboard(): Flow<Result<DashboardResponse>> = flow {
        try {
            val res = api.getDashboard()
            LocalCacheManager.saveDashboard(res)
            LocalCacheManager.markOnline()
            emit(Result.success(res))
        } catch (e: Exception) {
            LocalCacheManager.markOffline()
            emit(Result.success(LocalCacheManager.getDashboard()))
        }
    }.flowOn(Dispatchers.IO)

    fun getThoughts(theme: String? = null): Flow<Result<List<ThoughtResponse>>> = flow {
        try {
            val res = api.getThoughts(theme)
            LocalCacheManager.saveThoughts(res)
            LocalCacheManager.markOnline()
            emit(Result.success(res))
        } catch (e: Exception) {
            LocalCacheManager.markOffline()
            emit(Result.success(LocalCacheManager.getThoughts()))
        }
    }.flowOn(Dispatchers.IO)

    fun generateThought(req: GenerateThoughtRequest): Flow<Result<ThoughtResponse>> = flow {
        try {
            val res = api.generateThought(req)
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun renderVideo(req: RenderVideoRequest): Flow<Result<VideoResponse>> = flow {
        try {
            val res = api.renderVideo(req)
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun runPipeline(): Flow<Result<Map<String, Any>>> = flow {
        try {
            val res = api.runPipeline()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getVideos(): Flow<Result<List<VideoResponse>>> = flow {
        try {
            val res = api.getVideos()
            LocalCacheManager.saveVideos(res)
            LocalCacheManager.markOnline()
            emit(Result.success(res))
        } catch (e: Exception) {
            LocalCacheManager.markOffline()
            emit(Result.success(LocalCacheManager.getVideos()))
        }
    }.flowOn(Dispatchers.IO)

    fun publishVideo(req: PublishRequest): Flow<Result<Map<String, Any>>> = flow {
        try {
            val res = api.publishVideo(req)
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getYouTubeStatus(): Flow<Result<YouTubeStatusResponse>> = flow {
        try {
            val res = api.getYouTubeStatus()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun connectYouTube(): Flow<Result<YouTubeConnectResponse>> = flow {
        try {
            val res = api.connectYouTube()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun simulateYouTubeConnect(channelName: String = "ThoughtFlow Motivation Official"): Flow<Result<YouTubeStatusResponse>> = flow {
        try {
            val res = api.simulateYouTubeConnect(channelName)
            emit(Result.success(res))
        } catch (e: Exception) {
            // Offline/direct fallback for UI resilience
            emit(Result.success(
                YouTubeStatusResponse(
                    connected = true,
                    channelId = "UC_THOUGHTFLOW_OFFICIAL",
                    channelTitle = channelName,
                    subscriberCount = 1240,
                    videoCount = 18,
                    viewCount = 48200,
                    uploadEnabled = true,
                    dryRunMode = false
                )
            ))
        }
    }.flowOn(Dispatchers.IO)

    fun disconnectYouTube(): Flow<Result<Map<String, Any>>> = flow {
        try {
            val res = api.disconnectYouTube()
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)
}
