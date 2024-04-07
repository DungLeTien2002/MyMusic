package com.example.mymusic.base

import com.example.mymusic.base.data.models.musixmatch.MusixmatchLyricsReponse
import com.example.mymusic.base.data.models.musixmatch.MusixmatchLyricsResponseByQ
import com.example.mymusic.base.data.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.example.mymusic.base.data.models.musixmatch.SearchMusixmatchResponse
import com.example.mymusic.base.data.models.musixmatch.UserTokenResponse
import com.example.mymusic.base.models.MediaType
import com.example.mymusic.base.models.ReturnYouTubeDislikeResponse
import com.example.mymusic.base.models.SongInfo
import com.example.mymusic.base.models.WatchEndpoint
import com.example.mymusic.base.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.example.mymusic.base.models.YouTubeClient.Companion.WEB
import com.example.mymusic.base.models.YouTubeClient.Companion.WEB_REMIX
import com.example.mymusic.base.models.getContinuation
import com.example.mymusic.base.models.myMusic.GithubResponse
import com.example.mymusic.base.models.response.NextResponse
import com.example.mymusic.base.models.response.PipedResponse
import com.example.mymusic.base.models.response.PlayerResponse
import com.example.mymusic.base.pages.NextPage
import com.example.mymusic.base.pages.NextResult
import com.maxrave.kotlinytmusicscraper.parser.parseMusixmatchLyrics
import com.maxrave.kotlinytmusicscraper.parser.parseUnsyncedLyrics
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.random.Random

object Youtube {
    private val ytMusic = YtMusic()
    var musixMatchCookie: String?
        get() = ytMusic.musixMatchCookie
        set(value) {
            ytMusic.musixMatchCookie = value
        }

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
        println("Thumbnails " + playerResponse.videoDetails?.thumbnail)
        val firstThumb = playerResponse.videoDetails?.thumbnail?.thumbnails?.firstOrNull()
        val thumbnails =
            if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
        println("Player Response " + playerResponse.streamingData)

        if (playerResponse.playabilityStatus.status == "OK") {
            return@runCatching Triple(
                cpn, playerResponse.copy(
                    videoDetails = playerResponse.videoDetails?.copy(),
                ), thumbnails
            )
        } else {
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

    fun getMusixmatchCookie() = musixMatchCookie
    suspend fun searchMusixmatchTrackId(query: String, userToken: String) = runCatching {
        ytMusic.searchMusixmatchTrackId(query, userToken).body<SearchMusixmatchResponse>()
    }

    var musixmatchUserToken: String?
        get() = ytMusic.musixmatchUserToken
        set(value) {
            ytMusic.musixmatchUserToken = value
        }

    suspend fun getMusixmatchUserToken() = runCatching {
        ytMusic.getMusixmatchUserToken().body<UserTokenResponse>()
    }

    suspend fun getMusixmatchLyricsByQ(
        track: SearchMusixmatchResponse.Message.Body.Track.TrackX,
        userToken: String
    ) = runCatching {
        val response =
            ytMusic.getMusixmatchLyricsByQ(track, userToken).body<MusixmatchLyricsResponseByQ>()

        if (!response.message.body.subtitle_list.isNullOrEmpty() && response.message.body.subtitle_list.firstOrNull()?.subtitle?.subtitle_body != null) {
            return@runCatching parseMusixmatchLyrics(response.message.body.subtitle_list.firstOrNull()?.subtitle?.subtitle_body!!)
        } else {
            val unsyncedResponse =
                ytMusic.getMusixmatchUnsyncedLyrics(track.track_id.toString(), userToken)
                    .body<MusixmatchLyricsReponse>()
            if (unsyncedResponse.message.body.lyrics != null && unsyncedResponse.message.body.lyrics.lyrics_body != "") {
                return@runCatching parseUnsyncedLyrics(unsyncedResponse.message.body.lyrics.lyrics_body)
            } else {
                null
            }
        }
    }
    suspend fun getSongInfo(videoId: String): Result<SongInfo> = runCatching {
        val ytNext = ytMusic.next(WEB, videoId, null, null, null, null, null).body<NextResponse>()
//        val ytScrapeInitial: YouTubeInitialPage = ytMusic.player(WEB, videoId, null, null).body<YouTubeInitialPage>()
        val videoSecondary =
            ytNext.contents.twoColumnWatchNextResults?.results?.results?.content?.find {
                it?.videoSecondaryInfoRenderer != null
            }?.videoSecondaryInfoRenderer
        val videoPrimary =
            ytNext.contents.twoColumnWatchNextResults?.results?.results?.content?.find {
                it?.videoPrimaryInfoRenderer != null
            }?.videoPrimaryInfoRenderer
        val returnYouTubeDislikeResponse =
            ytMusic.returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()
        return@runCatching SongInfo(
            videoId = videoId,
            author = videoSecondary?.owner?.videoOwnerRenderer?.title?.runs?.firstOrNull()?.text?.replace(
                Regex(" - Topic| - Chủ đề|"),
                ""
            ),
            authorId = videoSecondary?.owner?.videoOwnerRenderer?.navigationEndpoint?.browseEndpoint?.browseId,
            authorThumbnail = videoSecondary?.owner?.videoOwnerRenderer?.thumbnail?.thumbnails?.find {
                it.height == 48
            }?.url?.replace("s48", "s960"),
            description = videoSecondary?.attributedDescription?.content,
            subscribers = videoSecondary?.owner?.videoOwnerRenderer?.subscriberCountText?.simpleText,
            uploadDate = videoPrimary?.dateText?.simpleText,
            viewCount = returnYouTubeDislikeResponse.viewCount,
            like = returnYouTubeDislikeResponse.likes,
            dislike = returnYouTubeDislikeResponse.dislikes
        )
        //Get author thumbnails, subscribers, description, like count
    }

    suspend fun checkForUpdate() = runCatching {
        ytMusic.checkForUpdate().body<GithubResponse>()
    }

    suspend fun getMusixmatchTranslateLyrics(trackId: String, userToken: String, language: String) =
        runCatching {
            ytMusic.getMusixmatchTranslateLyrics(trackId, userToken, language)
                .body<MusixmatchTranslationLyricsResponse>()
        }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> =
        runCatching {
            val response = ytMusic.next(
                WEB_REMIX,
                endpoint.videoId,
                endpoint.playlistId,
                endpoint.playlistSetVideoId,
                endpoint.index,
                endpoint.params,
                continuation
            ).body<NextResponse>()
            val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
                ?: response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer!!
            // load automix items
            if (playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint != null) {

                return@runCatching next(playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!).getOrThrow()
                    .let { result ->
                        result.copy(
                            title = playlistPanelRenderer.title,
                            items = playlistPanelRenderer.contents.mapNotNull {
                                it.playlistPanelVideoRenderer?.let { renderer ->
                                    NextPage.fromPlaylistPanelVideoRenderer(renderer)
                                }
                            } + result.items,
                            lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(
                                1
                            )?.tabRenderer?.endpoint?.browseEndpoint,
                            relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(
                                2
                            )?.tabRenderer?.endpoint?.browseEndpoint,
                            currentIndex = playlistPanelRenderer.currentIndex,
                            endpoint = playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!
                        )
                    }
            }
            NextResult(
                title = playlistPanelRenderer.title,
                items = playlistPanelRenderer.contents.mapNotNull {
                    it.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
                },
                currentIndex = playlistPanelRenderer.currentIndex,
                lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(
                    1
                )?.tabRenderer?.endpoint?.browseEndpoint,
                relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.getOrNull(
                    2
                )?.tabRenderer?.endpoint?.browseEndpoint,
                continuation = playlistPanelRenderer.continuations?.getContinuation(),
                endpoint = endpoint
            )
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