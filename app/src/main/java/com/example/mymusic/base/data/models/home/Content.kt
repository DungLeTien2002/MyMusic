package com.example.mymusic.base.data.models.home


import com.example.mymusic.base.data.models.searchResult.songs.Album
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName

data class Content(
    @SerializedName("album")
    val album: Album?,
    @SerializedName("artists")
    val artists: List<Artist>?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("isExplicit")
    val isExplicit: Boolean?,
    @SerializedName("playlistId")
    val playlistId: String?,
    @SerializedName("browseId")
    val browseId: String?,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String?,
    @SerializedName("views")
    val views: String?,
    @SerializedName("durationSeconds")
    val durationSeconds: Int? = null,
    val radio: String? = null,
)
