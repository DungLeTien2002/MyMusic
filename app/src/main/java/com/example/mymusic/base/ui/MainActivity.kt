package com.example.mymusic.base.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import coil.size.Size
import coil.transform.Transformation
import com.example.mymusic.R
import com.example.mymusic.base.baseview.BaseActivity
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import com.example.mymusic.base.utils.extension.load
import com.example.mymusic.base.utils.extension.setTextAnimation
import com.example.mymusic.base.viewmodel.SharedViewModel
import com.example.mymusic.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {
    //region variable
    private var action: String? = null
    val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
    val navController =navHostFragment?.findNavController()

    val viewModel by viewModels<SharedViewModel>()

    override fun initListener() {
        when (action) {
            "com.example.mymusic.action.HOME" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_home
            }

            "com.example.mymusic.action.SEARCH" -> {
                binding.bottomNavigationView.selectedItemId = R.id.bottom_navigation_item_search
            }

            "com.example.mymusic.action.LIBRARY" -> {
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
        }else{
            startMusicService()
        }
    }

    private fun startMusicService() {
        if(viewModel.recreatedActivity.value!=true){
            val intent=Intent(this,SimpleMediaServiceHandler::class.java)
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

    override fun initView() {

    }

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }
}