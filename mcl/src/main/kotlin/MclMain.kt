package me.voltual.mcl

import com.hivemc.chunker.conversion.WorldConverter
import com.hivemc.chunker.conversion.encoding.EncodingType
import java.io.File
import java.util.*
import kotlin.system.exitProcess

object MclMain {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("用法: java -jar mcl.jar <输入存档路径> <输出Mineclonia目录>")
            println("示例: java -jar mcl.jar ./MyWorld ./MinecloniaSaved")
            exitProcess(1)
        }

        val inputPath = File(args[0])
        val outputPath = File(args[1])

        if (!inputPath.exists()) {
            println("错误: 输入路径不存在: ${inputPath.absolutePath}")
            exitProcess(1)
        }

        println("--- Mineclonia 地图转换器启动 ---")
        println("输入: ${inputPath.absolutePath}")
        println("输出: ${outputPath.absolutePath}")

        // 1. 创建 WorldConverter 实例
        val converter = WorldConverter(UUID.randomUUID())

        // 2. 配置转换参数 (开启所有我们需要的功能)
        converter.isProcessItems = true
        converter.isProcessEntities = true
        converter.isProcessBlockEntities = true
        converter.isProcessBiomes = true
        converter.isProcessLighting = true
        converter.isProcessColumnPreTransform = true // 开启预转换以处理方块连接和实体位移

        try {
            // 3. 自动探测输入格式 (Java 1.12-1.21 或 Bedrock)
            println("正在检测输入存档格式...")
            val readerOptional = EncodingType.findReader(inputPath, converter)
            if (readerOptional.isEmpty) {
                println("错误: 无法识别输入存档格式！请确保目录包含 level.dat (Java) 或 db 文件夹 (Bedrock)。")
                exitProcess(1)
            }
            val reader = readerOptional.get()
            println("识别到格式: ${reader.encodingType.name} 版本: ${reader.version}")

            // 4. 创建我们的 Mineclonia Writer
            val writer = MclLevelWriter(outputPath)

            // 5. 启动转换任务
            println("转换开始，请稍候...")
            val startTime = System.currentTimeMillis()
            
            val trackedTask = converter.convert(reader, writer)
            
            // 6. 等待转换完成 (Chunker 使用 CompletableFuture)
            trackedTask.environment.future().get()

            val endTime = System.currentTimeMillis()
            println("--- 转换成功！ ---")
            println("总耗时: ${(endTime - startTime) / 1000.0} 秒")
            println("存档已保存至: ${outputPath.absolutePath}")

        } catch (e: Exception) {
            println("转换过程中发生崩溃:")
            e.printStackTrace()
            exitProcess(1)
        }
    }
}