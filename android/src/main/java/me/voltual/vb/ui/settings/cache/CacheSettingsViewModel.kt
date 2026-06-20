package me.voltual.vb.ui.settings.cache

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun calculateCacheSizes() {
        viewModelScope.launch(Dispatchers.IO) {
            val outputDir = File(context.filesDir, "world_output")
            val zipFile = File(context.filesDir, "world_output.zip")

            val outputLength = getFolderSize(outputDir)
            val zipLength = if (zipFile.exists()) zipFile.length() else 0L

            withContext(Dispatchers.Main) {
                outputFolderSize = Formatter.formatFileSize(context, outputLength)
                zipFileSize = Formatter.formatFileSize(context, zipLength)
                totalSize = Formatter.formatFileSize(context, outputLength + zipLength)
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val outputDir = File(context.filesDir, "world_output")
            val zipFile = File(context.filesDir, "world_output.zip")

            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
            if (zipFile.exists()) {
                zipFile.delete()
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