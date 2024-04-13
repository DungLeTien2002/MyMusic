package com.example.mymusic.base.utils.extension

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.NavController
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.home.Content
import com.example.mymusic.base.data.models.metadata.Line
import com.example.mymusic.base.data.models.metadata.Lyrics
import com.example.mymusic.base.data.models.musixmatch.MusixmatchTranslationLyricsResponse
import com.example.mymusic.base.data.models.searchResult.songs.Album
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.models.SongItem
import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.example.mymusic.base.parser.toListThumbnail
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()
fun SongEntity.toTrack(): Track {
    val listArtist = mutableListOf<Artist>()
    if (this.artistName != null) {
        for (i in 0 until this.artistName.size) {
            listArtist.add(Artist(this.artistId?.get(i) ?: "", this.artistName[i]))
        }
    }
    return Track(
        album = this.albumId?.let { this.albumName?.let { it1 -> Album(it, it1) } },
        artists = listArtist,
        duration = this.duration,
        durationSeconds = this.durationSeconds,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus,
        thumbnails = listOf(
            Thumbnail(
                720,
                this.thumbnails ?: "",
                1080
            )
        ),
        title = this.title,
        videoId = this.videoId,
        videoType = this.videoType,
        category = this.category,
        feedbackTokens = null,
        resultType = null,
        year = ""
    )
}

fun List<Artist>?.toListId(): List<String> {
    val list = mutableListOf<String>()
    if (this != null) {
        for (item in this) {
            list.add(item.id ?: "")
        }
    }
    return list
}

fun List<Artist>?.toListName(): List<String> {
    val list = mutableListOf<String>()
    if (this != null) {
        for (item in this) {
            list.add(item.name)
        }
    }
    return list
}

fun Track.toSongEntity(): SongEntity {
    return SongEntity(
        videoId = this.videoId,
        albumId = this.album?.id,
        albumName = this.album?.name,
        artistId = this.artists?.toListId(),
        artistName = this.artists?.toListName(),
        duration = this.duration ?: "",
        durationSeconds = this.durationSeconds ?: 0,
        isAvailable = this.isAvailable,
        isExplicit = this.isExplicit,
        likeStatus = this.likeStatus ?: "",
        thumbnails = this.thumbnails?.last()?.url?.let {
            if (it.contains("w120")) {
                return@let Regex("([wh])120").replace(it, "$1544")
            } else if (it.contains("sddefault")) {
                return@let it.replace("sddefault", "maxresdefault")
            } else {
                return@let it
            }
        },
        title = this.title,
        videoType = this.videoType ?: "",
        category = this.category,
        resultType = this.resultType,
        liked = false,
        totalPlayTime = 0,
        downloadState = 0
    )
}

fun LyricsEntity.toLyrics(): Lyrics {
    return Lyrics(
        error = this.error, lines = this.lines, syncType = this.syncType
    )
}

fun com.maxrave.kotlinytmusicscraper.models.lyrics.Lyrics.toLyrics(): Lyrics {
    val lines: ArrayList<Line> = arrayListOf()
    if (this.lyrics != null) {
        this.lyrics.lines?.forEach {
            lines.add(
                Line(
                    endTimeMs = it.endTimeMs,
                    startTimeMs = it.startTimeMs,
                    syllables = it.syllables ?: listOf(),
                    words = it.words
                )
            )
        }
        return Lyrics(
            error = false,
            lines = lines,
            syncType = this.lyrics.syncType
        )
    } else {
        return Lyrics(
            error = true,
            lines = null,
            syncType = null
        )
    }
}

fun Lyrics.toLyricsEntity(videoId: String): LyricsEntity {
    return LyricsEntity(
        videoId = videoId, error = this.error, lines = this.lines, syncType = this.syncType
    )
}

fun MusixmatchTranslationLyricsResponse.toLyrics(originalLyrics: Lyrics): Lyrics? {
    if (this.message.body.translations_list.isEmpty()) {
        return null
    } else {
        val listTranslation = this.message.body.translations_list
        val translation = originalLyrics.copy(
            lines = originalLyrics.lines?.mapIndexed { index, line ->
                line.copy(
                    words = if (!line.words.contains("♫")) {
                        listTranslation.find { it.translation.matched_line == line.words || it.translation.subtitle_matched_line == line.words || it.translation.snippet == line.words }?.translation?.description
                            ?: ""
                    } else {
                        line.words
                    }
                )
            }
        )
        return translation
    }
}

fun List<String>.connectArtists(): String {
    val stringBuilder = StringBuilder()

    for ((index, artist) in this.withIndex()) {
        stringBuilder.append(artist)

        if (index < this.size - 1) {
            stringBuilder.append(", ")
        }
    }

    return stringBuilder.toString()
}

fun NavController.navigateSafe(resId: Int, bundle: Bundle? = null) {
    if (currentDestination?.id != resId) {
        if (bundle != null) {
            navigate(resId, bundle)
        } else {
            navigate(resId)
        }
    }
}

fun List<SongItem>?.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    if (this != null) {
        for (item in this) {
            listTrack.add(item.toTrack())
        }
    }
    return listTrack
}

fun SongItem.toTrack(): Track {
    return Track(
        album = this.album.let { Album(it?.id ?: "", it?.name ?: "") },
        artists = this.artists.map { artist -> Artist(id = artist.id ?: "", name = artist.name) },
        duration = this.duration.toString(),
        durationSeconds = this.duration,
        isAvailable = false,
        isExplicit = this.explicit,
        likeStatus = null,
        thumbnails = this.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = this.title,
        videoId = this.id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null
    )
}

fun Content.toTrack(): Track {
    return Track(
        album = album,
        artists = artists ?: listOf(Artist("", "")),
        duration = "",
        durationSeconds = durationSeconds,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbnails,
        title = title,
        videoId = videoId!!,
        videoType = "",
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = ""
    )
}

fun setEnabledAll(v: View, enabled: Boolean) {
    v.isEnabled = enabled
    v.isFocusable = enabled
    if (v is ImageButton) {
        if (enabled) v.setColorFilter(Color.WHITE) else v.setColorFilter(Color.GRAY)
    }
    if (v is TextView) {
        v.isEnabled = enabled
    }
    if (v is ViewGroup) {
        val vg = v
        for (i in 0 until vg.childCount) setEnabledAll(vg.getChildAt(i), enabled)
    }
}

fun ArrayList<String>.removeConflicts(): ArrayList<String> {
    val nonConflictingSet = HashSet<String>()
    val nonConflictingList = ArrayList<String>()

    for (item in this) {
        if (nonConflictingSet.add(item)) {
            nonConflictingList.add(item)
        }
    }

    return nonConflictingList
}

operator fun File.div(child: String): File = File(this, child)
fun InputStream.zipInputStream(): ZipInputStream = ZipInputStream(this)
fun OutputStream.zipOutputStream(): ZipOutputStream = ZipOutputStream(this)