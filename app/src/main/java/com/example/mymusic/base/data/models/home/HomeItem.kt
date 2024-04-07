package com.example.mymusic.base.data.models.home


import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName

data class HomeItem(
    @SerializedName("contents")
    val contents: List<Content?>,
    @SerializedName("title")
    val title: String,
    val subtitle: String? = null,
    val thumbnail: List<Thumbnail>? = null,
    val channelId: String? = null,
)