//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
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
        // 初始化映射注册表
        MclMappingInitializer.initialize()
        
        // 【完全同步 Java 签名】：利用 settings 实体大写属性读取安全出生点
        val settings = chunkerLevel.settings
        val spawnX = settings?.SpawnX ?: 0
        val spawnY = settings?.SpawnY ?: 64
        val spawnZ = settings?.SpawnZ ?: 0

        // 初始化存储管理器并传入出生点
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