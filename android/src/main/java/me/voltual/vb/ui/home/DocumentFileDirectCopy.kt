package me.voltual.vb.ui.home

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException

/**
 * 针对超大型 Minecraft 存档深度优化的极速流式复制方案。
 * 1. 摒弃 DocumentFile 树扫描，直接通过底层 ContentResolver 游标高效遍历。
 * 2. 目标路径直接采用本地物理 java.io.File 写入，彻底绕过 SAF 写入瓶颈。
 * 3. 使用 Okio 缓冲区进行高效零拷贝对拷。
 */
fun DocumentFile.copyFolderDirectlyTo(
    context: Context,
    targetParentDir: File // 传入物理 java.io.File 目标本地路径
): Flow<DirectCopyResult> = callbackFlow {
    trySend(DirectCopyResult.Preparing)

    if (!targetParentDir.exists()) {
        targetParentDir.mkdirs()
    }

    val rootUri = this@copyFolderDirectlyTo.uri
    val rootDocId = DocumentsContract.getDocumentId(rootUri)
    var totalBytesMoved = 0L
    val okioBuffer = Buffer()

    // 递归读取游标并高速写入磁盘
    fun copyRecursive(currentDocId: String, currentDestDir: File) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(rootUri, currentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            if (idIndex == -1 || nameIndex == -1 || mimeIndex == -1) return@use

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeIndex)

                if (docId == null || name == null) continue

                val destFile = File(currentDestDir, name)

                if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                    // 如果是目录，直接在本地磁盘创建目录并递归
                    destFile.mkdirs()
                    copyRecursive(docId, destFile)
                } else {
                    // 如果是文件，直接通过 Okio 管道进行高速流式对拷
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(rootUri, docId)
                    try {
                        context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                            destFile.outputStream().sink().buffer().use { sink ->
                                inputStream.source().buffer().use { source ->
                                    var read: Long
                                    while (source.read(okioBuffer, 8192L).also { read = it } != -1L) {
                                        sink.write(okioBuffer, read)
                                        totalBytesMoved += read
                                    }
                                    sink.flush()
                                }
                            }
                        }
                        // 每次成功复制一个文件，向 UI 汇报一次进度，避免频繁汇报造成主线程卡顿
                        trySend(DirectCopyResult.InProgress(totalBytesMoved))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    try {
        copyRecursive(rootDocId, targetParentDir)
        trySend(DirectCopyResult.Completed(targetParentDir))
    } catch (e: Exception) {
        trySend(DirectCopyResult.Error(e))
    } finally {
        close()
    }
}

// 统一使用的状态密封类
sealed class DirectCopyResult {
    object Preparing : DirectCopyResult()
    data class InProgress(val bytesMoved: Long) : DirectCopyResult()
    data class Completed(val folder: File) : DirectCopyResult()
    data class Error(val exception: Throwable) : DirectCopyResult()
}