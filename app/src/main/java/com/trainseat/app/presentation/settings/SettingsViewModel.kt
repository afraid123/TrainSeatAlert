package com.trainseat.app.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainseat.app.data.prefs.UserPreferences
import com.trainseat.app.data.prefs.dataStore
import com.trainseat.app.data.repository.AlertRepository
import com.trainseat.app.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AlertRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult

    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult

    val rapidApiKey: StateFlow<String> = userPreferences.rapidApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setRapidApiKey(key: String) {
        viewModelScope.launch { userPreferences.setRapidApiKey(key) }
    }

    val defaultIntervalMinutes: StateFlow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEFAULT_INTERVAL] ?: 30 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 30)

    val alarmVolume: StateFlow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_ALARM_VOLUME] ?: 80 }
        .stateIn(viewModelScope, SharingStarted.Lazily, 80)

    val keepScreenOn: StateFlow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_KEEP_SCREEN_ON] ?: true }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val themeMode: StateFlow<ThemeMode> = context.dataStore.data
        .map { prefs ->
            when (prefs[KEY_THEME_MODE]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.SYSTEM)

    fun setDefaultInterval(minutes: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_DEFAULT_INTERVAL] = minutes }
        }
    }

    fun setAlarmVolume(volume: Int) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_ALARM_VOLUME] = volume }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
        }
    }

    fun exportAlerts() {
        viewModelScope.launch {
            val json = repository.exportAlerts()
            _exportResult.value = json
        }
    }

    fun importAlerts(json: String) {
        viewModelScope.launch {
            val count = repository.importAlerts(json)
            _importResult.value = count
        }
    }

    fun clearExportResult() { _exportResult.value = null }
    fun clearImportResult() { _importResult.value = null }

    companion object {
        val KEY_DEFAULT_INTERVAL = intPreferencesKey("default_interval")
        val KEY_ALARM_VOLUME = intPreferencesKey("alarm_volume")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
