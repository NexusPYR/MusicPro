package com.example.musicpro

import android.graphics.Bitmap
import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String? = "未知艺术家",
    val uri: Uri,
    val cover: Bitmap? = null,
    var isFavorite: Boolean = false // 🌟 新增：用于标记是否收藏
)
