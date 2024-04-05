package com.example.mymusic.base.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mymusic.base.data.db.entities.AlbumEntity
import com.example.mymusic.base.data.db.entities.ArtistEntity
import com.example.mymusic.base.data.db.entities.GoogleAccountEntity
import com.example.mymusic.base.data.db.entities.LocalPlaylistEntity
import com.example.mymusic.base.data.db.entities.LyricsEntity
import com.example.mymusic.base.data.db.entities.NewFormatEntity
import com.example.mymusic.base.data.db.entities.PairSongLocalPlaylist
import com.example.mymusic.base.data.db.entities.PlaylistEntity
import com.example.mymusic.base.data.db.entities.QueueEntity
import com.example.mymusic.base.data.db.entities.SearchHistory
import com.example.mymusic.base.data.db.entities.SetVideoIdEntity
import com.example.mymusic.base.data.db.entities.SongEntity
import com.example.mymusic.base.data.db.entities.SongInfoEntity

@Database(
    entities = [NewFormatEntity::class, SongInfoEntity::class, SearchHistory::class, SongEntity::class, ArtistEntity::class, AlbumEntity::class, PlaylistEntity::class, LocalPlaylistEntity::class, LyricsEntity::class, QueueEntity::class, SetVideoIdEntity::class, PairSongLocalPlaylist::class, GoogleAccountEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun getDatabaseDao(): DatabaseDao
}