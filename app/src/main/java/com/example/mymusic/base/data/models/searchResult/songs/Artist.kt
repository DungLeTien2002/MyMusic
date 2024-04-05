package com.example.mymusic.base.data.models.searchResult.songs


import com.google.gson.annotations.SerializedName

data class Artist(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String
)