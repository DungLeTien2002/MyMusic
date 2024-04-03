package com.example.mymusic.base.data.dataStore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mymusic.base.common.QUALITY as COMMON_QUALITY

class DataStoreManager @Inject constructor(private val settingsDataStore:DataStore<Preferences>) {
    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY]?.toString() ?: COMMON_QUALITY.items[0].toString()
    }



    companion object Settings{
        val QUALITY = stringPreferencesKey("quality")
    }
}


