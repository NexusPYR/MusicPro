package com.example.musicpro

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

object NcmUtils {

    // 标准网易云 NCM 核心密钥 (hzHRAmso5k0uUYe-)
    private val CORE_KEY = byteArrayOf(
        0x68, 0x7A, 0x48, 0x52, 0x41, 0x6D, 0x73, 0x6F,
        0x35, 0x6B, 0x30, 0x75, 0x55, 0x59, 0x65, 0x2D
    )

    /**
     * 将 NCM 文件解密并转换为 MP3/FLAC
     * 严格遵循 NCM 格式规范，处理无符号字节和音频偏移量
     */
    fun convertNcm(input: InputStream, output: OutputStream): Boolean {
        val bis = BufferedInputStream(input)
        val bos = BufferedOutputStream(output)
        try {
            // 1. 定义标准的 NCM 文件头字节 (CTENFDAM 的十六进制)
            val MAGIC_HEADER = byteArrayOf(0x43, 0x54, 0x45, 0x4E, 0x46, 0x44, 0x41, 0x4D)

            // 2. 准备一个 8 字节的空数组来装读取的数据
            val header = ByteArray(8)
            val headerReadCount = bis.read(header)

            // 3. 打印到底读到了什么（这步极其关键，用来破案）
            val hexString = header.joinToString("") { "%02x ".format(it) }
            android.util.Log.d("NCM_DEBUG", "实际读取了 $headerReadCount 个字节")
            android.util.Log.d("NCM_DEBUG", "读到的十六进制数据是: $hexString")
            android.util.Log.d("NCM_DEBUG", "读到的字符串是: ${String(header)}")

            // 4. 进行最严格的字节级比对
            if (headerReadCount != 8 || !header.contentEquals(MAGIC_HEADER)) {
                android.util.Log.e("NCM_DEBUG", "💥 校验失败！文件不是标准的 NCM 格式")
                return false
            }
            android.util.Log.d("NCM_DEBUG", "✅ 头校验通过，开始解密...")

            // 5. 跳过 2 字节版本 Gap (之前读了 8 字节，这里再跳 2 字节，凑满 10 字节头)
            bis.skip(2)

            // 2. 解析 Key 块 (RC4 密钥)
            val keyDataLen = readInt(bis)
            if (keyDataLen < 0) return false
            val keyData = ByteArray(keyDataLen)
            bis.read(keyData)
            for (i in keyData.indices) keyData[i] = keyData[i] xor 0x64
            
            val deKeyData = decryptAes(keyData, CORE_KEY)
            // 去掉 "neteasecloudmusic" 前缀 (17 字节)
            val keyBoxKey = deKeyData.sliceArray(17 until deKeyData.size)
            val keyBox = buildKeyBox(keyBoxKey)

            // 3. 解析 Metadata 块 (可选)
            val metaDataLen = readInt(bis)
            if (metaDataLen > 0) {
                val metaData = ByteArray(metaDataLen)
                bis.read(metaData)
                // 这里暂不处理元数据 JSON
            }

            // 4. 跳过 CRC (5 字节)
            bis.skip(5)

            // 5. 处理 Gap / Image 块 (关键：精准定位音频起点)
            // NCM 结构中 Meta 后通常跟着 4 字节的 Image 大小
            val imageLen = readInt(bis)
            if (imageLen > 0) {
                // 跳过封面图片数据
                var totalSkipped = 0L
                while (totalSkipped < imageLen) {
                    val skipped = bis.skip(imageLen.toLong() - totalSkipped)
                    if (skipped <= 0) break
                    totalSkipped += skipped
                }
            }

            // 6. 解密音频流 (真正的音频起点)
            val buffer = ByteArray(0x8000) // 32KB
            var bytesRead: Int
            var streamOffset = 0
            
            while (bis.read(buffer).also { bytesRead = it } != -1) {
                for (i in 0 until bytesRead) {
                    val j = (streamOffset + i + 1) and 0xff
                    // 核心 RC4 XOR 逻辑，严格处理无符号字节转换
                    val k = keyBox[j].toInt() and 0xff
                    buffer[i] = buffer[i] xor k.toByte()
                }
                bos.write(buffer, 0, bytesRead)
                streamOffset = (streamOffset + bytesRead) and 0xff
            }
            bos.flush()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { bis.close() } catch (e: Exception) {}
            try { bos.close() } catch (e: Exception) {}
        }
    }

    private fun readInt(input: InputStream): Int {
        val bytes = ByteArray(4)
        var totalRead = 0
        while (totalRead < 4) {
            val read = input.read(bytes, totalRead, 4 - totalRead)
            if (read == -1) return -1
            totalRead += read
        }
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.int
    }

    private fun decryptAes(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(data)
    }

    private fun buildKeyBox(key: ByteArray): ByteArray {
        val box = ByteArray(256)
        for (i in 0..255) box[i] = i.toByte()

        var j = 0
        for (i in 0..255) {
            val boxI = box[i].toInt() and 0xff
            val keyI = key[i % key.size].toInt() and 0xff
            j = (j + boxI + keyI) and 0xff

            val temp = box[i]
            box[i] = box[j]
            box[j] = temp
        }

        val lastBox = ByteArray(256)
        for (i in 0..255) {
            val idx = (i + 1) and 0xff
            val a = box[idx].toInt() and 0xff
            val b = box[(idx + a) and 0xff].toInt() and 0xff
            lastBox[i] = box[(a + b) and 0xff]
        }
        return lastBox
    }
}