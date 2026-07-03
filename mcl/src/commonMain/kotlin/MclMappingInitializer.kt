package me.voltual.mcl

object MclMappingInitializer {
    fun initialize() {
        val modules = listOf(
            MclCoreMapping,
            MclWoodMapping
        )
        
        println("--- Mineclonia 映射系统 ---")
        modules.forEach { 
            println("正在加载模块: ${it::class.java.simpleName}")
            it.register() 
        }
        println("映射初始化完成。")
    }
}