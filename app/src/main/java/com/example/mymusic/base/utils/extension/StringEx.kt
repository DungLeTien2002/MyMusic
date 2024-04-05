package com.example.mymusic.base.utils.extension

import androidx.sqlite.db.SimpleSQLiteQuery


fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)