package ru.ibakaidov.distypepro.utils

import android.content.Context
import androidx.preference.PreferenceManager
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TtsCacheManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
    private val cacheRoot: File by lazy {
        File(appContext.filesDir, CACHE_FOLDER_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun generateCacheKey(text: String, voice: String): String {
        val source = "$voice:$text"
        val digest = MessageDigest.getInstance("MD5").digest(source.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun getCachedFile(cacheKey: String): File? = withContext(Dispatchers.IO) {
        val file = File(cacheRoot, "$cacheKey.mp3")
        if (file.exists()) file else null
    }

    suspend fun isCached(cacheKey: String): Boolean = withContext(Dispatchers.IO) {
        File(cacheRoot, "$cacheKey.mp3").exists()
    }

    suspend fun saveToCache(cacheKey: String, bytes: ByteArray): File? {
        if (!getCacheEnabled()) return null
        ensureCacheLimitAsync(bytes.size)
        return withContext(Dispatchers.IO) {
            val target = File(cacheRoot, "$cacheKey.mp3")
            try {
                target.outputStream().use { stream ->
                    stream.write(bytes)
                }
                target
            } catch (ioError: IOException) {
                if (target.exists()) {
                    target.delete()
                }
                null
            }
        }
    }

    suspend fun getCacheInfo(): TtsCacheInfo = withContext(Dispatchers.IO) {
        val sizeBytes = calculateDirectorySize(cacheRoot)
        val fileCount = cacheRoot.listFiles()?.count { it.isFile } ?: 0
        TtsCacheInfo(
            enabled = getCacheEnabled(),
            sizeMb = bytesToMegabytes(sizeBytes),
            sizeLimitMb = getCacheSizeLimitMb(),
            fileCount = fileCount
        )
    }

    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            cacheRoot.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    fun getCacheEnabled(): Boolean =
        prefs.getBoolean(PREF_CACHE_ENABLED, true)

    fun setCacheEnabled(value: Boolean) {
        prefs.edit().putBoolean(PREF_CACHE_ENABLED, value).apply()
    }

    fun getCacheSizeLimitMb(): Double {
        val raw = prefs.getString(PREF_CACHE_SIZE_LIMIT_MB, DEFAULT_CACHE_LIMIT_MB_STRING)
        return raw?.toDoubleOrNull() ?: DEFAULT_CACHE_LIMIT_MB
    }

    fun setCacheSizeLimitMb(value: Double) {
        val normalized = max(0.0, value)
        prefs.edit().putString(PREF_CACHE_SIZE_LIMIT_MB, normalized.toString()).apply()
    }

    private suspend fun ensureCacheLimitAsync(pendingAdditionBytes: Int) {
        withContext(Dispatchers.IO) {
            if (!cacheRoot.exists()) return@withContext
            val limitBytes = (getCacheSizeLimitMb() * MEGABYTE).toLong()
            if (limitBytes <= 0L) return@withContext

            var currentSize = calculateDirectorySize(cacheRoot)
            if (currentSize + pendingAdditionBytes <= limitBytes) return@withContext

            val files = cacheRoot.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }
                ?: return@withContext

            for (file in files) {
                if (currentSize + pendingAdditionBytes <= limitBytes) break
                val length = file.length()
                if (file.delete()) {
                    currentSize -= length
                }
            }
        }
    }

    private fun calculateDirectorySize(root: File): Long {
        if (!root.exists()) return 0L
        var total = 0L
        root.listFiles()?.forEach { entry ->
            total += if (entry.isFile) {
                entry.length()
            } else {
                calculateDirectorySize(entry)
            }
        }
        return total
    }

    private fun bytesToMegabytes(bytes: Long): Double = bytes / MEGABYTE

    data class TtsCacheInfo(
        val enabled: Boolean,
        val sizeMb: Double,
        val sizeLimitMb: Double,
        val fileCount: Int
    ) {
        val usagePercentage: Double
            get() = if (sizeLimitMb <= 0) 0.0 else (sizeMb / sizeLimitMb) * 100.0

        val isNearLimit: Boolean
            get() = usagePercentage >= 90.0
    }

    companion object {
        private const val PREF_CACHE_ENABLED = "tts_cache_enabled"
        private const val PREF_CACHE_SIZE_LIMIT_MB = "tts_cache_size_limit_mb"
        private const val DEFAULT_CACHE_LIMIT_MB_STRING = "2048"
        private const val CACHE_FOLDER_NAME = "tts_cache"
        private const val MEGABYTE = 1024.0 * 1024.0
        private val DEFAULT_CACHE_LIMIT_MB = DEFAULT_CACHE_LIMIT_MB_STRING.toDouble()
    }
}
