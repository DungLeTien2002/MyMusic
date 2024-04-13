package com.example.mymusic.base.models

data class AccountInfo(
    val name: String,
    val email: String,
    val thumbnails: List<Thumbnail>
)
