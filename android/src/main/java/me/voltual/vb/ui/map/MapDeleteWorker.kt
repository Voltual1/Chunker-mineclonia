package me.voltual.vb.ui.map

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker
import androidx.work.workDataOf
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.File
import java.io.RandomAccessFile

class MapDeleteWorker(context: Context, params: WorkerParameters) : RemoteCoroutineWorker(context, params) {
    override suspend fun doRemoteWork(): Result = withContext(Dispatchers.IO) {
        val worldDirUri = inputData.getString("worldDirUri") ?: return@withContext Result.failure()
        val isBedrock = inputData.getBoolean("isBedrock", false)
        val dimId = inputData.getString("dimension") ?: "minecraft:overworld"
        val dimension = Dimension.values().find { it.identifier == dimId } ?: Dimension.OVERWORLD

        val chunkMinX = inputData.getInt("minX", 0)
        val chunkMinZ = inputData.getInt("minZ", 0)
        val chunkMaxX = inputData.getInt("maxX", 0)
        val chunkMaxZ = inputData.getInt("maxZ", 0)

        var deletedCount = 0

        try {
            if (isBedrock) {
                val dbDir = File(worldDirUri, "db")
                if (!dbDir.exists()) return@withContext Result.failure(workDataOf("error" to "DB directory missing"))

                File(dbDir, "LOCK").delete()
                val options = Options().createIfMissing(false)
                Iq80DBFactory.factory.open(dbDir, options).use { db ->
                    val batch = db.createWriteBatch()
                    for (cx in chunkMinX..chunkMaxX) {
                        for (cz in chunkMinZ..chunkMaxZ) {
                            val chunk = ChunkCoordPair(cx, cz)
                            for (type in LevelDBChunkType.values()) {
                                if (type == LevelDBChunkType.SUB_CHUNK_PREFIX) {
                                    for (y in -64..64) {
                                        batch.delete(LevelDBKey.key(dimension, chunk, y.toByte(), type))
                                    }
                                } else {
                                    batch.delete(LevelDBKey.key(dimension, chunk, type))
                                }
                            }
                            deletedCount++
                        }
                    }
                    db.write(batch)
                }
            } else {
                val regionXStart = chunkMinX shr 5
                val regionZStart = chunkMinZ shr 5
                val regionXEnd = chunkMaxX shr 5
                val regionZEnd = chunkMaxZ shr 5

                val dimFolder = when (dimension) {
                    Dimension.NETHER -> "DIM-1"
                    Dimension.THE_END -> "DIM1"
                    else -> ""
                }
                val dirs = listOf("region", "entities", "poi")

                for (rx in regionXStart..regionXEnd) {
                    for (rz in regionZStart..regionZEnd) {
                        dirs.forEach { dirName ->
                            val targetPath = if (dimFolder.isEmpty()) {
                                File(worldDirUri, "$dirName/r.$rx.$rz.mca")
                            } else {
                                File(worldDirUri, "$dimFolder/$dirName/r.$rx.$rz.mca")
                            }

                            if (targetPath.exists()) {
                                RandomAccessFile(targetPath, "rw").use { raf ->
                                    for (cx in chunkMinX..chunkMaxX) {
                                        for (cz in chunkMinZ..chunkMaxZ) {
                                            if ((cx shr 5) == rx && (cz shr 5) == rz) {
                                                val index = (cx and 31) + (cz and 31) * 32
                                                raf.seek(index * 4L)
                                                raf.writeInt(0)
                                                deletedCount++
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Result.success(workDataOf("deletedCount" to deletedCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf("error" to (e.message ?: "Deletion error")))
        }
    }
}