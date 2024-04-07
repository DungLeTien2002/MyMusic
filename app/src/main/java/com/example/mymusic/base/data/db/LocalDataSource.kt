package com.example.mymusic.base.data.db

import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
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
}