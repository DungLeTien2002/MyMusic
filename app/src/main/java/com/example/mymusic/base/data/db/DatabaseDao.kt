package com.example.mymusic.base.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity

import java.time.LocalDateTime

@Dao
interface DatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewFormat(format: NewFormatEntity)

    @Query("SELECT * FROM song WHERE videoId = :videoId")
    suspend fun getSong(videoId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity): Long

    @Query("UPDATE song SET inLibrary = :inLibrary WHERE videoId = :videoId")
    suspend fun updateSongInLibrary(inLibrary: LocalDateTime, videoId: String)

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + 1 WHERE videoId = :videoId")
    suspend fun updateTotalPlayTime(videoId: String)

    @Query("UPDATE song SET durationSeconds = :durationSeconds WHERE videoId = :videoId")
    suspend fun updateDurationSeconds(durationSeconds: Int, videoId: String)

    @Query("SELECT * FROM lyrics WHERE videoId = :videoId")
    suspend fun getLyrics(videoId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM queue")
    suspend fun deleteQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongInfo(songInfo: SongInfoEntity)

    @Query("SELECT * FROM song_info WHERE videoId = :videoId")
    suspend fun getSongInfo(videoId: String): SongInfoEntity?

    @Query("SELECT * FROM queue")
    suspend fun getQueue(): List<QueueEntity>?
}