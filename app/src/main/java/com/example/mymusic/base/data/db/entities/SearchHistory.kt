package com.example.mymusic.base.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "search_history")
data class SearchHistory (
    @PrimaryKey(autoGenerate = false)
    val query: String,
    )