package com.example.mymusic.base.data.models.searchResult.playlists


import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName

data class PlaylistsResult(
    @SerializedName("author")
    val author: String,
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("itemCount")
    val itemCount: String,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String
)