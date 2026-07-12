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

        // 海晶石阶梯与半砖
        registerOceanSubsets("prismarine", "prismarine")
        registerOceanSubsets("prismarine_brick", "prismarine_brick")
        registerOceanSubsets("prismarine_dark", "prismarine_dark")

        // ==========================================
        // 2. 海草与海带 (mcl_ocean:seagrass.lua / kelp.lua)
        // ==========================================
        // Minecraft 的海草分为单层和高海草。
        // Mineclonia 的海草根据基质有变体，这里默认映射到 sand 基质。
        registry.register(ChunkerVanillaBlockType.SEAGRASS, dsl.simple("mcl_ocean:seagrass_sand"))
        registry.register(ChunkerVanillaBlockType.TALL_SEAGRASS, BlockMapper { id ->
            val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
            // Mineclonia 采用 rooted 模式，通常只在底部生成
            if (half == Half.BOTTOM) MclNode("mcl_ocean:seagrass_sand") else MclNode("mcl_core:water_source")
        })

        // 海带：Minecraft 有 KELP (顶端) 和 KELP_PLANT (身段)
        registry.register(ChunkerVanillaBlockType.KELP, dsl.simple("mcl_ocean:kelp_sand"))
        registry.register(ChunkerVanillaBlockType.KELP_PLANT, dsl.simple("mcl_ocean:kelp_sand"))

        // ==========================================
        // 3. 珊瑚系列 (mcl_ocean:corals.lua)
        // ==========================================
        val coralTypes = listOf("tube", "brain", "bubble", "fire", "horn")
        for (type in coralTypes) {
            val upperType = type.uppercase()
            
            // 珊瑚块
            registry.register(enumValueOf("${upperType}_CORAL_BLOCK"), dsl.simple("mcl_ocean:${type}_coral_block"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_BLOCK"), dsl.simple("mcl_ocean:dead_${type}_coral_block"))
            
            // 珊瑚植物
            registry.register(enumValueOf("${upperType}_CORAL"), dsl.simple("mcl_ocean:${type}_coral"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL"), dsl.simple("mcl_ocean:dead_${type}_coral"))
            
            // 珊瑚扇 (Mineclonia 只有地面扇，没有墙扇的特定节点，统一映射)
            registry.register(enumValueOf("${upperType}_CORAL_FAN"), dsl.simple("mcl_ocean:${type}_coral_fan"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_FAN"), dsl.simple("mcl_ocean:dead_${type}_coral_fan"))
            registry.register(enumValueOf("${upperType}_CORAL_WALL_FAN"), dsl.simple("mcl_ocean:${type}_coral_fan"))
            registry.register(enumValueOf("DEAD_${upperType}_CORAL_WALL_FAN"), dsl.simple("mcl_ocean:dead_${type}_coral_fan"))
        }

        // ==========================================
        // 4. 海泡菜 (mcl_ocean:sea_pickle.lua)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.SEA_PICKLE, BlockMapper { id ->
            val count = id.getState(VanillaBlockStates.PICKLES) ?: Pickles._1
            val dead = id.getState(VanillaBlockStates.DEAD) ?: Bool.FALSE
            val num = count.ordinal + 1
            
            // Mineclonia 变体格式: mcl_ocean:sea_pickle_<数量>_<是否关闭>_<基质>
            // 我们默认映射到 dead_brain_coral_block 基质，因为它是文档默认基质
            val suffix = if (dead == Bool.TRUE) "_off_dead_brain_coral_block" else "_dead_brain_coral_block"
            MclNode("mcl_ocean:sea_pickle_$num$suffix")
        })
    }

    /**
     * 注册海洋材质的阶梯和半砖变体
     */
    private fun registerOceanSubsets(mclName: String, chunkerName: String) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        try {
            val stairType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_STAIRS")
            registry.register(stairType, dsl.stair("mcl_stairs:stair_$mclName"))
        } catch (_: Exception) {}

        try {
            val slabType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_SLAB")
            registry.register(slabType, dsl.slab(
                "mcl_stairs:slab_$mclName",
                "mcl_stairs:slab_${mclName}_top",
                "mcl_stairs:slab_${mclName}_double"
            ))
        } catch (_: Exception) {}
        
        // 墙体映射 (如果有)
        try {
            val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_WALL")
            registry.register(wallType, dsl.simple("mcl_walls:$mclName"))
        } catch (_: Exception) {}
    }
}