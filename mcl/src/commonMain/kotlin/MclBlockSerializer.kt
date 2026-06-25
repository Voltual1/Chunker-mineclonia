package me.voltual.mcl

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * Minetest 区块序列化器 (Version 25)
 */
object MclBlockSerializer {
    private const val SER_FMT_VER_HIGHEST_WRITE = 25.toByte()

    fun serialize(
        nodes: List<MclNode>,
        metadata: Map<Int, MclBlockEntityData>,
        isUnderground: Boolean
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val dos = DataOutputStream(output)

        // 1. 版本号
        dos.writeByte(SER_FMT_VER_HIGHEST_WRITE.toInt())

        // 2. 标志位
        var flags = 0x02 // day_night_differs
        if (isUnderground) flags = flags or 0x01
        dos.writeByte(flags)

        // 3. 数据宽度
        dos.writeByte(2) // content_width (param0)
        dos.writeByte(2) // params_width (param1 & param2)

        // --- 压缩数据段开始 ---
        val nodeDataStream = ByteArrayOutputStream()
        
        // 建立局部内容映射 (Name -> Local ID)
        val nameToId = mutableMapOf<String, Short>()
        val idToName = mutableListOf<String>()
        
        // 写入 Param0 (Content IDs)
        val p0Stream = ByteArrayOutputStream()
        val p0Dos = DataOutputStream(p0Stream)
        for (node in nodes) {
            val id = nameToId.getOrPut(node.name) {
                val newId = idToName.size.toShort()
                idToName.add(node.name)
                newId
            }
            p0Dos.writeShort(id.toInt())
        }
        nodeDataStream.write(p0Stream.toByteArray())

        // 写入 Param1 (Light)
        for (node in nodes) {
            nodeDataStream.write(node.param1.toInt())
        }

        // 写入 Param2
        for (node in nodes) {
            nodeDataStream.write(node.param2.toInt())
        }

        // 压缩节点数据
        dos.write(compress(nodeDataStream.toByteArray()))

        // 4. 序列化元数据 (Node Meta)
        val metaStream = ByteArrayOutputStream()
        if (metadata.isEmpty()) {
            metaStream.write(0)
        } else {
            val metaDos = DataOutputStream(metaStream)
            metaStream.write(1) // Version
            metaDos.writeShort(metadata.size)
            for ((idx, data) in metadata) {
                // Minetest 索引顺序转换: ZYX
                val z = (idx shr 4) and 0xF
                val y = (idx shr 8) and 0xF
                val x = idx and 0xF
                val mtIdx = (z * 256 + y * 16 + x).toShort()
                
                metaDos.writeShort(mtIdx.toInt())
                metaDos.writeInt(data.fields.size)
                for ((k, v) in data.fields) {
                    writeString(metaDos, k)
                    writeLongString(metaDos, v)
                }
                serializeInventory(metaDos, data.inventories)
            }
        }
        dos.write(compress(metaStream.toByteArray()))

        // 5. 静态对象 (Static Objects)
        dos.writeByte(0) // Version
        dos.writeShort(0) // Count

        // 6. 时间戳
        dos.writeInt(0xFFFFFFFF.toInt())

        // 7. 名字-ID 映射 (Name-ID mapping)
        dos.writeByte(0) // Version
        dos.writeShort(idToName.size)
        for (i in idToName.indices) {
            dos.writeShort(i)
            writeString(dos, idToName[i])
        }

        // 8. 节点定时器 (Node Timers)
        dos.writeByte(10) // Length
        dos.writeShort(0) // Count

        return output.toByteArray()
    }

    private fun compress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        try {
            DeflaterOutputStream(bos, deflater).use { deflaterStream ->
                deflaterStream.write(data)
            }
        } finally {
            // 关键：显式释放 C++ 层的原生内存，防止转换大存档时内存暴涨挂起
            deflater.end()
        }
        return bos.toByteArray()
    }

    private fun writeString(dos: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        dos.writeShort(bytes.size)
        dos.write(bytes)
    }

    private fun writeLongString(dos: DataOutputStream, s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun serializeInventory(dos: DataOutputStream, inventories: Map<String, MclInventory>) {
        for ((name, inv) in inventories) {
            dos.writeBytes("List $name ${inv.items.size}\n")
            dos.writeBytes("Width ${inv.width}\n")
            for (item in inv.items) {
                if (item.count == 0) {
                    dos.writeBytes("Empty\n")
                } else {
                    dos.writeBytes("Item ${item.name} ${item.count} ${item.wear}\n")
                }
            }
            dos.writeBytes("EndInventoryList\n")
        }
        dos.writeBytes("EndInventory\n")
    }
}