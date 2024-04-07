package com.example.mymusic.base.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mymusic.base.common.Config.SELECTED_LANGUAGE
import com.example.mymusic.base.common.SUPPORTED_LANGUAGE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.example.mymusic.base.common.QUALITY as COMMON_QUALITY

class DataStoreManager @Inject constructor(private val settingsDataStore: DataStore<Preferences>) {
    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY]?.toString() ?: COMMON_QUALITY.items[0].toString()
    }
    val language: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[stringPreferencesKey(SELECTED_LANGUAGE)] ?: SUPPORTED_LANGUAGE.codes.first()
    }

    companion object Settings {
        val QUALITY = stringPreferencesKey("quality")
        val WATCH_VIDEO_INSTEAD_OF_PLAYING_AUDIO =
            stringPreferencesKey("watch_video_instead_of_playing_audio")
        const val TRUE = "TRUE"
        const val FALSE = "FALSE"
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val MAX_SONG_CACHE_SIZE = intPreferencesKey("maxSongCacheSize")
        val SAVE_RECENT_SONG = stringPreferencesKey("save_recent_song")
        const val RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE = "RestoreLastPlayedTrackAndQueueDone"
        val RECENT_SONG_MEDIA_ID_KEY = stringPreferencesKey("recent_song_media_id")
        val USE_TRANSLATION_LANGUAGE = stringPreferencesKey("use_translation_language")
        val TRANSLATION_LANGUAGE = stringPreferencesKey("translation_language")
        val RECENT_SONG_POSITION_KEY = stringPreferencesKey("recent_song_position")
    }
    val translationLanguage = settingsDataStore.data.map { preferences ->
        preferences[TRANSLATION_LANGUAGE] ?: if (language.first().length >= 2) language.first()
            .substring(0..1) else "en"
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

    val recentPosition = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_POSITION_KEY] ?: "0"
    }

    val saveRecentSongAndQueue: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_RECENT_SONG] ?: FALSE
    }
    val recentMediaId = settingsDataStore.data.map { preferences ->
        preferences[RECENT_SONG_MEDIA_ID_KEY] ?: ""
    }
    fun getString(key: String): Flow<String?> {
        return settingsDataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key)]
        }
    }
    val enableTranslateLyric = settingsDataStore.data.map { preferences ->
        preferences[USE_TRANSLATION_LANGUAGE] ?: FALSE
    }

    suspend fun putString(key: String, value: String) {
        settingsDataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }
}


