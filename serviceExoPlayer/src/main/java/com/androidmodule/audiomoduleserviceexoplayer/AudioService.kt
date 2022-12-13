package com.androidmodule.audiomoduleserviceexoplayer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.getActivity
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidmodule.audiomodule.model.AudiosItem
import com.androidmodule.audiomodule.utils.isPathExistDownload
import com.androidmodule.audiomodule.utils.toPathDownload
import com.androidmodule.audiomodule.utils.validateDownload
import com.androidmodule.audiomodule.viewmodel.AudioViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import org.koin.android.ext.android.inject

private const val PLAYBACK_CHANNEL_ID = "playback_channel"
private const val PLAYBACK_NOTIFICATION_ID = 1

class AudioService : Service() {

    private var binderAudioService = BinderAudioService()
    var player: ExoPlayer? = null
    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var currentWindow = 0
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val audioPlayerViewModel: AudioPlayerViewModel by inject()
    private val audioViewModel: AudioViewModel by inject()

    companion object {
        const val ACTION_CREATE = "actionCreate"
        const val ACTION_PLAY = "actionPlay"
    }

    override fun onBind(intent: Intent): IBinder {
        return binderAudioService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (player == null)
            initializePlayer()
        when (intent?.action) {
            ACTION_CREATE -> {
                setPlayer()
            }
            ACTION_PLAY -> {
                playPlayer()
            }
        }
        return START_STICKY
    }

    inner class BinderAudioService : Binder() {
        val getService: AudioService = this@AudioService
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                super.onPositionDiscontinuity(oldPosition, newPosition, reason)
                currentWindow = newPosition.mediaItemIndex
                val currentPlaying = AudioUtils.getCurrentPlaylist()[currentWindow]
                replaceMediaItem(currentPlaying)
                audioPlayerViewModel.currentPlaying.value = currentPlaying
                if (AudioUtils.currentGroupName != AudioUtils.recentPlayedName) {
                    audioViewModel.addAudioToRecentPlayed(currentPlaying)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                player?.apply {
                    seekToNext()
                    prepare()
                    play()
                }
            }
        })
        // formerly created using PlayerNotificationManager.createWithNotificationChannel()
        playerNotificationManager = PlayerNotificationManager.Builder(
            this,
            PLAYBACK_NOTIFICATION_ID, PLAYBACK_CHANNEL_ID
        ).setSmallIconResourceId(R.drawable.ic_baseline_music_note_24)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return audioPlayerViewModel.currentPlaying.value?.title.toString()
                }

                @SuppressLint("UnspecifiedImmutableFlag")
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    val intent = Intent(this@AudioService, AudioUtils.detailAudioActivity)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                    return getActivity(
//                        this@AudioService,
//                        0,
//                        intent,
//                        FLAG_ONE_SHOT
//                    )
                    var pendingIntent: PendingIntent? = null
                    pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getActivity(this@AudioService, 0, intent, PendingIntent.FLAG_MUTABLE)
                    } else {
                        getActivity(this@AudioService, 0, intent, FLAG_ONE_SHOT)
                    }
                    return  pendingIntent
                }

                override fun getCurrentContentText(player: Player): CharSequence? {
                    return audioPlayerViewModel.currentPlaying.value?.artist
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    Glide.with(this@AudioService)
                        .asBitmap()
                        .load(audioPlayerViewModel.currentPlaying.value?.image)
                        .dontTransform()
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                callback.onBitmap(resource)
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {

                            }
                        })
                    return BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_baseline_music_note_24
                    )
                }

            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(
                    notificationId: Int,
                    notification: Notification,
                    ongoing: Boolean
                ) {
                    super.onNotificationPosted(notificationId, notification, ongoing)
                    if (ongoing) // allow notification to be dismissed if player is stopped
                        startForeground(notificationId, notification)
                    else
                        stopForeground(false)
                }

                override fun onNotificationCancelled(
                    notificationId: Int,
                    dismissedByUser: Boolean
                ) {
                    super.onNotificationCancelled(notificationId, dismissedByUser)
                    stopSelf()
                    stopForeground(true)
                }
            }).setChannelNameResourceId(R.string.channel_name).setChannelDescriptionResourceId(
                R.string.channel_desc
            )
            .build()

        playerNotificationManager?.let {
            with(it) {
                setPlayer(player)
                setColorized(true)
                setSmallIcon(R.drawable.ic_baseline_music_note_24)
            }
        }
        playerNotificationManager?.setPriority(NotificationCompat.PRIORITY_DEFAULT)

    }

    private fun setPlayer() {
        player?.clearMediaItems()
        val listMediaItems =
            AudioUtils.getCurrentPlaylist().filter { it.url?.isNotEmpty() == true }.map {
                MediaItem.fromUri(it.url!!)
            }
        player?.addMediaItems(listMediaItems)
        playPlayer()
    }

    private fun playPlayer() {
        currentWindow = AudioUtils.getCurrentIndex()
        player?.let {
            it.playWhenReady = playWhenReady
            it.seekTo(currentWindow, playbackPosition)
            it.prepare()
            it.play()
        }
    }

    private fun replaceMediaItem(currentPlaying: AudiosItem) {
        val pathDownload =
            currentPlaying.audioId.validateDownload().toPathDownload(this@AudioService)
        if (pathDownload.isPathExistDownload() && currentPlaying.url != pathDownload) {
            currentPlaying.url = pathDownload
            AudioUtils.updateItemPlaylist(currentWindow, currentPlaying)
            player?.let {
                player?.stop()
                player?.removeMediaItem(currentWindow)
                player?.addMediaItem(currentWindow, MediaItem.fromUri(pathDownload))
                player?.seekTo(currentWindow, playbackPosition)
                player?.prepare()
                player?.play()
                if (it.mediaItemCount > AudioUtils.getCurrentPlaylist().size) {
                    player?.removeMediaItem(it.mediaItemCount - 1)
                }
            }
        }
    }

    private fun releasePlayer() {
        playerNotificationManager?.setPlayer(null)
        player?.let {
            playWhenReady = it.playWhenReady
            playbackPosition = it.currentPosition
            it.release()
            player = null
        }
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

}