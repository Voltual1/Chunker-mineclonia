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

        val modDir = File(outputDir, "worldmods/__mc2mt")
        if (!modDir.exists()) {
            modDir.mkdirs()
            // 修正：出生点 Y 轴真实节点偏移为 64 (4 个 Chunk 高度)
            File(modDir, "init.lua").writeText("""
                minetest.set_mapgen_params({chunksize = 1})
                minetest.set_mapgen_params({mgname = 'singlenode'})
                local spawn_pos = {x=$spawnX, y=${spawnY - 64 + 1}, z=$spawnZ}
                minetest.register_on_newplayer(function(player)
                    player:set_pos(spawn_pos)
                end)
                minetest.register_on_respawnplayer(function(player)
                    player:set_pos(spawn_pos)
                    return true
                end)
            """.trimIndent())
        }

        val dbPath = File(outputDir, "map.sqlite").absolutePath
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

            // 【核心修正 1】：调整为 Minetest 官方序列化规定的物理外层循环顺序：Z -> Y -> X
            for (localZ in 0 until 16) {
                for (localY in 0 until 16) {
                    for (localX in 0 until 16) {
                        // 【核心修正 2】：摒弃所有的镜像轴翻转，执行最稳定的 1:1 绝对映射，保证建筑左右不颠倒
                        val mcX = localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        // 【核心修正 3】：采用完全精确的 Minetest ZYX 平面数组计算公式
                        val blockIdx = (localZ shl 8) or (localY shl 4) or localX
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        val node = MclMappingRegistry.convert(identifier)
                        
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val id = localNamesList.size.toShort()
                            localNamesList.add(node.name)
                            id
                        }
                        
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
                                // 传入准确的 blockIdx，供 Rust 直接使用
                                metadataMap[blockIdx] = data
                            }
                        }
                    }
                }
            }

            val localNamesJson = gson.toJson(localNamesList).toByteArray(StandardCharsets.UTF_8)
            val metadataJson = gson.toJson(metadataMap).toByteArray(StandardCharsets.UTF_8)

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