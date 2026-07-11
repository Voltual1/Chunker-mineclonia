package me.voltual.mcl.mapping

import me.voltual.mcl.core.MclNode
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType

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

    /**
     * 【线程安全修改】：接收一个回调函数 `onMissing`，将未映射文本通知给调用它的本地 Column 线程
     */
    fun convertAndDebug(
        identifier: ChunkerBlockIdentifier, 
        blockIdx: Int, 
        onMissing: (Int, String) -> Unit
    ): MclNode {
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
        
        // 提取干净的名称和状态
        val cleanName = identifier.toString()
            .replace("ChunkerBlockIdentifier{", "")
            .replace("}", "")
            
        // 通过回调将未识别信息存入当前线程局部的容器中
        onMissing(blockIdx, "[MISSING]\n$cleanName")
        
        System.err.println("\u001B[31m[Mapping Debug] Block converted to Sign: $cleanName\u001B[0m")
        
        // 返回橡木告示牌节点 (param2 = 0 默认朝北立着)
        return MclNode("mcl_signs:standing_sign_oak", param2 = 0)
    }

    @Deprecated("Use convertAndDebug instead", ReplaceWith("convertAndDebug(identifier, blockIdx) { _, _ -> }"))
    fun convert(identifier: ChunkerBlockIdentifier): MclNode {
        return convertAndDebug(identifier, 0) { _, _ -> }
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