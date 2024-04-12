package com.example.mymusic.base.common

import com.example.mymusic.R

object Config {
    const val RECOVER_TRACK_QUEUE = "RECOVER_TRACK_QUEUE"
    const val SONG_CLICK = "SONG_CLICK"
    const val SELECTED_LANGUAGE = "selected_language"
    const val VIDEO_CLICK = "VIDEO_CLICK"
    const val SHARE = "SHARE"
    const val PLAYLIST_CLICK = "PLAYLIST_CLICK"
    const val ALBUM_CLICK = "ALBUM_CLICK"
    const val MINIPLAYER_CLICK = "MINIPLAYER_CLICK"
}

object SPONSOR_BLOCK {
    val list: Array<CharSequence> = arrayOf(
        "sponsor",
        "selfpromo",
        "interaction",
        "intro",
        "outro",
        "preview",
        "music_offtopic",
        "poi_highlight",
        "filler"
    )
    val listName: Array<Int> = arrayOf(
        R.string.sponsor,
        R.string.self_promotion,
        R.string.interaction,
        R.string.intro,
        R.string.outro,
        R.string.preview,
        R.string.music_off_topic,
        R.string.poi_highlight,
        R.string.filler
    )
}

object MEDIA_CUSTOM_COMMAND {
    const val LIKE = "like"
    const val REPEAT = "repeat"
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

object SUPPORTED_LOCATION {
    val items: Array<CharSequence> = arrayOf(
        "AE",
        "AR",
        "AT",
        "AU",
        "AZ",
        "BA",
        "BD",
        "BE",
        "BG",
        "BH",
        "BO",
        "BR",
        "BY",
        "CA",
        "CH",
        "CL",
        "CO",
        "CR",
        "CY",
        "CZ",
        "DE",
        "DK",
        "DO",
        "DZ",
        "EC",
        "EE",
        "EG",
        "ES",
        "FI",
        "FR",
        "GB",
        "GE",
        "GH",
        "GR",
        "GT",
        "HK",
        "HN",
        "HR",
        "HU",
        "ID",
        "IE",
        "IL",
        "IN",
        "IQ",
        "IS",
        "IT",
        "JM",
        "JO",
        "JP",
        "KE",
        "KH",
        "KR",
        "KW",
        "KZ",
        "LA",
        "LB",
        "LI",
        "LK",
        "LT",
        "LU",
        "LV",
        "LY",
        "MA",
        "ME",
        "MK",
        "MT",
        "MX",
        "MY",
        "NG",
        "NI",
        "NL",
        "NO",
        "NP",
        "NZ",
        "OM",
        "PA",
        "PE",
        "PG",
        "PH",
        "PK",
        "PL",
        "PR",
        "PT",
        "PY",
        "QA",
        "RO",
        "RS",
        "RU",
        "SA",
        "SE",
        "SG",
        "SI",
        "SK",
        "SN",
        "SV",
        "TH",
        "TN",
        "TR",
        "TW",
        "TZ",
        "UA",
        "UG",
        "US",
        "UY",
        "VE",
        "VN",
        "YE",
        "ZA",
        "ZW"
    )
}

object DownloadState {
    const val STATE_NOT_DOWNLOADED = 0
    const val STATE_PREPARING = 1
    const val STATE_DOWNLOADING = 2
    const val STATE_DOWNLOADED = 3
}

const val DB_NAME = "Music Database"
const val FIRST_TIME_MIGRATION = "first_time_migration"
const val STATUS_DONE = "status_done"

