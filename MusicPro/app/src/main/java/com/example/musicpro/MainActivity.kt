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
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.core.net.toUri
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.transition.TransitionManager
import android.util.Size
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
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
import androidx.media3.common.Player
import androidx.media3.common.MediaMetadata

class MainActivity : BaseActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private lateinit var topBar: ViewGroup
    private lateinit var topBarContainer: ViewGroup
    private lateinit var bottomPlayerContainer: ViewGroup
    
    private lateinit var btnPrev: ImageView
    private lateinit var btnNext: ImageView
    private lateinit var btnPlayPause: ImageView
    private lateinit var btnPlaylist: ImageView
    private lateinit var bottomPlayerBar: View
    private lateinit var btnScanLocal: Button
    private lateinit var btnSettings: ImageView
    private lateinit var btnEditList: ImageView
    private lateinit var ivMiniCover: ImageView
    private lateinit var tvMiniTitle: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var rvSongs: RecyclerView
    private lateinit var pbScanning: ProgressBar
    private lateinit var libraryAdapter: SongAdapter
    private lateinit var btnShowFavorites: ImageView
    private lateinit var searchView: SearchView
    private lateinit var btnSearchIcon: ImageView
    private lateinit var btnBackSearch: View

    private lateinit var multiSelectContainer: ViewGroup
    private lateinit var tvSelectedCount: TextView
    private lateinit var btnSelectAll: Button
    private lateinit var btnBatchDelete: ImageView
    private lateinit var btnBatchAddPlaylist: ImageView
    private lateinit var btnBatchFavorite: ImageView
    private lateinit var btnExitMultiSelect: View

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
        if (audioGranted) showScanOptionsDialog()
    }

    private val pickFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { scanSpecificFolder(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 优先读取并应用主题，确保 super.onCreate 拿到的是正确的资源
        val prefs = getSharedPreferences("music_pro", Context.MODE_PRIVATE)
        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (AppCompatDelegate.getDefaultNightMode() != themeMode) {
            AppCompatDelegate.setDefaultNightMode(themeMode)
        }
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. 主题切换动画逻辑 (保持不变)
        if (themeScreenshot != null && !themeScreenshot!!.isRecycled) {
            val rootView = window.decorView as ViewGroup
            val coverImg = ImageView(this)
            coverImg.setImageBitmap(themeScreenshot)
            rootView.addView(coverImg, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            
            val screenshotToRelease = themeScreenshot
            themeScreenshot = null 
            
            coverImg.post {
                if (rootView.width > 0 && rootView.height > 0) {
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
                } else {
                    rootView.removeView(coverImg)
                    screenshotToRelease?.recycle()
                }
            }
        }

        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrev = findViewById(R.id.btn_prev)
        btnNext = findViewById(R.id.btn_next)
        btnScanLocal = findViewById(R.id.btn_scan_local)
        btnPlaylist = findViewById(R.id.btn_playlist)
        btnSettings = findViewById(R.id.btn_settings)
        btnEditList = findViewById(R.id.btn_edit_list)
        ivMiniCover = findViewById(R.id.iv_mini_cover)
        bottomPlayerBar = findViewById(R.id.bottom_player_bar)
        tvMiniTitle = findViewById(R.id.tv_mini_title)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        rvSongs = findViewById(R.id.rv_songs)
        pbScanning = findViewById<ProgressBar>(R.id.pb_scanning)
        btnShowFavorites = findViewById(R.id.btn_show_favorites)
        searchView = findViewById(R.id.search_view)
        btnSearchIcon = findViewById(R.id.btn_search_icon)
        btnBackSearch = findViewById(R.id.btn_back_search)
        
        multiSelectContainer = findViewById(R.id.multi_select_container)
        tvSelectedCount = findViewById(R.id.tv_selected_count)
        btnSelectAll = findViewById(R.id.btn_select_all)
        btnBatchDelete = findViewById(R.id.btn_batch_delete)
        btnBatchAddPlaylist = findViewById(R.id.btn_batch_add_playlist)
        btnBatchFavorite = findViewById(R.id.btn_batch_favorite)
        btnExitMultiSelect = findViewById(R.id.btn_exit_multi_select)
        
        topBar = findViewById(R.id.top_bar)
        topBarContainer = findViewById(R.id.top_bar_container)
        bottomPlayerContainer = findViewById(R.id.bottom_player_container)

        setupSearch()
        
        // 🌟 绝对安全的玻璃效果应用：在视图渲染任务队列中执行
        rvSongs.post {
            GlassUtils.applyGlassEffect(topBarContainer, blurRadius = 25f, cornerRadius = 0f)
            GlassUtils.applyGlassEffect(multiSelectContainer, blurRadius = 25f, cornerRadius = 0f)
            GlassUtils.applyGlassEffect(bottomPlayerContainer, blurRadius = 80f, cornerRadius = 100f)
        }

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
                    val intent = Intent(this, PlayerActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_up, R.anim.stay)
                } else {
                    playFromLibrary(song)
                }
            },
            onItemLongClick = { song -> showLibraryOptions(song) },
            onSelectionChanged = { count -> updateSelectedCount(count) }
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
        updateLibraryView() // Force refresh to ensure RecyclerView renders even if disk load is partial or empty

        btnScanLocal.setOnClickListener { requestPermissionsAndScan() }
        btnPlaylist.setOnClickListener { showPlaybackQueueDialog() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnEditList.setOnClickListener { enterMultiSelectMode() }
        btnSearchIcon.setOnClickListener { expandSearch() }
        btnBackSearch.setOnClickListener { collapseSearch() }
        ivMiniCover.setOnClickListener { showLyricsDialog() }

        btnSelectAll.setOnClickListener {
            val allSelected = libraryAdapter.getSelectedSongs().size == libraryAdapter.itemCount
            libraryAdapter.selectAll(!allSelected)
            btnSelectAll.text = if (!allSelected) getString(R.string.deselect_all) else getString(R.string.select_all)
        }

        btnBatchDelete.setOnClickListener {
            val selected = libraryAdapter.getSelectedSongs()
            if (selected.isEmpty()) return@setOnClickListener
            
            AlertDialog.Builder(this)
                .setTitle("批量删除")
                .setMessage("确定要从曲库移除选中的 ${selected.size} 首歌曲吗？")
                .setPositiveButton("删除") { _, _ ->
                    selected.forEach { deleteFromLibrary(it) }
                    exitMultiSelectMode()
                }
                .setNegativeButton("取消", null)
                .show()
        }

        btnBatchAddPlaylist.setOnClickListener {
            val selected = libraryAdapter.getSelectedSongs()
            if (selected.isEmpty()) return@setOnClickListener
            
            mediaController?.let { controller ->
                val items = selected.map { s ->
                    MediaItem.Builder()
                        .setUri(s.uri)
                        .setMediaId(s.id.toString())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(s.title).setArtist(s.artist).build())
                        .build()
                }
                controller.addMediaItems(items)
                Toast.makeText(this, "已添加 ${selected.size} 首歌曲到播放队列", Toast.LENGTH_SHORT).show()
                exitMultiSelectMode()
            }
        }

        btnBatchFavorite.setOnClickListener {
            val selected = libraryAdapter.getSelectedSongs()
            if (selected.isEmpty()) return@setOnClickListener

            // 逻辑：如果选中的里面有没收藏的，则全部设为收藏；如果全部已收藏，则全部取消收藏
            val anyNotFavorite = selected.any { !it.isFavorite }
            selected.forEach { it.isFavorite = anyNotFavorite }
            
            saveLibraryToDisk(masterSongList)
            updateLibraryView()
            Toast.makeText(this, if (anyNotFavorite) "已批量收藏" else "已取消批量收藏", Toast.LENGTH_SHORT).show()
            exitMultiSelectMode()
        }

        btnExitMultiSelect.setOnClickListener { exitMultiSelectMode() }
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
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                updateLibraryView()
                return true
            }
        })

        // 监听搜索框的关闭按钮（叉号）
        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)?.setOnClickListener {
            if (searchView.query.isNotEmpty()) {
                searchView.setQuery("", false)
            } else {
                collapseSearch()
            }
        }
    }

    private fun expandSearch() {
        TransitionManager.beginDelayedTransition(topBar)
        btnSearchIcon.visibility = View.GONE
        btnBackSearch.visibility = View.VISIBLE
        searchView.visibility = View.VISIBLE
        searchView.isIconified = false
        
        // 🌟 修复：延迟请求焦点和弹出键盘，确保 Transition 动画不会干扰焦点逻辑
        searchView.postDelayed({
            searchView.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // 针对 SearchView 的内部 EditText 请求弹出
            val searchEditText = searchView.findViewById<View>(androidx.appcompat.R.id.search_src_text)
            imm.showSoftInput(searchEditText ?: searchView, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        // 隐藏其他按钮以腾出空间
        btnScanLocal.visibility = View.GONE
        btnShowFavorites.visibility = View.GONE
        btnSettings.visibility = View.GONE
        btnEditList.visibility = View.GONE
    }

    private fun collapseSearch() {
        TransitionManager.beginDelayedTransition(topBar)
        btnSearchIcon.visibility = View.VISIBLE
        btnBackSearch.visibility = View.GONE
        searchView.visibility = View.GONE
        searchView.setQuery("", false)
        searchView.clearFocus()

        // 恢复其他按钮
        btnScanLocal.visibility = View.VISIBLE
        btnShowFavorites.visibility = View.VISIBLE
        btnSettings.visibility = View.VISIBLE
        btnEditList.visibility = View.VISIBLE

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView.windowToken, 0)
    }



    private fun setupController() {
        val controller = mediaController ?: return
        
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
        
        // 异步加载 Mini 播放器封面
        if (song?.cover != null) {
            ivMiniCover.setImageBitmap(song.cover)
        } else {
            ivMiniCover.setImageResource(android.R.drawable.ic_media_play)
            val uri = currentItem?.localConfiguration?.uri
            if (uri != null) {
                Thread {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(this, uri)
                        val artwork = retriever.embeddedPicture
                        if (artwork != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                            ivMiniCover.post { ivMiniCover.setImageBitmap(bitmap) }
                        }
                        retriever.release()
                    } catch (e: Exception) {}
                }.start()
            }
        }
    }

    private fun updateLibraryView() {
        val filteredList = if (isShowingFavorites) {
            masterSongList.filter { it.isFavorite }
        } else {
            masterSongList.toList()
        }.let { list ->
            if (currentSearchQuery.isNotEmpty()) {
                list.filter { song ->
                    song.title.contains(currentSearchQuery, ignoreCase = true) ||
                            song.artist?.contains(currentSearchQuery, ignoreCase = true) == true
                }
            } else {
                list
            }
        }

        runOnUiThread {
            if (filteredList.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvSongs.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvSongs.visibility = View.VISIBLE
                rvSongs.alpha = 1.0f
                libraryAdapter.updateData(filteredList)
                // 强制请求布局和刷新
                rvSongs.requestLayout()
                rvSongs.invalidate()
            }
        }
    }

    private fun showLyricsDialog() {
        val controller = mediaController ?: return
        val currentItem = controller.currentMediaItem ?: return
        val song = masterSongList.find { it.id.toString() == currentItem.mediaId } ?: return

        val dialog = BottomSheetDialog(this, R.style.Theme_MusicPro_Transparent)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_lyrics, null)
        
        dialog.setContentView(view)
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // 🌟 修复：在 show 之后或 DecorView 创建后再设置模糊，防止 NPE 闪退
        dialog.show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.setBackgroundBlurRadius(80)
        }
        
        val glassContainer = view.findViewById<ViewGroup>(R.id.lyrics_glass_container)
        GlassUtils.applyGlassEffect(glassContainer, blurRadius = 40f, cornerRadius = 60f)

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
    }

    private fun showSettingsDialog() {
        val dialog = BottomSheetDialog(this, R.style.Theme_MusicPro_Transparent)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        
        dialog.setContentView(view)
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        dialog.show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.setBackgroundBlurRadius(80)
        }
        
        val glassContainer = view.findViewById<ViewGroup>(R.id.settings_glass_container)
        GlassUtils.applyGlassEffect(glassContainer, blurRadius = 40f, cornerRadius = 60f)

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
                val checkedView = group.findViewById<View>(checkedId)
                if (checkedView != null) {
                    val location = IntArray(2)
                    checkedView.getLocationInWindow(location)
                    themeCenterX = location[0] + checkedView.width / 2
                    themeCenterY = location[1] + checkedView.height / 2
                }
                
                val rootView = window.decorView
                if (rootView.width > 0 && rootView.height > 0) {
                    val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            PixelCopy.request(window, bitmap, { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    themeScreenshot = bitmap
                                }
                                proceedWithThemeChange(prefs, newMode, dialog)
                            }, Handler(Looper.getMainLooper()))
                        } else {
                            val canvas = Canvas(bitmap)
                            rootView.draw(canvas)
                            themeScreenshot = bitmap
                            proceedWithThemeChange(prefs, newMode, dialog)
                        }
                    } catch (e: Exception) {
                        proceedWithThemeChange(prefs, newMode, dialog)
                    }
                } else {
                    proceedWithThemeChange(prefs, newMode, dialog)
                }
            }
        }
    }

    private fun proceedWithThemeChange(prefs: android.content.SharedPreferences, newMode: Int, dialog: BottomSheetDialog) {
        prefs.edit().putInt("theme_mode", newMode).apply()
        dialog.dismiss()
        
        // 确保在主线程应用并重启
        runOnUiThread {
            AppCompatDelegate.setDefaultNightMode(newMode)
            recreate()
        }
    }

    private fun showScanOptionsDialog() {
        val options = arrayOf("全部扫描 (过滤短音频)", "指定文件夹扫描")
        AlertDialog.Builder(this)
            .setTitle("选择扫描方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> scanLocalMusic()
                    1 -> pickFolderLauncher.launch(null)
                }
            }
            .show()
    }

    private fun scanLocalMusic() {
        pbScanning.visibility = View.VISIBLE
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songs = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        
        // 增加时长过滤（> 60秒）
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 60000"
        
        Thread {
            contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.DATE_ADDED} DESC")?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "未知歌曲"
                    val artist = cursor.getString(artistColumn) ?: "未知艺术家"
                    val uri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    songs.add(Song(id, title, artist, uri, null))
                }
            }
            
            runOnUiThread {
                pbScanning.visibility = View.GONE
                if (songs.isNotEmpty()) {
                    masterSongList.clear()
                    masterSongList.addAll(songs)
                    saveLibraryToDisk(masterSongList)
                    updateLibraryView()
                    Toast.makeText(this, "扫描完成，找到 ${songs.size} 首歌曲", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未找到符合条件的本地音乐", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun scanSpecificFolder(treeUri: Uri) {
        pbScanning.visibility = View.VISIBLE
        contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        
        Thread {
            val songs = mutableListOf<Song>()
            val documentFile = DocumentFile.fromTreeUri(this, treeUri)
            if (documentFile != null) {
                traverseDirectory(documentFile, songs)
            }
            
            runOnUiThread {
                pbScanning.visibility = View.GONE
                if (songs.isNotEmpty()) {
                    val currentIds = masterSongList.map { it.id }.toSet()
                    val newSongs = songs.filter { it.id !in currentIds }
                    masterSongList.addAll(newSongs)
                    
                    saveLibraryToDisk(masterSongList)
                    updateLibraryView()
                    Toast.makeText(this, "扫描完成，新增 ${newSongs.size} 首歌曲", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "文件夹内未找到音频文件", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun traverseDirectory(dir: DocumentFile, songs: MutableList<Song>) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                traverseDirectory(file, songs)
            } else {
                val name = file.name ?: ""
                if (name.endsWith(".mp3", true) || name.endsWith(".flac", true) || name.endsWith(".wav", true) || name.endsWith(".m4a", true)) {
                    val song = extractMetadata(file.uri)
                    if (song != null) songs.add(song)
                }
            }
        }
    }

    private fun extractMetadata(uri: Uri): Song? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "未知歌曲"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "未知艺术家"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            if (duration < 60000) return null // 同样过滤短音频
            
            // 使用 URI 的哈希作为 ID，或者使用文件名
            val id = uri.toString().hashCode().toLong()
            Song(id, title, artist, uri, null)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
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

    private fun showLibraryOptions(song: Song) {
        val favoriteAction = if (song.isFavorite) "取消收藏" else "收藏"
        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setItems(arrayOf(favoriteAction, "从曲库移除", "多选模式")) { _, which ->
                when (which) {
                    0 -> toggleFavorite(song)
                    1 -> deleteFromLibrary(song)
                    2 -> enterMultiSelectMode()
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

    private fun enterMultiSelectMode() {
        libraryAdapter.isMultiSelectMode = true
        TransitionManager.beginDelayedTransition(topBarContainer)
        multiSelectContainer.visibility = View.VISIBLE
        topBar.visibility = View.GONE
        bottomPlayerContainer.visibility = View.GONE
        updateSelectedCount(0)
    }

    private fun exitMultiSelectMode() {
        libraryAdapter.isMultiSelectMode = false
        TransitionManager.beginDelayedTransition(topBarContainer)
        multiSelectContainer.visibility = View.GONE
        topBar.visibility = View.VISIBLE
        bottomPlayerContainer.visibility = View.VISIBLE
        btnSelectAll.text = getString(R.string.select_all)
    }

    private fun updateSelectedCount(count: Int) {
        val actualCount = if (count == -1) libraryAdapter.getSelectedSongs().size else count
        tvSelectedCount.text = getString(R.string.selected_count, actualCount)
        
        val allSelected = actualCount == libraryAdapter.itemCount && libraryAdapter.itemCount > 0
        btnSelectAll.text = if (allSelected) getString(R.string.deselect_all) else getString(R.string.select_all)
    }

    override fun onBackPressed() {
        if (libraryAdapter.isMultiSelectMode) {
            exitMultiSelectMode()
        } else if (searchView.visibility == View.VISIBLE) {
            collapseSearch()
        } else {
            super.onBackPressed()
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

        val dialog = BottomSheetDialog(this, R.style.Theme_MusicPro_Transparent)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)
        
        dialog.setContentView(view)
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        dialog.show()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dialog.window?.setBackgroundBlurRadius(80)
        }
        
        val glassContainer = view.findViewById<ViewGroup>(R.id.playlist_glass_container)
        GlassUtils.applyGlassEffect(glassContainer, blurRadius = 40f, cornerRadius = 60f)

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
                // Skip thumbnail loading on main thread to prevent UI lag/blank screen
                s.add(Song(id, o.getString("title"), o.getString("artist"), uri, null, isFavorite))
            }
            masterSongList.clear()
            masterSongList.addAll(s)
            updateLibraryView()
        } catch (e: Exception) {
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v != null && (v is SearchView.SearchAutoComplete || v is android.widget.EditText)) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
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
