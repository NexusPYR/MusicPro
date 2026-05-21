# 🎵 Music Pro (离线音乐播放器 Pro)

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-blue?style=for-the-badge)

Music Pro 是一款专注于提供纯粹、高解析度本地听歌体验的 Android 离线音乐播放器。项目采用最新的 MVC/MVVM 混合架构，基于 Google 现代媒体框架 **Media3 (ExoPlayer)** 构建，旨在解决流媒体时代本地无损音乐（FLAC/WAV/MP3）管理与播放的痛点。

---

## ✨ 核心特性 (Features)

* 🌌 **玻璃拟态 UI**：全局应用高斯模糊与半透明容器，营造轻盈、通透的磨砂玻璃质感。
* 🌊 **动态流体背景**：基于 AGSL 着色器 (Android 13+) 的实时渲染背景，随音乐律动提供丝滑的视觉过渡。
* 🌓 **主题无缝切换**：支持深色/浅色模式，并配有基于 PixelCopy 的圆形遮罩扩散动画，让视觉切换毫无割裂感，支持跟随系统。
* 💿 **黑胶沉浸交互**：打造具备高度质感的主控制台与全屏播放页，利用 `ObjectAnimator` 实现拟物化黑胶唱片匀速旋转，实时解析并渲染音频内嵌的高清专辑封面。
* 🚀 **现代播放架构**：底层采用强大的 `ExoPlayer` 引擎与 `MediaSessionService`，彻底解耦 UI 控制与后台播放逻辑。完美支持系统级锁屏控制、通知栏媒体中心无缝对接以及后台持久运行。
* 🧠 **智能硬件感应**：深度集成系统级广播监听 (`BroadcastReceiver`)，精准捕获 `ACTION_AUDIO_BECOMING_NOISY` 意图。当有线或蓝牙耳机意外断开连接时，毫秒级自动暂停播放，避免外放社交尴尬。
* ⚡ **极速本地扫描**：规避低效的传统文件遍历，直接利用 `ContentResolver` 与 `MediaStore` API 进行 SQL 级条件查询，毫秒级构建本地全盘无损曲库。
* 🎨 **丝滑手势列表**：基于 `RecyclerView` 搭配 `ItemTouchHelper`，支持流畅的单曲侧滑移除与长按拖拽排序，并通过 `SharedPreferences` 实现列表状态的 JSON 持久化。
* 🔍 **智能本地扫描**：
  * **全盘扫描**：自动提取设备中的音乐文件并过滤掉短音频（如系统提示音）。
  * **指定目录扫描**：精准定位您的音乐库。
  * **自动识别 NCM 格式**：检测到不支持的加密格式时提供便捷的在线转换指引。
* 📂 **强大的媒体管理**：
  * **多选模式**：支持批量删除、批量收藏及批量添加到播放队列。
  * **收藏系统**：快速标记您喜爱的歌曲。
  * **搜索过滤**：即时搜索标题或艺术家。
* 🎼 **高标准音频架构**：
  * 基于 **Jetpack Media3 (ExoPlayer & MediaSession)** 构建，确保播放稳定且兼容标准媒体控制。
  * **内嵌歌词解析**：支持查看音频文件内嵌的歌词信息。
  * **智能感应**：支持耳机断开自动暂停，避免外放尴尬。


---

## 🛠️ 技术栈 (Tech Stack)

* **开发语言**：Kotlin
* **目标 SDK**：API 36 (Android 16)
* **核心框架**: Jetpack Media3 (ExoPlayer
* **核心媒体库**：`androidx.media3:media3-exoplayer:1.3.1` / `media3-session`
* **UI 架构**: Material Design 3 + Custom Glassmorphism System
* **着色器**: AGSL (Android Graphics Shading Language
* **异步机制**：Kotlin Coroutines & Lifecycle Scope
* **数据持久化**: SharedPreferences (JSON 序列化)

---
## 📸 预览


* **📖目录**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/44b12dcc-4954-4fd0-8ba8-eef936da0a94" />

* **💿正在播放的歌曲**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/85332a1c-83fc-40d1-9c5d-db570f3d6130" />

* **↩️正在播放的歌曲预测性返回手势**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/59a7a634-3c95-4a08-ac34-423e041c7034" />

* **⚙️设置**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/8d7abdbd-e065-40ec-993b-57565e732433" />

* **🔦扫描本地音乐**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/cce045ce-81ad-4e4a-ade5-3bf99d7987f3" />

* **✏️多项编辑**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/7abf1458-1061-489c-92e6-a61349c567a5" />

* **❤️收藏功能**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/208b4653-9614-4197-8e89-b903108d74fa" />

* **🚫格式不支持时可选择跳转第三方解密**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/7c6b9a53-cf77-431d-85ac-769ad70f3dda" />

* **☀️浅色模式下主页**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/58c16f63-9b0c-4a71-9604-4d121af061a7" />

* **🔍搜索栏**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/68d66da2-3a93-4040-9ead-d0e2e0a0d740" />

* **🔄播放歌曲时封面自动旋转**
<img width="390" height="854" alt="VID_20260521_212703" src="https://github.com/user-attachments/assets/aaad4cfc-b372-4ad5-a5a5-f44a9483fd85" />

* **👆长按选择功能**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/1caebf00-af69-43d8-9240-d02bc1b5eadb" />


* **📋查看歌曲详细信息**
<img width="40%" alt="Screenshot" src="https://github.com/user-attachments/assets/6ba9fe86-dabb-4dd3-af4c-109af640ff91" />



---

## 🚀 构建与运行 (Getting Started)

### 环境要求
* **IDE**: Android Studio (推荐最新版本)
* **Java**: JDK 17
* **测试设备**: 运行 Android 16 (API 34) 或更高版本的真机（推荐三星、小米、oppo、vivo等现代机型）以获得完整的媒体权限体验。

  

