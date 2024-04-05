package com.example.mymusic.base.utils.extension

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

const val SETTINGS_FILENAME = "settings"
val Context.dataStore by preferencesDataStore(name = SETTINGS_FILENAME)