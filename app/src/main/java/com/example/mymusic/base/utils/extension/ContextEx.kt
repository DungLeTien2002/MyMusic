package com.example.mymusic.base.utils.extension

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

const val SETTINGS_FILENAME = "settings"
val Context.dataStore by preferencesDataStore(name = SETTINGS_FILENAME)
fun Context.isMyServiceRunning(serviceClass: Class<out Service>) = try {
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
} catch (e: Exception) {
    false
}
