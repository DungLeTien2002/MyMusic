package com.example.mymusic.base.service

import android.content.Context
import androidx.media3.session.MediaLibraryService
import com.example.mymusic.base.data.repository.MainRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SimpleMediaSessionCallback @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mainRepository: MainRepository,
) : MediaLibraryService.MediaLibrarySession.Callback {

}