//Copyright (C) 2025 Voltual
package me.voltual.mcl.core

import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclBlockEntityRegistry
import me.voltual.mc2mt.MC2MTLib 
import com.google.gson.Gson
import java.io.File
import java.nio.charset.StandardCharsets

// 修改点：构造函数现在接受 4 个参数
class MclConverterManager(
    val outputDir: File,
    val spawnX: Int,
    val spawnY: Int,
    val spawnZ: Int
) : AutoCloseable {
    private val gson = Gson()

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
                static_spawnpoint = ($spawnX, $spawnY, $spawnZ)
            """.trimIndent())
        }

        val dbPath = File(outputDir, "map.sqlite").absolutePath
        
        // 将从 Chunker 获得的出生点通过 JNI 注入 Rust 引擎
        // 注意：Rust 内部会自动处理 Y 轴偏移
        val success = MC2MTLib.initNativeEngine(dbPath, spawnX, spawnY, spawnZ)
        if (!success) {
            throw RuntimeException("Failed to initialize Rust high-performance SQLite engine")
        }
    }

    fun convertColumn(column: ChunkerColumn) {
        val chunkX = column.position.chunkX
        val chunkZ = column.position.chunkZ

        for ((yByte, chunk) in column.chunks) {
            val y = yByte.toInt()
            
            val blockIds = ShortArray(4096)
            val param1 = ByteArray(4096)
            val param2 = ByteArray(4096)
            
            val nameToLocalId = HashMap<String, Short>()
            val localNamesList = ArrayList<String>()
            val metadataMap = HashMap<Int, MclBlockEntityData>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            for (localY in 0 until 16) {
                for (localZ in 0 until 16) {
                    for (localX in 0 until 16) {
                        val mcX = 15 - localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        val node = MclMappingRegistry.convert(identifier)
                        
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val id = localNamesList.size.toShort()
                            localNamesList.add(node.name)
                            id
                        }
                        
                        val blockIdx = (localY shl 8) or (localZ shl 4) or localX
                        blockIds[blockIdx] = localId

                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            val dayLight = Math.max(bl.toInt(), sl.toInt()) and 0x0F
                            val nightLight = bl.toInt() and 0x0F
                            param1[blockIdx] = ((nightLight shl 4) or dayLight).toByte()
                        } else {
                            param1[blockIdx] = (if (y < 4) 0x00 else 0x0F).toByte()
                        }
                        
                        param2[blockIdx] = node.param2

                        val worldY = (y shl 4) + mcY
                        column.getBlockEntity(mcX, worldY, mcZ)?.let { be ->
                            MclBlockEntityRegistry.convert(be)?.let { data ->
                                metadataMap[blockIdx] = data
                            }
                        }
                    }
                }
            }

            val localNamesJson = gson.toJson(localNamesList).toByteArray(StandardCharsets.UTF_8)
            val metadataJson = gson.toJson(metadataMap).toByteArray(StandardCharsets.UTF_8)

            // 调用 Rust 写入
            MC2MTLib.writeChunkFast(chunkX, y, chunkZ, blockIds, param1, param2, localNamesJson, metadataJson)
        }
    }

    fun flush() {
        MC2MTLib.flushNativeEngine()
    }

    override fun close() {
        MC2MTLib.closeNativeEngine()
    }
}