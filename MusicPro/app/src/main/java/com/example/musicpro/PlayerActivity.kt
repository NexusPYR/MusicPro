package com.example.musicpro

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class PlayerActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private lateinit var ivRecordCover: ImageView
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnPrev: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnDown: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvTimeCurrent: TextView
    private lateinit var tvTimeTotal: TextView
    private lateinit var seekBar: SeekBar

    // 旋转动画器
    private var rotationAnimator: ObjectAnimator? = null
    // 更新进度条的定时器
    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // 绑定 UI
        ivRecordCover = findViewById(R.id.iv_record_cover)
        btnPlayPause = findViewById(R.id.btn_player_play_pause)
        btnPrev = findViewById(R.id.btn_player_prev)
        btnNext = findViewById(R.id.btn_player_next)
        btnDown = findViewById(R.id.btn_down)
        tvTitle = findViewById(R.id.tv_player_title)
        tvArtist = findViewById(R.id.tv_player_artist)
        tvTimeCurrent = findViewById(R.id.tv_time_current)
        tvTimeTotal = findViewById(R.id.tv_time_total)
        seekBar = findViewById(R.id.seek_bar)

        // 初始化旋转动画 (匀速，无限循环)
        rotationAnimator = ObjectAnimator.ofFloat(ivRecordCover, "rotation", 0f, 360f).apply {
            duration = 20000 // 20秒转一圈
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
        }

        // 下拉关闭按钮
        btnDown.setOnClickListener { finish() }

        // 连接 Media3 后台服务
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                setupController()
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun setupController() {
        val controller = mediaController ?: return

        // 初始状态同步
        updateSongInfo()
        updatePlaybackState()

        // 监听进度条拖动
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvTimeCurrent.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(progressRunnable) // 拖动时暂停自动更新
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { controller.seekTo(it.progress.toLong()) }
                handler.post(progressRunnable)
            }
        })

        // 控制按钮事件
        btnPlayPause.setOnClickListener {
            if (controller.isPlaying) controller.pause() else controller.play()
        }
        btnPrev.setOnClickListener { controller.seekToPrevious() }
        btnNext.setOnClickListener { controller.seekToNext() }

        // 监听播放器状态改变
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateSongInfo()
            }
        })
    }

    // 更新歌曲信息和封面
    private fun updateSongInfo() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem ?: return

        // 从你之前设置的 Metadata 中获取标题和歌手
        tvTitle.text = currentItem.mediaMetadata.title ?: "未知歌曲"
        tvArtist.text = currentItem.mediaMetadata.artist ?: "未知歌手"

        // 提取高清封面 (通过底层 URI 解析)
        val uri = currentItem.localConfiguration?.uri
        if (uri != null) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val artwork = retriever.embeddedPicture
                if (artwork != null) {
                    val bitmap = BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                    ivRecordCover.setImageBitmap(bitmap)
                } else {
                    ivRecordCover.setImageResource(android.R.drawable.ic_media_play)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                retriever.release()
            }
        }
    }

    // 更新播放/暂停 UI 和动画
    private fun updatePlaybackState() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            if (rotationAnimator?.isStarted == false) rotationAnimator?.start()
            else rotationAnimator?.resume()
            handler.post(progressRunnable) // 开启进度条更新
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            rotationAnimator?.pause()
            handler.removeCallbacks(progressRunnable) // 停止进度条更新
        }
    }

    // 每秒更新进度条
    private fun updateProgress() {
        val controller = mediaController ?: return
        val position = controller.currentPosition
        val duration = controller.duration

        if (duration > 0) {
            seekBar.max = duration.toInt()
            seekBar.progress = position.toInt()
            tvTimeCurrent.text = formatTime(position)
            tvTimeTotal.text = formatTime(duration)
        }
    }

    // 格式化毫秒为 mm:ss
    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationAnimator?.cancel()
        handler.removeCallbacks(progressRunnable)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}