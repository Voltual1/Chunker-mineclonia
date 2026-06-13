package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*

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

    fun register(type: ChunkerVanillaBlockType, mapper: BlockMapper) {
        mappers.computeIfAbsent(type) { mutableListOf() }.add(mapper)
    }

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
        }
        return MclNode("mcl_core:cobble") // 默认回退
    }
}

/**
 * DSL 工具类
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