package com.example.mymusic.base.service

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.example.mymusic.R
import com.example.mymusic.base.common.MEDIA_CUSTOM_COMMAND
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.utils.extension.connectArtists
import com.example.mymusic.base.utils.extension.toListName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@UnstableApi
class SimpleMediaServiceHandler(
    val player: ExoPlayer,
    private val mediaSession: androidx.media3.session.MediaSession,
    mediaSessionCallback: SimpleMediaSessionCallback,
    private val dataStoreManager: DataStoreManager,
    private val mainRepository: MainRepository,
    var coroutineScope: LifecycleCoroutineScope,
    private val context: Context
) : Player.Listener {
    private val _sleepMinutes = MutableStateFlow<Int>(0)
    val sleepMinutes = _sleepMinutes.asSharedFlow()
    private var sleepTimerJob: Job? = null
    private var _stateFlow = MutableStateFlow<StateSource>(StateSource.STATE_CREATED)
    val stateFlow = _stateFlow.asStateFlow()
    private var _currentSongIndex = MutableStateFlow<Int>(0)
    val currentSongIndex = _currentSongIndex.asSharedFlow()
    private var _nowPlaying = MutableStateFlow(player.currentMediaItem)
    val nowPlaying = _nowPlaying.asSharedFlow()
    var added: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _nextTrackAvailable = MutableStateFlow<Boolean>(false)
    val nextTrackAvailable = _nextTrackAvailable.asSharedFlow()

    private val _previousTrackAvailable = MutableStateFlow<Boolean>(false)
    val previousTrackAvailable = _previousTrackAvailable.asSharedFlow()

    private var volumeNormalizationJob: Job? = null

    //Add MusicSource to this
    var catalogMetadata: ArrayList<Track> = (arrayListOf())
    private val _liked = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    private val _sleepDone = MutableStateFlow<Boolean>(false)
    val sleepDone = _sleepDone.asSharedFlow()
    private var loadJob: Job? = null
    private var job: Job? = null

    private val _shuffle = MutableStateFlow<Boolean>(false)
    val shuffle = _shuffle.asSharedFlow()

    private val _repeat = MutableStateFlow<RepeatState>(RepeatState.None)
    val repeat = _repeat.asSharedFlow()

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    val simpleMediaState = _simpleMediaState.asStateFlow()

    private var toggleLikeJob: Job? = null

    private var updateNotificationJob: Job? = null

    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
    }

    fun like(liked: Boolean) {
        _liked.value = liked
        updateNotification()
    }

    fun changeAddedState() {
        added.value = false
    }

    fun swap(from: Int, to: Int) {
        if (from < to) {
            for (i in from until to) {
                moveItemDown(i)
            }
        } else {
            for (i in from downTo to + 1) {
                moveItemUp(i)
            }
        }
    }

    @UnstableApi
    fun moveItemUp(position: Int) {
        moveMediaItem(position, position - 1)
        val temp = catalogMetadata[position]
        catalogMetadata[position] = catalogMetadata[position - 1]
        catalogMetadata[position - 1] = temp
        _currentSongIndex.value = currentIndex()
    }

    @UnstableApi
    fun moveItemDown(position: Int) {
        moveMediaItem(position, position + 1)
        val temp = catalogMetadata[position]
        catalogMetadata[position] = catalogMetadata[position + 1]
        catalogMetadata[position + 1] = temp
        _currentSongIndex.value = currentIndex()
    }

    fun removeMediaItem(position: Int) {
        player.removeMediaItem(position)
        catalogMetadata.removeAt(position)
        _currentSongIndex.value = currentIndex()
    }


    fun playMediaItemInMediaSource(index: Int) {
        player.seekTo(index, 0)
        player.prepare()
        player.playWhenReady = true
    }

    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    fun getProgress(): Long {
        return player.currentPosition
    }

    suspend fun playNext(track: Track) {
        _stateFlow.value = StateSource.STATE_INITIALIZING
        var thumbUrl = track.thumbnails?.last()?.url
            ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
        if (thumbUrl.contains("w120")) {
            thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
        }
        val artistName: String = track.artists.toListName().connectArtists()
        if (!catalogMetadata.contains(track) && (currentIndex() + 1 in 0..catalogMetadata.size)) {
            if (track.artists.isNullOrEmpty()) {
                mainRepository.getSongInfo(track.videoId).cancellable().first().let { songInfo ->
                    if (songInfo != null) {
                        catalogMetadata.add(
                            currentIndex() + 1,
                            track.copy(
                                artists = listOf(
                                    Artist(
                                        songInfo.authorId,
                                        songInfo.author ?: ""
                                    )
                                )
                            )
                        )
                        addMediaItemNotSet(
                            MediaItem.Builder().setUri(track.videoId)
                                .setMediaId(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(songInfo.author)
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .build()
                                )
                                .build(), currentIndex() + 1
                        )
                    } else {
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(track.videoId)
                            .setUri(track.videoId)
                            .setCustomCacheKey(track.videoId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setArtworkUri(thumbUrl.toUri())
                                    .setAlbumTitle(track.album?.name)
                                    .setTitle(track.title)
                                    .setArtist("Various Artists")
                                    .build()
                            )
                            .build()
                        addMediaItemNotSet(mediaItem, currentIndex() + 1)
                        catalogMetadata.add(
                            currentIndex() + 1,
                            track.copy(
                                artists = listOf(Artist("", "Various Artists"))
                            )
                        )
                    }
                }
            } else {
                addMediaItemNotSet(
                    MediaItem.Builder().setUri(track.videoId)
                        .setMediaId(track.videoId)
                        .setCustomCacheKey(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(artistName)
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .build()
                        )
                        .build(),
                    currentIndex() + 1
                )
                catalogMetadata.add(currentIndex() + 1, track)
            }
            Log.d(
                "MusicSource",
                "updateCatalog: ${track.title}, ${catalogMetadata.size}"
            )
            added.value = true
            Log.d("MusicSource", "updateCatalog: ${track.title}")
        }
        _stateFlow.value = StateSource.STATE_INITIALIZED
    }


    suspend fun loadMoreCatalog(listTrack: ArrayList<Track>) {
        _stateFlow.value = StateSource.STATE_INITIALIZING
        for (i in 0 until listTrack.size) {
            val track = listTrack[i]
            var thumbUrl = track.thumbnails?.last()?.url
                ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            val artistName: String = track.artists.toListName().connectArtists()
            if (!catalogMetadata.contains(track)) {
                if (track.artists.isNullOrEmpty()) {
                    mainRepository.getSongInfo(track.videoId).cancellable().first()
                        .let { songInfo ->
                            if (songInfo != null) {
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(
                                            Artist(
                                                songInfo.authorId,
                                                songInfo.author ?: ""
                                            )
                                        )
                                    )
                                )
                                addMediaItemNotSet(
                                    MediaItem.Builder().setUri(track.videoId)
                                        .setMediaId(track.videoId)
                                        .setCustomCacheKey(track.videoId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(track.title)
                                                .setArtist(songInfo.author)
                                                .setArtworkUri(thumbUrl.toUri())
                                                .setAlbumTitle(track.album?.name)
                                                .build()
                                        )
                                        .build()
                                )
                            } else {
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(track.videoId)
                                    .setUri(track.videoId)
                                    .setCustomCacheKey(track.videoId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setArtworkUri(thumbUrl.toUri())
                                            .setAlbumTitle(track.album?.name)
                                            .setTitle(track.title)
                                            .setArtist("Various Artists")
                                            .build()
                                    )
                                    .build()
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(Artist("", "Various Artists"))
                                    )
                                )
                            }
                        }
                } else {
                    addMediaItemNotSet(
                        MediaItem.Builder().setUri(track.videoId)
                            .setMediaId(track.videoId)
                            .setCustomCacheKey(track.videoId)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(artistName)
                                    .setArtworkUri(thumbUrl.toUri())
                                    .setAlbumTitle(track.album?.name)
                                    .build()
                            )
                            .build()
                    )
                    catalogMetadata.add(track)
                }
                Log.d(
                    "MusicSource",
                    "updateCatalog: ${track.title}, ${catalogMetadata.size}"
                )
                added.value = true
                Log.d("MusicSource", "updateCatalog: ${track.title}")
            }
        }
        _stateFlow.value = StateSource.STATE_INITIALIZED
    }

    private fun updateNotification() {
        updateNotificationJob?.cancel()
        updateNotificationJob = coroutineScope.launch {
            val liked =
                mainRepository.getSongById(player.currentMediaItem?.mediaId ?: "").first()?.liked
            if (liked != null) {
                _liked.value = liked
            }
            mediaSession.setCustomLayout(
                listOf(
                    CommandButton.Builder()
                        .setDisplayName(
                            if (liked == true) context.getString(R.string.liked) else context.getString(
                                R.string.like
                            )
                        )
                        .setIconResId(if (liked == true) R.drawable.baseline_favorite_24 else R.drawable.baseline_favorite_border_24)
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.LIKE, Bundle()))
                        .build(),
                    CommandButton.Builder()
                        .setDisplayName(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_one_description)
                                Player.REPEAT_MODE_ALL -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_all_description)
                                else -> context.getString(androidx.media3.ui.R.string.exo_controls_repeat_off_description)
                            }
                        )
                        .setSessionCommand(SessionCommand(MEDIA_CUSTOM_COMMAND.REPEAT, Bundle()))
                        .setIconResId(
                            when (player.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.baseline_repeat_one_24
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.baseline_repeat_24_enable
                            }
                        )
                        .build()
                )
            )
        }
    }

    fun mayBeSavePlaybackState() {
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == DataStoreManager.TRUE) {
            runBlocking {
                dataStoreManager.recoverShuffleAndRepeatKey(
                    player.shuffleModeEnabled,
                    player.repeatMode
                )
            }
        }
    }

    fun release() {
        player.stop()
        player.playWhenReady = false
        player.removeListener(this)
        sendCloseEqualizerIntent()
        if (job?.isActive == true) {
            job?.cancel()
            job = null
        }
        if (sleepTimerJob?.isActive == true) {
            sleepTimerJob?.cancel()
            sleepTimerJob = null
        }
        if (volumeNormalizationJob?.isActive == true) {
            volumeNormalizationJob?.cancel()
            volumeNormalizationJob = null
        }
        if (toggleLikeJob?.isActive == true) {
            toggleLikeJob?.cancel()
            toggleLikeJob = null
        }
        if (updateNotificationJob?.isActive == true) {
            updateNotificationJob?.cancel()
            updateNotificationJob = null
        }
        if (loadJob?.isActive == true) {
            loadJob?.cancel()
            loadJob = null
        }
    }

    private fun sendCloseEqualizerIntent() {
        context.sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
            }
        )
    }

    fun skipSegment(position: Long) {
        if (position in 0..player.duration) {
            player.seekTo(position)
        } else if (position > player.duration) {
            player.seekToNext()
        }
    }

    fun mayBeSaveRecentSong() {
        runBlocking {
            if (dataStoreManager.saveRecentSongAndQueue.first() == DataStoreManager.TRUE) {
                dataStoreManager.saveRecentSong(
                    player.currentMediaItem?.mediaId ?: "",
                    player.contentPosition
                )
                val temp: ArrayList<Track> = ArrayList()
                temp.clear()
                Queue.getNowPlaying()?.let { nowPlaying ->
                    if (nowPlaying.videoId != player.currentMediaItem?.mediaId) {
                        temp += nowPlaying
                    }
                }
                temp += Queue.getQueue()
                temp.find { it.videoId == player.currentMediaItem?.mediaId }?.let { track ->
                    temp.remove(track)
                }
                mainRepository.recoverQueue(temp)
                dataStoreManager.putString(
                    DataStoreManager.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE,
                    DataStoreManager.FALSE
                )
            }
        }
    }

    fun clearMediaItems() {
        player.clearMediaItems()
    }

    fun loadPlaylistOrAlbum(index: Int? = null) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            load(index = index)
        }
    }

    fun getPlayerDuration(): Long {
        return player.duration
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = false)
    }

    private suspend fun startProgressUpdate() = job.run {
        while (true) {
            delay(100)
            _simpleMediaState.value = SimpleMediaState.Progress(player.currentPosition)
        }
    }

    suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> {
                if (player.isPlaying) {
                    player.pause()
                    stopProgressUpdate()
                } else {
                    player.play()
                    _simpleMediaState.value = SimpleMediaState.Playing(isPlaying = true)
                    startProgressUpdate()
                }
            }

            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.Stop -> {
                stopProgressUpdate()
                player.stop()
            }

            is PlayerEvent.UpdateProgress -> player.seekTo((player.duration * playerEvent.newProgress / 100).toLong())
            PlayerEvent.Shuffle -> {
                if (player.shuffleModeEnabled) {
                    player.shuffleModeEnabled = false
                    _shuffle.value = false
                } else {
                    player.shuffleModeEnabled = true
                    _shuffle.value = true
                }
            }

            PlayerEvent.Repeat -> {
                when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                        _repeat.value = RepeatState.One
                    }

                    ExoPlayer.REPEAT_MODE_ONE -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        _repeat.value = RepeatState.All
                    }

                    ExoPlayer.REPEAT_MODE_ALL -> {
                        player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                        _repeat.value = RepeatState.None
                    }

                    else -> {
                        when (_repeat.value) {
                            RepeatState.None -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                                _repeat.value = RepeatState.One
                            }

                            RepeatState.One -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_ALL
                                _repeat.value = RepeatState.All
                            }

                            RepeatState.All -> {
                                player.repeatMode = ExoPlayer.REPEAT_MODE_OFF
                                _repeat.value = RepeatState.None
                            }
                        }
                    }
                }
            }
        }
    }


    fun reset() {
        _currentSongIndex.value = 0
        catalogMetadata.clear()
        _stateFlow.value = StateSource.STATE_CREATED
    }

    fun seekTo(position: String) {
        player.seekTo(position.toLong())
        Log.d("Check seek", "seekTo: ${player.currentPosition}")
    }

    @UnstableApi
    suspend fun load(downloaded: Int = 0, index: Int? = null) {
        updateCatalog(downloaded).let {
            _stateFlow.value = StateSource.STATE_INITIALIZED
            if (index != null) {
                when (index) {
                    -1 -> {

                    }

                    else -> {
                        Log.w("Check index", "load: $index")
                        addFirstMediaItemToIndex(getMediaItemWithIndex(0), index)
                        Queue.getNowPlaying().let { song ->
                            if (song != null) {
                                catalogMetadata.removeAt(0)
                                catalogMetadata.add(index, song)
                            }
                        }
                    }
                }
            }
        }
    }

    fun addMediaItemNotSet(mediaItem: MediaItem) {
        player.addMediaItem(mediaItem)
        if (player.mediaItemCount == 1) {
            player.prepare()
            player.playWhenReady = true
        }
        updateNextPreviousTrackAvailability()
    }

    fun addMediaItemNotSet(mediaItem: MediaItem, index: Int) {
        player.addMediaItem(index, mediaItem)
        if (player.mediaItemCount == 1) {
            player.prepare()
            player.playWhenReady = true
        }
        updateNextPreviousTrackAvailability()
    }

    private fun updateNextPreviousTrackAvailability() {
        _nextTrackAvailable.value = player.hasNextMediaItem()
        _previousTrackAvailable.value = player.hasPreviousMediaItem()
    }

    @UnstableApi
    suspend fun updateCatalog(downloaded: Int = 0): Boolean {
        _stateFlow.value = StateSource.STATE_INITIALIZING
        val tempQueue: ArrayList<Track> = arrayListOf()
        tempQueue.addAll(Queue.getQueue())
        for (i in 0 until tempQueue.size) {
            val track = tempQueue[i]
            var thumbUrl = track.thumbnails?.last()?.url
                ?: "http://i.ytimg.com/vi/${track.videoId}/maxresdefault.jpg"
            if (thumbUrl.contains("w120")) {
                thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
            }
            if (downloaded == 1) {
                if (track.artists.isNullOrEmpty()) {
                    mainRepository.getSongInfo(track.videoId).cancellable().first()
                        .let { songInfo ->
                            if (songInfo != null) {
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(track.videoId)
                                    .setUri(track.videoId)
                                    .setCustomCacheKey(track.videoId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setArtworkUri(thumbUrl.toUri())
                                            .setAlbumTitle(track.album?.name)
                                            .setTitle(track.title)
                                            .setArtist(songInfo.author)
                                            .build()
                                    )
                                    .build()
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(
                                            Artist(
                                                songInfo.authorId,
                                                songInfo.author ?: ""
                                            )
                                        )
                                    )
                                )
                            } else {
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(track.videoId)
                                    .setUri(track.videoId)
                                    .setCustomCacheKey(track.videoId)
                                    .setMediaMetadata(
                                        MediaMetadata.Builder()
                                            .setArtworkUri(thumbUrl.toUri())
                                            .setAlbumTitle(track.album?.name)
                                            .setTitle(track.title)
                                            .setArtist("Various Artists")
                                            .build()
                                    )
                                    .build()
                                addMediaItemNotSet(mediaItem)
                                catalogMetadata.add(
                                    track.copy(
                                        artists = listOf(Artist("", "Various Artists"))
                                    )
                                )
                            }
                        }
                } else {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(track.videoId)
                        .setUri(track.videoId)
                        .setCustomCacheKey(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .setTitle(track.title)
                                .setArtist(track.artists.toListName().connectArtists())
                                .build()
                        )
                        .build()
                    addMediaItemNotSet(mediaItem)
                    catalogMetadata.add(track)
                }
                Log.d("MusicSource", "updateCatalog: ${track.title}, ${catalogMetadata.size}")
                added.value = true
            } else {
                val artistName: String = track.artists.toListName().connectArtists()
                if (!catalogMetadata.contains(track)) {
                    if (track.artists.isNullOrEmpty()) {
                        mainRepository.getSongInfo(track.videoId).cancellable().first()
                            .let { songInfo ->
                                if (songInfo != null) {
                                    catalogMetadata.add(
                                        track.copy(
                                            artists = listOf(
                                                Artist(
                                                    songInfo.authorId,
                                                    songInfo.author ?: ""
                                                )
                                            )
                                        )
                                    )
                                    addMediaItemNotSet(
                                        MediaItem.Builder().setUri(track.videoId)
                                            .setMediaId(track.videoId)
                                            .setCustomCacheKey(track.videoId)
                                            .setMediaMetadata(
                                                MediaMetadata.Builder()
                                                    .setTitle(track.title)
                                                    .setArtist(songInfo.author)
                                                    .setArtworkUri(thumbUrl.toUri())
                                                    .setAlbumTitle(track.album?.name)
                                                    .build()
                                            )
                                            .build()
                                    )
                                } else {
                                    val mediaItem = MediaItem.Builder()
                                        .setMediaId(track.videoId)
                                        .setUri(track.videoId)
                                        .setCustomCacheKey(track.videoId)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setArtworkUri(thumbUrl.toUri())
                                                .setAlbumTitle(track.album?.name)
                                                .setTitle(track.title)
                                                .setArtist("Various Artists")
                                                .build()
                                        )
                                        .build()
                                    addMediaItemNotSet(mediaItem)
                                    catalogMetadata.add(
                                        track.copy(
                                            artists = listOf(Artist("", "Various Artists"))
                                        )
                                    )
                                }
                            }
                    } else {
                        addMediaItemNotSet(
                            MediaItem.Builder().setUri(track.videoId)
                                .setMediaId(track.videoId)
                                .setCustomCacheKey(track.videoId)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(artistName)
                                        .setArtworkUri(thumbUrl.toUri())
                                        .setAlbumTitle(track.album?.name)
                                        .build()
                                )
                                .build()
                        )
                        catalogMetadata.add(track)
                    }
                    Log.d(
                        "MusicSource",
                        "updateCatalog: ${track.title}, ${catalogMetadata.size}"
                    )
                    added.value = true
                    Log.d("MusicSource", "updateCatalog: ${track.title}")
                }
            }
        }
        return true
    }

    fun addQueueToPlayer() {
        Log.d("Check Queue in handler", Queue.getQueue().toString())
        loadJob?.cancel()
        loadJob = coroutineScope.launch {
            load()
        }
    }

    fun getMediaItemWithIndex(index: Int): MediaItem {
        return player.getMediaItemAt(index)
    }

    @UnstableApi
    fun addFirstMediaItemToIndex(mediaItem: MediaItem?, index: Int) {
        if (mediaItem != null) {
            Log.d("MusicSource", "addFirstMediaItem: ${mediaItem.mediaId}")
            moveMediaItem(0, index)
        }
    }

    fun moveMediaItem(fromIndex: Int, newIndex: Int) {
        player.moveMediaItem(fromIndex, newIndex)
        _currentSongIndex.value = currentIndex()
    }

    fun currentIndex(): Int {
        return player.currentMediaItemIndex
    }

    fun addMediaItem(mediaItem: MediaItem, playWhenReady: Boolean = true) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun addFirstMetadata(it: Track) {
        added.value = true
        catalogMetadata.add(0, it)
        Log.d("MusicSource", "addFirstMetadata: ${it.title}, ${catalogMetadata.size}")
    }

    fun sleepStart(minutes: Int) {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        sleepTimerJob = coroutineScope.launch(Dispatchers.Main) {
            _sleepMinutes.value = minutes
            var count = minutes
            while (count > 0) {
                delay(60 * 1000L)
                count--
                _sleepMinutes.value = count
            }
            player.pause()
            _sleepMinutes.value = 0
            _sleepDone.value = true
        }
    }

    fun sleepStop() {
        _sleepDone.value = false
        sleepTimerJob?.cancel()
        _sleepMinutes.value = 0
    }
}

enum class StateSource {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object Backward : PlayerEvent()
    data object Forward : PlayerEvent()
    data object Stop : PlayerEvent()
    data object Next : PlayerEvent()
    data object Previous : PlayerEvent()
    data object Shuffle : PlayerEvent()
    data object Repeat : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class SimpleMediaState {
    data object Initial : SimpleMediaState()
    data object Ended : SimpleMediaState()
    data class Ready(val duration: Long) : SimpleMediaState()
    data class Loading(val bufferedPercentage: Int, val duration: Long) : SimpleMediaState()
    data class Progress(val progress: Long) : SimpleMediaState()
    data class Buffering(val position: Long) : SimpleMediaState()
    data class Playing(val isPlaying: Boolean) : SimpleMediaState()
}

sealed class RepeatState {
    data object None : RepeatState()
    data object All : RepeatState()
    data object One : RepeatState()
}

