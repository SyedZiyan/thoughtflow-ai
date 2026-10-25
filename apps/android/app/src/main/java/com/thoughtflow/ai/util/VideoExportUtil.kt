package com.thoughtflow.ai.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.thoughtflow.ai.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object VideoExportUtil {

    private val httpClient = OkHttpClient()

    /**
     * Resolves a video path or relative URL to a fully qualified URL or local File.
     */
    fun resolveFullVideoUrl(videoPathOrUrl: String): String {
        return if (videoPathOrUrl.startsWith("http://") || videoPathOrUrl.startsWith("https://")) {
            videoPathOrUrl
        } else {
            val host = ApiClient.getHost().trimEnd('/')
            val path = if (videoPathOrUrl.startsWith("/")) videoPathOrUrl else "/$videoPathOrUrl"
            "$host$path"
        }
    }

    /**
     * Downloads or caches a video file locally and returns its FileProvider content URI.
     */
    suspend fun getLocalVideoUri(context: Context, videoPathOrUrl: String): Uri? = withContext(Dispatchers.IO) {
        try {
            // If already an existing local file
            val directFile = File(videoPathOrUrl)
            if (directFile.exists() && directFile.length() > 0) {
                return@withContext FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    directFile
                )
            }

            // Download from URL to app cache
            val fullUrl = resolveFullVideoUrl(videoPathOrUrl)
            val cacheFile = File(context.cacheDir, "shorts_${System.currentTimeMillis()}.mp4")

            val request = Request.Builder().url(fullUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) {
                // If remote server is unreachable, check if we have a fallback sample in assets
                return@withContext null
            }

            response.body!!.byteStream().use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Method 1: Direct 1-Tap Handover to the Official YouTube Shorts App.
     * Pre-fills the video, subject, and motivational hashtags.
     */
    suspend fun uploadToYouTubeShorts(
        context: Context,
        videoPathOrUrl: String,
        quoteText: String,
        author: String = "Creator"
    ) = withContext(Dispatchers.Main) {
        Toast.makeText(context, "Preparing video for YouTube Shorts...", Toast.LENGTH_SHORT).show()

        val contentUri = getLocalVideoUri(context, videoPathOrUrl)
        if (contentUri == null) {
            Toast.makeText(
                context,
                "Unable to prepare video file. Ensure server is connected.",
                Toast.LENGTH_LONG
            ).show()
            return@withContext
        }

        val title = if (quoteText.length > 60) "${quoteText.take(57)}..." else quoteText
        val description = "\"$quoteText\"\n\n— $author\n\n#shorts #motivation #inspiration #mindset #quotes #dailywisdom"

        val ytIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, description)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.google.android.youtube") // Targets official YouTube app directly!
        }

        try {
            context.startActivity(ytIntent)
            Toast.makeText(context, "Opening YouTube Shorts Creator!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // If official YouTube app package targeting fails, fallback to general system share sheet
            val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, description)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(chooserIntent, "Upload Short to YouTube / Socials"))
        }
    }

    /**
     * Saves rendered 9:16 video directly to device Gallery / Movies album.
     */
    suspend fun saveVideoToGallery(
        context: Context,
        videoPathOrUrl: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = resolveFullVideoUrl(videoPathOrUrl)
            val request = Request.Builder().url(fullUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful || response.body == null) return@withContext null

            val filename = "ThoughtFlow_Short_${System.currentTimeMillis()}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ThoughtFlow")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collectionUri, contentValues) ?: return@withContext null

            resolver.openOutputStream(itemUri).use { out ->
                if (out != null) {
                    response.body!!.byteStream().copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            itemUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
