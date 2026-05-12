# 🎵 Music Pro (离线音乐播放器 Pro)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-blue?style=for-the-badge)

Music Pro 是一款专注于提供纯粹、高解析度本地听歌体验的 Android 离线音乐播放器。项目采用最新的 MVC/MVVM 混合架构，基于 Google 现代媒体框架 **Media3 (ExoPlayer)** 构建，旨在解决流媒体时代本地无损音乐（FLAC/WAV/MP3）管理与播放的痛点。

---

## ✨ 核心特性 (Features)

* 💿 **黑胶沉浸交互**：打造具备高度质感的主控制台与全屏播放页，利用 `ObjectAnimator` 实现拟物化黑胶唱片匀速旋转，实时解析并渲染音频内嵌的高清专辑封面。
* 🚀 **现代播放架构**：底层采用强大的 `ExoPlayer` 引擎与 `MediaSessionService`，彻底解耦 UI 控制与后台播放逻辑。完美支持系统级锁屏控制、通知栏媒体中心无缝对接以及后台持久运行。
* 🧠 **智能硬件感应**：深度集成系统级广播监听 (`BroadcastReceiver`)，精准捕获 `ACTION_AUDIO_BECOMING_NOISY` 意图。当有线或蓝牙耳机意外断开连接时，毫秒级自动暂停播放，避免外放社交尴尬。
* ⚡ **极速本地扫描**：规避低效的传统文件遍历，直接利用 `ContentResolver` 与 `MediaStore` API 进行 SQL 级条件查询，毫秒级构建本地全盘无损曲库。
* 🎨 **丝滑手势列表**：基于 `RecyclerView` 搭配 `ItemTouchHelper`，支持流畅的单曲侧滑移除与长按拖拽排序，并通过 `SharedPreferences` 实现列表状态的 JSON 持久化。
* 🌓 **深浅主题自适应**：完全遵循 Material Design 规范，支持跟随系统或手动切换的深色/浅色模式，夜间听歌更护眼。

---

## 🛠️ 技术栈 (Tech Stack)

* **开发语言**：Kotlin
* **目标 SDK**：API 36 (Android 15/16)
* **核心媒体库**：`androidx.media3:media3-exoplayer:1.3.1` / `media3-session`
* **UI 组件库**：Material Components for Android (`com.google.android.material`)
* **异步机制**：ListenableFuture / 接口回调
* **数据存储**：SharedPreferences (JSON 序列化) / MediaStore API

---


## 🚀 构建与运行 (Getting Started)

### 环境要求
* **IDE**: Android Studio (推荐最新版本)
* **Java**: JDK 11 或更高版本
* **测试设备**: 运行 Android 13 (API 33) 或更高版本的真机（推荐三星、华为、小米等现代机型）以获得完整的媒体权限体验。

### 运行步骤
1. 克隆本项目到本地：
   ```bash
   git clone [https://github.com/NexusPYR/MusicPro.git](https://github.com/NexusPYR/MusicPro.git)
