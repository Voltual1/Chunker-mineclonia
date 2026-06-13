package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity

/**
 * 代表 Mineclonia 中的方块节点信息
 */
data class MclNode(
    val name: String,
    val param1: Byte = 0,
    val param2: Byte = 0
)

/**
 * Mineclonia 物品栈的内部表示
 */
data class MclItemStack(
    val name: String,
    val count: Int,
    val wear: Int = 0
)

/**
 * Mineclonia 物品栏的内部表示
 */
data class MclInventory(
    val width: Int,
    val items: List<MclItemStack>
)

/**
 * 转换后的 Mineclonia 方块实体数据
 */
data class MclBlockEntityData(
    val fields: Map<String, String> = emptyMap(),
    val inventories: Map<String, MclInventory> = emptyMap()
)