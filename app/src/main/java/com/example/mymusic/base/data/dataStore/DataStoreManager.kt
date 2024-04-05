package com.example.mymusic.base.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.mymusic.base.common.QUALITY as COMMON_QUALITY

class DataStoreManager @Inject constructor(private val settingsDataStore: DataStore<Preferences>) {
    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY]?.toString() ?: COMMON_QUALITY.items[0].toString()
    }

    companion object Settings {
        val QUALITY = stringPreferencesKey("quality")
        val WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO =
            stringPreferencesKey("watch_video_instead_of_playing_audio")
        const val TRUE = "TRUE"
        const val FALSE = "FALSE"
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val MAX_SONG_CACHE_SIZE = intPreferencesKey("maxSongCacheSize")
    }

    val watchVideoInsteadOfPlayingAudio = settingsDataStore.data.map { preferences ->
        preferences[WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO] ?: FALSE
    }

    val videoQuality = settingsDataStore.data.map { preferences ->
        preferences[VIDEO_QUALITY] ?: "720p"
    }
    val maxSongCacheSize = settingsDataStore.data.map { preferences ->
        preferences[MAX_SONG_CACHE_SIZE] ?: -1
    }
}


