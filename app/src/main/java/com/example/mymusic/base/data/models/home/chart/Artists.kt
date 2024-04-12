package com.example.mymusic.base.data.models.home.chart


import com.google.gson.annotations.SerializedName

data class Artists(
    @SerializedName("items")
    val itemArtists: ArrayList<ItemArtist>,
    @SerializedName("playlist")
    val playlist: Any
)