package com.example.mymusic.base.service

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.utils.extension.connectArtists
import com.example.mymusic.base.utils.extension.toListName
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    //Add MusicSource to this
    var catalogMetadata: ArrayList<Track> = (arrayListOf())
    private val _liked = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    private val _sleepDone = MutableStateFlow<Boolean>(false)
    val sleepDone = _sleepDone.asSharedFlow()
    private var loadJob: Job? = null

    fun getCurrentMediaItem(): MediaItem? {
        return player.currentMediaItem
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


    fun reset() {
        _currentSongIndex.value = 0
        catalogMetadata.clear()
        _stateFlow.value = StateSource.STATE_CREATED
    }

    fun seekTo(position: String)  {
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
}

enum class StateSource {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}