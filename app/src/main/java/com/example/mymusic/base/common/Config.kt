package com.example.mymusic.base.common

object Config{
    const val RECOVER_TRACK_QUEUE = "RECOVER_TRACK_QUEUE"
    const val SONG_CLICK = "SONG_CLICK"
    const val SELECTED_LANGUAGE = "selected_language"
    const val VIDEO_CLICK = "VIDEO_CLICK"
    const val SHARE = "SHARE"
    const val PLAYLIST_CLICK = "PLAYLIST_CLICK"
    const val ALBUM_CLICK = "ALBUM_CLICK"
}
object SUPPORTED_LANGUAGE {
    val items: Array<CharSequence> = arrayOf(
        "English",
        "Tiếng Việt",
        "Italiano",
        "Deutsch",
        "Русский",
        "Türkçe",
        "Suomi",
        "Polski",
        "Português",
        "Français",
        "Español",
        "简体中文",
        "Bahasa Indonesia"
    )
    val codes: Array<String> = arrayOf(
        "en-US",
        "vi-VN",
        "it-IT",
        "de-DE",
        "ru-RU",
        "tr-TR",
        "fi-FI",
        "pl-PL",
        "pt-PT",
        "fr-FR",
        "es-ES",
        "zh-CN",
        "in-ID"
    )
}

object MEDIA_NOTIFICATION {
    const val NOTIFICATION_ID = 200
    const val NOTIFICATION_CHANNEL_NAME = "MyMusic Playback Notification"
    const val NOTIFICATION_CHANNEL_ID = "MyMusic Playback Notification ID"
}

object QUALITY {
    val items: Array<CharSequence> = arrayOf("Low - 66kps", "High - 129kps")
    val itags: Array<Int> = arrayOf(250, 251)
}

object VIDEO_QUALITY {
    val items: Array<CharSequence> = arrayOf("720p", "360p")
    val itags: Array<Int> = arrayOf(22, 18)
}

object DownloadState {
    const val STATE_NOT_DOWNLOADED = 0
    const val STATE_PREPARING = 1
    const val STATE_DOWNLOADING = 2
    const val STATE_DOWNLOADED = 3
}
const val DB_NAME = "Music Database"

