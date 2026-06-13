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
    fun simple(targetName: String) = BlockMapper { MclNode(targetName) }

    fun log(targetName: String) = BlockMapper { id ->
        val axis = id.getState(VanillaBlockStates.AXIS) ?: Axis.Y
        val p2 = when (axis) {
            Axis.Y -> 0
            Axis.X -> 12
            Axis.Z -> 4
        }.toByte()
        MclNode(targetName, param2 = p2)
    }
    
    // 其他 DSL 方法 (stair, slab 等) 按需添加...
}