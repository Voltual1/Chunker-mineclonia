package me.voltual.vb.ui.settings.cache

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.extension.text.formatSize
import java.io.File

class CacheSettingsViewModel(
    private val context: Context
) : ViewModel() {

    var outputFolderSize by mutableStateOf("0 B")
        private set
    var zipFileSize by mutableStateOf("0 B")
        private set
    var totalSize by mutableStateOf("0 B")
        private set

    init {
        calculateCacheSizes()
    }

    private fun getWorldsDir(): File {
        val externalDir = context.getExternalFilesDir(null)
        return if (externalDir != null) {
            File(externalDir, "worlds")
        } else {
            File(context.filesDir, "worlds")
        }
    }

    fun calculateCacheSizes() {
        viewModelScope.launch(Dispatchers.IO) {
            // 旧版缓存路径
            val oldOutputDir = File(context.filesDir, "world_output")
            val oldZipFile = File(context.filesDir, "world_output.zip")

            // 新版 FTP 对齐缓存路径
            val worldsDir = getWorldsDir()
            val newOutputDir = File(worldsDir, "world_output")
            val newZipFile = File(worldsDir, "world_output.zip")

            // 累计大小
            val outputLength = getFolderSize(oldOutputDir) + getFolderSize(newOutputDir)
            val zipLength = (if (oldZipFile.exists()) oldZipFile.length() else 0L) +
                    (if (newZipFile.exists()) newZipFile.length() else 0L)

            withContext(Dispatchers.Main) {
                outputFolderSize = outputLength.formatSize()
                zipFileSize = zipLength.formatSize()
                totalSize = (outputLength + zipLength).formatSize()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            // 清理旧路径
            val oldOutputDir = File(context.filesDir, "world_output")
            val oldZipFile = File(context.filesDir, "world_output.zip")
            if (oldOutputDir.exists()) {
                oldOutputDir.deleteRecursively()
            }
            if (oldZipFile.exists()) {
                oldZipFile.delete()
            }

            // 清理新路径
            val worldsDir = getWorldsDir()
            val newOutputDir = File(worldsDir, "world_output")
            val newZipFile = File(worldsDir, "world_output.zip")
            if (newOutputDir.exists()) {
                newOutputDir.deleteRecursively()
            }
            if (newZipFile.exists()) {
                newZipFile.delete()
            }

            calculateCacheSizes()
        }
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val files = file.listFiles() ?: return 0L
        for (f in files) {
            size += getFolderSize(f)
        }
        return size
    }
}