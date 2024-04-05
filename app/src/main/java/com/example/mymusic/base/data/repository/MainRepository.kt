package com.example.mymusic.base.data.repository

import android.content.Context
import android.util.Log
import com.example.mymusic.base.Youtube
import com.example.mymusic.base.common.VIDEO_QUALITY
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.db.LocalDataSource
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.models.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
}