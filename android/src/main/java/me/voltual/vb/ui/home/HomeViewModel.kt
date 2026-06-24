package me.voltual.vb.ui.home

import android.content.Context
import android.text.format.Formatter
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
    // 增加一个状态用来控制是否显示“不确定进度条”
    var isIndeterminateProgress by mutableStateOf(true) 
    var copyProgress by mutableStateOf(0f)
    var copyStatusText by mutableStateOf("")

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
                "MINECLONIA", "JAVA_1_21", "JAVA_1_20_5", "JAVA_1_19_4",
                "JAVA_1_18_2", "BEDROCK_R21_80", "BEDROCK_R20_80", "BEDROCK_R19_30"
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

    fun startCopyAndNavigate(context: Context, navigator: Navigator) {
        val source = selectedFolder ?: return
        val format = selectedFormat ?: return

        isCopying = true
        isIndeterminateProgress = true // 默认开启无限循环滚动动画
        copyProgress = 0f
        copyStatusText = "正在准备复制..."

        viewModelScope.launch(Dispatchers.IO) {
            val inputDir = File(context.filesDir, "world_input")
            if (inputDir.exists()) {
                inputDir.deleteRecursively()
            }
            inputDir.mkdirs()

            val targetParentDoc = DocumentFile.fromFile(context.filesDir)
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
                            isIndeterminateProgress = true
                            copyStatusText = "正在准备文件..."
                        }
                        is SingleFolderResult.CountingFiles -> {
                            // 针对大地图，此阶段可能被卡住，提示用户正在快速跳过或加载中
                            isIndeterminateProgress = true
                            copyStatusText = "正在解析大型存档目录结构..."
                        }
                        is SingleFolderResult.Starting -> {
                            isIndeterminateProgress = true
                            copyStatusText = "开始复制文件..."
                        }
                        is SingleFolderResult.InProgress -> {
                            // 当获取到有效进度时，如果外部库支持返回合法进度，则切换为精确进度条
                            // 针对不返回总数的大地图，result.progress 如果始终为 0 或错误值，我们可以维持不确定状态
                            if (result.progress > 0f) {
                                isIndeterminateProgress = false
                                copyProgress = result.progress / 100f
                            } else {
                                isIndeterminateProgress = true
                            }
                            copyStatusText = "正在传输数据: 已复制 ${Formatter.formatFileSize(context, result.bytesMoved)}"
                        }
                        is SingleFolderResult.Completed -> {
                            isCopying = false
                            copyStatusText = "复制完成！"
                            val localInputPath = File(context.filesDir, "world_input").absolutePath
                            val localOutputPath = File(context.filesDir, "world_output").absolutePath
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
                        is SingleFolderResult.Error -> {
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