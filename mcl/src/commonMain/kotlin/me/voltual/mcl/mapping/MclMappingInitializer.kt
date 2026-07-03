package me.voltual.mcl.mapping

import me.voltual.mcl.mapping.modules.MclCoreMapping
import me.voltual.mcl.mapping.modules.MclWoodMapping
import me.voltual.mcl.mapping.modules.MclDimensionMapping

object MclMappingInitializer {
    fun initialize() {
        val modules = listOf(
            MclCoreMapping,
            MclWoodMapping,
            MclDimensionMapping
        )
        
        println("正在加载 Mineclonia 映射模块...")
        modules.forEach { it.register() }
        println("维度与环境方块加载完成。")
    }
}