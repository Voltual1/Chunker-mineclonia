package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Version
import com.hivemc.chunker.conversion.encoding.base.writer.LevelWriter
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel
import java.io.File

class MclLevelWriter(val outputDir: File) : LevelWriter {
    private lateinit var manager: MclConverterManager

    override fun writeLevel(chunkerLevel: ChunkerLevel): WorldWriter {
        // 1. 初始化映射表 (AI 生成的那些)
        MclMappingInitializer.initialize()
        
        // 2. 初始化管理器
        manager = MclConverterManager(outputDir)
        
        // 3. 返回维度写入器
        return MclWorldWriter(manager)
    }

    override fun flushLevel() {
        manager.flush()
        manager.close()
    }

    override fun getEncodingType(): EncodingType = EncodingType.SETTINGS // 暂时借用，或自定义

    override fun getVersion(): Version = Version(1, 0, 0)

    override fun getSupportedBiomes(): Set<ChunkerBiome.ChunkerVanillaBiome> = emptySet()
}