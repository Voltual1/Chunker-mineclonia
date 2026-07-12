//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
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
                        
                        val node = MclMappingRegistry.convertAndDebug(identifier, blockIdx) { idx, text ->
                            pendingDebugSigns[idx] = text
                        }
                        
                        val localId = nameToLocalId.getOrPut(node.name) {
                            val id = localNamesList.size.toShort()
                            localNamesList.add(node.name)
                            id
                        }
                        
                        blockIds[blockIdx] = localId

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

            for ((blockIdx, debugText) in pendingDebugSigns) {
                val shortVisualText = debugText.replace("[MISSING]\n", "")
                    .replace("ChunkerBlockIdentifier", "Missing:")
                    .take(60)

                val escapedFormspecText = debugText
                    .replace("\\", "\\\\")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace(";", "\\;")
                    .replace("\n", ", ")

                val debugMetadata = MclBlockEntityData(
                    fields = mapOf(
                        "text" to shortVisualText,
                        "infotext" to debugText,
                        "formspec" to "size[10,5]textarea[0.5,0.5;9.5,4;text;未识别的方块详细ID (请勿编辑提交);${escapedFormspecText}]"
                    )
                )
                metadataMap[blockIdx] = debugMetadata
            }

            // 使用 Gson 序列化
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