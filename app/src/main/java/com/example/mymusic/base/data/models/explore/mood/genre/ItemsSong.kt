package com.example.mymusic.base.data.models.explore.mood.genre

import com.example.mymusic.base.data.models.searchResult.songs.Artist


data class ItemsSong(
    val title: String,
    val artist: List<Artist>?,
    val videoId: String,
)
