package me.voltual.mcl.core

import java.io.File
import com.google.gson.Gson

class MclSqliteSaver(dbPath: String, spawnX: Int, spawnY: Int, spawnZ: Int) : AutoCloseable {
    
    private val gson = Gson()

    companion object {
        init {
            System.loadLibrary("mc2mt")
        }

        @JvmStatic
        private external fun initNativeEngine(
            dbPath: String, 
            spawnX: Int, 
            spawnY: Int, 
            spawnZ: Int
        ): Boolean

        @JvmStatic
        private external fun writeChunkFast(
            cx: Int, cy: Int, cz: Int,
            blockIds: ShortArray,
            param1: ByteArray,
            param2: ByteArray,
            localNamesJson: ByteArray,
            metadataJson: ByteArray
        ): Boolean

        @JvmStatic
        private external fun flushNativeEngine(): Boolean

        @JvmStatic
        private external fun closeNativeEngine()
    }

    init {
        // 创建目标数据库文件夹
        val file = File(dbPath)
        file.parentFile?.mkdirs()

        // 初始化原生数据库事务和出生点
        val success = initNativeEngine(dbPath, spawnX, spawnY, spawnZ)
        if (!success) {
            throw RuntimeException("Failed to initialize Rust native SQLite database engine.")
        }
    }

    /**
     * 将整个 Chunk 数据推送到 JNI 临界区，实现物理极速落盘
     */
    fun saveChunkNatively(
        cx: Int, cy: Int, cz: Int,
        blockIds: ShortArray,
        param1: ByteArray,
        param2: ByteArray,
        localNames: List<String>,
        metadata: Map<Int, MclBlockEntityData>
    ) {
        val namesJsonBytes = gson.toJson(localNames).toByteArray(Charsets.UTF_8)
        val metaJsonBytes = gson.toJson(metadata).toByteArray(Charsets.UTF_8)

        val status = writeChunkFast(
            cx, cy, cz,
            blockIds,
            param1,
            param2,
            namesJsonBytes,
            metaJsonBytes
        )
        if (!status) {
            System.err.println("[MclSqliteSaver] Native error occurred writing chunk at: ($cx, $cy, $cz)")
        }
    }

    fun commit() {
        flushNativeEngine()
    }

    override fun close() {
        closeNativeEngine()
    }
}