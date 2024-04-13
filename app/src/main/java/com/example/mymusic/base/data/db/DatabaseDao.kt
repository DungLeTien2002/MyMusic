package com.example.mymusic.base.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.mymusic.base.data.db.entities.AlbumEntity
import com.example.mymusic.base.data.db.entities.GoogleAccountEntity
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.PairSongLocalPlaylist
import com.example.mymusic.base.data.db.entities.PlaylistEntity
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SetVideoIdEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity
import com.example.mymusic.base.utils.extension.toSQLiteQuery

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

    @Query("SELECT * FROM song WHERE downloadState = 3")
    suspend fun getDownloadedSongs(): List<SongEntity>

    @Query("UPDATE song SET downloadState = :downloadState WHERE videoId = :videoId")
    suspend fun updateDownloadState(downloadState: Int, videoId: String)

    @Query("SELECT * FROM album WHERE downloadState = 3")
    suspend fun getDownloadedAlbums(): List<AlbumEntity>

    @Query("SELECT * FROM playlist WHERE downloadState = 3")
    suspend fun getDownloadedPlaylists(): List<PlaylistEntity>

    @Query("UPDATE album SET downloadState = :downloadState WHERE browseId = :browseId")
    suspend fun updateAlbumDownloadState(downloadState: Int, browseId: String)

    @Query("UPDATE playlist SET downloadState = :downloadState WHERE id = :playlistId")
    suspend fun updatePlaylistDownloadState(downloadState: Int, playlistId: String)

    @Query("UPDATE local_playlist SET downloadState = :downloadState WHERE id = :id")
    suspend fun updateLocalPlaylistDownloadState(downloadState: Int, id: Long)

    @Query("SELECT * FROM song WHERE downloadState = 1 OR downloadState = 2")
    suspend fun getDownloadingSongs(): List<SongEntity>?

    @Query("UPDATE song SET liked = :liked WHERE videoId = :videoId")
    suspend fun updateLiked(liked: Int, videoId: String)

    @Query("SELECT * FROM song WHERE downloadState = 1")
    suspend fun getPreparingSongs(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recoverQueue(queue: QueueEntity)

    @Query("SELECT * FROM local_playlist")
    suspend fun getAllLocalPlaylists(): List<LocalPlaylistEntity>

    @Query("SELECT * FROM song WHERE videoId IN (:primaryKeyList)")
    fun getSongByListVideoId(primaryKeyList: List<String>): List<SongEntity>

    @Query("UPDATE local_playlist SET tracks = :tracks WHERE id = :id")
    suspend fun updateLocalPlaylistTracks(tracks: List<String>, id: Long)

    @Query("UPDATE local_playlist SET youtube_sync_state = :state WHERE id = :id")
    suspend fun updateLocalPlaylistYouTubePlaylistSyncState(id: Long, state: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetVideoId(setVideoIdEntity: SetVideoIdEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist)

    @Query("SELECT * FROM song ORDER BY inLibrary DESC LIMIT :limit OFFSET :offset")
    suspend fun getRecentSongs(limit: Int, offset: Int): List<SongEntity>

    @Transaction
    suspend fun getAllDownloadedPlaylist(): List<Any> {
        val a = mutableListOf<Any>()
        a.addAll(getDownloadedAlbums())
        a.addAll(getDownloadedPlaylists())
        val sortedList = a.sortedWith<Any>(
            Comparator { p0, p1 ->
                val timeP0: LocalDateTime? = when (p0) {
                    is AlbumEntity -> p0.inLibrary
                    is PlaylistEntity -> p0.inLibrary
                    else -> null
                }
                val timeP1: LocalDateTime? = when (p1) {
                    is AlbumEntity -> p1.inLibrary
                    is PlaylistEntity -> p1.inLibrary
                    else -> null
                }
                if (timeP0 == null || timeP1 == null) {
                    return@Comparator if (timeP0 == null && timeP1 == null) 0 else if (timeP0 == null) -1 else 1
                }
                timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
            }
        )
        return sortedList
    }
    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw("pragma wal_checkpoint(full)".toSQLiteQuery())
    }

    @Query("SELECT * FROM googleaccountentity")
    suspend fun getAllGoogleAccount(): List<GoogleAccountEntity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoogleAccount(googleAccountEntity: GoogleAccountEntity)

    @Query("UPDATE googleaccountentity SET isUsed = :isUsed WHERE email = :email")
    suspend fun updateGoogleAccountUsed(isUsed: Boolean, email: String)

    @Query("DELETE FROM googleaccountentity WHERE email = :email")
    suspend fun deleteGoogleAccount(email: String)
}