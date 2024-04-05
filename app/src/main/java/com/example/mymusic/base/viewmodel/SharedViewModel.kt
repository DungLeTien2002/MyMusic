package com.example.mymusic.base.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class SharedViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _recreateActivity: MutableLiveData<Boolean> = MutableLiveData()
    val recreatedActivity: LiveData<Boolean> = _recreateActivity

    var simpleMediaServiceHandler: SimpleMediaServiceHandler? = null

    private val _progress = MutableStateFlow<Float>(0F)
    val progress:SharedFlow<Float> = _progress.asSharedFlow()

    var isPlaying=MutableStateFlow<Boolean>(false)

    private val _liked:MutableStateFlow<Boolean> = MutableStateFlow(false)
    val liked=_liked.asSharedFlow()

    fun activityRecreateDone(){
        _recreateActivity.value=false
    }
    val isServiceRunning = MutableLiveData<Boolean>(false)
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    fun init(){

    }

}