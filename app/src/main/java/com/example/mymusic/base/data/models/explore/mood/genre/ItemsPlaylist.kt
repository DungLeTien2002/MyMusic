package com.maxrave.simpmusic.data.model.explore.mood.genre


import com.example.mymusic.base.data.models.explore.mood.genre.Content
import com.google.gson.annotations.SerializedName

data class ItemsPlaylist(
    @SerializedName("contents")
    val contents: List<Content>,
    @SerializedName("header")
    val header: String,
    @SerializedName("type")
    val type: String
)