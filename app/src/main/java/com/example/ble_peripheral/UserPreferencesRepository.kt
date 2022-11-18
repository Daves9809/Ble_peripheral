package com.example.ble_peripheral

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val value = dataStore.data.map { it[CONNECTION_STATE] ?: false }

    suspend fun saveState(boolean: Boolean){
        dataStore.edit {
            it[CONNECTION_STATE] = boolean
        }
    }
    companion object PreferencesKeys{
        val CONNECTION_STATE = booleanPreferencesKey("connection_state")
    }
}