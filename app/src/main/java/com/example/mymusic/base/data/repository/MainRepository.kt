package com.example.mymusic.base.data.repository

import android.content.Context
import android.util.Log
import com.example.mymusic.base.Youtube
import com.example.mymusic.base.common.VIDEO_QUALITY
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.db.LocalDataSource
import com.example.mymusic.base.data.db.entities.GoogleAccountEntity
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.PairSongLocalPlaylist
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SetVideoIdEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.browse.playlist.PlaylistBrowse
import com.example.mymusic.base.data.models.explore.mood.Mood
import com.example.mymusic.base.data.models.explore.mood.MoodsMoment
import com.example.mymusic.base.data.models.home.HomeItem
import com.example.mymusic.base.data.models.home.chart.Chart
import com.example.mymusic.base.data.models.metadata.Lyrics
import com.example.mymusic.base.data.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.example.mymusic.base.data.models.musixmatch.SearchMusixmatchResponse
import com.example.mymusic.base.data.parser.parseChart
import com.example.mymusic.base.data.parser.parseGenreObject
import com.example.mymusic.base.data.parser.parseMoodsMomentObject
import com.example.mymusic.base.data.parser.parseNewRelease
import com.example.mymusic.base.data.parser.parsePlaylistData
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.models.AccountInfo
import com.example.mymusic.base.models.MediaType
import com.example.mymusic.base.models.MusicShelfRenderer
import com.example.mymusic.base.models.SongItem
import com.example.mymusic.base.models.WatchEndpoint
import com.example.mymusic.base.models.myMusic.GithubResponse
import com.example.mymusic.base.models.response.spotify.CanvasResponse
import com.example.mymusic.base.models.youtube.YouTubeInitialPage
import com.example.mymusic.base.parser.parseMixedContent
import com.example.mymusic.base.utils.Resource
import com.example.mymusic.base.utils.extension.bestMatchingIndex
import com.example.mymusic.base.utils.extension.toListTrack
import com.example.mymusic.base.utils.extension.toLyrics
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.simpmusic.data.model.explore.mood.Genre
import com.maxrave.simpmusic.data.model.explore.mood.genre.GenreObject
import com.maxrave.simpmusic.data.model.explore.mood.moodmoments.MoodsMomentObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class MainRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context
) {
    suspend fun insertNewFormat(newFormat: NewFormatEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertNewFormat(newFormat) }

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow { emit(localDataSource.getQueue()) }.flowOn(Dispatchers.IO)

    suspend fun removeQueue() {
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    suspend fun getGoogleAccounts(): Flow<List<GoogleAccountEntity>?> =
        flow<List<GoogleAccountEntity>?> { emit(localDataSource.getGoogleAccounts()) }.flowOn(
            Dispatchers.IO
        )

    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) =
        withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
        }

    suspend fun addYouTubePlaylistItem(youtubePlaylistId: String, videoId: String) = flow {
        runCatching {
            Youtube.addPlaylistItem(youtubePlaylistId, videoId).onSuccess {
                if (it.playlistEditResults.isNotEmpty()) {
                    for (playlistEditResult in it.playlistEditResults) {
                        insertSetVideoId(
                            SetVideoIdEntity(
                                playlistEditResult.playlistEditVideoAddedResultData.videoId,
                                playlistEditResult.playlistEditVideoAddedResultData.setVideoId
                            )
                        )
                    }
                    emit(it.status)
                } else {
                    emit("FAILED")
                }
            }.onFailure {
                emit("FAILED")
            }
        }
    }

    suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity) =
        withContext(Dispatchers.IO) {
            localDataSource.insertGoogleAccount(googleAccountEntity)
        }

    suspend fun updateGoogleAccountUsed(email: String, isUsed: Boolean) =
        withContext(Dispatchers.IO) { localDataSource.updateGoogleAccountUsed(email, isUsed) }

    suspend fun deleteGoogleAccount(email: String) =
        withContext(Dispatchers.IO) { localDataSource.deleteGoogleAccount(email) }

    suspend fun getAccountInfo() = flow<AccountInfo?> {
        Youtube.accountInfo().onSuccess { accountInfo ->
            emit(accountInfo)
        }.onFailure {
            it.printStackTrace()
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getMoodData(params: String): Flow<Resource<MoodsMomentObject>> = flow {
        runCatching {
            Youtube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseMoodsMomentObject(result)
                    if (data != null) {
                        emit(Resource.Success<MoodsMomentObject>(data))
                    } else {
                        emit(Resource.Error<MoodsMomentObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<MoodsMomentObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getGenreData(params: String): Flow<Resource<GenreObject>> = flow {
        kotlin.runCatching {
            Youtube.customQuery(browseId = "FEmusic_moods_and_genres_category", params = params)
                .onSuccess { result ->
                    val data = parseGenreObject(result)
                    if (data != null) {
                        emit(Resource.Success<GenreObject>(data))
                    } else {
                        emit(Resource.Error<GenreObject>("Error"))
                    }
                }
                .onFailure { e ->
                    emit(Resource.Error<GenreObject>(e.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getRecentSong(limit: Int, offset: Int) =
        localDataSource.getRecentSongs(limit, offset)

    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        withContext(Dispatchers.IO) {
            localDataSource.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
        }

    suspend fun insertSetVideoId(setVideoId: SetVideoIdEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertSetVideoId(setVideoId) }

    suspend fun getSongsByListVideoId(listVideoId: List<String>): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getSongByListVideoId(listVideoId)) }.flowOn(Dispatchers.IO)

    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) =
        withContext(Dispatchers.IO) { localDataSource.updateLocalPlaylistTracks(tracks, id) }

    suspend fun getChartData(countryCode: String = "KR"): Flow<Resource<Chart>> = flow {
        runCatching {
            Youtube.customQuery("FEmusic_charts", country = countryCode).onSuccess { result ->
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer
                val chart = parseChart(data)
                if (chart != null) {
                    emit(Resource.Success<Chart>(chart))
                } else {
                    emit(Resource.Error<Chart>("Error"))
                }
            }.onFailure { error ->
                emit(Resource.Error<Chart>(error.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getNewRelease(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        Youtube.newRelease().onSuccess { result ->
            emit(Resource.Success<ArrayList<HomeItem>>(parseNewRelease(result, context)))
        }.onFailure { error ->
            emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getAllLocalPlaylists(): Flow<List<LocalPlaylistEntity>> =
        flow { emit(localDataSource.getAllLocalPlaylists()) }.flowOn(Dispatchers.IO)

    suspend fun getMoodAndMomentsData(): Flow<Resource<Mood>> = flow {
        runCatching {
            Youtube.moodAndGenres().onSuccess { result ->
                val listMoodMoments: ArrayList<MoodsMoment> = arrayListOf()
                val listGenre: ArrayList<Genre> = arrayListOf()
                result[0].let { moodsmoment ->
                    for (item in moodsmoment.items) {
                        listMoodMoments.add(
                            MoodsMoment(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                result[1].let { genres ->
                    for (item in genres.items) {
                        listGenre.add(
                            Genre(
                                params = item.endpoint.params ?: "",
                                title = item.title
                            )
                        )
                    }
                }
                emit(Resource.Success<Mood>(Mood(listGenre, listMoodMoments)))

            }.onFailure { e ->
                emit(Resource.Error<Mood>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getHomeData(): Flow<Resource<ArrayList<HomeItem>>> = flow {
        runCatching {
            val limit = dataStoreManager.homeLimit.first()
            Youtube.customQuery(browseId = "FEmusic_home").onSuccess { result ->
                val list: ArrayList<HomeItem> = arrayListOf()
                if (result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                        0
                    )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(
                        0
                    )?.text != null
                ) {
                    val accountName =
                        result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                            0
                        )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.get(
                            0
                        )?.text ?: ""
                    val accountThumbUrl =
                        result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                            0
                        )?.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.get(
                            0
                        )?.url?.replace("s88", "s352") ?: ""
                    if (accountName != "" && accountThumbUrl != "") {
                        dataStoreManager.putString("AccountName", accountName)
                        dataStoreManager.putString("AccountThumbUrl", accountThumbUrl)
                    }
                }
                var continueParam =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.continuations?.get(
                        0
                    )?.nextContinuationData?.continuation
                val data =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents
                list.addAll(parseMixedContent(data, context))
                var count = 0
                while (count < limit && continueParam != null) {
                    Youtube.customQuery(browseId = "", continuation = continueParam)
                        .onSuccess { response ->
                            continueParam =
                                response.continuationContents?.sectionListContinuation?.continuations?.get(
                                    0
                                )?.nextContinuationData?.continuation
                            Log.d("Repository", "continueParam: $continueParam")
                            val dataContinue =
                                response.continuationContents?.sectionListContinuation?.contents
                            list.addAll(parseMixedContent(dataContinue, context))
                            count++
                            Log.d("Repository", "count: $count")
                        }.onFailure {
                            Log.e("Repository", "Error: ${it.message}")
                            count++
                        }
                }
                Log.d("Repository", "List size: ${list.size}")
                emit(Resource.Success<ArrayList<HomeItem>>(list))
            }.onFailure { error ->
                emit(Resource.Error<ArrayList<HomeItem>>(error.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateDownloadState(videoId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updateDownloadState(
                downloadState,
                videoId
            )
        }

    suspend fun updateLikeStatus(videoId: String, likeStatus: Int) =
        withContext(Dispatchers.Main) { localDataSource.updateLiked(likeStatus, videoId) }

    suspend fun recoverQueue(temp: List<Track>) {
        val queueEntity = QueueEntity(listTrack = temp)
        withContext(Dispatchers.IO) { localDataSource.recoverQueue(queueEntity) }
    }

    suspend fun updatePlaylistDownloadState(playlistId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updatePlaylistDownloadState(
                downloadState,
                playlistId
            )
        }

    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) =
        withContext(Dispatchers.IO) {
            localDataSource.updateLocalPlaylistDownloadState(
                downloadState,
                id
            )
        }

    suspend fun getAllDownloadedPlaylist(): Flow<List<Any>> =
        flow { emit(localDataSource.getAllDownloadedPlaylist()) }.flowOn(Dispatchers.IO)

    suspend fun getDownloadingSongs(): Flow<List<SongEntity>?> =
        flow { emit(localDataSource.getDownloadingSongs()) }.flowOn(Dispatchers.IO)

    suspend fun getPreparingSongs(): Flow<List<SongEntity>> =
        flow { emit(localDataSource.getPreparingSongs()) }.flowOn(Dispatchers.IO)

    suspend fun updateAlbumDownloadState(albumId: String, downloadState: Int) =
        withContext(Dispatchers.Main) {
            localDataSource.updateAlbumDownloadState(
                downloadState,
                albumId
            )
        }

    suspend fun insertSongInfo(songInfo: SongInfoEntity) = withContext(Dispatchers.IO) {
        localDataSource.insertSongInfo(songInfo)
    }

    suspend fun getDownloadedSongs(): Flow<List<SongEntity>?> =
        flow { emit(localDataSource.getDownloadedSongs()) }.flowOn(Dispatchers.IO)

    fun checkForUpdate(): Flow<GithubResponse?> = flow {
        Youtube.checkForUpdate().onSuccess {
            emit(it)
        }
            .onFailure {
                emit(null)
            }
    }

    suspend fun getSongInfo(videoId: String): Flow<SongInfoEntity?> = flow {
        runCatching {
            Youtube.getSongInfo(videoId).onSuccess { songInfo ->
                val song = SongInfoEntity(
                    videoId = songInfo.videoId,
                    author = songInfo.author,
                    authorId = songInfo.authorId,
                    authorThumbnail = songInfo.authorThumbnail,
                    description = songInfo.description,
                    uploadDate = songInfo.uploadDate,
                    subscribers = songInfo.subscribers,
                    viewCount = songInfo.viewCount,
                    like = songInfo.like,
                    dislike = songInfo.dislike,
                )
                emit(song)
                insertSongInfo(
                    song
                )
            }.onFailure {
                it.printStackTrace()
                emit(getSongInfoEntiy(videoId).firstOrNull())
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSongInfoEntiy(videoId: String): Flow<SongInfoEntity?> =
        flow { emit(localDataSource.getSongInfo(videoId)) }.flowOn(Dispatchers.Main)

    suspend fun getStream(videoId: String, itag: Int): Flow<String?> = flow {
        Youtube.player(videoId).onSuccess { data ->
            val acceptToPlayVideo =
                runBlocking { dataStoreManager.watchVideoInsteadOfPlayingAudio.first() == DataStoreManager.TRUE }
            val videoItag =
                VIDEO_QUALITY.itags.getOrNull(VIDEO_QUALITY.items.indexOf(dataStoreManager.videoQuality.first()))
                    ?: 22
            val response = data.second
            if (data.third == MediaType.Song) Log.w(
                "Stream",
                "response: is SONG"
            ) else Log.w("Stream", "response: is VIDEO")
            Log.w("Stream: ", data.toString())
            var format =
                if (acceptToPlayVideo) {
                    if (data.third == MediaType.Song) response.streamingData?.adaptiveFormats?.find { it.itag == itag } else response.streamingData?.formats?.find { it.itag == videoItag }
                } else {
                    response.streamingData?.adaptiveFormats?.find { it.itag == itag }
                }
            if (format == null) {
                format = response.streamingData?.adaptiveFormats?.lastOrNull()
            }
            Log.w("Stream", "format: $format")
            runBlocking {
                insertNewFormat(
                    NewFormatEntity(
                        videoId = videoId,
                        itag = format?.itag ?: itag,
                        mimeType = Regex("""([^;]+);\s*codecs=["']([^"']+)["']""").find(
                            format?.mimeType ?: ""
                        )?.groupValues?.getOrNull(1) ?: format?.mimeType ?: "",
                        codecs = Regex("""([^;]+);\s*codecs=["']([^"']+)["']""").find(
                            format?.mimeType ?: ""
                        )?.groupValues?.getOrNull(2) ?: format?.mimeType ?: "",
                        bitrate = format?.bitrate,
                        sampleRate = format?.audioSampleRate,
                        contentLength = format?.contentLength,
                        loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
                        lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
                        playbackTrackingVideostatsPlaybackUrl = response.playbackTracking?.videostatsPlaybackUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        playbackTrackingAtrUrl = response.playbackTracking?.atrUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        playbackTrackingVideostatsWatchtimeUrl = response.playbackTracking?.videostatsWatchtimeUrl?.baseUrl?.replace(
                            "https://s.youtube.com",
                            "https://music.youtube.com"
                        ),
                        cpn = data.first,
                    )
                )
            }
            if (data.first != null) {
                emit(format?.url?.plus("&cpn=${data.first}"))
            } else {
                emit(format?.url)
            }
//                insertFormat(
//                    FormatEntity(
//                        videoId = videoId,
//                        itag = format?.itag ?: itag,
//                        mimeType = format?.mimeType,
//                        bitrate = format?.bitrate?.toLong(),
//                        contentLength = format?.contentLength,
//                        lastModified = format?.lastModified,
//                        loudnessDb = response.playerConfig?.audioConfig?.loudnessDb?.toFloat(),
//                        uploader = response.videoDetails?.author?.replace(Regex(" - Topic| - Chủ đề|"), ""),
//                        uploaderId = response.videoDetails?.channelId,
//                        uploaderThumbnail = response.videoDetails?.authorAvatar,
//                        uploaderSubCount = response.videoDetails?.authorSubCount,
//                        description = response.videoDetails?.description,
//                        youtubeCaptionsUrl = response.captions?.playerCaptionsTracklistRenderer?.captionTracks?.get(
//                            0
//                        )?.baseUrl?.replace("&fmt=srv3", ""),
//                        lengthSeconds = response.videoDetails?.lengthSeconds?.toInt(),
//                    )
//                )
        }.onFailure {
            it.printStackTrace()
            Log.e("Stream", "Error: ${it.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSongById(id: String): Flow<SongEntity?> =
        flow { emit(localDataSource.getSong(id)) }.flowOn(Dispatchers.IO)

    suspend fun insertSong(songEntity: SongEntity): Flow<Long> =
        flow<Long> { emit(localDataSource.insertSong(songEntity)) }.flowOn(Dispatchers.IO)

    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) =
        withContext(Dispatchers.Main) { localDataSource.updateSongInLibrary(inLibrary, videoId) }

    suspend fun updateListenCount(videoId: String) =
        withContext(Dispatchers.IO) { localDataSource.updateListenCount(videoId) }

    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String) =
        withContext(Dispatchers.Main) {
            localDataSource.updateDurationSeconds(
                durationSeconds,
                videoId
            )
        }

    suspend fun getSavedLyrics(videoId: String): Flow<LyricsEntity?> =
        flow { emit(localDataSource.getSavedLyrics(videoId)) }.flowOn(Dispatchers.IO)

    suspend fun getLyricsData(
        query: String,
        durationInt: Int? = null
    ): Flow<Pair<String, Resource<Lyrics>>> = flow {
        runCatching {
            val q =
                query.replace(
                    Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                    " "
                ).replace(
                    Regex("( và | & | и | e | und |, |和| dan)"), " "
                ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
            Log.d("Lyrics", "query: $q")
            var musixMatchUserToken = Youtube.musixmatchUserToken
            if (musixMatchUserToken == null) {
                Youtube.getMusixmatchUserToken().onSuccess { usertoken ->
                    Youtube.musixmatchUserToken = usertoken.message.body.user_token
                    musixMatchUserToken = usertoken.message.body.user_token
                }
                    .onFailure { throwable ->
                        throwable.printStackTrace()
                        emit(Pair("", Resource.Error<Lyrics>("Not found")))
                    }
            }
            Youtube.searchMusixmatchTrackId(q, musixMatchUserToken!!).onSuccess { searchResult ->
                Log.d("Lyrics", "searchResult: $searchResult")
                if (searchResult.message.body.track_list.isNotEmpty()) {
                    val list = arrayListOf<String>()
                    for (i in searchResult.message.body.track_list) {
                        list.add(i.track.track_name + " " + i.track.artist_name)
                    }
                    var id = ""
                    var track: SearchMusixmatchResponse.Message.Body.Track.TrackX? = null
                    Log.d("DURATION", "duration: $durationInt")
                    val bestMatchingIndex = bestMatchingIndex(q, list)
                    if (durationInt != null && durationInt != 0) {
                        val trackLengthList = arrayListOf<Int>()
                        for (i in searchResult.message.body.track_list) {
                            trackLengthList.add(i.track.track_length)
                        }
                        val closestIndex =
                            trackLengthList.minByOrNull { kotlin.math.abs(it - durationInt) }
                        if (closestIndex != null && kotlin.math.abs(closestIndex - durationInt) < 2) {
                            id += searchResult.message.body.track_list.find { it.track.track_length == closestIndex }?.track?.track_id.toString()
                            track =
                                searchResult.message.body.track_list.find { it.track.track_length == closestIndex }?.track
                        }
                        if (id == "") {
                            if (list.get(bestMatchingIndex).contains(
                                    searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name
                                ) && query.contains(
                                    searchResult.message.body.track_list.get(
                                        bestMatchingIndex
                                    ).track.track_name
                                )
                            ) {
                                Log.w(
                                    "Lyrics",
                                    "item: ${
                                        searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name
                                    }"
                                )
                                id += searchResult.message.body.track_list.get(bestMatchingIndex).track.track_id.toString()
                                track =
                                    searchResult.message.body.track_list.get(bestMatchingIndex).track
                            }
                        }
                    } else {
                        if (list.get(bestMatchingIndex)
                                .contains(searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name) && query.contains(
                                searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name
                            )
                        ) {
                            Log.w(
                                "Lyrics",
                                "item: ${searchResult.message.body.track_list.get(bestMatchingIndex).track.track_name}"
                            )
                            id += searchResult.message.body.track_list.get(bestMatchingIndex).track.track_id.toString()
                            track =
                                searchResult.message.body.track_list.get(bestMatchingIndex).track
                        }
                    }
                    Log.d("DURATION", "id: $id")
                    Log.w(
                        "item lyrics",
                        searchResult.message.body.track_list.find { it.track.track_id == id.toInt() }?.track?.track_name + " " + searchResult.message.body.track_list.find { it.track.track_id == id.toInt() }?.track?.artist_name
                    )
                    if (id != "" && track != null) {
                        Youtube.getMusixmatchLyricsByQ(track, musixMatchUserToken!!).onSuccess {
                            if (it != null) {
                                emit(Pair(id, Resource.Success<Lyrics>(it.toLyrics())))
                            } else {
                                Log.w("Lyrics", "Error: Lỗi getLyrics ${it.toString()}")
                                emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                            }
                        }.onFailure { throwable ->
                            throwable.printStackTrace()
                            emit(Pair(id, Resource.Error<Lyrics>("Not found")))
                        }
                    } else {
                        emit(Pair("", Resource.Error<Lyrics>("Not found")))
                    }
                } else {
                    emit(Pair("", Resource.Error<Lyrics>("Not found")))
                }
            }
                .onFailure { throwable ->
                    throwable.printStackTrace()
                    emit(Pair("", Resource.Error<Lyrics>("Not found")))
                }

        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertLyrics(lyricsEntity: LyricsEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertLyrics(lyricsEntity) }


    suspend fun getTranslateLyrics(id: String): Flow<MusixmatchTranslationLyricsResponse?> = flow {
        runCatching {
            Youtube.musixmatchUserToken?.let {
                Youtube.getMusixmatchTranslateLyrics(
                    id,
                    it, dataStoreManager.translationLanguage.first()
                )
                    .onSuccess { lyrics ->
                        emit(lyrics)
                    }
                    .onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getRelatedData(videoId: String): Flow<Resource<ArrayList<Track>>> = flow {
        runCatching {
            Queue.setContinuation(null)
            Youtube.next(WatchEndpoint(playlistId = "RDAMVM$videoId"))
                .onSuccess { next ->
                    val data: ArrayList<SongItem> = arrayListOf()
                    data.addAll(next.items)
                    val nextContinuation = next.continuation
                    if (nextContinuation != null) {
                        Queue.setContinuation(Pair("RDAMVM$videoId", nextContinuation))
                    } else {
                        Queue.setContinuation(null)
                    }
                    emit(Resource.Success<ArrayList<Track>>(data.toListTrack()))
                }.onFailure { exception ->
                    exception.printStackTrace()
                    Queue.setContinuation(null)
                    emit(Resource.Error<ArrayList<Track>>(exception.message.toString()))
                }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getDownloadedSongsAsFlow() = localDataSource.getDownloadedSongsAsFlow()

    suspend fun getPlaylistData(playlistId: String): Flow<Resource<PlaylistBrowse>> = flow {
        runCatching {
            var id = ""
            id += if (!playlistId.startsWith("VL")) {
                "VL$playlistId"
            } else {
                playlistId
            }
            Log.d("Repository", "playlist id: $id")
            Youtube.customQuery(browseId = id, setLogin = true).onSuccess { result ->
                val listContent: ArrayList<MusicShelfRenderer.Content> = arrayListOf()
                val data: List<MusicShelfRenderer.Content>? =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                        0
                    )?.musicPlaylistShelfRenderer?.contents
                if (data != null) {
                    Log.d("Data", "data: $data")
                    Log.d("Data", "data size: ${data.size}")
                    listContent.addAll(data)
                }
                val header = result.header?.musicDetailHeaderRenderer
                    ?: result.header?.musicEditablePlaylistDetailHeaderRenderer
                Log.d("Header", "header: $header")
                var continueParam =
                    result.contents?.singleColumnBrowseResultsRenderer?.tabs?.get(0)?.tabRenderer?.content?.sectionListRenderer?.contents?.get(
                        0
                    )?.musicPlaylistShelfRenderer?.continuations?.get(0)?.nextContinuationData?.continuation
                var count = 0
                Log.d("Repository", "playlist data: ${listContent.size}")
                Log.d("Repository", "continueParam: $continueParam")
                while (continueParam != null) {
                    Youtube.customQuery(
                        browseId = "",
                        continuation = continueParam,
                        setLogin = true
                    ).onSuccess { values ->
                        Log.d("Continue", "continue: $continueParam")
                        val dataMore: List<MusicShelfRenderer.Content>? =
                            values.continuationContents?.musicPlaylistShelfContinuation?.contents
                        if (dataMore != null) {
                            listContent.addAll(dataMore)
                        }
                        continueParam =
                            values.continuationContents?.musicPlaylistShelfContinuation?.continuations?.get(
                                0
                            )?.nextContinuationData?.continuation
                        count++
                    }.onFailure {
                        Log.e("Continue", "Error: ${it.message}")
                        continueParam = null
                        count++
                    }
                }
                Log.d("Repository", "playlist final data: ${listContent.size}")
                parsePlaylistData(header, listContent, playlistId, context)?.let { playlist ->
                    emit(Resource.Success<PlaylistBrowse>(playlist))
                } ?: emit(Resource.Error<PlaylistBrowse>("Error"))
            }.onFailure { e ->
                emit(Resource.Error<PlaylistBrowse>(e.message.toString()))
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCanvas(videoId: String, duration: Int): Flow<CanvasResponse?> = flow {
        runCatching {
            getSongById(videoId).first().let { song ->
                val q = "${song?.title} ${song?.artistName?.firstOrNull() ?: ""}".replace(
                    Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                    " "
                ).replace(
                    Regex("( và | & | и | e | und |, |和| dan)"), " "
                ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                    .replace("  ", " ")
                var spotifyPersonalToken = ""
                if (dataStoreManager.spotifyPersonalToken.first()
                        .isNotEmpty() && dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() && dataStoreManager.spotifyPersonalTokenExpires.first() != 0L
                ) {
                    spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                    Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                    Youtube.getPersonalToken(dataStoreManager.spdc.first()).onSuccess {
                        spotifyPersonalToken = it.accessToken
                        dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                        dataStoreManager.setSpotifyPersonalTokenExpires(it.accessTokenExpirationTimestampMs)
                        Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                    }.onFailure {
                        it.printStackTrace()
                        emit(null)
                    }
                }
                if (spotifyPersonalToken.isNotEmpty()) {
                    var clientToken = dataStoreManager.spotifyClientToken.first()
                    Log.d("Lyrics", "clientToken: $clientToken")
                    Youtube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                        val track = if (duration != 0) {
                            searchResponse.tracks.items.find { abs(((it.duration_ms / 1000) - duration)) < 1 }
                                ?: searchResponse.tracks.items.firstOrNull()
                        } else {
                            searchResponse.tracks.items.firstOrNull()
                        }
                        Log.d("Lyrics", "track: $track")
                        if (track != null) {
                            Youtube.getSpotifyCanvas(track.id, spotifyPersonalToken).onSuccess {
                                Log.w("Spotify Canvas", "canvas: $it")
                                emit(it)
                            }.onFailure {
                                it.printStackTrace()
                                emit(null)
                            }
                        } else {
                            emit(null)
                        }
                    }.onFailure { throwable ->
                        throwable.printStackTrace()
                        Youtube.getClientToken().onSuccess { tokenResponse ->
                            clientToken = tokenResponse.accessToken
                            Log.w("Lyrics", "clientToken: $clientToken")
                            dataStoreManager.setSpotifyClientToken(clientToken)
                            Youtube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                                val track = if (duration != 0) {
                                    searchResponse.tracks.items.find { abs(((it.duration_ms / 1000) - duration)) < 1 }
                                        ?: searchResponse.tracks.items.firstOrNull()
                                } else {
                                    searchResponse.tracks.items.firstOrNull()
                                }
                                Log.d("Lyrics", "track: $track")
                                if (track != null) {
                                    Youtube.getSpotifyCanvas(track.id, spotifyPersonalToken)
                                        .onSuccess {
                                            Log.w("Spotify Canvas", "canvas: $it")
                                            emit(it)
                                        }.onFailure {
                                            it.printStackTrace()
                                            emit(null)
                                        }
                                } else {
                                    emit(null)
                                }
                            }
                        }.onFailure {
                            it.printStackTrace()
                            emit(null)
                        }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getContinueTrack(
        playlistId: String,
        continuation: String
    ): Flow<ArrayList<Track>?> = flow {
        runCatching {
            Queue.setContinuation(null)
            Youtube.next(WatchEndpoint(playlistId = playlistId), continuation = continuation)
                .onSuccess { next ->
                    val data: ArrayList<SongItem> = arrayListOf()
                    data.addAll(next.items)
                    val nextContinuation = next.continuation
                    if (nextContinuation != null) {
                        Queue.setContinuation(Pair(playlistId, nextContinuation))
                    } else {
                        Queue.setContinuation(null)
                    }
                    emit(data.toListTrack())
                }.onFailure { exception ->
                    exception.printStackTrace()
                    Queue.setContinuation(null)
                    emit(null)
                }
        }
    }

    suspend fun initPlayback(
        playback: String,
        atr: String,
        watchTime: String,
        cpn: String,
        playlistId: String?
    ): Flow<Pair<Int, Float>> = flow {
        Youtube.initPlayback(playback, atr, watchTime, cpn, playlistId).onSuccess { response ->
            emit(response)
        }.onFailure {
            emit(Pair(0, 0f))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateWatchTime(
        playbackTrackingVideostatsWatchtimeUrl: String,
        watchTimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?
    ): Flow<Int> = flow {
        runCatching {
            Youtube.updateWatchTime(
                playbackTrackingVideostatsWatchtimeUrl,
                watchTimeList,
                cpn,
                playlistId
            ).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun updateWatchTimeFull(
        watchTime: String,
        cpn: String,
        playlistId: String?
    ): Flow<Int> = flow {
        runCatching {
            Youtube.updateWatchTimeFull(watchTime, cpn, playlistId).onSuccess { response ->
                emit(response)
            }.onFailure {
                it.printStackTrace()
                emit(0)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSkipSegments(videoId: String): Flow<List<SkipSegments>?> = flow {
        Youtube.getSkipSegments(videoId).onSuccess {
            emit(it)
        }.onFailure {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getNewFormat(videoId: String): Flow<NewFormatEntity?> =
        flow { emit(localDataSource.getNewFormat(videoId)) }.flowOn(Dispatchers.Main)

    fun getFullMetadata(videoId: String): Flow<YouTubeInitialPage?> = flow {
        Log.w("getFullMetadata", "videoId: $videoId")
        Youtube.getFullMetadata(videoId).onSuccess {
            emit(it)
        }.onFailure {
            Log.e("getFullMetadata", "Error: ${it.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getYouTubeCaption(videoId: String): Flow<Resource<Lyrics>> = flow {
        runCatching {
            Youtube.getYouTubeCaption(videoId).onSuccess { lyrics ->
                Log.w("Lyrics", "lyrics: ${lyrics.toLyrics()}")
                emit(Resource.Success<Lyrics>(lyrics.toLyrics()))
            }.onFailure { e ->
                Log.d("Lyrics", "Error: ${e.message}")
                emit(Resource.Error<Lyrics>(e.message.toString()))
            }
        }
    }

    suspend fun removeFromYouTubeLiked(mediaId: String?): Flow<Int> = flow {
        if (mediaId != null) {
            runCatching {
                Youtube.removeFromLiked(mediaId).onSuccess {
                    Log.d("Liked", "Success: $it")
                    emit(it)
                }.onFailure {
                    it.printStackTrace()
                    emit(0)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getSpotifyLyrics(query: String, duration: Int?): Flow<Resource<Lyrics>> = flow {
        runCatching {
            val q =
                query.replace(
                    Regex("\\((feat\\.|ft.|cùng với|con|mukana|com|avec|合作音乐人: ) "),
                    " "
                ).replace(
                    Regex("( và | & | и | e | und |, |和| dan)"), " "
                ).replace("  ", " ").replace(Regex("([()])"), "").replace(".", " ")
                    .replace("  ", " ")
            Log.d("Lyrics", "query: $q")
            var spotifyPersonalToken = ""
            if (dataStoreManager.spotifyPersonalToken.first()
                    .isNotEmpty() && dataStoreManager.spotifyPersonalTokenExpires.first() > System.currentTimeMillis() && dataStoreManager.spotifyPersonalTokenExpires.first() != 0L
            ) {
                spotifyPersonalToken = dataStoreManager.spotifyPersonalToken.first()
                Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
            } else if (dataStoreManager.spdc.first().isNotEmpty()) {
                Youtube.getPersonalToken(dataStoreManager.spdc.first()).onSuccess {
                    spotifyPersonalToken = it.accessToken
                    dataStoreManager.setSpotifyPersonalToken(spotifyPersonalToken)
                    dataStoreManager.setSpotifyPersonalTokenExpires(it.accessTokenExpirationTimestampMs)
                    Log.d("Lyrics", "spotifyPersonalToken: $spotifyPersonalToken")
                }.onFailure {
                    it.printStackTrace()
                    emit(Resource.Error<Lyrics>("Not found"))
                }
            }
            if (spotifyPersonalToken.isNotEmpty()) {
                var clientToken = dataStoreManager.spotifyClientToken.first()
                Log.d("Lyrics", "clientToken: $clientToken")
                Youtube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                    val track = if (duration != null && duration != 0) {
                        searchResponse.tracks.items.find { abs(((it.duration_ms / 1000) - duration)) < 1 }
                            ?: searchResponse.tracks.items.firstOrNull()
                    } else {
                        searchResponse.tracks.items.firstOrNull()
                    }
                    Log.d("Lyrics", "track: $track")
                    if (track != null) {
                        Youtube.getSpotifyLyrics(track.id, spotifyPersonalToken).onSuccess {
                            emit(Resource.Success<Lyrics>(it.toLyrics()))
                        }.onFailure {
                            it.printStackTrace()
                            emit(Resource.Error<Lyrics>("Not found"))
                        }
                    } else {
                        emit(Resource.Error<Lyrics>("Not found"))
                    }
                }.onFailure { throwable ->
                    throwable.printStackTrace()
                    Youtube.getClientToken().onSuccess {
                        clientToken = it.accessToken
                        Log.w("Lyrics", "clientToken: $clientToken")
                        dataStoreManager.setSpotifyClientToken(clientToken)
                        Youtube.searchSpotifyTrack(q, clientToken).onSuccess { searchResponse ->
                            val track = if (duration != null && duration != 0) {
                                searchResponse.tracks.items.find { abs(((it.duration_ms / 1000) - duration)) < 1 }
                                    ?: searchResponse.tracks.items.firstOrNull()
                            } else {
                                searchResponse.tracks.items.firstOrNull()
                            }
                            Log.d("Lyrics", "track: $track")
                            if (track != null) {
                                Youtube.getSpotifyLyrics(track.id, spotifyPersonalToken).onSuccess {
                                    emit(Resource.Success<Lyrics>(it.toLyrics()))
                                }.onFailure {
                                    it.printStackTrace()
                                    emit(Resource.Error<Lyrics>("Not found"))
                                }
                            } else {
                                emit(Resource.Error<Lyrics>("Not found"))
                            }
                        }
                    }.onFailure {
                        it.printStackTrace()
                        emit(Resource.Error<Lyrics>("Not found"))
                    }
                }
            }
        }
    }

    suspend fun addToYouTubeLiked(mediaId: String?): Flow<Int> = flow {
        if (mediaId != null) {
            runCatching {
                Youtube.addToLiked(mediaId).onSuccess {
                    Log.d("Liked", "Success: $it")
                    emit(it)
                }.onFailure {
                    it.printStackTrace()
                    emit(0)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}