package me.voltual.vb.ui.packconverter

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.extension.openOutputStream
import com.anggrayudi.storage.extension.fromTreeUri
import com.anggrayudi.storage.file.makeFile
import com.anggrayudi.storage.file.openOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.geysermc.pack.converter.PackConverter
import org.geysermc.pack.converter.pipeline.AssetConverters
import org.geysermc.pack.converter.util.LogListener
import org.geysermc.pack.converter.util.IccProfileStore
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path

class PackConversionWorker(
    val context: Context,
    val params: WorkerParameters
) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result = withContext(Dispatchers.IO) {
        val inputUriStr = inputData.getString("inputUri") ?: return@withContext Result.failure()
        val outputTreeUriStr = inputData.getString("outputTreeUri") ?: return@withContext Result.failure()
        val packName = inputData.getString("packName") ?: "ConvertedPack"
        val debugMode = inputData.getBoolean("debugMode", false)

        val logFile = File(context.cacheDir, "pack_conversion_log.txt")
        val logger = WorkerLogListener(logFile, debugMode)

        // 临时文件配置
        val tempInputDir = File(context.cacheDir, "pack_converter_input")
        tempInputDir.mkdirs()
        val tempInputZip = File(tempInputDir, "input_pack.zip")
        
        val tempOutputDir = File(context.cacheDir, "pack_converter_output")
        tempOutputDir.mkdirs()
        val tempOutputMcpack = File(tempOutputDir, "$packName.mcpack")

        val vanillaPackZip = File(context.cacheDir, "Vanilla-Assets.zip")

        try {
            // 清理旧的日志
            if (logFile.exists()) logFile.delete()
            logFile.createNewFile()

            // 诊断 AWT Native JNI 库是否能被当前 CPU 架构正常加载
            try {
                logger.info("系统：正在诊断 AWT Native Library...")
                val hello = ro.andob.awtcompat.nativec.AwtCompatNativeComponents.getHelloWorldMesssage()
                logger.info("系统：AWT JNI 测试消息: $hello")
            } catch (t: Throwable) {
                logger.error("系统：AWT JNI 加载失败，这会导致 AWT/ImageIO 相关的材质转换不可用。请检查 APK 的 ABI 兼容性（如模拟器是否缺失 x86_64 支持）。", t)
            }

            // 初始化释放本地 Base64 ICC 颜色配置档
            logger.info("系统：释放并配置 AWT 颜色映射参数...")
            IccProfileStore.install(context.cacheDir)

            logger.info("系统：正在从 SAF 拷贝输入材质包...")
            
            // 1. 将用户的输入文件通过 SAF 复制到本地缓存
            val inputUri = Uri.parse(inputUriStr)
            val inputStream = context.contentResolver.openInputStream(inputUri)
            if (inputStream == null) {
                logger.error("系统：无法读取输入文件。")
                return@withContext Result.failure()
            }
            FileOutputStream(tempInputZip).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            
            logger.info("系统：输入文件拷贝完毕，开始转换过程。")

            // 2. 执行 Geyser PackConverter 转换
            val converter = PackConverter()
                .enforcePackCheck(true)
                .input(Path.of(tempInputZip.absolutePath))
                .output(Path.of(tempOutputMcpack.absolutePath))
                .packName(packName)
                .vanillaPackPath(Path.of(vanillaPackZip.absolutePath))
                .converters(AssetConverters.converters(debugMode))
                .logListener(logger)

            converter.convert().pack()

            logger.info("系统：转换处理完成，正在将结果写入用户选择的输出目录...")

            // 3. 将转换后的 .mcpack 写入到用户选择的 SAF 目录中
            val outputTreeUri = Uri.parse(outputTreeUriStr)
            val outputDirDoc = context.fromTreeUri(outputTreeUri)
            if (outputDirDoc == null || !outputDirDoc.canWrite()) {
                logger.error("系统：输出目录无法访问或没有写入权限。")
                return@withContext Result.failure()
            }

            // 在目标目录中创建文件 (解决重名自动递增可以通过 SimpleStorage 内部处理，或自行处理)
            val finalFileName = "$packName.mcpack"
            val targetDoc = outputDirDoc.makeFile(context, finalFileName)
            if (targetDoc == null) {
                logger.error("系统：无法在输出目录创建文件 $finalFileName。")
                return@withContext Result.failure()
            }

            val targetOutputStream = targetDoc.openOutputStream(context)
            if (targetOutputStream == null) {
                logger.error("系统：无法向新建文件写入数据。")
                return@withContext Result.failure()
            }

            tempOutputMcpack.inputStream().use { input ->
                targetOutputStream.use { output ->
                    input.copyTo(output)
                }
            }

            logger.info("系统：导出成功！转换已彻底完成。")

            return@withContext Result.success()
        } catch (e: Throwable) {
            logger.error("系统：转换期间发生严重错误", e)
            e.printStackTrace()
            return@withContext Result.failure()
        } finally {
            // 4. 清理缓存垃圾
            try {
                if (tempInputDir.exists()) tempInputDir.deleteRecursively()
                if (tempOutputDir.exists()) tempOutputDir.deleteRecursively()
            } catch (ignored: Exception) {}
        }
    }

    private class WorkerLogListener(val logFile: File, val debugMode: Boolean) : LogListener {
        private fun appendLog(text: String) {
            try {
                logFile.appendText("$text\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun isDebugEnabled(): Boolean {
            return debugMode
        }

        override fun debug(message: String) {
            if (debugMode) appendLog("DEBUG: $message")
        }

        override fun debugUnchecked(message: String) {
            if (debugMode) appendLog("DEBUG_UNCHECKED: $message")
        }

        override fun info(message: String) {
            appendLog("INFO: $message")
        }

        override fun warn(message: String) {
            appendLog("WARN: $message")
        }

        override fun error(message: String) {
            appendLog("ERROR: $message")
        }

        override fun error(message: String, error: Throwable?) {
            if (error != null) {
                appendLog("ERROR: $message\n${error.stackTraceToString()}")
            } else {
                appendLog("ERROR: $message")
            }
        }
    }
}