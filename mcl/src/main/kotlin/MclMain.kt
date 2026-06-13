package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import java.io.File
import java.nio.file.Path
import java.util.logging.Logger

object MclMain {
    private val logger = Logger.getLogger("MclMain")

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size < 2) {
            println("用法: java -jar mcl.jar <输入地图路径> <输出Mineclonia路径>")
            return
        }

        val inputPath = Path.of(args[0])
        val outputPath = args[1]

        // 1. 初始化所有映射注册表
        logger.info("正在初始化映射注册表...")
        MclMappingInitializer.initialize()

        // 2. 使用 EncodingType 检测并创建 Reader
        // Chunker 的标准做法是通过 EncodingType.detectReader 自动识别 Java/Bedrock
        logger.info("正在检测输入地图格式...")
        val readerOptional = EncodingType.detectReader(inputPath)
        
        if (readerOptional.isEmpty) {
            logger.severe("无法识别输入地图格式，请检查路径是否正确。")
            return
        }
        
        val reader = readerOptional.get()
        logger.info("识别到格式: ${reader.encodingType} 版本: ${reader.version}")

        // 3. 读取世界设置 (level.dat)
        val level = reader.readLevel()
        logger.info("地图名称: ${level.settings.levelName}")

        // 4. 转换维度
        // 我们目前支持主世界，下界和末地可以后续通过偏移或子目录支持
        val dimensions = listOf(Dimension.OVERWORLD, Dimension.NETHER, Dimension.THE_END)
        
        for (dim in dimensions) {
            logger.info("正在处理维度: ${dim.identifier}...")
            
            // 获取该维度下所有存在的区域 (Regions)
            val regions = reader.getRegions(dim)
            if (regions.isEmpty()) {
                logger.info("维度 ${dim.identifier} 为空，跳过。")
                continue
            }

            // 根据维度确定输出目录
            val dimOutputFolder = when(dim) {
                Dimension.OVERWORLD -> outputPath
                Dimension.NETHER -> File(outputPath, "nether").absolutePath
                Dimension.THE_END -> File(outputPath, "end").absolutePath
                else -> outputPath
            }

            // 5. 读取区块并写入 Mineclonia
            // readColumns 返回一个 Iterable<ChunkerColumn>
            val columns = reader.readColumns(dim, regions)
            
            logger.info("正在将维度 ${dim.identifier} 写入 Mineclonia 数据库...")
            MclConverterEntry.runConversion(columns, dimOutputFolder)
        }

        logger.info("转换完成！存档已输出至: $outputPath")
    }
}