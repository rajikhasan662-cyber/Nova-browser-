package com.example.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import com.example.data.db.DownloadDao
import com.example.data.db.DownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DownloadManagerService(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val scope: CoroutineScope
) {

    private val systemDownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueueDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ): Long {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)

        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(fileName)
            setDescription("Downloading in NOVA Browser")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            if (mimeType != null) {
                setMimeType(mimeType)
            }
        }

        val downloadId = systemDownloadManager.enqueue(request)

        val entity = DownloadEntity(
            downloadId = downloadId,
            fileName = fileName,
            url = url,
            filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$fileName",
            totalBytes = contentLength,
            downloadedBytes = 0,
            status = "RUNNING",
            mimeType = mimeType ?: "*/*"
        )

        scope.launch(Dispatchers.IO) {
            downloadDao.insertDownload(entity)
        }

        return downloadId
    }

    fun openDownloadedFile(item: DownloadEntity): Boolean {
        val file = File(item.filePath)
        if (!file.exists()) return false

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareDownloadedFile(item: DownloadEntity) {
        val file = File(item.filePath)
        if (!file.exists()) return

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share ${item.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // handle error
        }
    }

    fun deleteDownload(item: DownloadEntity) {
        scope.launch(Dispatchers.IO) {
            systemDownloadManager.remove(item.downloadId)
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            downloadDao.deleteById(item.downloadId)
        }
    }

    fun cancelDownload(downloadId: Long) {
        systemDownloadManager.remove(downloadId)
        scope.launch(Dispatchers.IO) {
            downloadDao.deleteById(downloadId)
        }
    }
}
