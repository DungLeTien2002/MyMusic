package com.example.mymusic.base

import com.example.mymusic.base.data.models.musixmatch.SearchMusixmatchResponse
import com.example.mymusic.base.encoder.brotli
import com.example.mymusic.base.models.Context
import com.example.mymusic.base.models.YouTubeClient
import com.example.mymusic.base.models.YouTubeLocale
import com.example.mymusic.base.models.body.NextBody
import com.example.mymusic.base.models.body.PlayerBody
import com.example.mymusic.base.utils.CustomRedirectConfig
import com.example.mymusic.base.utils.extension.parseCookieString
import com.example.mymusic.base.utils.extension.sha1
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import java.lang.reflect.Type
import java.net.Proxy
import java.util.Locale

class YtMusic {
    private var httpClient = createClient()
    var visitorData: String = "Cgt6SUNYVzB2VkJDbyjGrrSmBg%3D%3D"
    private var musixmatchClient = createMusixmatchClient()
    private var cookieMap = emptyMap<String, String>()
    var locale = YouTubeLocale(
        gl = Locale.getDefault().country,
        hl = Locale.getDefault().toLanguageTag()
    )
    var cookie: String? = null
        set(value) {
            field = value
            cookieMap = if (value == null) emptyMap() else parseCookieString(value)
        }
    var proxy: Proxy? = null
        set(value) {
            field = value
            httpClient.close()
            httpClient = createClient()
        }

    var musixmatchUserToken: String? = null

    var musixMatchCookie: String? = null
        set(value) {
            field = value
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createMusixmatchClient() = HttpClient(OkHttp) {
        expectSuccess = true
        followRedirects = false

        install(HttpSend) {
            maxSendCount = 100
        }
        install(HttpCookies) {
            storage = AcceptAllCookiesStorage()
        }
        install(CustomRedirectConfig) {
            checkHttpMethod = false
            allowHttpsDowngrade = true
            defaultHostUrl = "https://apic-desktop.musixmatch.com"
        }
        install(ContentNegotiation) {
            register(
                ContentType.Text.Plain, KotlinxSerializationConverter(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    }
                )
            )
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }
        install(ContentEncoding) {
            brotli(1.0F)
            gzip(0.9F)
            deflate(0.8F)
        }
        defaultRequest {
            url("https://apic-desktop.musixmatch.com/ws/1.1/")
        }
    }

    fun fromString(value: String?): List<String>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    suspend fun searchMusixmatchTrackId(q: String, userToken: String) =
        musixmatchClient.get("track.search?app_id=android-player-v1.0&page_size=5&page=1&s_track_rating=desc&quorum_factor=1.0") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                if (musixMatchCookie != null) {
                    val listCookies = fromString(musixMatchCookie)
                    if (!listCookies.isNullOrEmpty()) {
                        val appendCookie =
                            listCookies.joinToString(separator = "; ") { eachCookie ->
                                eachCookie
                            }
                        header(HttpHeaders.Cookie, appendCookie)
                    }
                }
            }

            parameter("q", q)
            parameter("usertoken", userToken)
        }

    suspend fun getMusixmatchUnsyncedLyrics(trackId: String, userToken: String) =
        musixmatchClient.get("track.lyrics.get?app_id=android-player-v1.0&subtitle_format=id3") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                if (musixMatchCookie != null) {
                    val listCookies = fromString(musixMatchCookie)
                    if (!listCookies.isNullOrEmpty()) {
                        val appendCookie =
                            listCookies.joinToString(separator = "; ") { eachCookie ->
                                eachCookie
                            }
                        header(HttpHeaders.Cookie, appendCookie)
                    }
                }
            }
            parameter("usertoken", userToken)
            parameter("track_id", trackId)
        }

    suspend fun getMusixmatchTranslateLyrics(trackId: String, userToken: String, language: String) =
        musixmatchClient.get("https://apic.musixmatch.com/ws/1.1/crowd.track.translations.get") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                if (musixMatchCookie != null) {
                    val listCookies = fromString(musixMatchCookie)
                    if (!listCookies.isNullOrEmpty()) {
                        val appendCookie =
                            listCookies.joinToString(separator = "; ") { eachCookie ->
                                eachCookie
                            }
                        header(HttpHeaders.Cookie, appendCookie)
                    }
                }
            }
            parameters {
                parameter("translation_fields_set", "minimal")
                parameter("track_id", trackId)
                parameter("selected_language", language)
                parameter("comment_format", "text")
                parameter("part", "user")
                parameter("format", "json")
                parameter("usertoken", userToken)
                parameter("app_id", "android-player-v1.0")
                parameter("tags", "playing")
            }
        }

    suspend fun next(
        client: YouTubeClient,
        videoId: String?,
        playlistId: String?,
        playlistSetVideoId: String?,
        index: Int?,
        params: String?,
        continuation: String? = null,
    ) = httpClient.post("next") {
        ytClient(client, setLogin = true)
        setBody(
            NextBody(
                context = client.toContext(locale, visitorData),
                videoId = videoId,
                playlistId = playlistId,
                playlistSetVideoId = playlistSetVideoId,
                index = index,
                params = params,
                continuation = continuation
            )
        )
    }

    suspend fun returnYouTubeDislike(videoId: String) =
        httpClient.get("https://returnyoutubedislikeapi.com/Votes?videoId=$videoId") {
            contentType(ContentType.Application.Json)
        }

    suspend fun checkForUpdate() =
        httpClient.get("https://api.github.com/repos/maxrave-dev/SimpMusic/releases/latest") {
            contentType(ContentType.Application.Json)
        }

    suspend fun getMusixmatchLyricsByQ(
        track: SearchMusixmatchResponse.Message.Body.Track.TrackX,
        userToken: String
    ) = musixmatchClient.get("https://apic.musixmatch.com/ws/1.1/track.subtitles.get") {
        contentType(ContentType.Application.Json)
        headers {
            header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.Connection, "keep-alive")
            if (musixMatchCookie != null) {
                val listCookies = fromString(musixMatchCookie)
                if (!listCookies.isNullOrEmpty()) {
                    val appendCookie = listCookies.joinToString(separator = "; ") { eachCookie ->
                        eachCookie
                    }
                    header(HttpHeaders.Cookie, appendCookie)
                }
            }
        }

        parameter("usertoken", userToken)
        parameter("track_id", track.track_id)
        parameter("f_subtitle_length_max_deviation", "1")
        parameter("page_size", "1")
        parameter(
            "questions_id_list",
            "track_esync_action%2Ctrack_sync_action%2Ctrack_translation_action%2Clyrics_ai_mood_analysis_v3"
        )
        parameter("optional_calls", "track.richsync%2Ccrowd.track.actions")
        parameter("q_artist", track.artist_name)
        parameter("q_track", track.track_name)
        parameter("app_id", "android-player-v1.0")
        parameter(
            "part",
            "lyrics_crowd%2Cuser%2Clyrics_vote%2Clyrics_poll%2Ctrack_lyrics_translation_status%2Clyrics_verified_by%2Clabels%2Ctrack_structure%2Ctrack_performer_tagging%2C"
        )
        parameter("language_iso_code", "1")
        parameter("format", "json")
        parameter("q_duration", track.track_length)
    }

    suspend fun getMusixmatchUserToken() =
        musixmatchClient.get("token.get?app_id=android-player-v1.0") {
            contentType(ContentType.Application.Json)
            headers {
                header(HttpHeaders.UserAgent, "PostmanRuntime/7.33.0")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
                header(HttpHeaders.Connection, "keep-alive")
                if (musixMatchCookie != null) {
                    val listCookies = fromString(musixMatchCookie)
                    if (!listCookies.isNullOrEmpty()) {
                        val appendCookie =
                            listCookies.joinToString(separator = "; ") { eachCookie ->
                                eachCookie
                            }
                        header(HttpHeaders.Cookie, appendCookie)
                    }
                }
            }
        }

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String?,
        cpn: String?,
    ) = httpClient.post("player") {
        ytClient(client, setLogin = true)
        setBody(
            PlayerBody(
                context = client.toContext(locale, visitorData).let {
                    if (client == YouTubeClient.TVHTML5) {
                        it.copy(
                            thirdParty = Context.ThirdParty(
                                embedUrl = "https://www.youtube.com/watch?v=${videoId}"
                            )
                        )
                    } else it
                },
                videoId = videoId,
                playlistId = playlistId,
                cpn = cpn
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient(OkHttp) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
            xml(
                format = XML {
                    xmlDeclMode = XmlDeclMode.Charset
                    autoPolymorphic = true
                },
                contentType = ContentType.Text.Xml
            )
        }

        install(ContentEncoding) {
            brotli(1.0F)
            gzip(0.9F)
            deflate(0.8F)
        }

        if (proxy != null) {
            engine {
                proxy = this@YtMusic.proxy
            }
        }

        defaultRequest {
            url("https://music.youtube.com/youtubei/v1/")
        }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient, setLogin: Boolean = false) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append(
                "X-YouTube-Client-Name",
                if (client != YouTubeClient.NOTIFICATION_CLIENT) client.clientName else "1"
            )
            append("X-YouTube-Client-Version", client.clientVersion)
            append(
                "x-origin",
                if (client != YouTubeClient.NOTIFICATION_CLIENT) "https://music.youtube.com" else "https://www.youtube.com"
            )
            append("X-Goog-Visitor-Id", visitorData)
            if (client == YouTubeClient.NOTIFICATION_CLIENT) {
                append("X-Youtube-Bootstrap-Logged-In", "true")
                append("X-Goog-Authuser", "0")
                append("Origin", "https://www.youtube.com")

            }
            if (client.referer != null) {
                append("Referer", client.referer)
            }
            if (setLogin) {
                cookie?.let { cookie ->
                    append("Cookie", cookie)
                    if ("SAPISID" !in cookieMap) return@let
                    val currentTime = System.currentTimeMillis() / 1000
                    val keyValue = cookieMap["SAPISID"] ?: cookieMap["__Secure-3PAPISID"]
                    println("keyValue: $keyValue")
                    val sapisidHash =
                        if (client != YouTubeClient.NOTIFICATION_CLIENT) sha1("$currentTime ${keyValue} https://music.youtube.com")
                        else sha1("$currentTime ${keyValue} https://www.youtube.com")
                    append("Authorization", "SAPISIDHASH ${currentTime}_${sapisidHash}")
                }
            }
        }
        userAgent(client.userAgent)
        parameter("key", client.api_key)
        parameter("prettyPrint", false)
    }

    suspend fun pipedStreams(videoId: String, pipedInstance: String) =
        httpClient.get("https://${pipedInstance}/streams/${videoId}") {
            contentType(ContentType.Application.Json)
        }
}