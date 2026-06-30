package com.photoviewer.samba.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.photoviewer.samba.data.model.SambaConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "samba_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_SERVER_IP  = stringPreferencesKey("server_ip")
        private val KEY_SHARE_NAME = stringPreferencesKey("share_name")
        private val KEY_USERNAME   = stringPreferencesKey("username")
        private val KEY_PASSWORD   = stringPreferencesKey("password")
    }

    val sambaConfig: Flow<SambaConfig> = context.dataStore.data.map { prefs ->
        SambaConfig(
            serverIp  = prefs[KEY_SERVER_IP]  ?: "",
            shareName = prefs[KEY_SHARE_NAME] ?: "",
            username  = prefs[KEY_USERNAME]   ?: "",
            password  = prefs[KEY_PASSWORD]   ?: ""
        )
    }

    suspend fun saveConfig(config: SambaConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_IP]  = config.serverIp
            prefs[KEY_SHARE_NAME] = config.shareName
            prefs[KEY_USERNAME]   = config.username
            prefs[KEY_PASSWORD]   = config.password
        }
    }
}
