package com.example.mymusic.base.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import com.example.mymusic.R
import com.example.mymusic.base.Youtube
import com.example.mymusic.base.common.Config.ALBUM_CLICK
import com.example.mymusic.base.common.Config.PLAYLIST_CLICK
import com.example.mymusic.base.common.Config.RECOVER_TRACK_QUEUE
import com.example.mymusic.base.common.Config.SELECTED_LANGUAGE
import com.example.mymusic.base.common.Config.SHARE
import com.example.mymusic.base.common.Config.SONG_CLICK
import com.example.mymusic.base.common.Config.VIDEO_CLICK
import com.example.mymusic.base.common.DownloadState
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import com.example.mymusic.base.data.dataStore.DataStoreManager.Settings.TRUE
import com.example.mymusic.base.data.db.entities.AlbumEntity
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.PlaylistEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.metadata.Line
import com.example.mymusic.base.data.models.metadata.Lyrics
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.di.DownloadCache
import com.example.mymusic.base.models.myMusic.GithubResponse
import com.example.mymusic.base.service.PlayerEvent
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import com.example.mymusic.base.utils.Resource
import com.example.mymusic.base.utils.extension.connectArtists
import com.example.mymusic.base.utils.extension.toListName
import com.example.mymusic.base.utils.extension.toLyrics
import com.example.mymusic.base.utils.extension.toLyricsEntity
import com.example.mymusic.base.utils.extension.toSongEntity
import com.example.mymusic.base.utils.extension.toTrack
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.internal.concurrent.formatDuration
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
@UnstableApi
class SharedViewModel @Inject constructor(
    @DownloadCache private val downloadedCache: SimpleCache,
    private val application: Application
) : AndroidViewModel(application) {

    private var _recreateActivity: MutableLiveData<Boolean> = MutableLiveData()
    val recreateActivity: LiveData<Boolean> = _recreateActivity

    private var _homeRefresh: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val homeRefresh: StateFlow<Boolean> = _homeRefresh.asStateFlow()

    var recentPosition: String = 0L.toString()
    val recreatedActivity: LiveData<Boolean> = _recreateActivity

    private var regionCode: String? = null
    private var language: String? = null

    private var _progressString: MutableStateFlow<String> = MutableStateFlow("00:00")
    val progressString: SharedFlow<String> = _progressString.asSharedFlow()

    private val _duration = MutableStateFlow<Long>(0L)
    val duration: SharedFlow<Long> = _duration.asSharedFlow()

    var simpleMediaServiceHandler: SimpleMediaServiceHandler? = null

    private val _progress = MutableStateFlow<Float>(0F)
    val progress: SharedFlow<Float> = _progress.asSharedFlow()

    private var _skipSegments: MutableStateFlow<List<SkipSegments>?> = MutableStateFlow(null)
    val skipSegments: StateFlow<List<SkipSegments>?> = _skipSegments

    var isPlaying = MutableStateFlow<Boolean>(false)

    private var _githubResponse = MutableLiveData<GithubResponse?>()
    val githubResponse: LiveData<GithubResponse?> = _githubResponse

    private val _liked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked = _liked.asSharedFlow()

    var videoId = MutableLiveData<String>()

    private var _saveLastPlayedSong: MutableLiveData<Boolean> = MutableLiveData()
    val saveLastPlayedSong: LiveData<Boolean> = _saveLastPlayedSong
    var from = MutableLiveData<String>()
    var from_backup: String? = null

    private var _related = MutableStateFlow<Resource<ArrayList<Track>>?>(null)
    val related: StateFlow<Resource<ArrayList<Track>>?> = _related

    fun activityRecreateDone() {
        _recreateActivity.value = false
    }

    fun homeRefreshDone() {
        _homeRefresh.value = false
    }

    protected val context
        get() = getApplication<Application>()

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            simpleMediaServiceHandler?.loadMoreCatalog(arrayListOf(track))
            Toast.makeText(context, context.getString(R.string.added_to_queue), Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun playNext(song: Track) {
        viewModelScope.launch {
            simpleMediaServiceHandler?.playNext(song)
            Toast.makeText(context, context.getString(R.string.play_next), Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun refreshSongDB() {
        viewModelScope.launch {
            mainRepository.getSongById(videoId.value!!).collect { songEntity ->
                _songDB.value = songEntity
                if (songEntity != null) {
                    _liked.value = songEntity.liked
                }
            }
        }
    }

    val isServiceRunning = MutableLiveData<Boolean>(false)

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository
    private var quality: String? = null
    private var _songDB: MutableLiveData<SongEntity?> = MutableLiveData()
    val songDB: LiveData<SongEntity?> = _songDB
    private var _nowPlayingMediaItem = MutableLiveData<MediaItem?>()
    val nowPlayingMediaItem: LiveData<MediaItem?> = _nowPlayingMediaItem
    var _lyrics = MutableStateFlow<Resource<Lyrics>?>(null)

    val intent: MutableStateFlow<Intent?> = MutableStateFlow(null)

    //    val lyrics: LiveData<Resource<Lyrics>> = _lyrics
    private var lyricsFormat: MutableLiveData<ArrayList<Line>> = MutableLiveData()
    var lyricsFull = MutableLiveData<String>()
    private var _translateLyrics: MutableStateFlow<Lyrics?> = MutableStateFlow(null)
    val translateLyrics: StateFlow<Lyrics?> = _translateLyrics
    private var _lyricsProvider: MutableStateFlow<LyricsProvider> =
        MutableStateFlow(LyricsProvider.MUSIXMATCH)
    val lyricsProvider: StateFlow<LyricsProvider> = _lyricsProvider
    private var _savedQueue: MutableLiveData<List<Track>> = MutableLiveData()
    val savedQueue: LiveData<List<Track>> = _savedQueue
    fun init() {

    }

    fun getCurrentMediaItem(): MediaItem? {
        _nowPlayingMediaItem.value = simpleMediaServiceHandler?.getCurrentMediaItem()
        return simpleMediaServiceHandler?.getCurrentMediaItem()
    }

    fun changeAllDownloadingToError() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED
                    )
                }
            }
        }
    }

    fun updateLikeInNotification(liked: Boolean) {
        simpleMediaServiceHandler?.like(liked)
    }

    fun updateLikeStatus(videoId: String, likeStatus: Boolean) {
        println("Update Like Status $videoId $likeStatus")
        viewModelScope.launch {
            if (simpleMediaServiceHandler?.nowPlaying?.first()?.mediaId == videoId) {
                _liked.value = likeStatus
                if (likeStatus) {
                    mainRepository.updateLikeStatus(videoId, 1)
                } else {
                    mainRepository.updateLikeStatus(videoId, 0)
                }
            }
        }
    }

    fun stopPlayer() {
        onUIEvent(UIEvent.Stop)
    }

    fun skipSegment(position: Long) {
        simpleMediaServiceHandler?.skipSegment(position)
    }

    fun sponsorBlockEnabled() = runBlocking { dataStoreManager.sponsorBlockEnabled.first() }
    fun sponsorBlockCategories() = runBlocking { dataStoreManager.getSponsorBlockCategories() }

    @UnstableApi
    fun onUIEvent(uiEvent: UIEvent) = viewModelScope.launch {
        when (uiEvent) {
            UIEvent.Backward -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Backward)
            UIEvent.Forward -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Forward)
            UIEvent.PlayPause -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.PlayPause)
            UIEvent.Next -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Next)
            UIEvent.Previous -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Previous)
            UIEvent.Stop -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Stop)
            is UIEvent.UpdateProgress -> {
                _progress.value = uiEvent.newProgress
                simpleMediaServiceHandler?.onPlayerEvent(
                    PlayerEvent.UpdateProgress(
                        uiEvent.newProgress
                    )
                )
            }

            UIEvent.Repeat -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Repeat)
            UIEvent.Shuffle -> simpleMediaServiceHandler?.onPlayerEvent(PlayerEvent.Shuffle)
        }
    }

    fun checkAuth() {
        viewModelScope.launch {
            dataStoreManager.cookie.first().let { cookie ->
                if (cookie != "") {
                    Youtube.cookie = cookie
                }

            }
            dataStoreManager.musixmatchCookie.first().let { cookie ->
                if (cookie != "") {
                    Youtube.musixMatchCookie = cookie
                }

            }
        }
    }

    fun checkAllDownloadingSongs() {
        viewModelScope.launch {
            mainRepository.getDownloadingSongs().collect { songs ->
                songs?.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED
                    )
                }
            }
            mainRepository.getPreparingSongs().collect { songs ->
                songs.forEach { song ->
                    mainRepository.updateDownloadState(
                        song.videoId,
                        DownloadState.STATE_NOT_DOWNLOADED
                    )
                }
            }
        }
    }

    fun checkIsRestoring() {
        viewModelScope.launch {
            mainRepository.getDownloadedSongs().first().let { songs ->
                songs?.forEach { song ->
                    if (!downloadedCache.keys.contains(song.videoId)) {
                        mainRepository.updateDownloadState(
                            song.videoId,
                            DownloadState.STATE_NOT_DOWNLOADED
                        )
                    }
                }
            }
            mainRepository.getAllDownloadedPlaylist().first().let { list ->
                for (data in list) {
                    when (data) {
                        is AlbumEntity -> {
                            if (data.tracks.isNullOrEmpty() || (!downloadedCache.keys.containsAll(
                                    data.tracks
                                ))
                            ) {
                                mainRepository.updateAlbumDownloadState(
                                    data.browseId,
                                    DownloadState.STATE_NOT_DOWNLOADED
                                )
                            }
                        }

                        is PlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() || (!downloadedCache.keys.containsAll(
                                    data.tracks
                                ))
                            ) {
                                mainRepository.updatePlaylistDownloadState(
                                    data.id,
                                    DownloadState.STATE_NOT_DOWNLOADED
                                )
                            }
                        }

                        is LocalPlaylistEntity -> {
                            if (data.tracks.isNullOrEmpty() || (!downloadedCache.keys.containsAll(
                                    data.tracks
                                ))
                            ) {
                                mainRepository.updateLocalPlaylistDownloadState(
                                    DownloadState.STATE_NOT_DOWNLOADED,
                                    data.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun getLocation() {
        regionCode = runBlocking { dataStoreManager.location.first() }
        quality = runBlocking { dataStoreManager.quality.first() }
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() }
        from_backup = runBlocking { dataStoreManager.playlistFromSaved.first() }
        recentPosition = runBlocking { (dataStoreManager.recentPosition.first()) }
    }

    fun resetLyrics() {
        _lyrics.value = (Resource.Error<Lyrics>("reset"))
        lyricsFormat.postValue(arrayListOf())
        lyricsFull.postValue("")
        _translateLyrics.value = null
    }

    fun parseLyrics(lyrics: Lyrics?) {
        if (lyrics != null) {
            if (!lyrics.error) {
                if (lyrics.syncType == "LINE_SYNCED") {
                    val firstLine = Line("0", "0", listOf(), "")
                    val lines: ArrayList<Line> = ArrayList()
                    lines.addAll(lyrics.lines as ArrayList<Line>)
                    lines.add(0, firstLine)
                    lyricsFormat.postValue(lines)
                    var txt = ""
                    for (line in lines) {
                        txt += if (line == lines.last()) {
                            line.words
                        } else {
                            line.words + "\n"
                        }
                    }
                    lyricsFull.postValue(txt)
//                    Log.d("Check Lyrics", lyricsFormat.value.toString())
                } else if (lyrics.syncType == "UNSYNCED") {
                    val lines: ArrayList<Line> = ArrayList()
                    lines.addAll(lyrics.lines as ArrayList<Line>)
                    var txt = ""
                    for (line in lines) {
                        if (line == lines.last()) {
                            txt += line.words
                        } else {
                            txt += line.words + "\n"
                        }
                    }
                    lyricsFormat.postValue(arrayListOf(Line("0", "0", listOf(), txt)))
                    lyricsFull.postValue(txt)
                }
            } else {
                val lines = Line("0", "0", listOf(), "Lyrics not found")
                lyricsFormat.postValue(arrayListOf(lines))
//                Log.d("Check Lyrics", "Lyrics not found")
            }
        }
    }

    fun getSavedLyrics(track: Track, query: String) {
        viewModelScope.launch {
            resetLyrics()
            mainRepository.getSavedLyrics(track.videoId).collect { lyrics ->
                if (lyrics != null) {
                    _lyricsProvider.value = LyricsProvider.OFFLINE
                    _lyrics.value = Resource.Success(lyrics.toLyrics())
                    val lyricsData = lyrics.toLyrics()
                    parseLyrics(lyricsData)
                } else {
                    resetLyrics()
                    mainRepository.getLyricsData(query, track.durationSeconds).collect { response ->
                        _lyrics.value = response.second
                        when (_lyrics.value) {
                            is Resource.Success -> {
                                if (_lyrics.value?.data != null) {
                                    _lyricsProvider.value = LyricsProvider.MUSIXMATCH
                                    insertLyrics(_lyrics.value?.data!!.toLyricsEntity(track.videoId))
                                    parseLyrics(_lyrics.value?.data)
                                    if (dataStoreManager.enableTranslateLyric.first() == TRUE) {
                                        mainRepository.getTranslateLyrics(response.first)
                                            .collect { translate ->
                                                if (translate != null) {
                                                    _translateLyrics.value =
                                                        translate.toLyrics(_lyrics.value?.data!!)
                                                }
                                            }
                                    }
                                }
                            }

                            else -> {
                                Log.d("Check lyrics", "Loading")
                            }
                        }
                    }
                }
            }
        }
    }

    fun insertLyrics(lyrics: LyricsEntity) {
        viewModelScope.launch {
            mainRepository.insertLyrics(lyrics)
        }
    }

    fun removeSaveQueue() {
        viewModelScope.launch {
            mainRepository.removeQueue()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @UnstableApi
    fun loadMediaItemFromTrack(track: Track, type: String, index: Int? = null) {
        quality = runBlocking { dataStoreManager.quality.first() }
        viewModelScope.launch {
            simpleMediaServiceHandler?.clearMediaItems()
            var uri = ""
            mainRepository.insertSong(track.toSongEntity()).first().let {
                println("insertSong: $it")
                mainRepository.getSongById(track.videoId)
                    .collect { songEntity ->
                        _songDB.value = songEntity
                        if (songEntity != null) {
                            _liked.value = songEntity.liked
                        }
                    }
            }
            mainRepository.updateSongInLibrary(LocalDateTime.now(), track.videoId)
            mainRepository.updateListenCount(track.videoId)
            track.durationSeconds?.let { mainRepository.updateDurationSeconds(it, track.videoId) }
            if (songDB.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                var thumbUrl = track.thumbnails?.last()?.url!!
                if (thumbUrl.contains("w120")) {
                    thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                }
                simpleMediaServiceHandler?.addMediaItem(
                    MediaItem.Builder()
                        .setUri(track.videoId)
                        .setMediaId(track.videoId)
                        .setCustomCacheKey(track.videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(track.title)
                                .setArtist(track.artists.toListName().connectArtists())
                                .setArtworkUri(thumbUrl.toUri())
                                .setAlbumTitle(track.album?.name)
                                .build()
                        )
                        .build(),
                    type != RECOVER_TRACK_QUEUE
                )
                _nowPlayingMediaItem.value = getCurrentMediaItem()
                simpleMediaServiceHandler?.addFirstMetadata(track)
                getSavedLyrics(track, "${track.title} ${track.artists?.firstOrNull()?.name}")
            } else {
                val artistName: String = track.artists.toListName().connectArtists()
                var thumbUrl = track.thumbnails?.last()?.url!!
                if (thumbUrl.contains("w120")) {
                    thumbUrl = Regex("([wh])120").replace(thumbUrl, "$1544")
                }
                Log.d("Check URI", uri)
                simpleMediaServiceHandler?.addMediaItem(
                    MediaItem.Builder()
                        .setUri(track.videoId)
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
                    type != RECOVER_TRACK_QUEUE
                )
                _nowPlayingMediaItem.value = getCurrentMediaItem()
                Log.d(
                    "Check MediaItem Thumbnail",
                    getCurrentMediaItem()?.mediaMetadata?.artworkUri.toString()
                )
                simpleMediaServiceHandler?.addFirstMetadata(track)
            }
            when (type) {
                SONG_CLICK -> {
                    getRelated(track.videoId)
                }

                VIDEO_CLICK -> {
                    getRelated(track.videoId)
                }

                SHARE -> {
                    getRelated(track.videoId)
                }

                PLAYLIST_CLICK -> {
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum()
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                ALBUM_CLICK -> {
                    Queue.setContinuation(null)
                    if (index == null) {
//                                        fetchSourceFromQueue(downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum()
                    } else {
//                                        fetchSourceFromQueue(index!!, downloaded = downloaded ?: 0)
                        loadPlaylistOrAlbum(index = index)
                    }
                }

                RECOVER_TRACK_QUEUE -> {
                    if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
                        recentPosition = runBlocking { dataStoreManager.recentPosition.first() }
                        restoreLastPLayedTrackDone()
                        from.postValue(from_backup)
                        simpleMediaServiceHandler?.seekTo(recentPosition)
                        Log.d("Check recentPosition", recentPosition)
                        if (songDB.value?.duration != null) {
                            if (songDB.value?.duration != "" && songDB.value?.duration?.contains(":") == true) {
                                songDB.value?.duration?.split(":")?.let { split ->
                                    _duration.emit(((split[0].toInt() * 60) + split[1].toInt()) * 1000.toLong())
                                    Log.d("Check Duration", _duration.value.toString())
                                    calculateProgressValues(recentPosition.toLong())
                                }
                            }
                        } else {
                            simpleMediaServiceHandler?.getPlayerDuration()?.let {
                                _duration.emit(it)
                                calculateProgressValues(recentPosition.toLong())
                            }
                        }
                        getSaveQueue()
                    }
                }
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            mainRepository.checkForUpdate().collect { response ->
                dataStoreManager.putString(
                    "CheckForUpdateAt",
                    System.currentTimeMillis().toString()
                )
                _githubResponse.postValue(response)
            }
        }
    }

    private fun getSaveQueue() {
        viewModelScope.launch {
            mainRepository.getSavedQueue().collect { queue ->
                Log.d("Check Queue", queue.toString())
                if (!queue.isNullOrEmpty()) {
                    _savedQueue.value = queue.first().listTrack
                }
            }
        }
    }

    private fun calculateProgressValues(currentProgress: Long) {
        _progress.value =
            if (currentProgress > 0) (currentProgress.toFloat() / _duration.value) else 0f
        _progressString.value = formatDuration(currentProgress)
    }

    fun restoreLastPLayedTrackDone() {
        putString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE, TRUE)
    }

    fun putString(key: String, value: String) {
        runBlocking { dataStoreManager.putString(key, value) }
    }

    private fun loadPlaylistOrAlbum(index: Int? = null) {
        simpleMediaServiceHandler?.loadPlaylistOrAlbum(index)
    }

    fun addQueueToPlayer() {
        Log.d("Check Queue in viewmodel", Queue.getQueue().toString())
        simpleMediaServiceHandler?.addQueueToPlayer()
    }

    fun resetRelated() {
        _related.value = null
    }

    fun getRelated(videoId: String) {
        Queue.clear()
        viewModelScope.launch {
            mainRepository.getRelatedData(videoId).collect { response ->
                _related.value = response
            }
        }
    }

    fun getSavedSongAndQueue() {
        viewModelScope.launch {
            dataStoreManager.recentMediaId.first().let { mediaId ->
                mainRepository.getSongById(mediaId).collect { song ->
                    if (song != null) {
                        Queue.clear()
                        Queue.setNowPlaying(song.toTrack())
                        loadMediaItemFromTrack(song.toTrack(), RECOVER_TRACK_QUEUE)
                    }
                }
            }
        }
    }

    fun getSaveLastPlayedSong() {
        viewModelScope.launch {
            dataStoreManager.saveRecentSongAndQueue.first().let { saved ->
                _saveLastPlayedSong.postValue(saved == TRUE)
            }
        }
    }

    fun getString(key: String): String? {
        return runBlocking { dataStoreManager.getString(key).first() }
    }

    fun activityRecreate() {
        _recreateActivity.value = true
    }

    fun homeRefresh() {
        _homeRefresh.value = true
    }

}

sealed class UIEvent {
    data object PlayPause : UIEvent()
    data object Backward : UIEvent()
    data object Forward : UIEvent()
    data object Next : UIEvent()
    data object Previous : UIEvent()
    data object Stop : UIEvent()
    data object Shuffle : UIEvent()
    data object Repeat : UIEvent()
    data class UpdateProgress(val newProgress: Float) : UIEvent()
}

enum class LyricsProvider {
    MUSIXMATCH,
    YOUTUBE,
    SPOTIFY,
    OFFLINE
}