//Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发和/或修改它。
//本程序是基于希望它有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.
package me.voltual.mc2mt

import android.util.Log

class MC2MTLib {

    // 定义 JNI 回调接口，用于向 JVM 实时推送进度
    interface ConversionCallback {
        fun onProgress(groupsDone: Long, totalGroups: Long, blocksDone: Long)
    }

    companion object {
        private const val TAG = "MC2MTLib"

        init {
            try {
                System.loadLibrary("mc2mt")
                Log.i(TAG, "Successfully loaded native library: libmc2mt.so")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library libmc2mt.so", e)
            }
        }

        /**
         * 调用 Rust 原生高并发转换引擎
         * @param inputPath 输入 Minecraft 存档目录 (包含 level.dat)
         * @param outputPath 输出 Minetest/Mineclonia 存档目录
         * @param callback 进度回调
         * @return 是否转换成功
         */
        @JvmStatic
        external fun convertMap(
            inputPath: String,
            outputPath: String,
            callback: ConversionCallback
        ): Boolean
    }
}