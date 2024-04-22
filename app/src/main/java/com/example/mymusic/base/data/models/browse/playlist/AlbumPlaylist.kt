package com.example.mymusic.base.data.models.browse.playlist


import com.google.gson.annotations.SerializedName

data class AlbumPlaylist(
    @SerializedName("id")
    val id: Any,
    @SerializedName("name")
    val name: String
)