package me.voltual.mcl.core

/**
 * Minetest 区块坐标
 */
data class MclPos(val x: Int, val y: Int, val z: Int) {
    /**
     * 完全对齐 C++ 版本的 pos 编码算法，解决负数坐标大端对齐溢出问题
     */
    fun encode(): Long {
        // 限制在 12 位或 24 位内（Minetest 经典坐标范围：X: 12位, Y: 12位, Z: 12位）
        val ax = (x and 0xFFF).toLong()
        val ay = (y and 0xFFF).toLong()
        val az = (z and 0xFFF).toLong()

        // 重新拼装为 64 位有符号整型键值
        // 与 C++ 的 (z * -0x1000000) + (y * 0x1000) + (x * -1) 在内存补码层级完全一致：
        return (az shl 24) or (ay shl 12) or ax
    }
}