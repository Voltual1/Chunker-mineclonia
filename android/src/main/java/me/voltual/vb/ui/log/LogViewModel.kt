//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import me.voltual.vb.core.database.entity.LogEntry
import me.voltual.vb.core.database.repository.LogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel


class LogViewModel(private val logRepository : LogRepository) : ViewModel() {

    val logs: StateFlow<List<LogEntry>> = logRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- 选择模式相关的状态 ---
    private val _selectedItems = MutableStateFlow<Set<Int>>(emptySet())
    val selectedItems: StateFlow<Set<Int>> = _selectedItems.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    //让事件携带一个 Pair<String, Int>，分别代表要复制的文本和数量
    private val _copyEvent = MutableSharedFlow<Pair<String, Int>>()
    val copyEvent: SharedFlow<Pair<String, Int>> = _copyEvent.asSharedFlow()

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            // 清空日志后，确保退出选择模式
            clearSelection()
        }
    }

    // --- 选择操作的函数 ---

    fun toggleSelection(logId: Int) {
        val currentSelected = _selectedItems.value
        _selectedItems.value = if (currentSelected.contains(logId)) {
            currentSelected - logId
        } else {
            currentSelected + logId
        }
        // 如果选择集为空，自动退出选择模式
        if (_selectedItems.value.isEmpty()) {
            _isSelectionMode.value = false
        }
    }

    fun startSelectionMode(initialLogId: Int) {
        _isSelectionMode.value = true
        toggleSelection(initialLogId)
    }

    // 无参数的 startSelectionMode 重载函数
    fun startSelectionMode() {
        _isSelectionMode.value = true
        // 不预先选择任何项目
    }

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
            // 核心修正：调用 Repository 的公共方法，而不是直接访问 DAO
            logRepository.deleteLogsByIds(idsToDelete)
            clearSelection()
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
                
                [Request]
                ${log.requestBody}
                
                [Response]
                ${log.responseBody}
                """.trimIndent()
            }
            _copyEvent.emit(formattedLogs to logsToCopy.size)
        }
    }
}