//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>。
package me.voltual.mcl.core

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclBlockEntityRegistry
import me.voltual.mc2mt.MC2MTLib // 引入我们的 JNI 动态链接库
import com.google.gson.Gson
import java.io.File
import java.nio.charset.StandardCharsets

class MclConverterManager(val outputDir: File) : AutoCloseable {
    private val gson = Gson()

    init {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        // 1. 建立 Minetest 的世界配置文件
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

        // 2. 初始化 Rust 底层极速 SQLite 引擎 (传入输出路径和默认出生点)
        val dbPath = File(outputDir, "map.sqlite").absolutePath
        val success = MC2MTLib.initNativeEngine(dbPath, 0, 64, 0)
        if (!success) {
            throw RuntimeException("Failed to initialize Rust high-performance SQLite engine")
        }
    }

    /**
     * 处理由 Chunker 读取并完美解决版本差异后的标准区块列 (Column)
     */
    fun convertColumn(column: ChunkerColumn) {
        val chunkX = column.position.chunkX
        val chunkZ = column.position.chunkZ

        for ((yByte, chunk) in column.chunks) {
            val y = yByte.toInt()
            
            // 构建扁平化的内存区块数据，通过 JNI 实现零拷贝/块拷贝拷贝
            val blockIds = ShortArray(4096)
            val param1 = ByteArray(4096)
            val param2 = ByteArray(4096)
            
            // 局部 Name 到 ID 映射表，保持区块内空间压缩度
            val nameToLocalId = HashMap<String, Short>()
            val localNamesList = ArrayList<String>()

            val metadataMap = HashMap<Int, MclBlockEntityData>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            // YZX 的标准局部循环顺序，由 JVM 端完成高可读性的坐标解析与变换
            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        // 1. 处理 X 轴反转
                        val mcX = 15 - localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        // 2. 通过 Chunker 获取没有任何版本差异的块标识符
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        
                        // 3. 利用 Kotlin 的 MclMappingDSL 进行 Mineclonia 块及属性映射
                        val node = MclMappingRegistry.convert(identifier)
                        
                        // 分配区块局部 ID
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val id = localNamesList.size.toShort()
                            localNamesList.add(node.name)
                            id
                        }
                        
                        val blockIdx = (localY shl 8) or (localZ shl 4) or localX
                        blockIds[blockIdx] = localId

                        // 4. 处理光照 param1
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            // (night_light << 4) | day_light
                            val dayLight = Math.max(bl.toInt(), sl.toInt()) and 0x0F
                            val nightLight = bl.toInt() and 0x0F
                            param1[blockIdx] = ((nightLight shl 4) or dayLight).toByte()
                        } else {
                            param1[blockIdx] = (if (y < 4) 0x00 else 0x0F).toByte()
                        }
                        
                        // 5. 处理朝向 param2
                        param2[blockIdx] = node.param2

                        // 6. 收集并处理方块实体 (BlockEntity) 
                        val worldY = (y shl 4) + mcY
                        column.getBlockEntity(mcX, worldY, mcZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                metadataMap[blockIdx] = data
                            }
                        }
                    }
                }
            }

            // 7. 将字典和方块实体对象转为 JSON 字节，由 JNI 内存层无损解析
            val localNamesJson = gson.toJson(localNamesList).toByteArray(StandardCharsets.UTF_8)
            val metadataJson = gson.toJson(metadataMap).toByteArray(StandardCharsets.UTF_8)

            // 8. 抛给 Rust 极速高并发序列化与 SQLite 物理写出通道 (cy = y, Y轴在 Rust 写入时自动偏移 -4 对齐海平面)
            val success = MC2MTLib.writeChunkFast(
                chunkX,
                y,
                chunkZ,
                blockIds,
                param1,
                param2,
                localNamesJson,
                metadataJson
            )
            
            if (!success) {
                System.err.println("[JNI Warning] Failed to write chunk fast at X: $chunkX, Y: $y, Z: $chunkZ")
            }
        }
    }

    fun flush() {
        MC2MTLib.flushNativeEngine()
    }

    override fun close() {
        MC2MTLib.closeNativeEngine()
    }
}