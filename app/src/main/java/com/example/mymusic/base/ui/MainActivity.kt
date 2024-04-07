package com.example.mymusic.base.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.switchMap
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import coil.size.Size
import coil.transform.Transformation
import com.example.mymusic.R
import com.example.mymusic.base.baseview.BaseActivity
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.service.SimpleMediaService
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import com.example.mymusic.base.utils.extension.load
import com.example.mymusic.base.utils.extension.setTextAnimation
import com.example.mymusic.base.viewmodel.SharedViewModel
import com.example.mymusic.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@UnstableApi
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {
    //region variable
    private var action: String? = null
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
    val navController = navHostFragment?.findNavController()
    val viewModel by viewModels<SharedViewModel>()

    override fun initListener() {
        when (action) {
            "com.maxrave.simpmusic.action.HOME" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_home
            }

            "com.maxrave.simpmusic.action.SEARCH" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_search
            }

            "com.maxrave.simpmusic.action.LIBRARY" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_library
            }

            else -> {}
        }


        navController?.addOnDestinationChangedListener { nav, destination, _ ->
            when (destination.id) {
                R.id.bottom_navigation_item_home -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_home)?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_search -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_search)?.isChecked =
                        true
                }

                R.id.bottom_navigation_item_library -> {
                    binding.bottomNavigationView.menu.findItem(R.id.bottom_navigation_item_library)?.isChecked =
                        true
                }


                else -> {}
            }
        }
    }

    override fun initData() {
        if (viewModel.recreatedActivity.value == true) {
            viewModel.simpleMediaServiceHandler?.coroutineScope = lifecycleScope
            runCollect()
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }

    }

    override fun initView() {
    }

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }


    private fun startMusicService() {
        if (viewModel.recreatedActivity.value != true) {
            val intent = Intent(this, SimpleMediaServiceHandler::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning.value = true
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is SimpleMediaService.MusicBinder) {
                viewModel.simpleMediaServiceHandler = SimpleMediaServiceHandler(
                    player = service.service.player,
                    mediaSession = service.service.mediaSession,
                    mediaSessionCallback = service.service.simpleMediaSessionCallback,
                    dataStoreManager = viewModel.dataStoreManager,
                    mainRepository = viewModel.mainRepository,
                    context = service.service,
                    coroutineScope = lifecycleScope
                )

                viewModel.init()
                mayBeRestoreLastPlayedTrackAndQueue()
                runCollect()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.simpleMediaServiceHandler = null
        }
    }

    private fun mayBeRestoreLastPlayedTrackAndQueue() {
        if (getString(RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE) == DataStoreManager.FALSE) {
            viewModel.getSaveLastPlayedSong()
            val queue = viewModel.saveLastPlayedSong.switchMap { saved: Boolean ->
                if (saved) {
                    viewModel.from.postValue(viewModel.from_backup)
                    viewModel.simpleMediaServiceHandler?.reset()
                    viewModel.getSavedSongAndQueue()
                    return@switchMap viewModel.savedQueue
                } else {
                    return@switchMap null
                }
            }
            val result: MediatorLiveData<List<Track>> = MediatorLiveData<List<Track>>().apply {
                addSource(queue) {
                    value = it ?: listOf()
                }
            }
            binding.miniPlayer.visibility = View.GONE
            result.observe(this) { data ->
                val queueData = data
                Log.w("Check queue saved", queueData.toString())
                binding.miniPlayer.visibility = View.VISIBLE
                if (queueData.isNotEmpty()) {
                    Log.w("Check queue saved", queueData.toString())
                    Queue.clear()
                    Queue.addAll(queueData)
                    viewModel.removeSaveQueue()
                    viewModel.resetRelated()
                    viewModel.addQueueToPlayer()
                    checkForUpdate()
                }
            }
        } else {
            binding.miniPlayer.visibility = View.GONE
            checkForUpdate()
        }
    }

    private fun checkForUpdate() {
        viewModel.checkForUpdate()
        viewModel.githubResponse.observe(this) { response ->
            if (response != null) {
                if (response.tagName != getString(R.string.version_name)) {
                    val inputFormat =
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                    val formatted = response.publishedAt?.let { input ->
                        inputFormat.parse(input)
                            ?.let { outputFormat.format(it) }
                    }

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.update_available))
                        .setMessage(
                            getString(
                                R.string.update_message,
                                response.tagName,
                                formatted,
                                response.body
                            )
                        )
                        .setPositiveButton(getString(R.string.download)) { _, _ ->
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(response.assets?.firstOrNull()?.browserDownloadUrl)
                            )
                            startActivity(browserIntent)
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }
    }

    private fun getString(key: String): String? {
        return viewModel.getString(key)
    }

    private fun createArtworkTransformation(): Transformation {
        return object : Transformation {
            override val cacheKey: String
                get() = "artwork_transformation"

            override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                val p = Palette.from(input).generate()
                val defaultColor = 0x000000
                var startColor = p.getDarkVibrantColor(defaultColor)
                if (startColor == defaultColor) {
                    startColor = p.getDarkMutedColor(defaultColor)
                    if (startColor == defaultColor) {
                        startColor = p.getVibrantColor(defaultColor)
                        if (startColor == defaultColor) {
                            startColor = p.getMutedColor(defaultColor)
                            if (startColor == defaultColor) {
                                startColor =
                                    p.getLightVibrantColor(defaultColor)
                                if (startColor == defaultColor) {
                                    startColor =
                                        p.getLightMutedColor(defaultColor)
                                }
                            }
                        }
                    }
                }
                val endColor = 0x1b1a1f
                val gd = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(startColor, endColor)
                )
                gd.cornerRadius = 0f
                gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                gd.gradientRadius = 0.5f
                gd.alpha = 150
                val bg = ColorUtils.setAlphaComponent(startColor, 255)
                binding.card.setCardBackgroundColor(bg)
                binding.cardBottom.setCardBackgroundColor(bg)
                return input
            }
        }
    }


    private fun runCollect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job5 = launch {
                    viewModel.simpleMediaServiceHandler?.nowPlaying?.collect {
                        if (it != null) {
                            viewModel.simpleMediaServiceHandler?.getCurrentMediaItem()?.mediaMetadata?.title.toString()
                        }
                        bindData(it)
                    }
                }
                val job2 = launch {
                    viewModel.progress.collect {
                        binding.progressBar.progress = (it * 100).toInt()
                    }
                }

                val job6 = launch {
                    viewModel.simpleMediaServiceHandler?.liked?.collect { liked ->
                        binding.cbFavorite.isChecked = liked
                    }
                }

                val job3 = launch {
                    viewModel.isPlaying.collect {
                        if (it) {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_pause_24)
                        } else {
                            binding.btPlayPause.setImageResource(R.drawable.baseline_play_arrow_24)
                        }
                    }
                }

                val job4 = launch {
                    viewModel.simpleMediaServiceHandler?.sleepDone?.collect { done ->
                        if (done) {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setTitle(getString(R.string.sleep_timer_off))
                                .setMessage(getString(R.string.good_night))
                                .setPositiveButton(getString(R.string.yes)) { d, _ ->
                                    d.dismiss()
                                }
                                .show()
                        }
                    }
                }
                val likedJob = launch {
                    viewModel.liked.collect {
                        binding.cbFavorite.isChecked = it
                    }
                }
                job2.join()
                job3.join()
                job5.join()
                job6.join()
                job4.join()
                likedJob.join()
            }
        }
    }

    private fun bindData(it: MediaItem?) {
        binding.apply {
            songTitle.setTextAnimation(it?.mediaMetadata?.title.toString())
            songTitle.isSelected = true
            songArtist.setTextAnimation(it?.mediaMetadata?.artist.toString())
            songArtist.isSelected = true
            ivArt.load(it?.mediaMetadata?.artworkUri) {
                crossfade(true)
                crossfade(300)
                placeholder(R.drawable.outline_album_24)
                transformations(createArtworkTransformation())
            }
        }
    }

}