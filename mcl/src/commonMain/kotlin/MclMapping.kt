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
 * 映射模块接口
 */
interface MclMappingModule {
    fun register()
}

/**
 * Mineclonia 映射注册表
 */
object MclMappingRegistry {
    private val mappers = mutableMapOf<ChunkerVanillaBlockType, MutableList<BlockMapper>>()

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
        
        // 未映射方块调试信息
        System.err.println("\u001B[33m[Mapping Debug] Missing block mapping for identifier: $identifier\u001B[0m")
        return MclNode("mcl_core:cobble") 
    }
}

/**
 * 颜色定义
 */
data class MclDyeColor(val name: String, val palette_index: Int)

object mcl_dyes {
    val colors = mapOf(
        "white" to MclDyeColor("white", 0), "silver" to MclDyeColor("silver", 1),
        "grey" to MclDyeColor("grey", 2), "black" to MclDyeColor("black", 3),
        "purple" to MclDyeColor("purple", 4), "blue" to MclDyeColor("blue", 5),
        "light_blue" to MclDyeColor("light_blue", 6), "cyan" to MclDyeColor("cyan", 7),
        "green" to MclDyeColor("green", 8), "lime" to MclDyeColor("lime", 9),
        "yellow" to MclDyeColor("yellow", 10), "brown" to MclDyeColor("brown", 11),
        "orange" to MclDyeColor("orange", 12), "red" to MclDyeColor("red", 13),
        "magenta" to MclDyeColor("magenta", 14), "pink" to MclDyeColor("pink", 15)
    )
}