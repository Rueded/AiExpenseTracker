package com.example.aiexpensetracker.network

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Collections

class DriveServiceHelper(private val mDriveService: Drive) {

    companion object {
        // 🟢 改为 ZIP 后缀，包含 JSON + 图片
        private const val BACKUP_FILE_NAME = "expense_backup.zip"
    }

    /**
     * 1. 上传 ZIP 文件
     * @param localZipFile 本地已打包好的 zip 文件
     */
    suspend fun uploadZipFile(localZipFile: java.io.File): String? = withContext(Dispatchers.IO) {
        try {
            // 先检查 Drive 上是否已有备份
            val fileId = searchFile(BACKUP_FILE_NAME)

            val metadata = File()
                .setName(BACKUP_FILE_NAME)
                .setMimeType("application/zip") // 🟢 类型改为 zip
                .setParents(Collections.singletonList("appDataFolder")) // 存入私有目录

            // 🟢 使用 FileContent 上传文件流
            val content = FileContent("application/zip", localZipFile)

            if (fileId == null) {
                // 不存在 -> 创建
                val file = mDriveService.files().create(metadata, content)
                    .setFields("id")
                    .execute()
                file.id
            } else {
                // 已存在 -> 覆盖
                mDriveService.files().update(fileId, null, content).execute()
                fileId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 2. 下载 ZIP 文件
     * @param targetFile 下载到的本地目标文件 (通常是 cache 里的临时文件)
     * @param fileId Drive 上的文件 ID
     */
    suspend fun downloadZipFile(targetFile: java.io.File, fileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val outputStream = FileOutputStream(targetFile)
            // 执行下载并将流写入 targetFile
            mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 3. 搜索文件 ID (通用方法)
     */
    suspend fun searchFile(fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = mDriveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$fileName' and trashed = false")
                .setFields("files(id, name)")
                .execute()

            if (result.files.isNotEmpty()) result.files[0].id else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}