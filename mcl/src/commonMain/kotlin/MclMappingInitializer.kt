package me.voltual.mcl

object MclMappingInitializer {
    fun initialize() {
        val modules = listOf(
            MclCoreMapping,
            // MclWoodMapping,
            // MclRedstoneMapping,
            // MclVegetationMapping
        )
        
        println("正在加载 Mineclonia 映射模块...")
        modules.forEach { it.register() }
        println("成功加载 ${modules.size} 个映射模块")
    }
}