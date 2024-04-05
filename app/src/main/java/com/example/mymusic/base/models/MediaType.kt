package com.example.mymusic.base.models

sealed class MediaType {
    data object Song : MediaType()
    data object Video : MediaType()
}