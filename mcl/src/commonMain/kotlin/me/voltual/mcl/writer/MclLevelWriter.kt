package me.voltual.mcl.writer

import me.voltual.mcl.core.MclConverterManager
import me.voltual.mcl.mapping.MclMappingInitializer

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
        MclMappingInitializer.initialize()
        
        // 从 Chunker 元数据提取世界出生点
        val spawnX = chunkerLevel.spawnX ?: 0
        val spawnY = chunkerLevel.spawnY ?: 64
        val spawnZ = chunkerLevel.spawnZ ?: 0

        // 构建原生高速处理器
        manager = MclConverterManager(outputDir, spawnX, spawnY, spawnZ)
        return MclWorldWriter(manager)
    }

    override fun flushLevel() {
        if (::manager.isInitialized) {
            manager.flush()
            manager.close()
        }
    }

    override fun getEncodingType(): EncodingType = EncodingType.SETTINGS
    override fun getVersion(): Version = Version(1, 0, 0)
    override fun getSupportedBiomes(): Set<ChunkerBiome.ChunkerVanillaBiome> = emptySet()
}