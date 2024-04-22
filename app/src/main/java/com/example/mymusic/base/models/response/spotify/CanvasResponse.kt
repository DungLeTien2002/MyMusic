package com.example.mymusic.base.models.response.spotify

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
data class CanvasResponse(
    @ProtoNumber(1)
    val canvases: List<Canvas>
) {
    @Serializable
    data class Canvas(
        @ProtoNumber(1)
        val id: String,
        @ProtoNumber(2)
        val canvas_url: String,
        @ProtoNumber(5)
        val track_uri: String,
        @ProtoNumber(6)
        val artist: Artist,
        @ProtoNumber(9)
        val other_id: String,
        @ProtoNumber(11)
        val canvas_uri: String
    ) {
        @Serializable
        data class Artist(
            @ProtoNumber(1)
            val artist_uri: String,
            @ProtoNumber(2)
            val artist_name: String,
            @ProtoNumber(3)
            val artist_img_url: String
        )
    }
}

