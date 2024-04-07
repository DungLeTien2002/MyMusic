package com.example.mymusic.base.pages

import com.example.mymusic.base.models.PlaylistItem
import com.example.mymusic.base.models.VideoItem

data class ExplorePage(
    val released: List<PlaylistItem>,
    val musicVideo: List<VideoItem>,
)
