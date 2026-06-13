package me.voltual.mcl

/**
 * Minetest 区块坐标
 */
data class MclPos(val x: Int, val y: Int, val z: Int) {
    /**
     * 按照 MC2MT 的逻辑进行坐标编码
     */
    fun encode(): Long {
        return z.toLong() * -16777216L + y.toLong() * 4096L + x.toLong() * -1L
    }
}