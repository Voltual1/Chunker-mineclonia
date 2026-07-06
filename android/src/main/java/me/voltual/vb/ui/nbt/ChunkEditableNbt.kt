// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.nbt

import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBChunkType
import com.hivemc.chunker.conversion.encoding.bedrock.util.LevelDBKey
import com.hivemc.chunker.conversion.intermediate.column.chunk.ChunkCoordPair
import com.hivemc.chunker.conversion.intermediate.world.Dimension
import com.hivemc.chunker.nbt.TagType
import com.hivemc.chunker.nbt.io.Reader
import com.hivemc.chunker.nbt.io.Writer
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import com.hivemc.chunker.nbt.tags.collection.ListTag
import com.hivemc.chunker.nbt.tags.primitive.StringTag
import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class ChunkEditableNbt(
    private val worldDir: File,
    private val chunkX: Int,
    private val chunkZ: Int,
    private val dimension: Dimension, // 引入维度
    private val isEntity: Boolean,
    private val isBedrock: Boolean
) : EditableNbt() {

    private var rootTag: CompoundTag = CompoundTag()
    private var mcaFile: File? = null
    
    init {
        loadData()
    }
    
    private fun loadData() {
        if (isBedrock) {
            val dbDir = File(worldDir, "db")
            if (!dbDir.exists()) {
                rootTag.put("DB_NOT_FOUND", StringTag("未能在 ${worldDir.name} 下找到 db 目录！"))
                return
            }

            try {
                File(dbDir, "LOCK").delete()
                val options = Options().createIfMissing(false)
                
                Iq80DBFactory.factory.open(dbDir, options).use { db ->
                    val chunkPair = ChunkCoordPair(chunkX, chunkZ)
                    val chunkType = if (isEntity) LevelDBChunkType.ENTITY else LevelDBChunkType.BLOCK_ENTITY
                    
                    // 完美结合：Bedrock 的 LevelDBKey 自带维度识别
                    val key = LevelDBKey.key(dimension, chunkPair, chunkType)
                    val bytes = db.get(key)
                    
                    if (bytes != null && bytes.isNotEmpty()) {
                        val listTag = ListTag<CompoundTag, Map<String, Tag<*>>>()
                        ByteArrayInputStream(bytes).use { bais ->
                            DataInputStream(bais).use { dis ->
                                val reader = Reader.toBedrockReader(dis)
                                while (bais.available() > 0) {
                                    val pair = Tag.decodeNamed(reader, CompoundTag::class.java) ?: break
                                    listTag.add(pair.tag())
                                }
                            }
                        }
                        rootTag.put(if (isEntity) "Entities" else "BlockEntities", listTag)
                    } else {
                        rootTag.put("EMPTY_CHUNK_DATA", StringTag("该区块 (${chunkX}, ${chunkZ}) 暂无${if (isEntity) "实体" else "方块实体"}数据。"))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                rootTag.put("EXCEPTION", StringTag("LevelDB 读取失败: " + e.localizedMessage))
            }
            return
        }

        // Java MCA 多维度精确物理寻址
        val regionX = chunkX shr 5
        val regionZ = chunkZ shr 5
        val dimFolder = when (dimension) {
            Dimension.NETHER -> "DIM-1"
            Dimension.THE_END -> "DIM1"
            else -> ""
        }
        val typeFolder = if (isEntity) "entities" else "region"
        
        val targetPath = if (dimFolder.isEmpty()) {
            File(worldDir, "$typeFolder/r.$regionX.$regionZ.mca")
        } else {
            File(worldDir, "$dimFolder/$typeFolder/r.$regionX.$regionZ.mca")
        }

        mcaFile = targetPath

        if (!targetPath.exists()) {
            rootTag.put("FILE_NOT_FOUND", StringTag("无法定位区域文件: ${targetPath.absolutePath}"))
            return
        }

        try {
            RandomAccessFile(targetPath, "r").use { raf ->
                val reader = Reader.toJavaReader(raf)
                val offsets = IntArray(1024)
                val temp = ByteArray(4096)
                reader.readBytes(temp)
                for (i in 0 until 1024) {
                    val tempIndex = i shl 2
                    val offset = ((temp[tempIndex].toInt() and 0xFF) shl 16) or
                                 ((temp[tempIndex + 1].toInt() and 0xFF) shl 8) or
                                 (temp[tempIndex + 2].toInt() and 0xFF)
                    offsets[i] = offset
                }
                
                val index = (chunkX and 31) + (chunkZ and 31) * 32
                val offset = offsets[index]
                
                if (offset > 0) {
                    raf.seek(offset * 4096L)
                    val chunkLength = reader.readInt() - 1
                    val compressionType = (reader.readByte().toInt() and 0x7F).toByte()
                    
                    val compressedColumn = ByteArray(chunkLength)
                    reader.readBytes(compressedColumn)
                    
                    val tag = when (compressionType.toInt()) {
                        1 -> Tag.readGZipJavaNBT(compressedColumn)
                        2 -> Tag.readZLibJavaNBT(compressedColumn)
                        3 -> Tag.readUncompressedJavaNBT(compressedColumn)
                        4 -> Tag.readLZ4JavaNBT(compressedColumn)
                        else -> {
                            rootTag.put("COMPRESSION_ERROR", StringTag("不支持的压缩类型: $compressionType"))
                            null
                        }
                    }
                    if (tag != null) {
                        rootTag = tag
                    } else if (!rootTag.contains("COMPRESSION_ERROR")) {
                        rootTag.put("PARSE_ERROR", StringTag("无法解析 NBT 数据。"))
                    }
                } else {
                    rootTag.put("UNGENERATED_CHUNK", StringTag("区块 ($chunkX, $chunkZ) 尚未生成 (Offset=0)。"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            rootTag.put("EXCEPTION", StringTag(e.toString()))
        }
    }

    override fun getTags(): List<Pair<String, Tag<*>>> {
        return rootTag.value?.entries?.map { it.key to it.value } ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    override fun save(): Boolean {
        if (isBedrock) {
            val dbDir = File(worldDir, "db")
            if (!dbDir.exists()) return false

            return try {
                File(dbDir, "LOCK").delete()
                val options = Options().createIfMissing(false)
                
                Iq80DBFactory.factory.open(dbDir, options).use { db ->
                    val chunkPair = ChunkCoordPair(chunkX, chunkZ)
                    val chunkType = if (isEntity) LevelDBChunkType.ENTITY else LevelDBChunkType.BLOCK_ENTITY
                    val key = LevelDBKey.key(dimension, chunkPair, chunkType)
                    
                    val listName = if (isEntity) "Entities" else "BlockEntities"
                    val listTag = rootTag.get(listName) as? ListTag<CompoundTag, *>
                    
                    if (listTag != null && listTag.size() > 0) {
                        val bytes = ByteArrayOutputStream().use { baos ->
                            DataOutputStream(baos).use { dos ->
                                val writer = Writer.toBedrockWriter(dos)
                                for (compound in listTag.value) {
                                    Tag.encodeNamed(writer, "", compound)
                                }
                            }
                            baos.toByteArray()
                        }
                        db.put(key, bytes)
                    } else {
                        db.delete(key)
                    }
                }
                clearModified()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            val file = mcaFile ?: return false
            if (!file.exists()) return false
            
            return try {
                val compressedData = ByteArrayOutputStream().use { baos ->
                    DeflaterOutputStream(baos, Deflater(Deflater.BEST_SPEED)).use { dos ->
                        DataOutputStream(dos).use { daos ->
                            Tag.encodeNamed(Writer.toJavaWriter(daos), "", rootTag)
                        }
                    }
                    baos.toByteArray()
                }
                
                RandomAccessFile(file, "rw").use { raf ->
                    val fileLen = raf.length()
                    val padEnd = (4096 - (fileLen % 4096)) % 4096
                    raf.seek(fileLen + padEnd)
                    
                    val newOffsetSector = (raf.filePointer / 4096).toInt()
                    
                    raf.writeInt(compressedData.size + 1)
                    raf.writeByte(2)
                    raf.write(compressedData)
                    
                    val chunkPad = (4096 - (raf.filePointer % 4096)) % 4096
                    val padding = ByteArray(chunkPad.toInt())
                    raf.write(padding)
                    
                    val sectorCount = Math.ceil((compressedData.size + 5).toDouble() / 4096.0).toInt()
                    
                    val index = (chunkX and 31) + (chunkZ and 31) * 32
                    raf.seek(index * 4L)
                    val isSaved = (newOffsetSector shr 16) and 0xFF
                    raf.writeByte(isSaved)
                    raf.writeByte((newOffsetSector shr 8) and 0xFF)
                    raf.writeByte(newOffsetSector and 0xFF)
                    raf.writeByte(sectorCount and 0xFF)
                }
                clearModified()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    override fun getRootTitle(): String = "区块 ($chunkX, $chunkZ) ${if (isEntity) "实体" else "信息"}"

    override fun addRootTag(name: String, tag: Tag<*>) {
        rootTag.put(name, tag)
        markModified()
    }

    override fun removeRootTag(name: String) {
        rootTag.remove(name)
        markModified()
    }
}