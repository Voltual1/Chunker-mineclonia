package me.voltual.mcl.mapping

import me.voltual.mcl.core.MclNode
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
    
    // 临时存放当前 Column 转换中未映射方块的全局位置到 NBT 文本的映射
    val pendingDebugSigns = HashMap<Int, String>()

    fun register(type: ChunkerVanillaBlockType, mapper: BlockMapper) {
        mappers.computeIfAbsent(type) { mutableListOf() }.add(mapper)
    }

    /**
     * 转换核心：若未映射，则动态将其包装为调试告示牌，并注册待生成的 NBT 文本
     */
    fun convertAndDebug(identifier: ChunkerBlockIdentifier, blockIdx: Int): MclNode {
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
        
        // =========================================================================
        // 【核心调试机制】：未映射方块就地转换为“立式橡木告示牌”，并在告示牌上打印其原始 ID
        // =========================================================================
        val cleanName = identifier.toString()
            .replace("ChunkerBlockIdentifier{", "")
            .replace("}", "")
            
        // 将未识别的元数据存入待挂载的临时 Map 中，供 MclConverterManager 读取
        pendingDebugSigns[blockIdx] = "[MISSING]\n$cleanName"
        
        // 打印到控制台，方便在后台查看
        System.err.println("\u001B[31m[Mapping Debug] Block converted to Sign: $cleanName\u001B[0m")
        
        // 返回橡木告示牌节点 (param2 = 0 默认朝北立着)
        return MclNode("mcl_signs:standing_sign_oak", param2 = 0)
    }

    // 废弃旧的 convert，以防被误用
    @Deprecated("Use convertAndDebug instead", ReplaceWith("convertAndDebug(identifier, blockIdx)"))
    fun convert(identifier: ChunkerBlockIdentifier): MclNode {
        return convertAndDebug(identifier, 0)
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