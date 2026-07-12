package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclAmethystMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 1. 紫水晶基础方块
        registry.register(ChunkerVanillaBlockType.AMETHYST_BLOCK, dsl.simple("mcl_amethyst:amethyst_block"))
        registry.register(ChunkerVanillaBlockType.BUDDING_AMETHYST, dsl.simple("mcl_amethyst:budding_amethyst_block"))
        
        // 2. 晶簇与晶芽 (使用 wallmounted 处理 6 方向朝向)
        registry.register(ChunkerVanillaBlockType.AMETHYST_CLUSTER, dsl.wallmounted("mcl_amethyst:amethyst_cluster"))
        registry.register(ChunkerVanillaBlockType.LARGE_AMETHYST_BUD, dsl.wallmounted("mcl_amethyst:large_amethyst_bud"))
        registry.register(ChunkerVanillaBlockType.MEDIUM_AMETHYST_BUD, dsl.wallmounted("mcl_amethyst:medium_amethyst_bud"))
        registry.register(ChunkerVanillaBlockType.SMALL_AMETHYST_BUD, dsl.wallmounted("mcl_amethyst:small_amethyst_bud"))

        // 3. 伴生方块
        registry.register(ChunkerVanillaBlockType.CALCITE, dsl.simple("mcl_amethyst:calcite"))
        registry.register(ChunkerVanillaBlockType.TINTED_GLASS, dsl.simple("mcl_amethyst:tinted_glass"))
    }
}