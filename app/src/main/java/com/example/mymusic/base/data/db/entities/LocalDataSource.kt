package com.example.mymusic.base.data.db.entities

import com.example.mymusic.base.data.db.DatabaseDao
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val databaseDao: DatabaseDao) {
    suspend fun insertNewFormat(format: NewFormatEntity) = databaseDao.insertNewFormat(format)
}