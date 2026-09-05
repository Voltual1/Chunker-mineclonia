package me.voltual.vb.ui.settings.chunker

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.voltual.vb.core.database.repository.ConversionTaskRepository
import me.voltual.vb.data.ChunkerSettingsDataStore
import me.voltual.vb.data.model.ConversionManifest

class ChunkerSettingsViewModel(
    private val dataStore: ChunkerSettingsDataStore,
    private val conversionTaskRepository: ConversionTaskRepository
) : ViewModel() {

    private val jsonFormat = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    val threadCount: StateFlow<Int> = dataStore.threadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val processMaps: StateFlow<Boolean> = dataStore.processMaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val energySavingMode: StateFlow<Boolean> = dataStore.energySavingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val enableSlicing: StateFlow<Boolean> = dataStore.enableSlicing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val maxCores = ChunkerSettingsDataStore.maxAvailableCores

    fun updateThreadCount(count: Int) {
        viewModelScope.launch {
            dataStore.setThreadCount(count)
        }
    }

    fun updateProcessMaps(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setProcessMaps(enabled)
        }
    }

    fun updateEnergySavingMode(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setEnergySavingMode(enabled)
        }
    }

    fun updateEnableSlicing(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setEnableSlicing(enabled)
        }
    }

    fun clearAllProgress() {
        viewModelScope.launch {
            conversionTaskRepository.clearAll()
        }
    }

    /**
     * 将全量断点记录序列化并导出为 breakpoints_backup.json 至用户选定沙盒目录
     */
    fun exportBreakpoints(
        folder: DocumentFile,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val manifests = conversionTaskRepository.getAllManifests()
                if (manifests.isEmpty()) {
                    onError("REGISTRY_EMPTY // 当前断点数据库中没有任何记录，无需导出")
                    return@launch
                }

                val jsonStr = withContext(Dispatchers.Default) {
                    jsonFormat.encodeToString(manifests)
                }

                val targetFile = folder.makeFile(
                    context = context,
                    name = "breakpoints_backup.json",
                    mimeType = "application/json",
                    mode = CreateMode.REPLACE
                )

                if (targetFile != null) {
                    withContext(Dispatchers.IO) {
                        targetFile.openOutputStream(context, append = false)?.use { output ->
                            output.write(jsonStr.toByteArray(Charsets.UTF_8))
                        } ?: throw Exception("Failed to open file output stream")
                    }
                    onSuccess()
                } else {
                    onError("IO_ERROR // 无法在目标文件夹中创建或覆盖备份文件")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.localizedMessage ?: "Unknown IO Exception")
            }
        }
    }

    /**
     * 从外部加载 JSON 断点备份，合入并覆写到 Room 本地数据库底座中
     */
    fun importBreakpoints(
        file: DocumentFile,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val jsonStr = withContext(Dispatchers.IO) {
                    file.openInputStream(context)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw Exception("Failed to open file input stream")
                }

                val manifests = withContext(Dispatchers.Default) {
                    jsonFormat.decodeFromString<List<ConversionManifest>>(jsonStr)
                }

                if (manifests.isEmpty()) {
                    onError("INVALID_BACKUP // 备份包内容为空")
                    return@launch
                }

                manifests.forEach { manifest ->
                    conversionTaskRepository.saveManifest(manifest)
                }
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                onError("PARSE_ERR // JSON 解析失败，格式可能有损: ${e.localizedMessage}")
            }
        }
    }
}