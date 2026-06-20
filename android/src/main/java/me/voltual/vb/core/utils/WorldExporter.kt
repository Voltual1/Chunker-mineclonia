package me.voltual.vb.core.utils

import android.content.Context
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
     * 启动局域网 HTTP 服务 (基于 Socket 纯手工实现，规避 R8 缺失类问题)
     */
    @Synchronized
    fun startHttpServer(context: Context, onStarted: (String) -> Unit, onError: (String) -> Unit) {
        if (isRunning) {
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
            
            serverSocket = ServerSocket(PORT)
            isRunning = true

            thread(name = "VB-HTTP-Server") {
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        thread {
                            handleClient(socket, zipFile)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            }

            onStarted("http://$ip:$PORT/download")
        } catch (e: Exception) {
            e.printStackTrace()
            serverSocket = null
            isRunning = false
            onError(e.message ?: "启动服务器失败")
        }
    }

    private fun handleClient(socket: Socket, zipFile: File) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            
            // 解析 HTTP 请求行，例如: GET /download HTTP/1.1 或 GET / HTTP/1.1
            val tokens = requestLine.split(" ")
            if (tokens.size >= 2 && (tokens[1] == "/download" || tokens[1] == "/")) {
                if (!zipFile.exists()) {
                    val body = "Error: Output ZIP file not found. Please convert a world first."
                    val headers = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain; charset=utf-8\r\n" +
                            "Content-Length: ${body.toByteArray().size}\r\n" +
                            "Connection: close\r\n\r\n"
                    socket.getOutputStream().write(headers.toByteArray())
                    socket.getOutputStream().write(body.toByteArray())
                } else {
                    val headers = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/zip\r\n" +
                            "Content-Length: ${zipFile.length()}\r\n" +
                            "Content-Disposition: attachment; filename=\"world_output.zip\"\r\n" +
                            "Connection: close\r\n\r\n"
                    socket.getOutputStream().write(headers.toByteArray())
                    
                    FileInputStream(zipFile).use { input ->
                        input.copyTo(socket.getOutputStream())
                    }
                }
            } else {
                val body = "Only GET /download or GET / is supported."
                val headers = "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: ${body.toByteArray().size}\r\n" +
                        "Connection: close\r\n\r\n"
                socket.getOutputStream().write(headers.toByteArray())
                socket.getOutputStream().write(body.toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 停止局域网 HTTP 服务
     */
    @Synchronized
    fun stopHttpServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
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