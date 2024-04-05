package com.example.mymusic.base

import com.example.mymusic.base.encoder.brotli
import com.example.mymusic.base.models.Context
import com.example.mymusic.base.models.YouTubeClient
import com.example.mymusic.base.models.YouTubeLocale
import com.example.mymusic.base.models.body.PlayerBody
import com.example.mymusic.base.utils.extension.parseCookieString
import com.example.mymusic.base.utils.extension.sha1
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.serialization.XML
import java.net.Proxy
import java.util.Locale

class YtMusic {
    private var httpClient = createClient()
    var visitorData: String = "Cgt6SUNYVzB2VkJDbyjGrrSmBg%3D%3D"
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