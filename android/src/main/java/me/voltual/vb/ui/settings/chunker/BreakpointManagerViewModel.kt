package me.voltual.vb.ui.settings.chunker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.voltual.vb.core.database.repository.ConversionTaskRepository
import me.voltual.vb.data.model.ConversionManifest

data class BreakpointManagerState(
    val manifests: List<ConversionManifest> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class BreakpointManagerViewModel(
    private val repository: ConversionTaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreakpointManagerState())
    val uiState: StateFlow<BreakpointManagerState> = _uiState.asStateFlow()

    init {
        loadAllBreakpointManifests()
    }

    fun loadAllBreakpointManifests() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val list = repository.getAllManifests()
            _uiState.update { it.copy(manifests = list, isLoading = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 写入或更新断点配置文件。
     * 如果 worldId 被修改，我们需要删除老 id 的条目以防止数据库产生脏数据。
     */
    fun saveManifest(originalWorldId: String?, updatedManifest: ConversionManifest) {
        viewModelScope.launch {
            if (originalWorldId != null && originalWorldId != updatedManifest.worldId) {
                // 如果用户覆写了主键，则执行一删一增逻辑以保证主键更名一致
                repository.deleteManifest(originalWorldId)
            }
            repository.saveManifest(updatedManifest)
            loadAllBreakpointManifests()
        }
    }

    fun deleteManifest(worldId: String) {
        viewModelScope.launch {
            repository.deleteManifest(worldId)
            loadAllBreakpointManifests()
        }
    }

    /**
     * 新增一条全空断点
     */
    fun createEmptyManifest(): ConversionManifest {
        val uniqueId = "W_" + System.currentTimeMillis().toString().takeLast(6)
        return ConversionManifest(
            worldId = uniqueId,
            inputPath = "/storage/emulated/0/",
            outputPath = "/storage/emulated/0/",
            format = "JAVA_1_20_5",
            progressIndex = 0,
            lastBedrockKeyBase64 = null,
            isActive = false
        )
    }
}