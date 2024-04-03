package com.example.mymusic.base.service

import android.media.session.MediaSession
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class SimpleMediaServiceHandler(
    val player: ExoPlayer,
    private val mediaSession: MediaSession,
    mediaSessionCallback: SimpleMediaServiceHandler,
    var coroutineScope: LifecycleCoroutineScope
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