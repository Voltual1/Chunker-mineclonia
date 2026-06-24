package me.voltual.vb.ui.export

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.callback.SingleFileConflictCallback
import com.anggrayudi.storage.file.copyFileTo
import com.anggrayudi.storage.media.FileDescription
import com.anggrayudi.storage.result.SingleFileResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.WorldExporter
import java.io.File

class ExportViewModel(
    private val context: Context
) : ViewModel() {

    var isZipping by mutableStateOf(false)
        private set
    var zipSuccess by mutableStateOf(false)
        private set

    var isExportingToLocal by mutableStateOf(false)
        private set
    var localExportStatus by mutableStateOf("")
        private set

    init {
        performZip()
    }

    private fun getWorldsDir(): File {
        val externalDir = context.getExternalFilesDir(null)
        return if (externalDir != null) {
            File(externalDir, "worlds")
        } else {
            File(context.filesDir, "worlds")
        }
    }

    fun performZip() {
        isZipping = true
        viewModelScope.launch(Dispatchers.IO) {
            val worldsDir = getWorldsDir()
            val sourceDir = File(worldsDir, "world_output")
            val targetZip = File(worldsDir, "world_output.zip")
            val success = WorldExporter.zipFolder(sourceDir, targetZip)
            withContext(Dispatchers.Main) {
                zipSuccess = success
                isZipping = false
            }
        }
    }

    fun exportToLocal(targetFolder: DocumentFile, onComplete: () -> Unit, onError: (String) -> Unit) {
        isExportingToLocal = true
        localExportStatus = "正在复制到目标目录..."
        viewModelScope.launch(Dispatchers.IO) {
            val worldsDir = getWorldsDir()
            val localZipFile = File(worldsDir, "world_output.zip")
            val docZipFile = DocumentFile.fromFile(localZipFile)

            val fileDescription = FileDescription("world_output", "", "application/zip")
            val conflictCallback = object : SingleFileConflictCallback<DocumentFile>(viewModelScope) {
                override fun onFileConflict(destinationFile: DocumentFile, action: FileConflictAction) {
                    action.confirmResolution(ConflictResolution.REPLACE)
                }
            }

            docZipFile.copyFileTo(
                context = context,
                targetFolder = targetFolder,
                fileDescription = fileDescription,
                onConflict = conflictCallback
            ).collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is SingleFileResult.Completed -> {
                            isExportingToLocal = false
                            onComplete()
                        }
                        is SingleFileResult.Error -> {
                            isExportingToLocal = false
                            onError(result.errorCode.toString())
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}