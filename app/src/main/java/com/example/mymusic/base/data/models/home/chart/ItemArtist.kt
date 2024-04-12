package com.example.mymusic.base.data.models.home.chart


import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName

data class ItemArtist(
    @SerializedName("browseId")
    val browseId: String,
    @SerializedName("rank")
    val rank: String,
    @SerializedName("subscribers")
    val subscribers: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("trend")
    val trend: String
)