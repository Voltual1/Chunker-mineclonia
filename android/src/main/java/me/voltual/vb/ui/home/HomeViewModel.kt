package me.voltual.vb.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.callback.SingleFolderConflictCallback
import com.anggrayudi.storage.file.copyFolderTo
import com.anggrayudi.storage.result.SingleFolderResult
import com.hivemc.chunker.conversion.encoding.EncodingType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.voltual.vb.core.utils.extension.text.formatSize
import me.voltual.vb.ui.Navigator
import me.voltual.vb.ui.TerminalExec
import java.io.File

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState = _uiState.asStateFlow()

    var selectedFolder by mutableStateOf<DocumentFile?>(null)
    var selectedFormat by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")

    var isCopying by mutableStateOf(false)
    var copyProgress by mutableStateOf(0f)
    var copyStatusText by mutableStateOf("")

    // 新增中转站文件状态
    var hasExistingInput by mutableStateOf(false)
        private set
    var useExistingInput by mutableStateOf(false)

    val availableFormats: List<String> by lazy {
        val formats = mutableListOf<String>()
        formats.add("MINECLONIA（实验性）")
        try {
            val writeableTypes = EncodingType.getWriteableTypes()
            for (type in writeableTypes) {
                if (type.isInternal) continue
                val typeName = type.name.uppercase()
                for (version in type.supportedVersions) {
                    val versionStr = version.toString().replace('.', '_')
                    formats.add("${typeName}_$versionStr")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (formats.size <= 1) {
            listOf(
                "MINECLONIA",
                "JAVA_1_21",
                "JAVA_1_20_5",
                "JAVA_1_19_4",
                "JAVA_1_18_2",
                "BEDROCK_R21_80",
                "BEDROCK_R20_80",
                "BEDROCK_R19_30"
            )
        } else {
            formats.sorted()
        }
    }

    val filteredFormats: List<String>
        get() = if (searchQuery.isBlank()) {
            availableFormats
        } else {
            availableFormats.filter { it.contains(searchQuery, ignoreCase = true) }
        }

    private fun getWorldsDir(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        val worldsDir = if (externalDir != null) {
            File(externalDir, "worlds")
        } else {
            File(context.filesDir, "worlds")
        }
        if (!worldsDir.exists()) {
            worldsDir.mkdirs()
        }
        return worldsDir
    }

    fun checkExistingInput(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val worldsDir = getWorldsDir(context)
            val inputDir = File(worldsDir, "world_input")
            val hasFiles = inputDir.exists() && (inputDir.listFiles()?.isNotEmpty() == true)
            withContext(Dispatchers.Main) {
                hasExistingInput = hasFiles
            }
        }
    }

    fun startCopyAndNavigate(context: Context, navigator: Navigator) {
        val format = selectedFormat ?: return
        val worldsDir = getWorldsDir(context)
        val localInputPath = File(worldsDir, "world_input").absolutePath
        val localOutputPath = File(worldsDir, "world_output").absolutePath

        // 如果用户选择直接使用 FTP 导入的文件，则不进行 SAF 复制
        if (useExistingInput) {
            val outputDir = File(localOutputPath)
            if (outputDir.exists()) {
                outputDir.deleteRecursively()
            }
            outputDir.mkdirs()

            navigator.navigate(
                TerminalExec(
                    inputPath = localInputPath,
                    outputPath = localOutputPath,
                    format = format
                )
            )
            return
        }

        val source = selectedFolder ?: return
        isCopying = true
        copyProgress = 0f
        copyStatusText = "正在准备复制..."

        viewModelScope.launch(Dispatchers.IO) {
            val inputDir = File(worldsDir, "world_input")
            if (inputDir.exists()) {
                inputDir.deleteRecursively()
            }
            inputDir.mkdirs()

            val targetParentDoc = DocumentFile.fromFile(worldsDir)
            val conflictCallback = object : SingleFolderConflictCallback(viewModelScope) {
                override fun onParentConflict(
                    destinationFolder: DocumentFile,
                    action: ParentFolderConflictAction,
                    canMerge: Boolean
                ) {
                    action.confirmResolution(ConflictResolution.REPLACE)
                }
            }

            source.copyFolderTo(
                context = context,
                targetParentFolder = targetParentDoc,
                skipEmptyFiles = false,
                newFolderNameInTargetPath = "world_input",
                onConflict = conflictCallback
            ).collect { result ->
                withContext(Dispatchers.Main) {
                    when (result) {
                        is SingleFolderResult.Preparing -> {
                            copyStatusText = "正在准备文件..."
                        }
                        is SingleFolderResult.CountingFiles -> {
                            copyStatusText = "正在计算文件数量..."
                        }
                        is SingleFolderResult.Starting -> {
                            copyStatusText = "开始复制文件..."
                        }
                        is SingleFolderResult.InProgress -> {
                            copyProgress = result.progress / 100f
                            copyStatusText = "已复制: ${(result.progress).toInt()}% (${result.bytesMoved.formatSize()})"
                        }
                        is SingleFolderResult.Completed -> {
                            isCopying = false
                            copyStatusText = "复制完成！"
                            val outputDir = File(localOutputPath)
                            if (outputDir.exists()) {
                                outputDir.deleteRecursively()
                            }
                            outputDir.mkdirs()

                            navigator.navigate(
                                TerminalExec(
                                    inputPath = localInputPath,
                                    outputPath = localOutputPath,
                                    format = format
                                )
                            )
                        }
                        is SingleFileResult.Error -> {
                            isCopying = false
                            copyStatusText = "复制失败: ${result.errorCode}"
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

sealed interface HomeUiState {
    data object Idle : HomeUiState
}