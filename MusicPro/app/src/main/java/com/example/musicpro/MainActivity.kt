package com.example.musicpro

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.net.toUri
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private lateinit var btnPrev: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnPlaylist: ImageView
    private lateinit var bottomPlayerBar: View
    private lateinit var btnScanLocal: Button
    private lateinit var btnSettings: ImageView
    private lateinit var ivMiniCover: ImageView
    private lateinit var tvMiniTitle: TextView
    private lateinit var rvSongs: RecyclerView
    private lateinit var libraryAdapter: SongAdapter
    private lateinit var btnShowFavorites: ImageView
    private lateinit var searchView: SearchView

    private var isShowingFavorites = false
    private var masterSongList = mutableListOf<Song>()
    private var currentSearchQuery = ""

    companion object {
        var themeScreenshot: Bitmap? = null
        var themeCenterX: Int = 0
        var themeCenterY: Int = 0
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        private val MUSIC_PRO_PREFS = "music_pro"

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                val prefs = getSharedPreferences(MUSIC_PRO_PREFS, Context.MODE_PRIVATE)
                val shouldPause = prefs.getBoolean("pause_on_unplug", true)
                if (shouldPause) {
                    mediaController?.pause()
                    Toast.makeText(this@MainActivity, "耳机已断开，播放已暂停", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (audioGranted) scanLocalMusic()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查是否有主题切换动画
        if (themeScreenshot != null) {
            val rootView = window.decorView as ViewGroup
            val coverImg = ImageView(this)
            coverImg.setImageBitmap(themeScreenshot)
            rootView.addView(coverImg, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            
            val screenshotToRelease = themeScreenshot
            themeScreenshot = null // 用完置空
            
            coverImg.post {
                val finalRadius = Math.hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()
                val anim = ViewAnimationUtils.createCircularReveal(coverImg, themeCenterX, themeCenterY, finalRadius, 0f)
                anim.duration = 600
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        rootView.removeView(coverImg)
                        screenshotToRelease?.recycle()
                    }
                })
                anim.start()
            }
        }

        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnScanLocal = findViewById(R.id.btn_scan_local)
        btnPlaylist = findViewById(R.id.btn_playlist)
        btnSettings = findViewById(R.id.btn_settings)
        ivMiniCover = findViewById(R.id.iv_mini_cover)
        bottomPlayerBar = findViewById(R.id.bottom_player_bar)
        tvMiniTitle = findViewById(R.id.tv_mini_title)
        rvSongs = findViewById(R.id.rv_songs)
        btnShowFavorites = findViewById(R.id.btn_show_favorites)
        searchView = findViewById(R.id.search_view)

        setupSearch()

        bottomPlayerBar.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
        }

        libraryAdapter = SongAdapter(
            songList = mutableListOf(),
            onItemClick = { song ->
                val controller = mediaController
                if (controller != null && controller.currentMediaItem?.mediaId == song.id.toString()) {
                    // 如果点击的是正在播放的歌曲，直接跳转到详情页
                    val intent = Intent(this, PlayerActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
                } else {
                    // 否则从头播放该歌曲
                    playFromLibrary(song)
                }
            },
            onItemLongClick = { song -> showLibraryOptions(song) }
        )
        rvSongs.layoutManager = LinearLayoutManager(this)
        rvSongs.adapter = libraryAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteFromLibrary(libraryAdapter.getSongs()[viewHolder.bindingAdapterPosition])
            }
        }).attachToRecyclerView(rvSongs)

        loadLibraryFromDisk()

        btnScanLocal.setOnClickListener { requestPermissionsAndScan() }
        btnPlaylist.setOnClickListener { showPlaybackQueueDialog() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        ivMiniCover.setOnClickListener { showLyricsDialog() }
        btnShowFavorites.setOnClickListener {
            isShowingFavorites = !isShowingFavorites
            updateLibraryView()
            btnShowFavorites.imageAlpha = if (isShowingFavorites) 255 else 128
        }

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupController()
        }, ContextCompat.getMainExecutor(this))

        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                updateLibraryView()
                return true
            }
        })
    }

    private fun applySavedTheme() {
        val themeMode = getSharedPreferences("music_pro", Context.MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun setupController() {
        val controller = mediaController ?: return
        
        // 立即同步当前播放状态，防止 Activity 重建后 UI 归零
        syncMiniPlayerUI(controller)

        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                btnPlayPause.setImageResource(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                syncMiniPlayerUI(controller)
            }
        })
        btnPlayPause.setOnClickListener {
            mediaController?.let { c ->
                if (c.mediaItemCount == 0) {
                    val library = libraryAdapter.getSongs()
                    if (library.isNotEmpty()) {
                        playFromLibrary(library.first())
                    } else {
                        Toast.makeText(this, "曲库为空，请先扫描歌曲", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (c.isPlaying) c.pause() else c.play()
                }
            }
        }
        btnPrev.setOnClickListener { mediaController?.seekToPrevious() }
        btnNext.setOnClickListener { mediaController?.seekToNext() }
    }

    private fun syncMiniPlayerUI(controller: MediaController) {
        val currentItem = controller.currentMediaItem
        btnPlayPause.setImageResource(if (controller.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        
        val song = masterSongList.find { it.id.toString() == currentItem?.mediaId }
        tvMiniTitle.text = song?.title ?: currentItem?.mediaMetadata?.title ?: "准备播放..."
        if (song?.cover != null) ivMiniCover.setImageBitmap(song.cover)
        else ivMiniCover.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun updateLibraryView() {
        var filteredList = masterSongList.toList()

        if (isShowingFavorites) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { song ->
                song.title.contains(currentSearchQuery, ignoreCase = true) ||
                        song.artist?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }

        libraryAdapter.updateData(filteredList)
    }

    private fun showLyricsDialog() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem ?: return
        val song = masterSongList.find { it.id.toString() == currentItem.mediaId } ?: return

        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_lyrics, null)
        view.findViewById<TextView>(R.id.tv_lyric_title).text = song.title
        view.findViewById<TextView>(R.id.tv_lyric_artist).text = song.artist

        val retriever = MediaMetadataRetriever()
        var lyrics: String? = null
        try {
            retriever.setDataSource(this, song.uri)
            lyrics = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        } catch (e: Exception) {
        } finally {
            retriever.release()
        }

        view.findViewById<TextView>(R.id.tv_lyrics_content).text = lyrics ?: "暂无内嵌歌词"
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val switchPause = view.findViewById<SwitchMaterial>(R.id.switch_pause_unplug)
        val switchAutoResume = view.findViewById<SwitchMaterial>(R.id.switch_auto_resume)
        val rgTheme = view.findViewById<RadioGroup>(R.id.rg_theme)
        val prefs = getSharedPreferences("music_pro", Context.MODE_PRIVATE)

        switchPause.isChecked = prefs.getBoolean("pause_on_unplug", true)
        switchAutoResume.isChecked = prefs.getBoolean("auto_resume_focus", true)
        
        val currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> rgTheme.check(R.id.rb_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> rgTheme.check(R.id.rb_theme_dark)
            else -> rgTheme.check(R.id.rb_theme_system)
        }

        switchPause.setOnCheckedChangeListener { _, isChecked -> 
            prefs.edit().putBoolean("pause_on_unplug", isChecked).apply() 
        }
        
        switchAutoResume.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_resume_focus", isChecked).apply()
        }

        rgTheme.setOnCheckedChangeListener { group, checkedId ->
            val newMode = when (checkedId) {
                R.id.rb_theme_light -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.rb_theme_dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (newMode != currentTheme) {
                // 准备动画数据
                val location = IntArray(2)
                group.findViewById<View>(checkedId).getLocationInWindow(location)
                themeCenterX = location[0] + group.findViewById<View>(checkedId).width / 2
                themeCenterY = location[1] + group.findViewById<View>(checkedId).height / 2
                
                val rootView = window.decorView
                themeScreenshot = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(themeScreenshot!!)
                rootView.draw(canvas)

                prefs.edit().putInt("theme_mode", newMode).apply()
                dialog.dismiss()
                
                rootView.postDelayed({
                    AppCompatDelegate.setDefaultNightMode(newMode)
                }, 100)
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun scanLocalMusic() {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songs = mutableListOf<Song>()
        contentResolver.query(collection, arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ARTIST), "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                val cover = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) contentResolver.loadThumbnail(uri, Size(300, 300), null)
                    else null
                } catch (e: Exception) {
                    null
                }
                songs.add(Song(id, cursor.getString(1), cursor.getString(2) ?: "未知", uri, cover))
            }
        }
        if (songs.isNotEmpty()) {
            masterSongList.clear()
            masterSongList.addAll(songs)
            saveLibraryToDisk(masterSongList)
            updateLibraryView()
        }
    }

    private fun playFromLibrary(song: Song) {
        val controller = mediaController ?: return
        val library = libraryAdapter.getSongs()
        val items = library.map { s ->
            MediaItem.Builder().setUri(s.uri).setMediaId(s.id.toString()).setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).build()).build()
        }
        controller.setMediaItems(items, library.indexOfFirst { it.id == song.id }, 0)
        controller.prepare()
        controller.play()
    }

    private fun showLibraryOptions(song: Song) {
        val favoriteAction = if (song.isFavorite) "取消收藏" else "收藏"
        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setItems(arrayOf(favoriteAction, "从曲库移除")) { _, which ->
                when (which) {
                    0 -> toggleFavorite(song)
                    1 -> deleteFromLibrary(song)
                }
            }
            .show()
    }

    private fun toggleFavorite(song: Song) {
        song.isFavorite = !song.isFavorite
        saveLibraryToDisk(masterSongList)
        updateLibraryView()
        Toast.makeText(this, if (song.isFavorite) "已收藏" else "已取消收藏", Toast.LENGTH_SHORT).show()
    }

    private fun deleteFromLibrary(song: Song) {
        val index = masterSongList.indexOfFirst { it.id == song.id }
        if (index != -1) {
            if (mediaController?.currentMediaItem?.mediaId == song.id.toString()) {
                mediaController?.removeMediaItem(libraryAdapter.getSongs().indexOf(song))
                if (mediaController?.mediaItemCount == 0) mediaController?.stop()
            }
            masterSongList.removeAt(index)
            saveLibraryToDisk(masterSongList)
            updateLibraryView()
        }
    }

    private fun showPlaybackQueueDialog() {
        val controller = mediaController ?: return
        if (controller.mediaItemCount == 0) {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show()
            return
        }
        val queueSongs = mutableListOf<Song>()
        val lib = masterSongList
        for (i in 0 until controller.mediaItemCount) {
            val item = controller.getMediaItemAt(i)
            val song = lib.find { it.id.toString() == item.mediaId }
            queueSongs.add(song ?: Song(item.mediaId.toLongOrNull() ?: 0L, item.mediaMetadata.title?.toString() ?: "未知", uri = item.localConfiguration?.uri ?: Uri.EMPTY))
        }

        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)

        val tvPlaylistCount = view.findViewById<TextView>(R.id.tv_playlist_count)
        tvPlaylistCount.text = "共 ${queueSongs.size} 首歌曲"

        val rv = view.findViewById<RecyclerView>(R.id.rv_dialog_playlist)
        rv.layoutManager = LinearLayoutManager(this)
        val dialogAdapter = SongAdapter(queueSongs, { s ->
            controller.seekTo(queueSongs.indexOf(s), 0)
            controller.play()
            dialog.dismiss()
        }, {
        })
        rv.adapter = dialogAdapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                controller.removeMediaItem(position)
                queueSongs.removeAt(position)
                dialogAdapter.notifyItemRemoved(position)

                tvPlaylistCount.text = "共 ${queueSongs.size} 首歌曲"

                if (queueSongs.isEmpty()) {
                    dialog.dismiss()
                }
            }
        }).attachToRecyclerView(rv)

        dialog.setContentView(view)
        dialog.show()
    }

    private fun requestPermissionsAndScan() {
        val p = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            p.add(Manifest.permission.READ_MEDIA_AUDIO)
            p.add(Manifest.permission.POST_NOTIFICATIONS)
        } else p.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestMultiplePermissionsLauncher.launch(p.toTypedArray())
    }

    private fun saveLibraryToDisk(s: List<Song>) {
        val arr = JSONArray()
        s.forEach { arr.put(JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("uri", it.uri.toString()).put("isFavorite", it.isFavorite)) }
        getSharedPreferences("music_pro", Context.MODE_PRIVATE).edit().putString("lib", arr.toString()).apply()
    }

    private fun loadLibraryFromDisk() {
        val j = getSharedPreferences("music_pro", Context.MODE_PRIVATE).getString("lib", null) ?: return
        val s = mutableListOf<Song>()
        try {
            val a = JSONArray(j)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val id = o.getLong("id")
                val uri = o.getString("uri").toUri()
                val isFavorite = o.optBoolean("isFavorite", false)
                val cover = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) contentResolver.loadThumbnail(uri, Size(300, 300), null) else null
                } catch (e: Exception) {
                    null
                }
                s.add(Song(id, o.getString("title"), o.getString("artist"), uri, cover, isFavorite))
            }
            masterSongList.clear()
            masterSongList.addAll(s)
            updateLibraryView()
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
