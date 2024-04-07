package com.example.mymusic.base.pages

import com.example.mymusic.base.models.Album
import com.example.mymusic.base.models.Artist
import com.example.mymusic.base.models.BrowseEndpoint
import com.example.mymusic.base.models.PlaylistPanelVideoRenderer
import com.example.mymusic.base.models.SongItem
import com.example.mymusic.base.models.WatchEndpoint
import com.example.mymusic.base.models.oddElements
import com.example.mymusic.base.models.splitBySeparator
import com.example.mymusic.base.utils.extension.parseTime


data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator() ?: return null
        return SongItem(
            id = renderer.videoId ?: return null,
            title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
            artists = longByLineRuns.firstOrNull()?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: return null,
            album = longByLineRuns.getOrNull(1)?.firstOrNull()?.takeIf {
                it.navigationEndpoint?.browseEndpoint != null
            }?.let {
                Album(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                )
            },
            duration = renderer.lengthText?.runs?.firstOrNull()?.text?.parseTime() ?: return null,
            thumbnail = renderer.thumbnail.thumbnails.lastOrNull()?.url ?: return null,
            explicit = renderer.badges?.find {
                it.musicInlineBadgeRenderer.icon.iconType == "MUSIC_EXPLICIT_BADGE"
            } != null,
            thumbnails = renderer.thumbnail
        )
    }
}
