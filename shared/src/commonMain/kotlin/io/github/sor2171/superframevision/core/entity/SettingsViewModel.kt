package io.github.sor2171.superframevision.core.entity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel() {
    private val _settings = MutableStateFlow<SettingsRepository.OverallSettings?>(null)
    val settings: StateFlow<SettingsRepository.OverallSettings?> = _settings.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _settings.value = SettingsRepository.load()
        }
    }
}