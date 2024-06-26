package com.example.mymusic.base

import android.util.Log
import com.example.mymusic.base.data.models.musixmatch.MusixmatchLyricsReponse
import com.example.mymusic.base.data.models.musixmatch.MusixmatchLyricsResponseByQ
import com.example.mymusic.base.data.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.example.mymusic.base.data.models.musixmatch.SearchMusixmatchResponse
import com.example.mymusic.base.data.models.musixmatch.UserTokenResponse
import com.example.mymusic.base.models.AccountInfo
import com.example.mymusic.base.models.MediaType
import com.example.mymusic.base.models.PlaylistItem
import com.example.mymusic.base.models.ReturnYouTubeDislikeResponse
import com.example.mymusic.base.models.SongInfo
import com.example.mymusic.base.models.VideoItem
import com.example.mymusic.base.models.WatchEndpoint
import com.example.mymusic.base.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.example.mymusic.base.models.YouTubeClient.Companion.WEB
import com.example.mymusic.base.models.YouTubeClient.Companion.WEB_REMIX
import com.example.mymusic.base.models.YouTubeLocale
import com.example.mymusic.base.models.getContinuation
import com.example.mymusic.base.models.myMusic.GithubResponse
import com.example.mymusic.base.models.response.AccountMenuResponse
import com.example.mymusic.base.models.response.AddItemYouTubePlaylistResponse
import com.example.mymusic.base.models.response.BrowseResponse
import com.example.mymusic.base.models.response.NextResponse
import com.example.mymusic.base.models.response.PipedResponse
import com.example.mymusic.base.models.response.PlayerResponse
import com.example.mymusic.base.models.response.spotify.CanvasResponse
import com.example.mymusic.base.models.response.spotify.PersonalTokenResponse
import com.example.mymusic.base.models.response.spotify.SpotifyLyricsResponse
import com.example.mymusic.base.models.response.spotify.TokenResponse
import com.example.mymusic.base.models.youtube.Transcript
import com.example.mymusic.base.models.youtube.YouTubeInitialPage
import com.example.mymusic.base.pages.ArtistPage
import com.example.mymusic.base.pages.ExplorePage
import com.example.mymusic.base.pages.MoodAndGenres
import com.example.mymusic.base.pages.NextPage
import com.example.mymusic.base.pages.NextResult
import com.example.mymusic.base.pages.RelatedPage
import com.maxrave.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.maxrave.kotlinytmusicscraper.parser.parseMusixmatchLyrics
import com.maxrave.kotlinytmusicscraper.parser.parseUnsyncedLyrics
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser

object Youtube {
    private val ytMusic = YtMusic()
    var musixMatchCookie: String?
        get() = ytMusic.musixMatchCookie
        set(value) {
            ytMusic.musixMatchCookie = value
        }
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            ytMusic.cookie = value
        }

    var locale: YouTubeLocale
        get() = ytMusic.locale
        set(value) {
            ytMusic.locale = value
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

    suspend fun newRelease(): Result<ExplorePage> = runCatching {
        val response =
            ytMusic.browse(WEB_REMIX, browseId = "FEmusic_new_releases").body<BrowseResponse>()
        println(response)
        ExplorePage(
            released = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(RelatedPage::fromMusicTwoRowItemRenderer)
                .orEmpty() as List<PlaylistItem>,
            musicVideo = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()?.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(
                    ArtistPage::fromMusicTwoRowItemRenderer
                ).orEmpty() as List<VideoItem>

        )
    }

    suspend fun addPlaylistItem(playlistId: String, videoId: String) = runCatching {
        ytMusic.addItemYouTubePlaylist(playlistId, videoId).body<AddItemYouTubePlaylistResponse>()
    }

    suspend fun customQuery(
        browseId: String,
        params: String? = null,
        continuation: String? = null,
        country: String? = null,
        setLogin: Boolean = true
    ) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin)
            .body<BrowseResponse>()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response =
            ytMusic.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
    }

    suspend fun getPersonalToken(spdc: String) = runCatching {
        ytMusic.getSpotifyLyricsToken(spdc).body<PersonalTokenResponse>()
    }

    suspend fun addToLiked(mediaId: String) = runCatching {
        ytMusic.addToLiked(mediaId).status.value
    }

    suspend fun searchSpotifyTrack(query: String, token: String) = runCatching {
        ytMusic.searchSpotifyTrack(query, token)
            .body<com.example.mymusic.base.models.response.spotify.SearchResponse>()
    }

    suspend fun getSpotifyCanvas(trackId: String, token: String) = runCatching {
        ytMusic.getSpotifyCanvas(trackId, token).body<CanvasResponse>()
    }

    suspend fun getClientToken() = runCatching {
        ytMusic.getSpotifyToken().body<TokenResponse>()
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

    suspend fun accountInfo(): Result<AccountInfo?> = runCatching {
        ytMusic.accountMenu(WEB_REMIX).apply {
            this.bodyAsText().let {
                println(it)
            }
        }
            .body<AccountMenuResponse>().actions[0].openPopupAction.popup.multiPageMenuRenderer.header?.activeAccountHeaderRenderer?.toAccountInfo()
    }

    suspend fun initPlayback(
        playbackUrl: String,
        atrUrl: String,
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?
    ): Result<Pair<Int, Float>> {
        println("playbackUrl $playbackUrl")
        println("atrUrl $atrUrl")
        println("watchtimeUrl $watchtimeUrl")
        return runCatching {
            ytMusic.initPlayback(playbackUrl, cpn, null, playlistId).status.value.let { status ->
                if (status == 204) {
                    println("playback done")
                    ytMusic.initPlayback(
                        watchtimeUrl,
                        cpn,
                        mapOf("st" to "0", "et" to "5.54"),
                        playlistId
                    ).status.value.let { firstWatchTime ->
                        if (firstWatchTime == 204) {
                            println("first watchtime done")
                            delay(5000)
                            ytMusic.atr(atrUrl, cpn, null, playlistId).status.value.let { atr ->
                                if (atr == 204) {
                                    println("atr done")
                                    delay(500)
                                    val secondWatchTime =
                                        (Math.round(Random.nextFloat() * 100.0) / 100.0).toFloat() + 12f
                                    ytMusic.initPlayback(
                                        watchtimeUrl,
                                        cpn,
                                        mapOf<String, String>(
                                            "st" to "0,5.54",
                                            "et" to "5.54,$secondWatchTime"
                                        ),
                                        playlistId
                                    ).status.value.let { watchtime ->
                                        if (watchtime == 204) {
                                            println("watchtime done")
                                            return@runCatching Pair(watchtime, secondWatchTime)
                                        } else {
                                            return@runCatching Pair(watchtime, secondWatchTime)
                                        }
                                    }
                                } else {
                                    return@runCatching Pair(atr, 0f)
                                }
                            }
                        } else {
                            return@runCatching Pair(firstWatchTime, 0f)
                        }
                    }
                } else {
                    return@runCatching Pair(status, 0f)
                }
            }
        }
    }

    suspend fun updateWatchTime(
        watchtimeUrl: String,
        watchtimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?
    ): Result<Int> =
        runCatching {
            val et = watchtimeList.takeLast(2).joinToString(",")
            val watchtime = watchtimeList.dropLast(1).takeLast(2).joinToString(",")
            ytMusic.initPlayback(
                watchtimeUrl,
                cpn,
                mapOf("st" to watchtime, "et" to et),
                playlistId
            ).status.value.let { status ->
                if (status == 204) {
                    println("watchtime done")
                }
                return@runCatching status
            }
        }

    suspend fun updateWatchTimeFull(
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?
    ): Result<Int> =
        runCatching {
            val regex = Regex("len=([^&]+)")
            val length = regex.find(watchtimeUrl)?.groupValues?.firstOrNull()?.drop(4) ?: "0"
            println(length)
            ytMusic.initPlayback(
                watchtimeUrl,
                cpn,
                mapOf("st" to length, "et" to length),
                playlistId
            ).status.value.let { status ->
                if (status == 204) {
                    println("watchtime full done")
                }
                return@runCatching status
            }
        }

    suspend fun getSkipSegments(videoId: String) = runCatching {
        ytMusic.getSkipSegments(videoId).body<List<SkipSegments>>()
    }

    suspend fun getFullMetadata(videoId: String): Result<YouTubeInitialPage> = runCatching {
        val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
        var response = ""
        val ksoupHtmlParser = KsoupHtmlParser(
            object : KsoupHtmlHandler {
                override fun onText(text: String) {
                    super.onText(text)
                    if (text.contains("var ytInitialPlayerResponse")) {
                        val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
                        Log.d("Scrape", "Temp $temp")
                        response = temp.trimIndent()
                    }
                }
            }
        )
        ksoupHtmlParser.write(ytScrape)
        ksoupHtmlParser.end()
        val json = Json { ignoreUnknownKeys = true }
        return@runCatching json.decodeFromString<YouTubeInitialPage>(response)
    }

    suspend fun getYouTubeCaption(videoId: String) = runCatching {
        val ytWeb = ytMusic.player(WEB, videoId, null, null).body<YouTubeInitialPage>()
        ytMusic.getYouTubeCaption(
            ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                "&fmt=srv3",
                ""
            ) ?: ""
        ).body<Transcript>()
    }
    suspend fun removeFromLiked(mediaId: String) = runCatching {
        ytMusic.removeFromLiked(mediaId).status.value
    }
    suspend fun getSpotifyLyrics(trackId: String, token: String) = runCatching {
        ytMusic.getSpotifyLyrics(token, trackId).body<SpotifyLyricsResponse>()
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