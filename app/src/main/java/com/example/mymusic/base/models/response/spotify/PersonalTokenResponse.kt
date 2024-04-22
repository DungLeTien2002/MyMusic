package com.example.mymusic.base.models.response.spotify

import kotlinx.serialization.Serializable

@Serializable
data class PersonalTokenResponse(
    val clientId: String,
    val accessToken: String,
    val accessTokenExpirationTimestampMs: Long,
    val isAnonymous: Boolean
)
