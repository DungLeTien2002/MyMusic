package com.example.mymusic.base.models.body

import com.example.mymusic.base.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context,
    val videoId: String,
    val playlistId: String?,
    val cpn: String?,
    val param: String? = "8AUB",
)
