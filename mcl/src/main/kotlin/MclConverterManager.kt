package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import java.io.File
import java.util.logging.Logger

class MclConverterManager(val outputDir: File) : AutoCloseable {
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
        saver = MclSqliteSaver(dbPath)
    }

    /**
     * 处理从 Chunker 读取到的一个区块列 (Column)
     */
    fun convertColumn(column: ChunkerColumn) {
        val chunkX = column.position.chunkX
        val chunkZ = column.position.chunkZ

        for ((yByte, chunk) in column.chunks) {
            val y = yByte.toInt()
            
            val mclNodes = ArrayList<MclNode>(4096)
            val metadata = mutableMapOf<Int, MclBlockEntityData>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        val identifier = palette.get(localX, localY, localZ) ?: ChunkerBlockIdentifier.AIR
                        
                        val node = MclMappingRegistry.convert(identifier)
                        
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[localX][localY]?.get(localZ) ?: 0
                            val sl = skyLight[localX][localY]?.get(localZ) ?: 0
                            node.setLight(bl, sl)
                        } else {
                            // 默认光照：地表全亮 (0x0F)，地下全黑 (0x00)
                            node.param1 = if (y < 0) 0x00.toByte() else 0x0F.toByte()
                        }
                        
                        mclNodes.add(node)
                        
                        val worldY = (y shl 4) + localY
                        column.getBlockEntity(localX, worldY, localZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                val blockIdx = (localY shl 8) or (localZ shl 4) or localX
                                metadata[blockIdx] = data
                            }
                        }
                    }
                }
            }

            val mclPos = MclPos(-chunkX - 1, y - 4, chunkZ)
            
            val serializedData = MclBlockSerializer.serialize(
                mclNodes, 
                metadata, 
                isUnderground = y < 0
            )
            
            saver.saveBlock(mclPos, serializedData)
        }
    }

    /**
     * 提交所有更改
     */
    fun flush() {
        saver.commit()
    }

    override fun close() {
        saver.close()
    }
}