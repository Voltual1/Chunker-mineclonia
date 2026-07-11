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
            
            // 【核心修正】：改为当前方法的线程局部变量，彻底杜绝 JVM 并发冲突
            val pendingDebugSigns = HashMap<Int, String>()
            
            val palette = chunk.palette
            val blockLight = chunk.blockLight
            val skyLight = chunk.skyLight

            for (localZ in 0 until 16) {
                for (localY in 0 until 16) {
                    for (localX in 0 until 16) {
                        val mcX = localX 
                        val mcY = localY
                        val mcZ = localZ
                        
                        val blockIdx = (localZ shl 8) or (localY shl 4) or localX
                        
                        val identifier = palette.get(mcX, mcY, mcZ) ?: ChunkerBlockIdentifier.AIR
                        
                        // 传入局部 Lambda 表达式收集当前线程未映射的方块提示信息
                        val node = MclMappingRegistry.convertAndDebug(identifier, blockIdx) { idx, text ->
                            pendingDebugSigns[idx] = text
                        }
                        
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val id = localNamesList.size.toShort()
                            localNamesList.add(node.name)
                            id
                        }
                        
                        blockIds[blockIdx] = localId

                        // 光照系统
                        var lightInited = false
                        if (blockLight != null && skyLight != null) {
                            val bl = blockLight[mcX][mcY]?.get(mcZ) ?: 0
                            val sl = skyLight[mcX][mcY]?.get(mcZ) ?: 0
                            
                            if (bl > 0 || sl > 0) {
                                val dayLight = Math.max(bl.toInt(), sl.toInt()) and 0x0F
                                val nightLight = bl.toInt() and 0x0F
                                param1[blockIdx] = ((nightLight shl 4) or dayLight).toByte()
                                lightInited = true
                            }
                        }
                        
                        if (!lightInited) {
                            val isAirLike = node.name == "air" || node.name.contains("water")
                            if (y < -3 && !isAirLike) {
                                param1[blockIdx] = 0x00.toByte()
                            } else {
                                param1[blockIdx] = 0x0F.toByte()
                            }
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

            // 处理当前局部 Column 线程累积的调试元数据
            for ((blockIdx, debugText) in pendingDebugSigns) {
                val debugMetadata = MclBlockEntityData(
                    fields = mapOf(
                        "text" to debugText,
                        "infotext" to debugText,
                        "formspec" to "size[8,4]textarea[0.5,0.5;7.5,3;text;;${debugText}]"
                    )
                )
                metadataMap[blockIdx] = debugMetadata
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