package me.voltual.vb.ui.home

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.extension.closeStreamQuietly
import com.anggrayudi.storage.extension.openOutputStream
import com.anggrayudi.storage.file.CreateMode
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.makeFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.IOException

/**
 * 专门针对超大型存档设计的流式复制方案。
 * 摒弃前置的树扫描计数，采用「边扫描边复制」策略，实现秒级响应。
 */
fun DocumentFile.copyFolderDirectlyTo(
    context: Context,
    targetParentFolder: DocumentFile,
    newFolderName: String
): Flow<DirectCopyResult> = callbackFlow {
    trySend(DirectCopyResult.Preparing)

    // 1. 在目标路径创建根目录
    val targetFolder = targetParentFolder.makeFolder(context, newFolderName, CreateMode.REUSE)
    if (targetFolder == null) {
        trySend(DirectCopyResult.Error(IOException("无法在目标路径创建根目录")))
        close()
        return@callbackFlow
    }

    var totalBytesMoved = 0L
    val buffer = ByteArray(8192) // 8KB 缓冲区

    // 2. 递归流式复制函数
    fun copyRecursive(sourceDir: DocumentFile, currentDestDir: DocumentFile) {
        val children = sourceDir.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                // 边扫描边创建子目录
                val nextDestDir = currentDestDir.makeFolder(context, child.name.orEmpty(), CreateMode.REUSE)
                if (nextDestDir != null) {
                    copyRecursive(child, nextDestDir)
                }
            } else if (child.isFile) {
                // 边扫描边创建并复制文件
                val destFile = currentDestDir.makeFile(context, child.name.orEmpty(), child.type, CreateMode.REUSE)
                if (destFile == null) continue

                // 使用原生 ContentResolver 打开源文件的输入流，安全避开编译链接问题
                var inputStream = context.contentResolver.openInputStream(child.uri)
                var outputStream = destFile.openOutputStream(context, append = false)
                try {
                    if (inputStream != null && outputStream != null) {
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            totalBytesMoved += bytes
                            bytes = inputStream.read(buffer)
                        }
                        // 实时向 UI 回传已传输的物理字节大小
                        trySend(DirectCopyResult.InProgress(totalBytesMoved))
                    }
                } catch (e: Exception) {
                    // 容错处理：单个文件失败不中断整个大存档复制
                    e.printStackTrace()
                } finally {
                    inputStream.closeStreamQuietly()
                    outputStream.closeStreamQuietly()
                }
            }
        }
    }

    // 3. 执行物理流式复制
    copyRecursive(this@copyFolderDirectlyTo, targetFolder)
    
    trySend(DirectCopyResult.Completed(targetFolder))
    close()
}

// 轻量级状态密封类
sealed class DirectCopyResult {
    object Preparing : DirectCopyResult()
    data class InProgress(val bytesMoved: Long) : DirectCopyResult()
    data class Completed(val folder: DocumentFile) : DirectCopyResult()
    data class Error(val exception: Throwable) : DirectCopyResult()
}