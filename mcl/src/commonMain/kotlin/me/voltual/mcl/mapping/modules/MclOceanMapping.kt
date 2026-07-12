package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.core.MclNode
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclOceanMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // ==========================================
        // 1. 海晶石系列 (mcl_ocean:prismarine.lua)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.PRISMARINE, dsl.simple("mcl_ocean:prismarine"))
        registry.register(ChunkerVanillaBlockType.PRISMARINE_BRICKS, dsl.simple("mcl_ocean:prismarine_brick"))
        registry.register(ChunkerVanillaBlockType.DARK_PRISMARINE, dsl.simple("mcl_ocean:prismarine_dark"))
        registry.register(ChunkerVanillaBlockType.SEA_LANTERN, dsl.simple("mcl_ocean:sea_lantern"))

        // 海晶石阶梯与半砖 - 修正 Chunker 枚举前缀不一致问题 (DARK_PRISMARINE vs PRISMARINE_BRICK)
        registerOceanSubsets("prismarine", "PRISMARINE")
        registerOceanSubsets("prismarine_brick", "PRISMARINE_BRICK")
        registerOceanSubsets("prismarine_dark", "DARK_PRISMARINE")

        // ==========================================
        // 2. 海草与海带 (mcl_ocean:seagrass.lua / kelp.lua)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.SEAGRASS, dsl.simple("mcl_ocean:seagrass_sand"))
        registry.register(ChunkerVanillaBlockType.TALL_SEAGRASS, BlockMapper { id ->
            val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
            if (half == Half.BOTTOM) MclNode("mcl_ocean:seagrass_sand") else MclNode("mcl_core:water_source")
        })

        registry.register(ChunkerVanillaBlockType.KELP, dsl.simple("mcl_ocean:kelp_sand"))
        registry.register(ChunkerVanillaBlockType.KELP_PLANT, dsl.simple("mcl_ocean:kelp_sand"))
        registry.register(ChunkerVanillaBlockType.DRIED_KELP_BLOCK, dsl.simple("mcl_ocean:dried_kelp_block"))

        // ==========================================
        // 3. 珊瑚系列 (mcl_ocean:corals.lua)
        // ==========================================
        val coralTypes = listOf("tube", "brain", "bubble", "fire", "horn")
        for (type in coralTypes) {
            val upperType = type.uppercase()
            
            registry.register(enumValueOf("${upperType}_CORAL_BLOCK"), dsl.simple("mcl_ocean:${type}_coral_block"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_BLOCK"), dsl.simple("mcl_ocean:dead_${type}_coral_block"))
            
            registry.register(enumValueOf("${upperType}_CORAL"), dsl.simple("mcl_ocean:${type}_coral"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL"), dsl.simple("mcl_ocean:dead_${type}_coral"))
            
            registry.register(enumValueOf("${upperType}_CORAL_FAN"), dsl.simple("mcl_ocean:${type}_coral_fan"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_FAN"), dsl.simple("mcl_ocean:dead_${type}_coral_fan"))
            registry.register(enumValueOf("${upperType}_CORAL_WALL_FAN"), dsl.simple("mcl_ocean:${type}_coral_fan"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_WALL_FAN"), dsl.simple("mcl_ocean:dead_${type}_coral_fan"))
        }

        // ==========================================
        // 4. 海泡菜与导管 (mcl_ocean:sea_pickle.lua / mcl_conduits)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.SEA_PICKLE, BlockMapper { id ->
            val count = id.getState(VanillaBlockStates.PICKLES) ?: Pickles._1
            val dead = id.getState(VanillaBlockStates.DEAD) ?: Bool.FALSE
            val num = count.ordinal + 1
            
            val suffix = if (dead == Bool.TRUE) "_off_dead_brain_coral_block" else "_dead_brain_coral_block"
            MclNode("mcl_ocean:sea_pickle_$num$suffix")
        })

        // 潮涌核心映射
        registry.register(ChunkerVanillaBlockType.CONDUIT, dsl.simple("mcl_conduits:conduit"))
    }

    /**
     * 注册海洋材质的阶梯和半砖变体
     * @param mclName Mineclonia 节点名称后缀
     * @param chunkerPrefix Chunker 枚举名称前缀 (如 "PRISMARINE" 或 "DARK_PRISMARINE")
     */
    private fun registerOceanSubsets(mclName: String, chunkerPrefix: String) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        try {
            val stairType = enumValueOf<ChunkerVanillaBlockType>("${chunkerPrefix}_STAIRS")
            registry.register(stairType, dsl.stair("mcl_stairs:stair_$mclName"))
        } catch (_: Exception) {}

        try {
            val slabType = enumValueOf<ChunkerVanillaBlockType>("${chunkerPrefix}_SLAB")
            registry.register(slabType, dsl.slab(
                "mcl_stairs:slab_$mclName",
                "mcl_stairs:slab_${mclName}_top",
                "mcl_stairs:slab_${mclName}_double"
            ))
        } catch (_: Exception) {}
        
        try {
            val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerPrefix}_WALL")
            registry.register(wallType, dsl.simple("mcl_walls:$mclName"))
        } catch (_: Exception) {}
    }
}