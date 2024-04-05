package com.example.mymusic.base.data.models.metadata


import com.google.gson.annotations.SerializedName

data class Line(
    @SerializedName("endTimeMs")
    val endTimeMs: String,
    @SerializedName("startTimeMs")
    val startTimeMs: String,
    @SerializedName("syllables")
    val syllables: List<Any>,
    @SerializedName("words")
    val words: String
)