package com.example.mymusic.base.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.switchMap
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import coil.size.Size
import coil.transform.Transformation
import com.daimajia.swipe.SwipeLayout
import com.example.mymusic.R
import com.example.mymusic.base.Youtube
import com.example.mymusic.base.baseview.BaseActivity
import com.example.mymusic.base.common.Config
import com.example.mymusic.base.common.Config.SELECTED_LANGUAGE
import com.example.mymusic.base.common.FIRST_TIME_MIGRATION
import com.example.mymusic.base.common.STATUS_DONE
import com.example.mymusic.base.common.SUPPORTED_LANGUAGE
import com.example.mymusic.base.common.SUPPORTED_LOCATION
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.dataStore.DataStoreManager.Settings.RESTORE_LAST_PLAYED_TRACK_AND_QUEUE_DONE
import com.example.mymusic.base.data.models.browse.album.Track
import com.example.mymusic.base.data.queue.Queue
import com.example.mymusic.base.models.YouTubeLocale
import com.example.mymusic.base.service.SimpleMediaService
import com.example.mymusic.base.service.SimpleMediaServiceHandler
import com.example.mymusic.base.utils.extension.findNavController
import com.example.mymusic.base.utils.extension.isMyServiceRunning
import com.example.mymusic.base.utils.extension.load
import com.example.mymusic.base.utils.extension.navigateSafe
import com.example.mymusic.base.utils.extension.setTextAnimation
import com.example.mymusic.base.viewmodel.SharedViewModel
import com.example.mymusic.base.viewmodel.UIEvent
import com.example.mymusic.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.EasyPermissions
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
    private var data: Uri? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        action = intent?.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        viewModel.intent.value = intent
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun initListener() {
        binding.root.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val rect = Rect(left, top, right, bottom)
            val oldRect = Rect(oldLeft, oldTop, oldRight, oldBottom)
            if ((rect.width() != oldRect.width() || rect.height() != oldRect.height()) && oldRect != Rect(
                    0,
                    0,
                    0,
                    0
                )
            ) {
                viewModel.activityRecreate()
            }
        }
        binding.bottomNavigationView.setOnItemReselectedListener {
            val id = navController?.currentDestination?.id
            if (id != R.id.bottom_navigation_item_home && id != R.id.bottom_navigation_item_search && id != R.id.bottom_navigation_item_library) {
                navController?.popBackStack(it.itemId, inclusive = false)
            } else if (id == R.id.bottom_navigation_item_home) {
                viewModel.homeRefresh()
                viewModel.homeRefresh()
            }
        }
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
        binding.miniPlayer.showMode = SwipeLayout.ShowMode.PullOut
        binding.miniPlayer.addDrag(SwipeLayout.DragEdge.Right, binding.llBottom)
        binding.miniPlayer.addSwipeListener(object : SwipeLayout.SwipeListener {
            override fun onStartOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onOpen(layout: SwipeLayout?) {
                binding.card.radius = 0f
            }

            override fun onStartClose(layout: SwipeLayout?) {
                binding.card.radius = 12f
            }

            override fun onClose(layout: SwipeLayout?) {
                binding.card.radius = 12f
            }

            override fun onUpdate(layout: SwipeLayout?, leftOffset: Int, topOffset: Int) {
                binding.card.radius = 12f
            }

            override fun onHandRelease(layout: SwipeLayout?, xvel: Float, yvel: Float) {
            }

        })
        binding.btRemoveMiniPlayer.setOnClickListener {
            viewModel.stopPlayer()
            viewModel.isServiceRunning.postValue(false)
            viewModel.videoId.postValue(null)
            binding.miniPlayer.visibility = View.GONE
            binding.card.radius = 12f
        }
        binding.btSkipNext.setOnClickListener {
            viewModel.onUIEvent(UIEvent.Next)
            binding.card.radius = 12f
        }

        binding.card.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("type", Config.MINIPLAYER_CLICK)
            navController?.navigateSafe(R.id.action_global_nowPlayingFragment, bundle)
        }
        binding.btPlayPause.setOnClickListener {
            viewModel.onUIEvent(UIEvent.PlayPause)
        }
        binding.card.animation = AnimationUtils.loadAnimation(this, R.anim.bottom_to_top)
        binding.cbFavorite.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(nowPlayingSong.mediaId, false)
                    viewModel.updateLikeInNotification(false)
                }
            } else {
                Log.d("cbFavorite", "onCheckedChanged: $isChecked")
                viewModel.nowPlayingMediaItem.value?.let { nowPlayingSong ->
                    viewModel.updateLikeStatus(nowPlayingSong.mediaId, true)
                    viewModel.updateLikeInNotification(true)
                }
            }
        }
    }

    override fun initData() {
        if (viewModel.recreateActivity.value == true) {
            viewModel.simpleMediaServiceHandler?.coroutineScope = lifecycleScope
            runCollect()
            viewModel.activityRecreateDone()
        } else {
            startMusicService()
        }
        action = intent.action
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
        if (data != null) {
            viewModel.intent.value = intent
        }
        if (getString(FIRST_TIME_MIGRATION) != STATUS_DONE) {
            if (SUPPORTED_LANGUAGE.codes.contains(Locale.getDefault().toLanguageTag())) {
                putString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
                if (SUPPORTED_LOCATION.items.contains(Locale.getDefault().country)) {
                    putString("location", Locale.getDefault().country)
                } else {
                    putString("location", "US")
                }
                Youtube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = Locale.getDefault().toLanguageTag()
                )
            } else {
                putString(SELECTED_LANGUAGE, "en-US")
                Youtube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = "en-US"
                )
            }
            getString(SELECTED_LANGUAGE)?.let {
                val localeList = LocaleListCompat.forLanguageTags(it)
                AppCompatDelegate.setApplicationLocales(localeList)
                putString(FIRST_TIME_MIGRATION, STATUS_DONE)
            }
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != getString(
                    SELECTED_LANGUAGE
                )
            ) {
                putString(
                    SELECTED_LANGUAGE,
                    AppCompatDelegate.getApplicationLocales().toLanguageTags()
                )
                Youtube.locale = YouTubeLocale(
                    gl = getString("location") ?: "US",
                    hl = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                )
            }
            enableEdgeToEdge()
            viewModel.checkIsRestoring()
            if (!EasyPermissions.hasPermissions(this, Manifest.permission.POST_NOTIFICATIONS)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    EasyPermissions.requestPermissions(
                        this,
                        getString(R.string.this_app_needs_to_access_your_notification),
                        1,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
            viewModel.getLocation()
            viewModel.checkAuth()
            viewModel.checkAllDownloadingSongs()
            runBlocking { delay(500) }
            binding.bottomNavigationView.applyInsetter {
                type(navigationBars = true) {
                    padding()
                }
            }
            window.navigationBarColor = Color.parseColor("#CB0B0A0A")
            if (!isMyServiceRunning(SimpleMediaService::class.java)) {
                binding.miniPlayer.visibility = View.GONE
            }
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.intent.collectLatest { intent ->
                    if (intent != null) {
                        data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
                        if (data != null) {
                            when (val path = data!!.pathSegments.firstOrNull()) {
                                "playlist" -> data!!.getQueryParameter("list")
                                    ?.let { playlistId ->
                                        if (playlistId.startsWith("OLAK5uy_")) {
                                            viewModel.intent.value = null
                                            navController?.navigateSafe(
                                                R.id.action_global_albumFragment,
                                                Bundle().apply {
                                                    putString("browseId", playlistId)
                                                })
                                        } else if (playlistId.startsWith("VL")) {
                                            viewModel.intent.value = null
                                            navController?.navigateSafe(
                                                R.id.action_global_playlistFragment,
                                                Bundle().apply {
                                                    putString("id", playlistId)
                                                })
                                        } else {
                                            viewModel.intent.value = null
                                            navController?.navigateSafe(
                                                R.id.action_global_playlistFragment,
                                                Bundle().apply {
                                                    putString("id", "VL$playlistId")
                                                })
                                        }
                                    }

                                "channel", "c" -> data!!.lastPathSegment?.let { artistId ->
                                    if (artistId.startsWith("UC")) {
                                        viewModel.intent.value = null
                                        navController?.navigateSafe(
                                            R.id.action_global_artistFragment,
                                            Bundle().apply {
                                                putString("channelId", artistId)
                                            })
                                    }
                                }

                                else -> when {
                                    path == "watch" -> data!!.getQueryParameter("v")
                                    data!!.host == "youtu.be" -> path
                                    else -> null
                                }?.let { videoId ->
                                    val args = Bundle()
                                    args.putString("videoId", videoId)
                                    args.putString("from", getString(R.string.shared))
                                    args.putString("type", Config.SHARE)
                                    viewModel.videoId.value = videoId
                                    hideBottomNav()
                                    if (navController?.currentDestination?.id == R.id.nowPlayingFragment) {
                                        findNavController(R.id.fragment_container_view).popBackStack()
                                        navController.navigateSafe(
                                            R.id.action_global_nowPlayingFragment,
                                            args
                                        )
                                    } else {
                                        navController?.navigateSafe(
                                            R.id.action_global_nowPlayingFragment,
                                            args
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            job1.join()
        }
        lifecycleScope.launch {
            val job1 = launch {
                viewModel.progress.collect { progress ->
                    val skipSegments = viewModel.skipSegments.first()
                    val enabled = viewModel.sponsorBlockEnabled()
                    val listCategory = viewModel.sponsorBlockCategories()
                    if (skipSegments != null && enabled == DataStoreManager.TRUE) {
                        for (skip in skipSegments) {
                            if (listCategory.contains(skip.category)) {
                                val firstPart = (skip.segment[0] / skip.videoDuration).toFloat()
                                val secondPart = (skip.segment[1] / skip.videoDuration).toFloat()
                                if (progress in firstPart..secondPart) {
                                    viewModel.skipSegment((skip.segment[1] * 1000).toLong())
                                }
                            }
                        }
                    }
                }
            }
            job1.join()
        }
    }

    override fun initView() {
    }

    override fun inflateViewBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }


    private fun startMusicService() {
        if (viewModel.recreateActivity.value != true) {
            val intent = Intent(this, SimpleMediaServiceHandler::class.java)
            startService(intent)
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            viewModel.isServiceRunning.value = true
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopService()
    }
    private fun stopService() {
        if (viewModel.recreateActivity.value != true) {
            viewModel.isServiceRunning.value = false
            viewModel.simpleMediaServiceHandler?.mayBeSaveRecentSong()
            viewModel.simpleMediaServiceHandler?.mayBeSavePlaybackState()
            viewModel.simpleMediaServiceHandler?.release()
            viewModel.simpleMediaServiceHandler = null
            unbindService(serviceConnection)
            if (this.isMyServiceRunning(DownloadService::class.java)) {
                stopService(Intent(this, DownloadService::class.java))
                viewModel.changeAllDownloadingToError()
            }
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
                    val outputFormat =
                        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
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

    fun hideBottomNav() {
        binding.bottomNavigationView.visibility = View.GONE
        binding.miniPlayer.visibility = View.GONE
    }

    fun showBottomNav() {
        binding.bottomNavigationView.visibility = View.VISIBLE
        binding.miniPlayerContainer.visibility = View.VISIBLE
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

    private fun putString(key: String, value: String) {
        viewModel.putString(key, value)
    }
}
