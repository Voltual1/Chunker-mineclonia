package me.voltual.vb.core.ftp

import android.content.Context
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class FtpServerManager(private val context: Context) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var ftpServer: FtpServer? = null

    var isRunning = false
        private set

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
            // 确保根目录存在并获取规范路径
            val canonicalRoot = ftpRootDir.canonicalFile
            if (!canonicalRoot.exists()) {
                canonicalRoot.mkdirs()
            }

            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()
            listenerFactory.port = port

            // 1. 绑定监听器
            serverFactory.addListener("default", listenerFactory.createListener())

            // 2. 自定义用户配置
            val userManager = serverFactory.userManager
            val user = BaseUser().apply {
                name = username
                this.password = password
                homeDirectory = canonicalRoot.absolutePath
                val authorities: MutableList<Authority> = ArrayList()
                authorities.add(WritePermission())
                setAuthorities(authorities)
            }
            userManager.save(user)

            // 3. 注入针对 Android 符号链接路径优化的文件系统工厂
            serverFactory.fileSystem = AndroidFtpFileSystemFactory(canonicalRoot)

            // 4. 配置连接参数
            val connectionConfigFactory = ConnectionConfigFactory()
            connectionConfigFactory.isAnonymousLoginEnabled = false
            connectionConfigFactory.maxLoginFailures = 3
            serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()

            // 5. 启动服务器
            ftpServer = serverFactory.createServer()
            ftpServer?.start()
            isRunning = true
            logger.info("FTP Server started successfully on port $port. Root: ${canonicalRoot.absolutePath}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to launch FTP server", e)
            e.printStackTrace() // 打印堆栈信息以便调试
            ftpServer = null
            isRunning = false
            return false
        }
    }

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

// =========================================================================
// 专为 Android 路径设计的 Ftp 存储工厂，跳过 NativeFileSystem 的 Jail 检查限制
// =========================================================================

class AndroidFtpFileSystemFactory(private val rootDir: File) : FileSystemFactory {
    override fun createFileSystemView(user: User): FileSystemView {
        return AndroidFtpFileSystemView(rootDir)
    }
}

class AndroidFtpFileSystemView(private val rootDir: File) : FileSystemView {
    private var currDir: File = rootDir

    override fun getHomeDirectory(): FtpFile {
        return AndroidFtpFile(rootDir, rootDir)
    }

    override fun getWorkingDirectory(): FtpFile {
        return AndroidFtpFile(currDir, rootDir)
    }

    override fun changeWorkingDirectory(dir: String): Boolean {
        val newDir = if (dir.startsWith("/")) {
            File(rootDir, dir.substring(1))
        } else {
            File(currDir, dir)
        }
        val canonicalNewDir = newDir.canonicalFile
        val canonicalRootDir = rootDir.canonicalFile
        
        // 安全检测：确保访问的目标路径在被挂载的根目录下
        if (canonicalNewDir.absolutePath.startsWith(canonicalRootDir.absolutePath)) {
            if (canonicalNewDir.exists() && canonicalNewDir.isDirectory) {
                currDir = canonicalNewDir
                return true
            }
        }
        return false
    }

    override fun getFile(file: String): FtpFile {
        val fileObj = if (file.startsWith("/")) {
            File(rootDir, file.substring(1))
        } else {
            File(currDir, file)
        }
        return AndroidFtpFile(fileObj, rootDir)
    }

    override fun isRandomAccessible(): Boolean = true
    override fun dispose() {}
}

class AndroidFtpFile(private val file: File, private val rootDir: File) : FtpFile {
    override fun getAbsolutePath(): String {
        val rootPath = rootDir.canonicalFile.absolutePath
        val filePath = file.canonicalFile.absolutePath
        if (filePath.startsWith(rootPath)) {
            val relPath = filePath.substring(rootPath.length)
            return if (relPath.isEmpty()) "/" else relPath.replace('\\', '/')
        }
        return "/"
    }

    override fun getName(): String = file.name
    override fun isDirectory(): Boolean = file.isDirectory
    override fun isFile(): Boolean = file.isFile
    override fun doesExist(): Boolean = file.exists()
    override fun isReadable(): Boolean = file.canRead()
    override fun isWritable(): Boolean = file.canWrite() || (!file.exists() && file.parentFile?.canWrite() == true)
    override fun isRemovable(): Boolean = file.canWrite() && file.canonicalFile.absolutePath != rootDir.canonicalFile.absolutePath
    override fun getOwnerName(): String = "admin"
    override fun getGroupName(): String = "admin"
    override fun getLinkCount(): Int = if (isDirectory) 3 else 1
    override fun getLastModified(): Long = file.lastModified()
    override fun setLastModified(time: Long): Boolean = file.setLastModified(time)
    override fun getSize(): Long = if (isFile) file.length() else 0
    override fun getPhysicalFile(): Any = file
    override fun isHidden(): Boolean = file.isHidden

    override fun mkdir(): Boolean {
        if (!file.exists()) {
            return file.mkdirs()
        }
        return false
    }

    override fun delete(): Boolean {
        return file.delete()
    }

    override fun move(target: FtpFile): Boolean {
        val dest = target.physicalFile as? File ?: return false
        return file.renameTo(dest)
    }

    override fun listFiles(): List<FtpFile> {
        val files = file.listFiles() ?: return emptyList()
        return files.map { AndroidFtpFile(it, rootDir) }
    }

    override fun createOutputStream(offset: Long): OutputStream {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        return FileOutputStream(file, offset > 0)
    }

    override fun createInputStream(offset: Long): InputStream {
        val fis = FileInputStream(file)
        if (offset > 0) {
            fis.skip(offset)
        }
        return fis
    }
}