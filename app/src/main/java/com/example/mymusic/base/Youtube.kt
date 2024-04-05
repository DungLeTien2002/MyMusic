package com.example.mymusic.base

import com.example.mymusic.base.models.MediaType
import com.example.mymusic.base.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.example.mymusic.base.models.response.PipedResponse
import com.example.mymusic.base.models.response.PlayerResponse
import io.ktor.client.call.body
import kotlin.random.Random

object Youtube {
    private val ytMusic = YtMusic()
    suspend fun player(
        videoId: String,
        playlistId: String? = null
    ): Result<Triple<String?, PlayerResponse, MediaType>> = runCatching {
        val cpn = (1..16).map {
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[Random.Default.nextInt(
                0,
                64
            )]
        }.joinToString("")
        val playerResponse =
            ytMusic.player(ANDROID_MUSIC, videoId, playlistId, cpn).body<PlayerResponse>()
//        val ytScrapeInitial: YouTubeInitialPage = ytMusic.player(WEB, videoId, playlistId, cpn).body<YouTubeInitialPage>()
        println("Thumbnails " + playerResponse.videoDetails?.thumbnail)
        val firstThumb = playerResponse.videoDetails?.thumbnail?.thumbnails?.firstOrNull()
        val thumbnails =
            if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
        println("Player Response " + playerResponse.streamingData)

//        println( playerResponse.streamingData?.adaptiveFormats?.findLast { it.itag == 251 }?.mimeType.toString())
        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching Triple(
                cpn, playerResponse.copy(
                    videoDetails = playerResponse.videoDetails?.copy(),
                ), thumbnails
            )
        }
        else {
            val piped = ytMusic.pipedStreams(videoId, "pipedapi.kavin.rocks").body<PipedResponse>()
            val audioStreams = piped.audioStreams
            val videoStreams = piped.videoStreams
            val stream = audioStreams + videoStreams
            return@runCatching Triple(
                null, playerResponse.copy(
                    streamingData = PlayerResponse.StreamingData(
                        formats = stream.toListFormat(),
                        adaptiveFormats = stream.toListFormat(),
                        expiresInSeconds = 0
                    ),
                    videoDetails = playerResponse.videoDetails?.copy(),
                ), thumbnails
            )
        }
    }
}
private fun List<PipedResponse.AudioStream>.toListFormat(): List<PlayerResponse.StreamingData.Format> {
    val list = mutableListOf<PlayerResponse.StreamingData.Format>()
    this.forEach {
        list.add(
            PlayerResponse.StreamingData.Format(
                itag = it.itag,
                url = it.url,
                mimeType = it.mimeType ?: "",
                bitrate = it.bitrate,
                width = it.width,
                height = it.height,
                contentLength = it.contentLength.toLong(),
                quality = it.quality,
                fps = it.fps,
                qualityLabel = "",
                averageBitrate = it.bitrate,
                audioQuality = it.quality,
                approxDurationMs = "",
                audioSampleRate = 0,
                audioChannels = 0,
                loudnessDb = 0.0,
                lastModified = 0,
            )
        )
    }

    return list
}