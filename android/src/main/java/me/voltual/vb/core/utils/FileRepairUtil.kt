// Copyright (C) 2025 Voltual
// 本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第3版
//（或任意更新的版本）的条款重新分发 and/或 修改 it 的条款。
// 本程序是基于希望 it 有用而分发的，但没有任何担保；甚至没有适销性或特定用途适用性的隐含担保。
// 有关更多细节，请参阅 GNU 通用公共许可证。
//
// 你应该已经收到了一份 GNU 通用公共许可证的副本
// 如果没有，请查阅 <http://www.gnu.org/licenses/>.

package me.voltual.vb.core.utils

import java.io.File

object FileRepairUtil {

    /**
     * 递归修复因 SimpleStorage 复制导致的文件名被强行追加 .bin 后缀的问题。
     * 例如将 000022.ldb.bin 恢复为 000022.ldb，将 CURRENT.bin 恢复为 CURRENT。
     */
    fun repairCopiedDatabaseFiles(dir: File) {
        if (!dir.exists() || !dir.isDirectory) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                repairCopiedDatabaseFiles(file)
            } else {
                val fileName = file.name
                if (fileName.endsWith(".bin", ignoreCase = true)) {
                    val originalName = fileName.substring(0, fileName.length - 4)
                    // 仅当去掉 .bin 后的名称是 LevelDB 或基岩版存档特有数据文件时进行恢复
                    if (originalName.endsWith(".ldb", ignoreCase = true) ||
                        originalName.endsWith(".log", ignoreCase = true) ||
                        originalName.equals("CURRENT", ignoreCase = true) ||
                        originalName.equals("LOCK", ignoreCase = true) ||
                        originalName.startsWith("MANIFEST-", ignoreCase = true)
                    ) {
                        val destFile = File(file.parentFile, originalName)
                        if (destFile.exists()) {
                            destFile.delete()
                        }
                        file.renameTo(destFile)
                    }
                }
            }
        }
    }
}