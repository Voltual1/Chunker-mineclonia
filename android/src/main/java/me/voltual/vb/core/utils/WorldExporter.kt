package me.voltual.vb.core.utils

import android.content.Context
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object WorldExporter {

    private var httpServer: HttpServer? = null
    private const val PORT = 8080

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

    /**
     * 启动局域网 HTTP 服务
     */
    @Synchronized
    fun startHttpServer(context: Context, onStarted: (String) -> Unit, onError: (String) -> Unit) {
        if (httpServer != null) {
            val ip = getLocalIpAddress()
            if (ip != null) {
                onStarted("http://$ip:$PORT/download")
            } else {
                onError("无法获取本地 IP 地址，请检查 Wi-Fi 连接")
            }
            return
        }

        try {
            val ip = getLocalIpAddress() ?: throw SocketException("未连接到局域网或无法获取 IP")
            val zipFile = File(context.filesDir, "world_output.zip")

            httpServer = HttpServer.create(InetSocketAddress(PORT), 0).apply {
                createContext("/download") { exchange ->
                    if (!zipFile.exists()) {
                        val response = "Error: Output ZIP file not found. Please convert a world first.".toByteArray()
                        exchange.sendResponseHeaders(404, response.size.toLong())
                        exchange.responseBody.use { it.write(response) }
                        return@createContext
                    }

                    exchange.responseHeaders.set("Content-Type", "application/zip")
                    exchange.responseHeaders.set("Content-Disposition", "attachment; filename=\"world_output.zip\"")
                    exchange.sendResponseHeaders(200, zipFile.length())

                    FileInputStream(zipFile).use { input ->
                        exchange.responseBody.use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                executor = null // 使用默认的单线程执行器
                start()
            }
            onStarted("http://$ip:$PORT/download")
        } catch (e: Exception) {
            e.printStackTrace()
            httpServer = null
            onError(e.message ?: "启动服务器失败")
        }
    }

    /**
     * 停止局域网 HTTP 服务
     */
    @Synchronized
    fun stopHttpServer() {
        httpServer?.stop(0)
        httpServer = null
    }

    /**
     * 获取手机在局域网中的 IPv4 地址
     */
    fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }
}