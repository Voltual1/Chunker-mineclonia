package me.voltual.mcl.core

import kotlinx.serialization.Serializable
import kotlin.math.max

/**
 * 代表 Mineclonia 中的方块节点信息
 * （如果不参与 JSON 传输，可以不加 @Serializable，但加上最稳妥）
 */
@Serializable
data class MclNode(
    val name: String,
    var param1: Byte = 0,
    val param2: Byte = 0
) {
    /**
     * 设置光照 (Minecraft -> Minetest)
     * Minecraft: blockLight, skyLight
     * Minetest param1: (night_light << 4) | day_light
     */
    fun setLight(blockLight: Byte, skyLight: Byte) {
        val dayLight = (max(blockLight.toInt(), skyLight.toInt()) and 0x0F)
        val nightLight = (blockLight.toInt() and 0x0F)
        this.param1 = ((nightLight shl 4) or dayLight).toByte()
    }
}

/**
 * Mineclonia 物品栈的内部表示
 */
@Serializable
data class MclItemStack(
    val name: String,
    val count: Int,
    val wear: Int = 0
)

/**
 * Mineclonia 物品栏的内部表示
 */
@Serializable
data class MclInventory(
    val width: Int,
    val items: List<MclItemStack>
)

/**
 * 转换后的 Mineclonia 方块实体数据
 */
@Serializable
data class MclBlockEntityData(
    val fields: Map<String, String> = emptyMap(),
    val inventories: Map<String, MclInventory> = emptyMap()
)