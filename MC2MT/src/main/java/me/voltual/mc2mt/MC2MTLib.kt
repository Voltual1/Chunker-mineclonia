package me.voltual.mc2mt

import androidx.annotation.Keep

@Keep
class MC2MTLib {

    @Keep
    interface ConvertCallback {
        /**
         * 转换进度回调
         * @param groupsDone 已完成的区块组数量 (每组包含 32x32 个 Chunk)
         * @param totalGroups 总区块组数量
         * @param blocksDone 已转换的 Minetest Block 节点数量
         */
        fun onProgress(groupsDone: Long, totalGroups: Long, blocksDone: Long)
    }

    /**
     * 将 Minecraft 存档转换为 Minetest 格式
     * @param inputPath Minecraft 存档目录路径 (需包含 level.dat)
     * @param outputPath 输出 Minetest 数据库地图文件的保存路径 (如 map.sqlite)
     * @param callback 进度监听回调
     * @return 转换是否成功
     */
    external fun convertMap(inputPath: String, outputPath: String, callback: ConvertCallback?): Boolean

    companion object {
        init {
            // 加载 CMake 生成的 C++ 共享动态库
            System.loadLibrary("MC2MT")
        }
    }
}