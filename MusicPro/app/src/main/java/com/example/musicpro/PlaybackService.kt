package com.example.musicpro

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // 开启自动音频焦点管理
            .setHandleAudioBecomingNoisy(true) // 耳机拔出时自动暂停
            .build()

        // 监听播放器状态，实现条件自动恢复
        player.addListener(object : Player.Listener {
            private var wasPausedByFocusLoss = false

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // 当因为音频焦点丢失导致暂停时
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                    wasPausedByFocusLoss = true
                } 
                // 当焦点恢复，播放器尝试自动播放时
                else if (playWhenReady && wasPausedByFocusLoss) {
                    val prefs = getSharedPreferences("music_pro", Context.MODE_PRIVATE)
                    val isAutoResumeEnabled = prefs.getBoolean("auto_resume_focus", true)
                    
                    if (!isAutoResumeEnabled) {
                        // 如果用户关闭了自动恢复，则强制暂停
                        player.pause()
                    }
                    wasPausedByFocusLoss = false
                }
            }
        })

        // 设置播放器的重复模式，确保播放列表循环
        player.repeatMode = Player.REPEAT_MODE_ALL

        // 设置点击通知栏时启动哪个 Activity
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {
            override fun onPlaybackResumption(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val mediaItems = mutableListOf<MediaItem>()
                for (i in 0 until mediaSession.player.mediaItemCount) {
                    mediaItems.add(mediaSession.player.getMediaItemAt(i))
                }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        mediaItems,
                        mediaSession.player.currentMediaItemIndex,
                        mediaSession.player.currentPosition
                        )
                )
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent) // 关联 Activity
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
