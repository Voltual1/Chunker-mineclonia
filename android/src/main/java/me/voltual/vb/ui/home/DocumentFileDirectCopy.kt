package me.voltual.vb.ui.home

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.id
import com.anggrayudi.storage.file.toRawFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

sealed class DirectCopyResult {
    object Preparing : DirectCopyResult()
    data class InProgress(val bytesMoved: Long) : DirectCopyResult()
    data class Completed(val folder: DocumentFile) : DirectCopyResult()
    data class Error(val exception: Throwable) : DirectCopyResult()
}

private class ChildInfo(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val isDirectory: Boolean
)

fun DocumentFile.copyFolderDirectlyTo(
    context: Context,
    targetParentFolder: DocumentFile,
    newFolderName: String
): Flow<DirectCopyResult> = callbackFlow {
    trySend(DirectCopyResult.Preparing)

    // 1. 将目标 DocumentFile 解析为本地物理 File
    val targetParentFile = targetParentFolder.toRawFile(context)
        ?: File(targetParentFolder.uri.path ?: "/").also {
            if (!it.exists()) it.mkdirs()
        }

    val destFolder = File(targetParentFile, newFolderName)
    if (!destFolder.exists() && !destFolder.mkdirs()) {
        trySend(DirectCopyResult.Error(IOException("无法创建目标本地文件夹: ${destFolder.absolutePath}")))
        close()
        return@callbackFlow
    }

    // 2. 检查源文件夹是否可以直接通过 JVM File 访问（如已授权或 API 29 以下）
    val srcRawFile = this@copyFolderDirectlyTo.toRawFile(context)
    if (srcRawFile != null && srcRawFile.exists() && srcRawFile.canRead()) {
        // 【通道 A】：纯本地 JVM 递归复制（极其快速，零拷贝）
        try {
            var totalBytesMoved = 0L

            fun copyFileToFile(src: File, dest: File): Boolean {
                var inChannel: FileChannel? = null
                var outChannel: FileChannel? = null
                var fis: FileInputStream? = null
                var fos: FileOutputStream? = null
                return try {
                    fis = FileInputStream(src)
                    fos = FileOutputStream(dest)
                    inChannel = fis.channel
                    outChannel = fos.channel
                    inChannel.transferTo(0, inChannel.size(), outChannel)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                } finally {
                    inChannel?.close()
                    outChannel?.close()
                    fis?.close()
                    fos?.close()
                }
            }

            fun recurseRaw(s: File, d: File) {
                if (s.isDirectory) {
                    d.mkdirs()
                    s.listFiles()?.forEach { child ->
                        recurseRaw(child, File(d, child.name))
                    }
                } else if (s.isFile) {
                    if (copyFileToFile(s, d)) {
                        totalBytesMoved += s.length()
                        trySend(DirectCopyResult.InProgress(totalBytesMoved))
                    }
                }
            }

            recurseRaw(srcRawFile, destFolder)
            trySend(DirectCopyResult.Completed(DocumentFile.fromFile(destFolder)))
        } catch (e: Exception) {
            trySend(DirectCopyResult.Error(e))
        }
    } else {
        // 【通道 B】：SAF Tree 自动批量查询 -> 本地物理写入
        try {
            var totalBytesMoved = 0L

            fun copyUriToFile(sourceUri: Uri, targetFile: File): Boolean {
                var inChannel: FileChannel? = null
                var outChannel: FileChannel? = null
                var pfd: android.os.ParcelFileDescriptor? = null
                var fos: FileOutputStream? = null
                return try {
                    pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return false
                    val fis = FileInputStream(pfd.fileDescriptor)
                    fos = FileOutputStream(targetFile)
                    inChannel = fis.channel
                    outChannel = fos.channel
                    inChannel.transferTo(0, inChannel.size(), outChannel)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                } finally {
                    inChannel?.close()
                    outChannel?.close()
                    fos?.close()
                    pfd?.close()
                }
            }

            fun listChildren(parentUri: Uri, parentId: String): List<ChildInfo> {
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentId)
                val results = mutableListOf<ChildInfo>()
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                )
                try {
                    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                        val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                        val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                        val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                        while (cursor.moveToNext()) {
                            val docId = cursor.getString(idIndex)
                            val name = cursor.getString(nameIndex)
                            val mimeType = cursor.getString(mimeIndex)
                            val size = if (cursor.isNull(sizeIndex)) 0L else cursor.getLong(sizeIndex)
                            val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId)
                            val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                            results.add(ChildInfo(childUri, name, mimeType, size, isDir))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return results
            }

            fun recurseSaf(currentUri: Uri, currentId: String, d: File) {
                d.mkdirs()
                val children = listChildren(currentUri, currentId)
                for (child in children) {
                    val targetChild = File(d, child.name)
                    if (child.isDirectory) {
                        val childId = DocumentsContract.getDocumentId(child.uri)
                        recurseSaf(child.uri, childId, targetChild)
                    } else {
                        if (copyUriToFile(child.uri, targetChild)) {
                            totalBytesMoved += child.size
                            trySend(DirectCopyResult.InProgress(totalBytesMoved))
                        }
                    }
                }
            }

            val rootId = this@copyFolderDirectlyTo.id
            recurseSaf(this@copyFolderDirectlyTo.uri, rootId, destFolder)
            trySend(DirectCopyResult.Completed(DocumentFile.fromFile(destFolder)))
        } catch (e: Exception) {
            trySend(DirectCopyResult.Error(e))
        }
    }
    close()
}