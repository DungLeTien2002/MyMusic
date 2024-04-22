package com.example.mymusic.base.models.body.spotify

import kotlinx.serialization.Serializable

@Serializable
data class CanvasBody(
    val tracks: List<CanvasBody.Track>
) {
    @Serializable
    data class Track(
        val track_uri: String,
    )
}

