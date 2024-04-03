package com.example.mymusic.base.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.mymusic.R
import com.example.mymusic.base.common.MEDIA_NOTIFICATION
import com.example.mymusic.base.common.QUALITY
import com.example.mymusic.base.data.dataStore.DataStoreManager
import com.example.mymusic.base.data.repository.MainRepository
import com.example.mymusic.base.di.DownloadCache
import com.example.mymusic.base.di.PlayerCache
import com.example.mymusic.base.service.test.CoilBitmapLoader
import com.example.mymusic.base.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject


@AndroidEntryPoint
@UnstableApi
class SimpleMediaService : MediaLibraryService() {
    //
    lateinit var player: ExoPlayer
    lateinit var mediaSession: MediaLibrarySession

    @Inject
    lateinit var simpleMediaSessionCallback: SimpleMediaSessionCallback

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    @Inject
    lateinit var mainRepository: MainRepository

    //
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { MEDIA_NOTIFICATION.NOTIFICATION_ID },
                MEDIA_NOTIFICATION.NOTIFICATION_CHANNEL_ID,
                R.string.notification_channel_name
            )
                .apply {
                    setSmallIcon(R.drawable.mono)
                }
        )
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(provideAudioAttributes(), true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .setSeekForwardIncrementMs(5000)
            .setSeekBackIncrementMs(5000)
            .setMediaSourceFactory(provideMediaSourceFactory())
            .setRenderersFactory(provideRendererFactory(this))
            .build()
        mediaSession = provideMediaLibrarySession(this, this, player, simpleMediaSessionCallback)
    }

    fun provideAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    @UnstableApi
    fun provideMediaSourceFactory(): DefaultMediaSourceFactory = DefaultMediaSourceFactory(
        provideResolvingDataSourceFactory(
            provideCacheDataSource(downloadCache, playerCache),
            downloadCache,
            playerCache,
            mainRepository, dataStoreManager
        ),
        provideExtractorFactory()
    )

    @UnstableApi
    fun provideExtractorFactory(): ExtractorsFactory = ExtractorsFactory {
        arrayOf(
            MatroskaExtractor(),
            FragmentedMp4Extractor(),
            androidx.media3.extractor.mp4.Mp4Extractor(),
        )
    }

    @UnstableApi
    fun provideCacheDataSource(
        @DownloadCache downloadCache: SimpleCache,
        @PlayerCache playerCache: SimpleCache
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultHttpDataSource.Factory()
                            .setAllowCrossProtocolRedirects(true)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.0.0 Safari/537.36")
                            .setConnectTimeoutMs(5000)
                    )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @UnstableApi
    fun provideResolvingDataSourceFactory(
        cacheDataSourceFactory: CacheDataSource.Factory,
        @DownloadCache downloadCache: SimpleCache,
        @PlayerCache playerCache: SimpleCache,
        mainRepository: MainRepository,
        dataStoreManager: DataStoreManager
    ): DataSource.Factory {
        return ResolvingDataSource.Factory(cacheDataSourceFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val CHUNK_LENGTH = 512 * 1024L
            if (downloadCache.isCached(
                    mediaId,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) || playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                return@Factory dataSpec
            }
            var dataSpecReturn: DataSpec = dataSpec
            runBlocking(Dispatchers.IO) {
                val itag = dataStoreManager.quality.first()

                mainRepository.getStream(
                    mediaId,
                    if (itag == QUALITY.items[0].toString()) QUALITY.itags[0] else QUALITY.itags[1]
                ).cancellable().collect {
                    if (it != null) {
                        dataSpecReturn = dataSpec.withUri(it.toUri())
                    }
                }
            }
            return@Factory dataSpecReturn
        }
    }

    @UnstableApi
    fun provideRendererFactory(context: Context): DefaultRenderersFactory =
        object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(
                            emptyArray(),
                            SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                            SonicAudioProcessor()
                        )
                    )
                    .build()
            }
        }

    @UnstableApi
    fun provideMediaLibrarySession(
        context: Context,
        service: MediaLibraryService,
        player: ExoPlayer,
        callback: SimpleMediaSessionCallback
    ): MediaLibrarySession = MediaLibrarySession.Builder(
        service, player, callback
    )
        .setSessionActivity(
            PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setBitmapLoader(provideCoilBitmapLoader(context))
        .build()

    @UnstableApi
    fun provideCoilBitmapLoader(context: Context): CoilBitmapLoader = CoilBitmapLoader(context)
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaSession
}
