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

        val converter = WorldConverter(UUID.randomUUID())

        converter.setProcessItems(true)
        converter.setProcessEntities(true)
        converter.setProcessBlockEntities(true)
        converter.setProcessBiomes(true)
        
        // 与安卓端保持一致：关闭光照计算与区块列预处理，大幅度提升性能并防止死锁
        converter.setProcessLighting(false)
        converter.setProcessColumnPreTransform(false)

        try {
            println("正在检测输入存档格式...")
            val readerOptional = EncodingType.findReader(inputPath, converter)
            if (!readerOptional.isPresent) {
                println("错误: 无法识别输入存档格式！")
                exitProcess(1)
            }
            val reader = readerOptional.get()
            println("识别到格式: ${reader.encodingType.name} 版本: ${reader.version}")

            val writer = MclLevelWriter(outputPath)

            println("转换开始，请稍候...")
            val startTime = System.currentTimeMillis()
            
            val trackedTask = converter.convert(reader, writer)
            
            trackedTask.future().get()

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