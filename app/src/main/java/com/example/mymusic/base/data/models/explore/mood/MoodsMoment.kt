package com.example.mymusic.base.data.models.explore.mood


import com.google.gson.annotations.SerializedName

data class MoodsMoment(
    @SerializedName("params")
    val params: String,
    @SerializedName("title")
    val title: String
)