package com.thoughtflow.ai.data.api

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val isEmulator: Boolean
        get() = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86"))

    // Defaults to your PC Wi-Fi IP address or saved custom/cloud host
    private var currentHost: String? = null

    fun getBaseUrl(): String = "${getHost()}/api/v1/"

    fun getHost(): String {
        if (currentHost == null) {
            val saved = com.thoughtflow.ai.data.local.LocalCacheManager.getSavedHost()
            currentHost = if (!saved.isNullOrEmpty()) {
                saved
            } else if (isEmulator) {
                "http://10.0.2.2:8000"
            } else {
                "http://192.168.0.101:8000"
            }
        }
        return currentHost!!
    }

    fun setHost(newHost: String) {
        currentHost = newHost.trimEnd('/')
        com.thoughtflow.ai.data.local.LocalCacheManager.saveServerHost(currentHost!!)
        instance = null
    }

    private var instance: ThoughtFlowApi? = null

    val api: ThoughtFlowApi
        get() {
            if (instance == null) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build()

                instance = Retrofit.Builder()
                    .baseUrl(getBaseUrl())
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ThoughtFlowApi::class.java)
            }
            return instance!!
        }
}
