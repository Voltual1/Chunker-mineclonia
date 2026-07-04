package me.voltual.mcl.core

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclBlockEntityRegistry
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

            // 完全对齐 C++ 内部的 YZX 局部循环顺序
            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        // Minecraft X 轴在转换到 Minetest 时需要被反转
                        val mcX = 15 - localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        
                        // 转换方块类型和状态
                        val node = MclMappingRegistry.convert(identifier)
                        
                        // 处理光照
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            node.setLight(bl, sl)
                        } else {
                            node.param1 = if (y < 4) 0x00.toByte() else 0x0F.toByte()
                        }
                        
                        mclNodes.add(node)
                        
                        // 处理方块实体
                        val worldY = (y shl 4) + mcY
                        column.getBlockEntity(mcX, worldY, mcZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                val blockIdx = (localY shl 8) or (localZ shl 4) or localX
                                metadata[blockIdx] = data
                            }
                        }
                    }
                }
            }

            // 对齐 C++ 的全局坐标变换：
            // Minecraft 的 X 轴在区域里是反向的 (-chunkX - 1)
            // Minetest 的 Y 轴偏移 -4 以对齐海平面 (Y=64 -> Y=0)
            val mclPos = MclPos(-chunkX - 1, y - 4, chunkZ)
            
            // 序列化
            val serializedData = MclBlockSerializer.serialize(
                mclNodes, 
                metadata, 
                isUnderground = (y - 4) < 0
            )
            
            // 写入数据库
            saver.saveBlock(mclPos, serializedData)
        }
    }

    fun flush() {
        saver.commit()
    }

    override fun close() {
        saver.close()
    }
}