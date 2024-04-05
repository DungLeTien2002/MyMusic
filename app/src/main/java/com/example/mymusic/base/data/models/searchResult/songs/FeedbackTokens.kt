package com.example.mymusic.base.data.models.searchResult.songs


import com.google.gson.annotations.SerializedName

data class FeedbackTokens(
    @SerializedName("add")
    val add: Any,
    @SerializedName("remove")
    val remove: Any
)