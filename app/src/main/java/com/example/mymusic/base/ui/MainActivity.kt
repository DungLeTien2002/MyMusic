package com.example.mymusic.base.ui

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import coil.size.Size
import coil.transform.Transformation
import com.example.mymusic.R
import com.example.mymusic.base.baseview.BaseActivity
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.service.SimpleMediaService
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import com.example.mymusic.base.utils.extension.load
import com.example.mymusic.base.utils.extension.setTextAnimation
import com.example.mymusic.base.viewmodel.SharedViewModel
import com.example.mymusic.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    //region variable
    private var action: String? = null
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
    val navController = navHostFragment?.findNavController()

    private lateinit var binding: ActivityMainBinding
    val viewModel by viewModels<SharedViewModel>()



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
                Log.w("MainActivity", "onServiceConnected: ")

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
                Log.w("TEST", viewModel.simpleMediaServiceHandler?.player.toString())
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w("MainActivity", "onServiceDisconnected: ")
            viewModel.simpleMediaServiceHandler = null
        }
    }
    private fun mayBeRestoreLastPlayedTrackAndQueue() {

    }

    private fun runCollect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                val job5 = launch {
                    viewModel.simpleMediaServiceHandler?.nowPlaying?.collect {
                        if (it != null) {
                            viewModel.simpleMediaServiceHandler?.getCurrentMediaItem()?.mediaMetadata?.title.toString()
                        }
                        binding.songTitle.setTextAnimation(it?.mediaMetadata?.title.toString())
                        binding.songTitle.isSelected = true
                        binding.songArtist.setTextAnimation(it?.mediaMetadata?.artist.toString())
                        binding.songArtist.isSelected = true
                        binding.ivArt.load(it?.mediaMetadata?.artworkUri) {
                            crossfade(true)
                            crossfade(300)
                            placeholder(R.drawable.outline_album_24)
                            transformations(object : Transformation {
                                override val cacheKey: String
                                    get() = it?.mediaMetadata?.artworkUri.toString()

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
                            })
                        }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        if (viewModel.recreatedActivity.value == true) {
            viewModel.simpleMediaServiceHandler?.coroutineScope = lifecycleScope
            runCollect()
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
    }
}