package me.voltual.mcl.mapping

import me.voltual.mcl.mapping.modules.*

object MclMappingInitializer {
    fun initialize() {
        val modules = listOf(
            MclCoreMapping,
            MclWoodMapping,
            MclDimensionMapping,
            MclRedstoneMapping,
            MclVegetationMapping,
            MclOceanMapping,
            MclCandleMapping,
            MclBedMapping,
            MclAmethystMapping // 添加紫水晶模块
        )
        
        println("正在加载 Mineclonia 映射模块...")
        modules.forEach { it.register() }
        println("所有模块加载完成。")
    }
}