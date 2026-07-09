package me.voltual.vb.ui.log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.database.repository.LogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LogViewModel(
    private val context: Context, // 注入 Context 用于定位 cacheDir
    private val logRepository: LogRepository
) : ViewModel() {

    // 控制是否同时显示文件切片日志
    private val _showSliceLogs = MutableStateFlow(true)
    val showSliceLogs = _showSliceLogs.asStateFlow()

    // 定时触发器，用于刷新文件日志
    private val _fileRefreshTrigger = MutableStateFlow(0)

    private val dbLogs: Flow<List<LogEntry>> = logRepository.allLogs

    private val fileLogs: Flow<List<LogEntry>> = _fileRefreshTrigger
        .map {
            withContext(Dispatchers.IO) {
                parseSliceLogFile()
            }
        }

    // 3. 最终合并的日志流：将数据库日志与文件文本日志合并，并按时间降序排序
    val logs: StateFlow<List<LogEntry>> = combine(dbLogs, fileLogs, _showSliceLogs) { dbList, fileList, showFile ->
        if (showFile) {
            (dbList + fileList).sortedByDescending { it.id } // 假设时间正相关于 id，或者按真实时间
        } else {
            dbList.sortedByDescending { it.id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- 选择模式相关的状态 ---
    private val _selectedItems = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItems: StateFlow<Set<Int>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _copyEvent = MutableSharedFlow<Pair<String, Int>>()
    val copyEvent: SharedFlow<Pair<String, Int>> = _copyEvent.asSharedFlow()

    init {
        // 初始加载一次文件日志
        refreshFileLogs()
    }

    fun refreshFileLogs() {
        _fileRefreshTrigger.value += 1
    }

    /**
     * 解析 slice_log.txt 文件并构建虚拟的 LogEntry 列表
     */
    private fun parseSliceLogFile(): List<LogEntry> {
        val logFile = File(context.cacheDir, "slice_log.txt")
        if (!logFile.exists()) return emptyList()

        val entries = mutableListOf<LogEntry>()
        try {
            val lines = logFile.readLines()
            var currentId = -999999 // 为文件日志分配一个负数虚拟ID，避免与数据库冲突
            
            // 简单按行或者按块粗暴解析
            lines.forEachIndexed { index, line ->
                if (line.isBlank()) return@forEachIndexed
                
                // 识别你在 ConversionWorker 中定义的标记
                val isError = line.contains("[Memory Monitor]") || line.contains("critically high") || line.contains("Exception") || line.contains("Error")
                
                entries.add(
                    LogEntry(
                        id = currentId++,
                        type = if (line.contains("[Slicing]")) "SLICING" else "CONVERTER",
                        status = if (isError) "FAILURE" else "SUCCESS",
                        requestBody = "行号: ${index + 1}",
                        responseBody = line,
                        timestamp = logFile.lastModified() // 由于纯文本没有每行精确时间戳，暂取文件修改时间
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 最新的日志行排在前面
        return entries.reversed()
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            // 同时顺便清空切片日志文件
            withContext(Dispatchers.IO) {
                val logFile = File(context.cacheDir, "slice_log.txt")
                if (logFile.exists()) logFile.delete()
            }
            refreshFileLogs()
            clearSelection()
        }
    }

    // --- 选择操作函数（保持不变，但需过滤虚拟负数ID的删除操作） ---
    fun toggleSelection(logId: Int) {
        val currentSelected = _selectedItems.value
        _selectedItems.value = if (currentSelected.contains(logId)) {
            currentSelected - logId
        } else {
            currentSelected + logId
        }
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun startSelectionMode(initialLogId: Int) {
        _isSelectionMode.value = true
        toggleSelection(initialLogId)
    }

    fun startSelectionMode() { _isSelectionMode.value = true }

    fun selectAll() {
        _selectedItems.value = logs.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun invertSelection() {
        val allIds = logs.value.map { it.id }.toSet()
        _selectedItems.value = allIds - _selectedItems.value
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val idsToDelete = _selectedItems.value.toList()
            
            // 分流处理：正数走数据库删除
            val dbIds = idsToDelete.filter { it > 0 }
            if (dbIds.isNotEmpty()) {
                logRepository.deleteLogsByIds(dbIds)
            }
            
            // 如果选择了虚拟文件日志，暂不支持单条在文本中删除，通常建议直接通过“清空全部”清除
            clearSelection()
            refreshFileLogs()
        }
    }

    fun copySelectedLogs() {
        viewModelScope.launch {
            val selectedIds = _selectedItems.value
            if (selectedIds.isEmpty()) return@launch

            val logsToCopy = logs.value.filter { it.id in selectedIds }

            val formattedLogs = logsToCopy.joinToString(separator = "\n\n=======\n\n") { log ->
                """
                [${log.formattedTime()}] [${log.type}] - ${log.status}
                
                [Info/Line]
                ${log.requestBody}
                
                [Message]
                ${log.responseBody}
                """.trimIndent()
            }
            _copyEvent.emit(formattedLogs to logsToCopy.size)
        }
    }
}