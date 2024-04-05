package com.example.mymusic.base.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mymusic.base.data.models.browse.album.Track

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey(autoGenerate = false)
    val queueId: Long = 0,
    val listTrack: List<Track>
)