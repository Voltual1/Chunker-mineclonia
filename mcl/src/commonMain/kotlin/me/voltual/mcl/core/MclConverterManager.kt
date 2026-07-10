package me.voltual.mcl.core

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclBlockEntityRegistry
import java.io.File
import java.util.logging.Logger

class MclConverterManager(
    val outputDir: File, 
    spawnX: Int = 0, 
    spawnY: Int = 64, 
    spawnZ: Int = 0
) : AutoCloseable {
    private val logger = Logger.getLogger("MclConverterManager")
    private val saver: MclSqliteSaver

    init {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val worldMt = File(outputDir, "world.mt")
        if (!worldMt.exists()) {
            worldMt.writeText("""
                backend = sqlite3
                gameid = mineclonia
                player_backend = sqlite3
                auth_backend = sqlite3
                mod_storage_backend = sqlite3
            """.trimIndent())
        }

        val dbPath = File(outputDir, "map.sqlite").absolutePath
        // 跨界传递出生点，保证生存期无缝
        saver = MclSqliteSaver(dbPath, spawnX, spawnY - 4 + 1, spawnZ)
    }

    fun convertColumn(column: ChunkerColumn) {
        val chunkX = column.position.chunkX
        val chunkZ = column.position.chunkZ

        for ((yByte, chunk) in column.chunks) {
            val y = yByte.toInt()
            
            // 构建零拷贝平面数组数组
            val blockIds = ShortArray(4096)
            val param1 = ByteArray(4096)
            val param2 = ByteArray(4096)
            
            val localNames = mutableListOf<String>()
            val nameToLocalId = mutableMapOf<String, Short>()
            val metadata = mutableMapOf<Int, MclBlockEntityData>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            var i = 0
            // 完全对齐 C++ / Rust 统一的 YZX 局部循环顺序
            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        val mcX = 15 - localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        val node = MclMappingRegistry.convert(identifier)
                        
                        // 计算局部名字 ID
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val nextId = localNames.size.toShort()
                            localNames.add(node.name)
                            nextId
                        }
                        
                        blockIds[i] = localId
                        
                        // 处理光照
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            
                            val dayLight = Math.max(bl.toInt(), sl.toInt()) and 0x0F
                            val nightLight = bl.toInt() and 0x0F
                            param1[i] = ((nightLight shl 4) or dayLight).toByte()
                        } else {
                            param1[i] = (if (y < 4) 0x00 else 0x0F).toByte()
                        }
                        
                        param2[i] = node.param2
                        
                        // 处理方块实体 (BlockEntity)
                        val worldY = (y shl 4) + mcY
                        column.getBlockEntity(mcX, worldY, mcZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                metadata[i] = data
                            }
                        }
                        i++
                    }
                }
            }

            // 直接调用 Rust 动态链接库并发序列化压缩与写入
            saver.saveChunkNatively(
                chunkX, y, chunkZ,
                blockIds,
                param1,
                param2,
                localNames,
                metadata
            )
        }
    }

    fun flush() {
        saver.commit()
    }

    override fun close() {
        saver.close()
    }
}