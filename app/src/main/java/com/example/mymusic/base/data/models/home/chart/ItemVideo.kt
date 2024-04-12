package com.example.mymusic.base.data.models.home.chart


import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName

data class ItemVideo(
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("playlistId")
    val playlistId: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("views")
    val views: String
)
fun ItemVideo.toTrack(): Track {
    return Track(
        album = null,
        artists = artists,
        duration = "",
        durationSeconds = 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails = thumbnails,
        title = title,
        videoId = videoId,
        videoType = "",
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = ""

    )
}