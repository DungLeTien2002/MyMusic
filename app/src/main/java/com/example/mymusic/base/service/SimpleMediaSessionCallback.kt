package com.example.mymusic.base.service

import androidx.media3.session.MediaLibraryService
import javax.inject.Inject

class SimpleMediaSessionCallback @Inject constructor() :
    MediaLibraryService.MediaLibrarySession.Callback {
}