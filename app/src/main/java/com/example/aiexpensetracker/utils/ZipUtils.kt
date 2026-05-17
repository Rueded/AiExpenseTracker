package com.example.aiexpensetracker.utils

import android.content.Context
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    // 1. 压缩：把 JSON 内容和图片列表打包成 zip
    fun zipData(context: Context, jsonString: String, imagePaths: List<String>, zipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            // A. 写入 JSON 数据
            val jsonEntry = ZipEntry("data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(jsonString.toByteArray())
            zos.closeEntry()

            // B. 写入图片
            for (path in imagePaths) {
                if (path.isNullOrBlank()) continue
                val sourceFile = File(path)
                if (sourceFile.exists()) {
                    // 在 zip 里的文件名 (防止路径冲突，只取文件名)
                    val fileName = sourceFile.name
                    val imageEntry = ZipEntry("images/$fileName")
                    zos.putNextEntry(imageEntry)

                    FileInputStream(sourceFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    // 2. 解压：解压 zip，返回 JSON 内容，并把图片释放到 app 私有目录
    fun unzipData(context: Context, zipFile: File): String? {
        var jsonContent: String? = null
        val imagesDir = context.filesDir // App 私有目录

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryName = entry.name

                if (entryName == "data.json") {
                    // A. 读取 JSON
                    jsonContent = zis.bufferedReader().readText()
                } else if (entryName.startsWith("images/")) {
                    // B. 恢复图片
                    // 只有文件名，例如 "images/receipt_123.jpg" -> "receipt_123.jpg"
                    val fileName = File(entryName).name
                    val outFile = File(imagesDir, fileName)

                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return jsonContent
    }
}