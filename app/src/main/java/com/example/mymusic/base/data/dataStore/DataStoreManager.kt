package com.example.mymusic.base.data.dataStore

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import com.example.mymusic.base.common.Config.SELECTED_LANGUAGE
import com.example.mymusic.base.common.SPONSOR_BLOCK
import com.example.mymusic.base.common.SUPPORTED_LANGUAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.mymusic.base.common.QUALITY as COMMON_QUALITY

class DataStoreManager @Inject constructor(private val settingsDataStore: DataStore<Preferences>) {

    val location: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[LOCATION] ?: "VN"
    }
    val cookie: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[COOKIE] ?: ""
    }
    val playlistFromSaved = settingsDataStore.data.map { preferences ->
        preferences[FROM_SAVED_PLAYLIST] ?: ""
    }
    val quality: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[QUALITY]?.toString() ?: COMMON_QUALITY.items[0].toString()
    }
    val language: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[stringPreferencesKey(SELECTED_LANGUAGE)] ?: SUPPORTED_LANGUAGE.codes.first()
    }
    val musixmatchCookie = settingsDataStore.data.map { preferences ->
        preferences[MUSIXMATCH_COOKIE] ?: ""
    }

    val saveStateOfPlayback: Flow<String> = settingsDataStore.data.map { preferences ->
        preferences[SAVE_STATE_OF_PLAYBACK] ?: FALSE
    }

    val homeLimit: Flow<Int> = settingsDataStore.data.map { preferences ->
        preferences[HOME_LIMIT] ?: 5
    }

    suspend fun saveRecentSong (mediaId: String, position: Long) {
        withContext(Dispatchers.IO) {
            settingsDataStore.edit { settings ->
                settings[RECENT_SONG_MEDIA_ID_KEY] = mediaId
                settings[RECENT_SONG_POSITION_KEY] = position.toString()
            }
        }
    }

    companion object Settings {
        val HOME_LIMIT = intPreferencesKey("home_limit")
        val SAVE_STATE_OF_PLAYBACK = stringPreferencesKey("save_state_of_playback")
        val SPONSOR_BLOCK_ENABLED = stringPreferencesKey("sponsor_block_enabled")
        val MUSIXMATCH_COOKIE = stringPreferencesKey("musixmatch_cookie")
        val COOKIE = stringPreferencesKey("cookie")
        val FROM_SAVED_PLAYLIST = stringPreferencesKey("from_saved_playlist")
        val LOCATION = stringPreferencesKey("location")
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
        val SHUFFLE_KEY = stringPreferencesKey("shuffle_key")
        val REPEAT_KEY = stringPreferencesKey("repeat_key")
        const val REPEAT_MODE_OFF = "REPEAT_MODE_OFF"
        const val REPEAT_ONE = "REPEAT_ONE"
        const val REPEAT_ALL = "REPEAT_ALL"
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

    suspend fun recoverShuffleAndRepeatKey(shuffle: Boolean, repeat: Int) {
        withContext(Dispatchers.IO) {
            if (shuffle) {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = TRUE
                }
            }
            else {
                settingsDataStore.edit { settings ->
                    settings[SHUFFLE_KEY] = FALSE
                }
            }
            settingsDataStore.edit { settings ->
                settings[REPEAT_KEY] = when (repeat) {
                    Player.REPEAT_MODE_ONE -> REPEAT_ONE
                    Player.REPEAT_MODE_ALL -> REPEAT_ALL
                    Player.REPEAT_MODE_OFF -> REPEAT_MODE_OFF
                    else -> REPEAT_MODE_OFF
                }
            }
        }
    }

    suspend fun putString(key: String, value: String) {
        settingsDataStore.edit { settings ->
            settings[stringPreferencesKey(key)] = value
        }
    }

    val sponsorBlockEnabled = settingsDataStore.data.map { preferences ->
        preferences[SPONSOR_BLOCK_ENABLED] ?: FALSE
    }
    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            if (enabled) {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = TRUE
                }
            } else {
                settingsDataStore.edit { settings ->
                    settings[SPONSOR_BLOCK_ENABLED] = FALSE
                }
            }
        }
    }
    suspend fun getSponsorBlockCategories(): ArrayList<String> {
        val list : ArrayList<String> = arrayListOf()
        for (category in SPONSOR_BLOCK.list) {
            if (getString(category.toString()).first() == TRUE) list.add(category.toString())
        }
        return list
    }
}


