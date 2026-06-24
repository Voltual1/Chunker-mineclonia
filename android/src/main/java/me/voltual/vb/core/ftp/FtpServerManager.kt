package me.voltual.vb.core.ftp

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.voltual.vb.data.FtpSettingsDataStore
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.slf4j.LoggerFactory
import java.io.File

class FtpServerManager(
    private val context: Context,
    private val ftpSettingsStore: FtpSettingsDataStore
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var ftpServer: FtpServer? = null

    val isRunning: Boolean
        get() = runBlocking { ftpSettingsStore.ftpSettingsFlow.first().isRunning }

    fun startServer(ftpRootDir: File): Boolean {
        if (ftpServer != null && isRunning) {
            logger.info("FTP Server is already running.")
            return true
        }

        try {
            val settings = runBlocking { ftpSettingsStore.ftpSettingsFlow.first() }

            if (!ftpRootDir.exists()) {
                ftpRootDir.mkdirs()
            }

            val serverFactory = FtpServerFactory()
            val listenerFactory = org.apache.ftpserver.listener.ListenerFactory()
            listenerFactory.port = settings.port

            serverFactory.addListener("default", listenerFactory.createListener())

            val userManager = serverFactory.userManager
            val user = BaseUser().apply {
                name = settings.username
                password = settings.password
                homeDirectory = ftpRootDir.absolutePath
                val authorities: MutableList<Authority> = ArrayList()
                authorities.add(WritePermission())
                setAuthorities(authorities)
            }
            userManager.save(user)

            val connectionConfigFactory = ConnectionConfigFactory()
            connectionConfigFactory.isAnonymousLoginEnabled = false
            connectionConfigFactory.maxLoginFailures = 3
            serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()

            ftpServer = serverFactory.createServer()
            ftpServer?.start()

            runBlocking {
                ftpSettingsStore.updateSettings { it.copy(isRunning = true) }
            }
            logger.info("FTP Server started on port ${settings.port}, root: ${ftpRootDir.absolutePath}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to launch FTP server", e)
            ftpServer = null
            runBlocking {
                ftpSettingsStore.updateSettings { it.copy(isRunning = false) }
            }
            return false
        }
    }

    fun stopServer() {
        try {
            ftpServer?.stop()
            ftpServer = null
            runBlocking {
                ftpSettingsStore.updateSettings { it.copy(isRunning = false) }
            }
            logger.info("FTP Server stopped.")
        } catch (e: Exception) {
            logger.error("Error stopping FTP server", e)
        }
    }
}