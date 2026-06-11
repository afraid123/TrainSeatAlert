package com.trainseat.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Single app-wide DataStore instance (name "settings"). Declared once here and
// reused everywhere via import.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Falls back to the embedded BuildConfig key so the app works without manual setup.
    val rapidApiKeyFlow: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[KEY_RAPIDAPI]?.takeIf { it.isNotBlank() }
                ?: com.trainseat.app.BuildConfig.RAPIDAPI_KEY
        }

    suspend fun setRapidApiKey(key: String) {
        context.dataStore.edit { it[KEY_RAPIDAPI] = key.trim() }
    }

    companion object {
        val KEY_RAPIDAPI = stringPreferencesKey("rapidapi_key")
    }
}
