package com.maxrave.simpmusic.data.model.explore.mood.genre


import com.example.mymusic.base.data.models.explore.mood.genre.ItemsSong
import com.google.gson.annotations.SerializedName

data class GenreObject(
    @SerializedName("header")
    val header: String,
    @SerializedName("itemsPlaylist")
    val itemsPlaylist: List<ItemsPlaylist>,
    @SerializedName("itemsSong")
    val itemsSong: List<ItemsSong>?
)