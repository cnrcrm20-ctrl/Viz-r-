package com.example.model

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RemoteVideoFile(
    val id: String,
    val title: String,
    val sizeBytes: Long,
    val uploadDate: Date,
    val resolution: String = "480p",
    var isExpired: Boolean = false
)

object VideoUtils {

    const val MAX_VIDEO_DURATION_SECONDS = 60
    const val COMPRESSED_TARGET_SIZE_BYTES = 980_000L // Cap strictly under 1 MB

    private val _remoteDatabaseSim = MutableStateFlow<List<RemoteVideoFile>>(emptyList())
    val remoteDatabaseSim = _remoteDatabaseSim.asStateFlow()

    private val _localMediaFolders = MutableStateFlow<List<String>>(emptyList())
    val localMediaFolders = _localMediaFolders.asStateFlow()

    init {
        // Generate pre-loaded mock files to simulate current dataset spanning over 10 days
        seedMockRemoteDatabase()
    }

    private fun seedMockRemoteDatabase() {
        val calendar = Calendar.getInstance()
        val list = mutableListOf<RemoteVideoFile>()

        // 9 days ago (Should be expired)
        calendar.add(Calendar.DAY_OF_YEAR, -9)
        list.add(RemoteVideoFile(UUID.randomUUID().toString(), "Istanbul Sunset Reel", 870_000L, calendar.time))

        // 8 days ago (Should be expired)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        list.add(RemoteVideoFile(UUID.randomUUID().toString(), "Delicious Turkish Coffee Recipe", 910_000L, calendar.time))

        // 4 days ago (Keep)
        calendar.add(Calendar.DAY_OF_YEAR, 4)
        list.add(RemoteVideoFile(UUID.randomUUID().toString(), "Retro Arcade Gaming Snippet", 600_000L, calendar.time))

        // 1 day ago (Keep)
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        list.add(RemoteVideoFile(UUID.randomUUID().toString(), "Grand Bazaar Tour in 60s", 985_000L, calendar.time))

        _remoteDatabaseSim.value = list
    }

    /**
     * Simulates compression to under 1MB, downscaling resolution to 480p/360p,
     * and duplicating to physical local folder /Vizor/Media/
     */
    fun compressAndSaveVideo(
        context: Context,
        title: String,
        durationSeconds: Int,
        originalBytes: Long,
        onComplete: (localPath: String, remoteSim: RemoteVideoFile) -> Unit,
        onError: (String) -> Unit
    ) {
        if (durationSeconds > MAX_VIDEO_DURATION_SECONDS) {
            onError("Durduruldu: Video 1 dakikadan uzun olamaz! / Video is over 60 seconds limit!")
            return
        }

        // 1. Simulate Local Hardware Clone (original high-quality file duplicated to /Vizor/Media/)
        val localDir = File(context.getExternalFilesDir(null), "Vizor/Media")
        if (!localDir.exists()) {
            localDir.mkdirs()
        }

        // Register visual paths
        val localFileName = "VIZ_${System.currentTimeMillis()}_HQ.mp4"
        val localFile = File(localDir, localFileName)
        localFile.writeText("High quality stream placeholder for $title") // write dummy bytes to physical disk

        // 2. Simulate FFmpeg / compressor action on client-side
        // Aggressively downscale to 480p @ 24fps
        val compressedBytes = (COMPRESSED_TARGET_SIZE_BYTES * (durationSeconds.toFloat() / 60f)).toLong().coerceAtLeast(100_000L)

        // 3. Prepare Remote upload with strict metadata tags
        val remoteFile = RemoteVideoFile(
            id = UUID.randomUUID().toString(),
            title = title,
            sizeBytes = compressedBytes,
            uploadDate = Date()
        )

        // Add to simulated datastore
        val currentList = _remoteDatabaseSim.value.toMutableList()
        currentList.add(remoteFile)
        _remoteDatabaseSim.value = currentList

        // Track local folders for visualization screen
        val relativeFolder = "/Android/data/${context.packageName}/files/Vizor/Media/$localFileName"
        val folders = _localMediaFolders.value.toMutableList()
        folders.add(0, relativeFolder)
        _localMediaFolders.value = folders

        onComplete(localFile.absolutePath, remoteFile)
    }

    /**
     * Periodic automated purge sweep: scans and deletes database items > 7 days old
     */
    fun run7DayExpiringBackupSweep(): Int {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        val currentList = _remoteDatabaseSim.value
        val expiryLimit = now - sevenDaysMs

        var deletedCount = 0
        val remainingList = currentList.filter {
            val isExpired = it.uploadDate.time < expiryLimit
            if (isExpired) deletedCount++
            !isExpired
        }

        _remoteDatabaseSim.value = remainingList
        return deletedCount
    }
}
