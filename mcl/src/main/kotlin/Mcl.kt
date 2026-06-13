package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Half
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.SlabType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Axis
import com.hivemc.chunker.conversion.intermediate.column.blockentity.BlockEntity

/**
 * 代表 Mineclonia 中的方块节点信息
 */
data class MclNode(
    val name: String,
    val param1: Byte = 0,
    val param2: Byte = 0,
    val blockEntityConverter: ((BlockEntity) -> Map<String, Any>)? = null
)

/**
 * 方块映射器接口
 */
fun interface BlockMapper {
    fun map(identifier: ChunkerBlockIdentifier): MclNode?
}

/**
 * Mineclonia 映射注册表
 */
object MclMappingRegistry {
    private val mappers = mutableMapOf<ChunkerVanillaBlockType, MutableList<BlockMapper>>()
    private val customMappers = mutableListOf<BlockMapper>()

    /**
     * 注册一个 Vanilla 方块的映射规则
     */
    fun register(type: ChunkerVanillaBlockType, mapper: BlockMapper) {
        mappers.computeIfAbsent(type) { mutableListOf() }.add(mapper)
    }

    /**
     * 注册自定义方块（非 Vanilla）的映射规则
     */
    fun registerCustom(mapper: BlockMapper) {
        customMappers.add(mapper)
    }

    /**
     * 执行转换逻辑
     */
    fun convert(identifier: ChunkerBlockIdentifier): MclNode {
        val type = identifier.type
        if (type is ChunkerVanillaBlockType) {
            val list = mappers[type]
            if (list != null) {
                for (mapper in list) {
                    val result = mapper.map(identifier)
                    if (result != null) return result
                }
            }
        } else {
            // 处理自定义方块
            for (mapper in customMappers) {
                val result = mapper.map(identifier)
                if (result != null) return result
            }
        }
        // 默认回退到 air
        return MclNode("air")
    }
}

/**
 * DSL 辅助工具类，用于编写映射规则
 */
object MclMappingDsl {

    // 1. 简单无状态方块映射
    fun simple(targetName: String) = BlockMapper { _ ->
        MclNode(targetName)
    }

    // 2. 朝向方块映射 (例如：熔炉、胸箱)
    fun directional(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 3. 楼梯方块映射 (Stairs)
    fun stair(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        
        // Minetest 的 facedir 楼梯朝向计算
        val baseDir = when (facing) {
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.WEST -> 3
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.NORTH -> 2
        }
        val param2 = if (half == Half.TOP) {
            // 倒置楼梯
            (baseDir + 20).toByte()
        } else {
            baseDir.toByte()
        }
        MclNode(targetName, param2 = param2)
    }

    // 4. 台阶方块映射 (Slabs)
    fun slab(bottomTarget: String, topTarget: String, doubleTarget: String) = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.SLAB_TYPE) ?: SlabType.BOTTOM
        when (type) {
            SlabType.BOTTOM -> MclNode(bottomTarget)
            SlabType.TOP -> MclNode(topTarget)
            SlabType.DOUBLE -> MclNode(doubleTarget)
        }
    }

    // 5. 原木轴向映射 (Log Axis)
    fun log(targetName: String) = BlockMapper { id ->
        val axis = id.getState(VanillaBlockStates.AXIS) ?: Axis.Y
        val param2 = when (axis) {
            Axis.Y -> 0
            Axis.X -> 12
            Axis.Z -> 4
        }.toByte()
        MclNode(targetName, param2 = param2)
    }
}