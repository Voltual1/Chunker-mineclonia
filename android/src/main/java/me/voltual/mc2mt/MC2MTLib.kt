package me.voltual.mc2mt

object MC2MTLib {
    init {
        System.loadLibrary("mc2mt")
    }

    /**
     * 原本的整库直接转换（可选，保留兼容）
     */
    @JvmStatic
    external fun convertMap(inputPath: String, outputPath: String, callback: ConversionCallback?): Boolean

    interface ConversionCallback {
        fun onProgress(groupsDone: Long, totalGroups: Long, blocksDone: Long)
    }

    // =========================================================================
    // 异步管道 JNI 接口：面向 Chunker 转换时的极速内存和文件持久化
    // =========================================================================

    @JvmStatic
    external fun initNativeEngine(dbPath: String, spawnX: Int, spawnY: Int, spawnZ: Int): Boolean

    @JvmStatic
    external fun writeChunkFast(
        cx: Int,
        cy: Int,
        cz: Int,
        blockIds: ShortArray,
        param1: ByteArray,
        param2: ByteArray,
        localNamesJson: ByteArray,
        metadataJson: ByteArray
    ): Boolean

    @JvmStatic
    external fun flushNativeEngine(): Boolean

    @JvmStatic
    external fun closeNativeEngine()
}