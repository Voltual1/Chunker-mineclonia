// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.core.utils

import android.content.Context
import android.os.Environment
import com.anggrayudi.storage.media.FileDescription
import com.anggrayudi.storage.media.MediaStoreCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.net.NetworkInterface
import java.net.SocketException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

object WorldExporter {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private const val PORT = 8080

    /**
     * 提供外部调用的导出方法。
     * 将临时预览文件夹压缩打包，并通过 MediaStore 零 SAF 限制高速输出到系统的 Downloads 公共文件夹中。
     */
    fun exportWorld(context: Context, sourceFolder: File): Boolean {
        if (!sourceFolder.exists()) return false
        
        // 1. 在缓存目录生成一个临时 zip
        val tempZip = File(context.cacheDir, "${sourceFolder.name}_exported.zip")
        val zipSuccess = zipFolder(sourceFolder, tempZip)
        if (!zipSuccess || !tempZip.exists()) return false

        try {
            // 2. 利用 MediaStoreCompat (由 SimpleStorage 提供) 将 zip 发送到公共下载目录
            val desc = FileDescription("${sourceFolder.name}_export.zip", "", "application/zip")
            val mediaFile = MediaStoreCompat.createDownload(context, desc)
            if (mediaFile != null) {
                mediaFile.openOutputStream(false)?.use { outputStream ->
                    tempZip.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                tempZip.delete() // 清理临时文件
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 将指定文件夹打包压缩为 ZIP 文件
     */
    fun zipFolder(sourceFolder: File, zipFile: File): Boolean {
        if (!sourceFolder.exists()) return false
        try {
            if (zipFile.exists()) {
                zipFile.delete()
            }
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                zipFileOrDirectory(sourceFolder, sourceFolder, zos)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun zipFileOrDirectory(rootFolder: File, sourceFile: File, zos: ZipOutputStream) {
        if (sourceFile.isDirectory) {
            val files = sourceFile.listFiles() ?: return
            for (file in files) {
                zipFileOrDirectory(rootFolder, file, zos)
            }
        } else {
            val buffer = ByteArray(8192)
            FileInputStream(sourceFile).use { fis ->
                val entryName = sourceFile.absolutePath.substring(rootFolder.absolutePath.length + 1)
                zos.putNextEntry(ZipEntry(entryName))
                var length: Int
                while (fis.read(buffer).also { length = it } > 0) {
                    zos.write(buffer, 0, length)
                }
                zos.closeEntry()
            }
        }
    }
}