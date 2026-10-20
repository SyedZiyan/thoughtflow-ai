package com.thoughtflow.ai.data.repository

import com.thoughtflow.ai.data.api.ApiClient
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
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun getThoughts(theme: String? = null): Flow<Result<List<ThoughtResponse>>> = flow {
        try {
            val res = api.getThoughts(theme)
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
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
            emit(Result.success(res))
        } catch (e: Exception) {
            emit(Result.failure(e))
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
}
