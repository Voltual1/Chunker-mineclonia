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

            // 按照 Minetest 标准 ZYX 顺序进行循环，并反转 Z 轴以匹配左/右手坐标系转换
            for (localZ in 0 until 16) {
                for (localY in 0 until 16) {
                    for (localX in 0 until 16) {
                        val mcX = localX
                        val mcY = localY
                        val mcZ = 15 - localZ // 局部 Z 轴反转
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        
                        // 转换方块类型和状态
                        val node = MclMappingRegistry.convert(identifier)
                        
                        // 处理光照
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            node.setLight(bl, sl)
                        } else {
                            // 默认光照：如果是地下则全黑，地上则全亮
                            node.param1 = if (y < 0) 0x00.toByte() else 0x0F.toByte()
                        }
                        
                        mclNodes.add(node)
                        
                        // 处理方块实体 (Block Entity)
                        val worldY = (y shl 4) + mcY
                        column.getBlockEntity(mcX, worldY, mcZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                // 保持元数据索引为 YZX 格式，供 Serializer 转换
                                val blockIdx = (mcY shl 8) or (localZ shl 4) or mcX
                                metadata[blockIdx] = data
                            }
                        }
                    }
                }
            }

            // 自然坐标转换逻辑：X轴不反转，Z轴反转（-chunkZ - 1）以对齐南北朝向
            val mclPos = MclPos(chunkX, y - 4, -chunkZ - 1)
            
            // 序列化为 Minetest Blob
            val serializedData = MclBlockSerializer.serialize(
                mclNodes, 
                metadata, 
                isUnderground = y < 0
            )
            
            // 写入数据库
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