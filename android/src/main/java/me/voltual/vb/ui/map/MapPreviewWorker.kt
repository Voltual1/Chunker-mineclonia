package me.voltual.vb.ui.map

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.workDataOf
import androidx.work.Data
import com.hivemc.chunker.conversion.encoding.EncodingType
import com.hivemc.chunker.conversion.encoding.base.Converter
import com.hivemc.chunker.conversion.handlers.ColumnConversionHandler
import com.hivemc.chunker.conversion.handlers.LevelConversionHandler
import com.hivemc.chunker.conversion.handlers.WorldConversionHandler
import com.hivemc.chunker.conversion.intermediate.column.ChunkerColumn
import com.hivemc.chunker.conversion.intermediate.column.biome.ChunkerBiome
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.column.chunk.RegionCoordPair
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevel
import com.hivemc.chunker.conversion.intermediate.level.ChunkerLevelSettings
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.conversion.intermediate.world.DimensionRegistry
import com.hivemc.chunker.mapping.resolver.MappingsFileResolvers
import com.hivemc.chunker.scheduling.task.FutureTask
import com.hivemc.chunker.scheduling.task.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class MapPreviewWorker(val context: Context, params: WorkerParameters) : RemoteCoroutineWorker(context, params) {

    override suspend fun doRemoteWork(): Result = withContext(Dispatchers.IO) {
        val worldDirUri = inputData.getString("worldDirUri") ?: return@withContext Result.failure()
        val outputPath = inputData.getString("outputPath") ?: return@withContext Result.failure()

        // 接收可能存在的局部网格限制参数
        val targetDimId = inputData.getString("targetDimension")
        val targetRegionX = inputData.getInt("targetRegionX", Int.MAX_VALUE)
        val targetRegionZ = inputData.getInt("targetRegionZ", Int.MAX_VALUE)
        val limitMode = targetRegionX != Int.MAX_VALUE && targetRegionZ != Int.MAX_VALUE

        val outputDir = File(outputPath)
        if (!limitMode) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val regionRGBAData = ConcurrentHashMap<Pair<Dimension, RegionCoordPair>, ConcurrentHashMap<ChunkCoordPair, IntArray>>()

        val converterStub = object : Converter {
            override fun shouldLevelDBCompaction() = false
            override fun shouldProcessMaps() = false
            override fun shouldProcessItems() = false
            override fun shouldProcessEntities() = false
            override fun shouldProcessBlockEntities() = false
            override fun shouldProcessLootTables() = false
            override fun shouldProcessBiomes() = false
            override fun shouldProcessHeightMap() = false
            override fun shouldProcessColumnPreTransform() = false
            override fun shouldProcessLighting() = false
            
            override fun shouldProcessDimension(dimension: Dimension?): Boolean {
                if (limitMode && targetDimId != null) {
                    return dimension?.getIdentifier() == targetDimId
                }
                return true
            }

            override fun shouldProcessRegion(dimension: Dimension?, regionPair: RegionCoordPair?): Boolean {
                if (limitMode && regionPair != null) {
                    return regionPair.regionX() == targetRegionX && regionPair.regionZ() == targetRegionZ
                }
                return true
            }

            override fun shouldProcessColumn(dimension: Dimension?, columnPair: ChunkCoordPair?): Boolean {
                if (limitMode && columnPair != null) {
                    val r = columnPair.region
                    return r.regionX() == targetRegionX && r.regionZ() == targetRegionZ
                }
                return true
            }

            override fun shouldAllowNBTCopying() = false
            override fun shouldAllowCustomIdentifiers() = false
            override fun getBlockMappings(): MappingsFileResolvers? = null
            override fun getDimensionRegistry() = DimensionRegistry()
            override fun shouldDiscardEmptyChunks() = true
            override fun shouldPreventYBiomeBlending() = false
            override fun getNewDimension(dimension: Dimension?) = Optional.ofNullable(dimension)
            override fun getNewBiome(biome: ChunkerBiome?) = biome
            override fun level() = Optional.empty<ChunkerLevel>()
        }

        val finalWorldDirectory = File(worldDirUri)
        val readerOpt = EncodingType.findReader(finalWorldDirectory, converterStub)
        if (!readerOpt.isPresent) return@withContext Result.failure()

        val levelReader = readerOpt.get()
        val isBedrockFormat = levelReader.encodingType == EncodingType.BEDROCK

        val previewWriter = ComposeMapPreviewWriter(
            onColumnRendered = { dimension, region, chunk, argb ->
                val dimRegion = Pair(dimension, region)
                val chunksInRegion = regionRGBAData.computeIfAbsent(dimRegion) { ConcurrentHashMap() }
                chunksInRegion[chunk] = argb
            },
            onFlushRegion = { dimension, region ->
                val dimRegion = Pair(dimension, region)
                val chunkMap = regionRGBAData.remove(dimRegion)
                if (chunkMap != null) {
                    val pixels = IntArray(512 * 512)
                    for ((chunkCoord, argbArray) in chunkMap) {
                        if (argbArray.isEmpty()) continue
                        val startX = (chunkCoord.chunkX() and 31) shl 4
                        val startY = (chunkCoord.chunkZ() and 31) shl 4
                        for (cz in 0 until 16) {
                            for (cx in 0 until 16) {
                                pixels[(startY + cz) * 512 + (startX + cx)] = argbArray[cz * 16 + cx]
                            }
                        }
                    }
                    val dimId = dimension.identifier.replace(":", "_")
                    val tempFile = File(outputDir, "${dimId}_${region.regionX()}_${region.regionZ()}.tmp")
                    val finalFile = File(outputDir, "${dimId}_${region.regionX()}_${region.regionZ()}.bin")
                    
                    DataOutputStream(FileOutputStream(tempFile).buffered()).use { dos ->
                        for (p in pixels) dos.writeInt(p)
                    }
                    tempFile.renameTo(finalFile)
                }
            }
        )

        val environment = Task.environment("Map Preview Worker", maxOf(1, Runtime.getRuntime().availableProcessors() - 1), { it.printStackTrace() }, null)
        try {
            levelReader.readLevel(object : LevelConversionHandler {
                override fun convertLevel(level: ChunkerLevel?): Task<WorldConversionHandler> {
                    val safeLevel = level ?: ChunkerLevel(ChunkerLevelSettings(), null, emptyList(), null, emptyList())
                    val worldWriter = previewWriter.writeLevel(safeLevel)
                    val worldHandler = object : WorldConversionHandler {
                        override fun convertWorld(world: ChunkerWorld?): Task<ColumnConversionHandler> {
                            val columnWriter = worldWriter.writeWorld(world ?: throw NullPointerException())
                            val columnHandler = object : ColumnConversionHandler {
                                override fun convertColumn(column: ChunkerColumn): Task<java.lang.Void> {
                                    return columnWriter.writeColumn(column)
                                }
                                override fun flushRegion(regionCoordPair: RegionCoordPair): Task<java.lang.Void> {
                                    columnWriter.flushRegion(regionCoordPair)
                                    return FutureTask(CompletableFuture.completedFuture(null))
                                }
                                override fun flushColumns(): Task<java.lang.Void> {
                                    columnWriter.flushColumns()
                                    return FutureTask(CompletableFuture.completedFuture(null))
                                }
                            }
                            return FutureTask(CompletableFuture.completedFuture(columnHandler))
                        }
                        override fun flushWorld(world: ChunkerWorld?) { worldWriter.flushWorld(world) }
                        override fun flushWorlds() { worldWriter.flushWorlds() }
                    }
                    return FutureTask(CompletableFuture.completedFuture(worldHandler))
                }
                override fun flushLevel() { previewWriter.flushLevel() }
            })
            environment.close()
            environment.future().get()
        } catch (e: Exception) {
            return@withContext Result.failure(workDataOf("error" to (e.message ?: "解析出错")))
        }

        Result.success(workDataOf("isBedrock" to isBedrockFormat))
    }
}