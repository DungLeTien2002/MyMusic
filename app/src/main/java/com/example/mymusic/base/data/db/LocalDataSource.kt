package com.example.mymusic.base.data.db

import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.PairSongLocalPlaylist
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SetVideoIdEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity
import java.time.LocalDateTime
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val databaseDao: DatabaseDao) {
    suspend fun insertNewFormat(format: NewFormatEntity) = databaseDao.insertNewFormat(format)
    suspend fun getSong(videoId: String) = databaseDao.getSong(videoId)

    suspend fun insertSong(song: SongEntity) = databaseDao.insertSong(song)
    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String) =
        databaseDao.updateSongInLibrary(inLibrary, videoId)

    suspend fun updateListenCount(videoId: String) = databaseDao.updateTotalPlayTime(videoId)
    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String) =
        databaseDao.updateDurationSeconds(durationSeconds, videoId)

    suspend fun getSavedLyrics(videoId: String) = databaseDao.getLyrics(videoId)
    suspend fun insertLyrics(lyrics: LyricsEntity) = databaseDao.insertLyrics(lyrics)
    suspend fun deleteQueue() = databaseDao.deleteQueue()
    suspend fun insertSongInfo(songInfo: SongInfoEntity) = databaseDao.insertSongInfo(songInfo)
    suspend fun getSongInfo(videoId: String) = databaseDao.getSongInfo(videoId)
    suspend fun getQueue() = databaseDao.getQueue()
    suspend fun getDownloadedSongs() = databaseDao.getDownloadedSongs()
    suspend fun updateDownloadState(downloadState: Int, videoId: String) = databaseDao.updateDownloadState(downloadState, videoId)
    suspend fun getAllDownloadedPlaylist() = databaseDao.getAllDownloadedPlaylist()
    suspend fun updateAlbumDownloadState(downloadState: Int, albumId: String) = databaseDao.updateAlbumDownloadState(downloadState, albumId)
    suspend fun updatePlaylistDownloadState(downloadState: Int, playlistId: String) = databaseDao.updatePlaylistDownloadState(downloadState, playlistId)
    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long) =
        databaseDao.updateLocalPlaylistDownloadState(downloadState, id)
    suspend fun getDownloadingSongs() = databaseDao.getDownloadingSongs()
    suspend fun getPreparingSongs() = databaseDao.getPreparingSongs()
    suspend fun recoverQueue(queueEntity: QueueEntity) = databaseDao.recoverQueue(queueEntity)
    suspend fun updateLiked(liked: Int, videoId: String) = databaseDao.updateLiked(liked, videoId)
    suspend fun getAllLocalPlaylists() = databaseDao.getAllLocalPlaylists()
    suspend fun getSongByListVideoId(primaryKeyList: List<String>) = databaseDao.getSongByListVideoId(primaryKeyList)
    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long) = databaseDao.updateLocalPlaylistTracks(tracks, id)
    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, syncState: Int) =
        databaseDao.updateLocalPlaylistYouTubePlaylistSyncState(id, syncState)
    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity) =
        databaseDao.insertSetVideoId(setVideoIdEntity)
    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) =
        databaseDao.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
}