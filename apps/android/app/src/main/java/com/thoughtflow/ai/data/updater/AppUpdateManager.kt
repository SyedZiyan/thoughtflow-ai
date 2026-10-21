package com.thoughtflow.ai.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.thoughtflow.ai.data.api.ApiClient
import com.thoughtflow.ai.data.model.AppVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isDismissed: Boolean = false,
    val installedVersionCode: Int = 1,
    val installedVersionName: String = "1.0.0",
    val latestVersionInfo: AppVersionInfo? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val isReadyToInstall: Boolean = false,
    val statusMessage: String? = null
)

object AppUpdateManager {

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    @Suppress("DEPRECATION")
    fun getInstalledVersion(context: Context): Pair<Int, String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
            val name = packageInfo.versionName ?: "1.0.0"
            Pair(code, name)
        } catch (e: Exception) {
            Pair(1, "1.0.0")
        }
    }

    suspend fun checkForUpdates(context: Context, silent: Boolean = true) {
        withContext(Dispatchers.IO) {
            val (installedCode, installedName) = getInstalledVersion(context)
            _updateState.value = _updateState.value.copy(
                isChecking = true,
                installedVersionCode = installedCode,
                installedVersionName = installedName,
                statusMessage = if (!silent) "Checking for updates..." else null
            )

            try {
                val latest = ApiClient.api.getAppVersion()
                val isNewer = latest.versionCode > installedCode

                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    isUpdateAvailable = isNewer,
                    latestVersionInfo = latest,
                    statusMessage = if (isNewer) {
                        "New version ${latest.versionName} available!"
                    } else {
                        "You have the latest version (${installedName})"
                    }
                )
            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    statusMessage = if (!silent) "Could not check for updates: ${e.message}" else null
                )
            }
        }
    }

    fun dismissUpdateBanner() {
        _updateState.value = _updateState.value.copy(isDismissed = true)
    }

    suspend fun downloadAndInstall(
        context: Context,
        onStatus: (String) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                _updateState.value = _updateState.value.copy(
                    isDownloading = true,
                    downloadProgress = 0,
                    statusMessage = "Starting APK download..."
                )
                onStatus("Downloading latest update...")

                val responseBody = ApiClient.api.downloadApk()
                val totalBytes = responseBody.contentLength()
                val inputStream = responseBody.byteStream()

                val apkDir = File(context.cacheDir, "updates")
                if (!apkDir.exists()) apkDir.mkdirs()

                val apkFile = File(apkDir, "ThoughtFlow-AI-Latest.apk")
                if (apkFile.exists()) apkFile.delete()

                val outputStream = FileOutputStream(apkFile)
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Long = 0
                var len: Int
                var lastReportedProgress = 0

                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                    bytesRead += len
                    if (totalBytes > 0) {
                        val progress = ((bytesRead * 100) / totalBytes).toInt()
                        if (progress > lastReportedProgress) {
                            lastReportedProgress = progress
                            _updateState.value = _updateState.value.copy(downloadProgress = progress)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    downloadProgress = 100,
                    isReadyToInstall = true,
                    statusMessage = "Download complete. Launching installer..."
                )

                withContext(Dispatchers.Main) {
                    installApk(context, apkFile)
                }

            } catch (e: Exception) {
                _updateState.value = _updateState.value.copy(
                    isDownloading = false,
                    statusMessage = "Download failed: ${e.message}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Toast.makeText(context, "Update package is empty or corrupted", Toast.LENGTH_SHORT).show()
            return
        }

        // Android 8.0+ Check for unknown app install permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Allow ThoughtFlow AI to install updates to proceed",
                    Toast.LENGTH_LONG
                ).show()
                val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
