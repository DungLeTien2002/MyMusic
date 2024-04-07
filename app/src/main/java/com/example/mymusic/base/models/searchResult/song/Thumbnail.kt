package com.example.mymusic.base.models.searchResult.song


import com.google.gson.annotations.SerializedName

data class Thumbnail(
    @SerializedName("height")
    val height: Int,
    @SerializedName("url")
    val url: String,
    @SerializedName("width")
    val width: Int
)