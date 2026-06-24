package me.voltual.vb.core.ftp

import android.content.Context
import android.os.Environment
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.slf4j.LoggerFactory
import java.io.File

class FtpServerManager(private val context: Context) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var ftpServer: FtpServer? = null

    // 状态流供 Compose 订阅
    var isRunning = false
        private set

    /**
     * 启动 FTP 服务器
     * @param port 端口号，默认 2121
     * @param username 用户名
     * @param password 密码
     * @param ftpRootDir 共享的根目录（例如：应用的 world_input/output 目录）
     */
    fun startServer(
        port: Int = 2121,
        username: String = "admin",
        password: String = "admin123",
        ftpRootDir: File
    ): Boolean {
        if (ftpServer != null && isRunning) {
            logger.info("FTP Server is already running.")
            return true
        }

        try {
            // 创建根目录
            if (!ftpRootDir.exists()) {
                ftpRootDir.mkdirs()
            }

            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()
            listenerFactory.port = port

            // 1. 绑定监听器
            serverFactory.addListener("default", listenerFactory.createListener())

            // 2. 自定义简单用户管理器
            val userManager = serverFactory.userManager
            val user = BaseUser().apply {
                name = username
                this.password = password
                homeDirectory = ftpRootDir.absolutePath
                // 赋予写权限（上传权限）
                val authorities: MutableList<Authority> = ArrayList()
                authorities.add(WritePermission())
                setAuthorities(authorities)
            }
            userManager.save(user)

            // 3. 配置连接参数
            val connectionConfigFactory = ConnectionConfigFactory()
            connectionConfigFactory.isAnonymousLoginEnabled = false
            connectionConfigFactory.maxLoginFailures = 3
            serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()

            // 4. 创建并启动服务器
            ftpServer = serverFactory.createServer()
            ftpServer?.start()
            isRunning = true
            logger.info("FTP Server started on port $port, root: ${ftpRootDir.absolutePath}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to launch FTP server", e)
            ftpServer = null
            isRunning = false
            return false
        }
    }

    /**
     * 停止 FTP 服务器
     */
    fun stopServer() {
        try {
            ftpServer?.stop()
            ftpServer = null
            isRunning = false
            logger.info("FTP Server stopped.")
        } catch (e: Exception) {
            logger.error("Error stopping FTP server", e)
        }
    }
}