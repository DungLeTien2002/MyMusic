package com.example.mymusic.base.service

import android.content.Context
import android.media.session.MediaSession
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.repository.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class SimpleMediaServiceHandler(
    val player: ExoPlayer,
    private val mediaSession: androidx.media3.session.MediaSession,
    mediaSessionCallback: SimpleMediaSessionCallback,
    private val dataStoreManager: DataStoreManager,
    private val mainRepository: MainRepository,
    var coroutineScope: LifecycleCoroutineScope,
    private val context: Context
) : Player.Listener {

    private var _nowPlaying = MutableStateFlow(player.currentMediaItem)
    val nowPlaying = _nowPlaying.asSharedFlow()

    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    private val _liked = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    private val _sleepDone=MutableStateFlow<Boolean>(false)
    val sleepDone=_sleepDone.asSharedFlow()
}