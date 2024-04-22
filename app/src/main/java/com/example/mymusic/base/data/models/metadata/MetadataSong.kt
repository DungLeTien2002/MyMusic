package com.example.mymusic.base.data.models.metadata


import com.example.mymusic.base.data.models.searchResult.songs.Album
import com.example.mymusic.base.data.models.searchResult.songs.Artist
import com.example.mymusic.base.models.searchResult.song.Thumbnail
import com.google.gson.annotations.SerializedName


data class MetadataSong(
    @SerializedName("album")
    val album: Album,
    @SerializedName("artists")
    val artists: List<Artist>,
    @SerializedName("duration")
    val duration: String,
    @SerializedName("duration_seconds")
    val durationSeconds: Int,
    @SerializedName("isExplicit")
    val isExplicit: Boolean,
    @SerializedName("lyrics")
    val lyrics: Lyrics,
    @SerializedName("resultType")
    val resultType: String,
    @SerializedName("thumbnails")
    val thumbnails: List<Thumbnail>,
    @SerializedName("title")
    val title: String,
    @SerializedName("videoId")
    val videoId: String,
    @SerializedName("videoType")
    val videoType: String,
    @SerializedName("year")
    val year: Any
)