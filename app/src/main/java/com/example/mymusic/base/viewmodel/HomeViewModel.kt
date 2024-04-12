package com.example.mymusic.base.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.example.mymusic.R
import com.example.mymusic.base.common.Config.SELECTED_LANGUAGE
import com.example.mymusic.base.common.DownloadState
import com.example.mymusic.base.common.SUPPORTED_LANGUAGE
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.data.db.entities.PairSongLocalPlaylist
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.home.HomeDataCombine
import com.example.mymusic.base.data.models.home.HomeItem
import com.example.mymusic.base.data.models.home.chart.Chart
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.service.test.download.DownloadUtils
import com.example.mymusic.base.utils.Resource
import com.example.mymusic.base.utils.extension.toSongEntity
import com.example.mymusic.base.data.models.explore.mood.Mood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mainRepository: MainRepository,
    private val application: Application,
    private var dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {
    @Inject
    lateinit var downloadUtils: DownloadUtils

    private val _homeItemList: MutableLiveData<Resource<ArrayList<HomeItem>>> = MutableLiveData()
    val homeItemList: LiveData<Resource<ArrayList<HomeItem>>> = _homeItemList
    private val _exploreMoodItem: MutableLiveData<Resource<Mood>> = MutableLiveData()
    val exploreMoodItem: LiveData<Resource<Mood>> = _exploreMoodItem
    private val _accountInfo: MutableLiveData<Pair<String?, String?>?> = MutableLiveData()
    val accountInfo: LiveData<Pair<String?, String?>?> = _accountInfo

    val showSnackBarErrorState = MutableSharedFlow<String>()

    private val _chart: MutableLiveData<Resource<Chart>> = MutableLiveData()
    val chart: LiveData<Resource<Chart>> = _chart
    private val _newRelease: MutableLiveData<Resource<ArrayList<HomeItem>>> = MutableLiveData()
    val newRelease: LiveData<Resource<ArrayList<HomeItem>>> = _newRelease
    var regionCodeChart: MutableLiveData<String> = MutableLiveData()

    val loading = MutableLiveData<Boolean>()
    val loadingChart = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()
    private var regionCode: String = ""
    private var language: String = ""

    private val _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
    val songEntity: LiveData<SongEntity?> = _songEntity

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError("Exception handled: ${throwable.localizedMessage}")
    }

    init {
        viewModelScope.launch {
            language = dataStoreManager.getString(SELECTED_LANGUAGE).first()
                ?: SUPPORTED_LANGUAGE.codes.first()
            //  refresh when region change
            val job1 = launch {
                dataStoreManager.location.distinctUntilChanged().collect {
                    regionCode = it
                    getHomeItemList()
                }
            }
            //  refresh when language change
            val job2 = launch {
                dataStoreManager.language.distinctUntilChanged().collect {
                    language = it
                    getHomeItemList()
                }
            }
            val job3 = launch {
                dataStoreManager.cookie.distinctUntilChanged().collect {
                    getHomeItemList()
                    _accountInfo.postValue(
                        Pair(
                            dataStoreManager.getString("AccountName").first(),
                            dataStoreManager.getString("AccountThumbUrl").first()
                        )
                    )
                }
            }
            job1.join()
            job2.join()
            job3.join()
        }

    }

    fun getHomeItemList() {
        language = runBlocking { dataStoreManager.getString(SELECTED_LANGUAGE).first() ?: SUPPORTED_LANGUAGE.codes.first() }
        regionCode = runBlocking { dataStoreManager.location.first() }
        loading.value = true
        viewModelScope.launch {
            combine(
//                mainRepository.getHome(
//                    regionCode,
//                    SUPPORTED_LANGUAGE.serverCodes[SUPPORTED_LANGUAGE.codes.indexOf(language)]
//                ),
                mainRepository.getHomeData(),
                mainRepository.getMoodAndMomentsData(),
                mainRepository.getChartData("ZZ"),
                mainRepository.getNewRelease()
            ) { home, exploreMood, exploreChart, newRelease ->
                HomeDataCombine(home, exploreMood, exploreChart, newRelease)
            }.collect { result ->
                val home = result.home
                Log.d("home size", "${home.data?.size}")
                val exploreMoodItem = result.mood
                val chart = result.chart
                val newRelease = result.newRelease
                _homeItemList.value = home
                _exploreMoodItem.value = exploreMoodItem
                regionCodeChart.value = "ZZ"
                _chart.value = chart
                _newRelease.value = newRelease
                Log.d("HomeViewModel", "getHomeItemList: $result")
                loading.value = false
                dataStoreManager.cookie.first().let {
                    if (it != "") {
                        _accountInfo.postValue(
                            Pair(
                                dataStoreManager.getString("AccountName").first(),
                                dataStoreManager.getString("AccountThumbUrl").first()
                            )
                        )
                    }
                }
                when {
                    home is Resource.Error -> home.message
                    exploreMoodItem is Resource.Error -> exploreMoodItem.message
                    chart is Resource.Error -> chart.message
                    else -> null
                }?.let {
                    showSnackBarErrorState.emit(it)
                    Log.w("Error", "getHomeItemList: ${home.message}")
                    Log.w("Error", "getHomeItemList: ${exploreMoodItem.message}")
                    Log.w("Error", "getHomeItemList: ${chart.message}")
                }
            }
        }
    }

    fun exploreChart(region: String) {
        viewModelScope.launch {
            loadingChart.value = true
            mainRepository.getChartData(
                region).collect { values ->
                regionCodeChart.value = region
                _chart.value = values
                Log.d("HomeViewModel", "getHomeItemList: ${chart.value?.data}")
                loadingChart.value = false
            }
        }
    }

    private fun onError(message: String) {
        errorMessage.value = message
        loading.value = false
    }

    fun updateLikeStatus(videoId: String, b: Boolean) {
        viewModelScope.launch {
            if (b) {
                mainRepository.updateLikeStatus(videoId, 1)
            } else {
                mainRepository.updateLikeStatus(videoId, 0)
            }
        }
    }

    fun getSongEntity(track: Track) {
        viewModelScope.launch {
            mainRepository.insertSong(track.toSongEntity()).first().let {
                println("Insert song $it")
            }
            mainRepository.getSongById(track.videoId).collect { values ->
                _songEntity.value = values
            }
        }
    }

    private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
    val localPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist
    fun getAllLocalPlaylist() {
        viewModelScope.launch {
            mainRepository.getAllLocalPlaylists().collect { values ->
                _listLocalPlaylist.postValue(values)
            }
        }
    }

    fun updateDownloadState(videoId: String, state: Int) {
        viewModelScope.launch {
            mainRepository.getSongById(videoId).collect { songEntity ->
                _songEntity.value = songEntity
            }
            mainRepository.updateDownloadState(videoId, state)
        }
    }

    private var _downloadState: MutableStateFlow<Download?> = MutableStateFlow(null)
    var downloadState: StateFlow<Download?> = _downloadState.asStateFlow()

    @UnstableApi
    fun getDownloadStateFromService(videoId: String) {
        viewModelScope.launch {
            downloadState = downloadUtils.getDownload(videoId).stateIn(viewModelScope)
            downloadState.collect { down ->
                if (down != null) {
                    when (down.state) {
                        Download.STATE_COMPLETED -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADED) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        DownloadState.STATE_DOWNLOADED
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Downloaded")
                        }

                        Download.STATE_FAILED -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != DownloadState.STATE_NOT_DOWNLOADED) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        DownloadState.STATE_NOT_DOWNLOADED
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Failed")
                        }

                        Download.STATE_DOWNLOADING -> {
                            mainRepository.getSongById(videoId).collect { song ->
                                if (song?.downloadState != DownloadState.STATE_DOWNLOADING) {
                                    mainRepository.updateDownloadState(
                                        videoId,
                                        DownloadState.STATE_DOWNLOADING
                                    )
                                }
                            }
                            Log.d("Check Downloaded", "Downloading ${down.percentDownloaded}")
                        }
                    }
                }
            }
        }
    }

    fun updateLocalPlaylistTracks(list: List<String>, id: Long) {
        viewModelScope.launch {
            mainRepository.getSongsByListVideoId(list).collect { values ->
                var count = 0
                values.forEach { song ->
                    if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                        count++
                    }
                }
                mainRepository.updateLocalPlaylistTracks(list, id)
                Toast.makeText(
                    getApplication(),
                    application.getString(R.string.added_to_playlist),
                    Toast.LENGTH_SHORT
                ).show()
                if (count == values.size) {
                    mainRepository.updateLocalPlaylistDownloadState(
                        DownloadState.STATE_DOWNLOADED,
                        id
                    )
                } else {
                    mainRepository.updateLocalPlaylistDownloadState(
                        DownloadState.STATE_NOT_DOWNLOADED,
                        id
                    )
                }
            }
        }
    }

    fun addToYouTubePlaylist(localPlaylistId: Long, youtubePlaylistId: String, videoId: String) {
        viewModelScope.launch {
            mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                localPlaylistId,
                LocalPlaylistEntity.YouTubeSyncState.Syncing
            )
            mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                if (response == "STATUS_SUCCEEDED") {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.Synced
                    )
                    Toast.makeText(
                        getApplication(),
                        application.getString(R.string.added_to_youtube_playlist),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                        localPlaylistId,
                        LocalPlaylistEntity.YouTubeSyncState.NotSynced
                    )
                    Toast.makeText(
                        getApplication(),
                        application.getString(R.string.error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateInLibrary(videoId: String) {
        viewModelScope.launch {
            mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
        }
    }

    fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
        viewModelScope.launch {
            mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }
    }

}