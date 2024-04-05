package com.example.mymusic.base.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.mymusic.base.data.db.entities.NewFormatEntity

@Dao
interface DatabaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewFormat(format: NewFormatEntity)
}