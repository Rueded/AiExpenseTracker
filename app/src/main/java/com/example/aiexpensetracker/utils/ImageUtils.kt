package com.example.aiexpensetracker.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    // 将选中的图片保存到 App 私有目录，并返回保存后的路径
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // 创建一个随机文件名的图片文件
            val fileName = "receipt_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)

            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            file.absolutePath // 返回绝对路径
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}