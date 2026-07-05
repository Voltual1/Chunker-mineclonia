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
import com.hivemc.chunker.nbt.tags.primitive.StringTag
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class ChunkEditableNbt(
    private val worldDir: File,
    private val chunkX: Int,
    private val chunkZ: Int,
    private val isEntity: Boolean,
    private val isBedrock: Boolean
) : EditableNbt() {

    private var rootTag: CompoundTag = CompoundTag()
    private var mcaFile: File? = null
    
    init {
        loadData()
    }
    
    /**
     * 在存档主目录内，递归搜索目标 MCA 文件
     */
    private fun findMcaFile(directory: File, targetName: String): File? {
        val files = directory.listFiles() ?: return null
        // 优先搜索当前层级下的匹配文件，避免过度深层扫描
        for (file in files) {
            if (file.isFile && file.name.equals(targetName, ignoreCase = true)) {
                return file
            }
        }
        // 如果第一层没找到，递归搜索子目录（排除无用目录）
        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                val found = findMcaFile(file, targetName)
                if (found != null) return found
            }
        }
        return null
    }

    private fun loadData() {
        if (isBedrock) {
            rootTag.put("BEDROCK_UNSUPPORTED", StringTag("基岩版的区块编辑目前正在完善底层绑定，目前仅支持 Java 格式直接修改。"))
            return
        }

        val regionX = chunkX shr 5
        val regionZ = chunkZ shr 5
        
        // 自动计算应寻找的目标 MCA 文件名
        // 实体对应 entities，区块方块对应 region
        val filePrefix = if (isEntity) "r" else "r" // MCA 格式的区域前缀均是 r.X.Z.mca
        val targetFileName = "r.$regionX.$regionZ.mca"

        // 使用自适应搜索逻辑
        val foundFile = findMcaFile(worldDir, targetFileName)
        mcaFile = foundFile

        if (foundFile == null || !foundFile.exists()) {
            rootTag.put("FILE_NOT_FOUND", StringTag("无法在 ${worldDir.name} 及其子目录中定位区域文件: $targetFileName"))
            return
        }

        try {
            RandomAccessFile(foundFile, "r").use { raf ->
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
                    val rawType = reader.readByte()
                    val compressionType = (rawType.toInt() and 0x7F).toByte()
                    
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
            rootTag.put("EXCEPTION", StringTag(e.toString() + "\n" + e.stackTraceToString()))
        }
    }

    override fun getTags(): List<Pair<String, Tag<*>>> {
        return rootTag.value?.entries?.map { it.key to it.value } ?: emptyList()
    }

    override fun save(): Boolean {
        if (isBedrock) return false
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
                raf.writeByte(2) // 2 = Zlib
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

    override fun getRootTitle(): String {
        return "区块 ($chunkX, $chunkZ) ${if (isEntity) "实体" else "信息"}"
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