package com.example.mymusic.base.data.repository

import android.content.Context
import android.util.Log
import com.example.mymusic.base.Youtube
import com.example.mymusic.base.common.VIDEO_QUALITY
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.db.LocalDataSource
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.metadata.Lyrics
import com.example.mymusic.base.data.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.example.mymusic.base.data.models.musixmatch.SearchMusixmatchResponse
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.models.MediaType
import com.example.mymusic.base.models.SongItem
import com.example.mymusic.base.models.WatchEndpoint
import com.example.mymusic.base.models.myMusic.GithubResponse
import com.example.mymusic.base.utils.Resource
import com.example.mymusic.base.utils.extension.bestMatchingIndex
import com.example.mymusic.base.utils.extension.toListTrack
import com.example.mymusic.base.utils.extension.toLyrics
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

@Singleton
class MainRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val context: Context
) {
    suspend fun insertNewFormat(newFormat: NewFormatEntity) =
        withContext(Dispatchers.IO) { localDataSource.insertNewFormat(newFormat) }

    suspend fun getSavedQueue(): Flow<List<QueueEntity>?> =
        flow { emit(localDataSource.getQueue())  }.flowOn(Dispatchers.IO)

    suspend fun removeQueue() {
        withContext(Dispatchers.IO) { localDataSource.deleteQueue() }
    }

    suspend fun insertSongInfo(songInfo: SongInfoEntity) = withContext(Dispatchers.IO) {
        localDataSource.insertSongInfo(songInfo)
    }

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

}