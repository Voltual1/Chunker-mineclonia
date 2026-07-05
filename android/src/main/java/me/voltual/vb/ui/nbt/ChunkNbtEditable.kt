// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.ui.nbt

import com.hivemc.chunker.nbt.io.Reader
import com.hivemc.chunker.nbt.io.Writer
import com.hivemc.chunker.nbt.tags.Tag
import com.hivemc.chunker.nbt.tags.collection.CompoundTag
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * 外科手术式区块 NBT 编辑器。
 * 直接利用 MCA 的文件分配表 (FAT) 读取特定区块，并在保存时将新压缩数据追加到文件末尾并更新头指针。
 */
class ChunkEditableNbt(
    private val worldDir: File,
    private val chunkX: Int,
    private val chunkZ: Int,
    private val isEntity: Boolean,
    private val isBedrock: Boolean
) : EditableNbt() {

    private var rootTag: CompoundTag = CompoundTag()
    
    // 用于 Java MCA
    private var mcaFile: File? = null
    
    init {
        loadData()
    }
    
    private fun loadData() {
        if (isBedrock) {
            // Bedrock 实现：依赖 litl.leveldb。
            // 在此预留接口位置，实际需打开 LevelDB 并根据 LevelDBKey.key() 查询
            rootTag = CompoundTag()
        } else {
            // Java MCA 实现
            val regionX = chunkX shr 5
            val regionZ = chunkZ shr 5
            val folderName = if (isEntity) "entities" else "region"
            mcaFile = File(worldDir, "$folderName/r.$regionX.$regionZ.mca")
            
            if (mcaFile?.exists() == true) {
                try {
                    RandomAccessFile(mcaFile, "r").use { raf ->
                        val reader = Reader.toJavaReader(raf)
                        // 读取 4096 字节的 Offset 表
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
                            val rawType = reader.readByte()
                            val compressionType = (rawType.toInt() and 0x7F).toByte()
                            
                            val compressedColumn = ByteArray(chunkLength)
                            reader.readBytes(compressedColumn)
                            
                            val tag = when (compressionType.toInt()) {
                                1 -> Tag.readGZipJavaNBT(compressedColumn)
                                2 -> Tag.readZLibJavaNBT(compressedColumn)
                                3 -> Tag.readUncompressedJavaNBT(compressedColumn)
                                4 -> Tag.readLZ4JavaNBT(compressedColumn)
                                else -> null
                            }
                            if (tag != null) {
                                rootTag = tag
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun getTags(): List<Pair<String, Tag<*>>> {
        return rootTag.value?.entries?.map { it.key to it.value } ?: emptyList()
    }

    override fun save(): Boolean {
        if (isBedrock) {
            // Bedrock 保存：将修改后的 NBT 写入 LevelDB
            return false
        } else {
            // Java MCA 保存：追加写入并在头部更新扇区指针
            val file = mcaFile ?: return false
            if (!file.exists()) return false
            
            return try {
                // 以 ZLib (Deflate) 格式重新压缩数据
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
                    // 填充文件末尾直至 4096 的整数倍（扇区对齐）
                    val padEnd = (4096 - (fileLen % 4096)) % 4096
                    raf.seek(fileLen + padEnd)
                    
                    val newOffsetSector = (raf.filePointer / 4096).toInt()
                    
                    // 写入 Chunk 头: length (数据长度 + 1 字节压缩类型标识)
                    raf.writeInt(compressedData.size + 1)
                    raf.writeByte(2) // 2 = Zlib
                    raf.write(compressedData)
                    
                    // 在数据之后填充到 4096 字节的倍数
                    val chunkPad = (4096 - (raf.filePointer % 4096)) % 4096
                    val padding = ByteArray(chunkPad.toInt())
                    raf.write(padding)
                    
                    // 计算所占扇区总数
                    val sectorCount = Math.ceil((compressedData.size + 5).toDouble() / 4096.0).toInt()
                    
                    // 更新头部的 Offset 表
                    val index = (chunkX and 31) + (chunkZ and 31) * 32
                    raf.seek(index * 4L)
                    raf.writeByte((newOffsetSector shr 16) and 0xFF)
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

    override fun getRootTitle(): String {
        return "Chunk ($chunkX, $chunkZ) ${if (isEntity) "Entities" else "Block Entities"}"
    }

    override fun addRootTag(name: String, tag: Tag<*>) {
        rootTag.put(name, tag)
        markModified()
    }

    override fun removeRootTag(name: String) {
        rootTag.remove(name)
        markModified()
    }
}