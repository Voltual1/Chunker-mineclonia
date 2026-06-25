package me.voltual.vb.ui.settings.conversion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.voltual.vb.data.ConversionSettingsDataStore

class ConversionSettingsViewModel(
    private val dataStore: ConversionSettingsDataStore
) : ViewModel() {

    val threadCount: StateFlow<Int> = dataStore.threadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val processMaps: StateFlow<Boolean> = dataStore.processMaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val maxCores = ConversionSettingsDataStore.maxAvailableCores

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
}