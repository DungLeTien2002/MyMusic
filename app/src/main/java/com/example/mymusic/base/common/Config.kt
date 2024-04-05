package com.example.mymusic.base.common

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

