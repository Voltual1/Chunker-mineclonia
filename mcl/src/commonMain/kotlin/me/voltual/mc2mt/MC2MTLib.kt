package me.voltual.mc2mt

object MC2MTLib {
    init {
        System.loadLibrary("mc2mt")
    }

    @JvmStatic
    external fun convertMap(inputPath: String, outputPath: String, callback: ConversionCallback?): Boolean

    interface ConversionCallback {
        fun onProgress(groupsDone: Long, totalGroups: Long, blocksDone: Long)
    }

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