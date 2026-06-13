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
        
        // 1. 初始化 world.mt (Minetest 识别存档的关键)
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

        // 2. 初始化 SQLite 保存器
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
            
            // 准备该 16x16x16 区块的所有节点 (4096个)
            val mclNodes = ArrayList<MclNode>(4096)
            val metadata = mutableMapOf<Int, MclBlockEntityData>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            // Minetest 内部存储顺序通常是 YZX 或 ZYX，
            // 我们的 Serializer 已经处理了顺序，这里按顺序填充即可
            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        val identifier = palette.get(localX, localY, localZ) ?: ChunkerBlockIdentifier.AIR
                        
                        // 转换方块类型和状态
                        val node = MclMappingRegistry.convert(identifier)
                        
                        // 处理光照 (如果 Chunker 提供了光照数据)
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[localX][localY][localZ]
                            val sl = skyLight[localX][localY][localZ]
                            node.setLight(bl, sl)
                        } else {
                            // 默认光照：如果是地下则全黑，地上则全亮
                            node.param1 = if (y < 0) 0x00.toByte() else 0x0F.toByte()
                        }
                        
                        mclNodes.add(node)
                        
                        // 处理方块实体 (Block Entity)
                        val worldY = (y shl 4) + localY
                        column.getBlockEntity(localX, worldY, localZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                // 这里的索引必须与节点列表中的顺序一致
                                val blockIdx = (localY shl 8) or (localZ shl 4) or localX
                                metadata[blockIdx] = data
                            }
                        }
                    }
                }
            }

            // 坐标转换逻辑 (参考 MC2MT):
            // Minetest X = -Minecraft X - 1
            // Minetest Y = Minecraft Y_slice - 4 (对齐海平面)
            // Minetest Z = Minecraft Z
            val mclPos = MclPos(-chunkX - 1, y - 4, chunkZ)
            
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