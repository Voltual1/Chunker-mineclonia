package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Age_3
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirection
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.core.MclNode

object MclDimensionMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // ==========================================
        // 1. 下界核心方块 (mcl_nether / mcl_blackstone)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.NETHERRACK, dsl.simple("mcl_nether:netherrack"))
        registry.register(ChunkerVanillaBlockType.GLOWSTONE, dsl.simple("mcl_nether:glowstone"))
        registry.register(ChunkerVanillaBlockType.MAGMA_BLOCK, dsl.simple("mcl_nether:magma"))
        registry.register(ChunkerVanillaBlockType.SOUL_SAND, dsl.simple("mcl_nether:soul_sand"))
        registry.register(ChunkerVanillaBlockType.SOUL_SOIL, dsl.simple("mcl_blackstone:soul_soil"))
        
        // 下界矿石
        registry.register(ChunkerVanillaBlockType.NETHER_QUARTZ_ORE, dsl.simple("mcl_nether:quartz_ore"))
        registry.register(ChunkerVanillaBlockType.NETHER_GOLD_ORE, dsl.simple("mcl_nether:gold_ore"))
        registry.register(ChunkerVanillaBlockType.ANCIENT_DEBRIS, dsl.simple("mcl_nether:ancient_debris"))
        registry.register(ChunkerVanillaBlockType.NETHERITE_BLOCK, dsl.simple("mcl_nether:netheriteblock"))

        // 下界砖块系列
        registry.register(ChunkerVanillaBlockType.NETHER_BRICKS, dsl.simple("mcl_nether:nether_brick"))
        registry.register(ChunkerVanillaBlockType.RED_NETHER_BRICKS, dsl.simple("mcl_nether:red_nether_brick"))
        registry.register(ChunkerVanillaBlockType.CHISELED_NETHER_BRICKS, dsl.simple("mcl_nether:chiseled_nether_brick"))
        registry.register(ChunkerVanillaBlockType.CRACKED_NETHER_BRICKS, dsl.simple("mcl_nether:cracked_nether_brick"))
        registry.register(ChunkerVanillaBlockType.NETHER_WART_BLOCK, dsl.simple("mcl_nether:nether_wart_block"))

        // 石英方块系列
        registry.register(ChunkerVanillaBlockType.QUARTZ_BLOCK, dsl.simple("mcl_nether:quartz_block"))
        registry.register(ChunkerVanillaBlockType.CHISELED_QUARTZ_BLOCK, dsl.simple("mcl_nether:quartz_chiseled"))
        registry.register(ChunkerVanillaBlockType.QUARTZ_PILLAR, dsl.log("mcl_nether:quartz_pillar"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_QUARTZ, dsl.simple("mcl_nether:quartz_smooth"))
        registry.register(ChunkerVanillaBlockType.QUARTZ_BRICKS, dsl.simple("mcl_blackstone:quartz_brick"))

        // 玄武岩系列 (mcl_blackstone)
        registry.register(ChunkerVanillaBlockType.BASALT, dsl.log("mcl_blackstone:basalt"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BASALT, dsl.log("mcl_blackstone:basalt_polished"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_BASALT, dsl.simple("mcl_blackstone:basalt_smooth"))

        // 黑石系列 (mcl_blackstone)
        registry.register(ChunkerVanillaBlockType.BLACKSTONE, dsl.simple("mcl_blackstone:blackstone"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE, dsl.simple("mcl_blackstone:polished"))
        registry.register(ChunkerVanillaBlockType.CHISELED_POLISHED_BLACKSTONE, dsl.simple("mcl_blackstone:chiseled_polished"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BRICKS, dsl.simple("mcl_blackstone:polished_bricks"))
        registry.register(ChunkerVanillaBlockType.CRACKED_POLISHED_BLACKSTONE_BRICKS, dsl.simple("mcl_blackstone:polished_bricks_cracked"))
        registry.register(ChunkerVanillaBlockType.GILDED_BLACKSTONE, dsl.simple("mcl_blackstone:gilded"))

        // 下界植被 (地狱孢子)
        registry.register(ChunkerVanillaBlockType.NETHER_WART, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_3) ?: Age_3._0
            val nodeName = when (age) {
                Age_3._0 -> "mcl_nether:nether_wart_0"
                Age_3._1 -> "mcl_nether:nether_wart_1"
                Age_3._2 -> "mcl_nether:nether_wart_2"
                Age_3._3 -> "mcl_nether:nether_wart"
            }
            MclNode(nodeName)
        })

        // 下界岩浆 (mcl_nether:nether_lava)
        registry.register(ChunkerVanillaBlockType.LAVA, BlockMapper { id ->
            // 注意：Chunker 并不直接区分 Nether 还是 Overworld Lava，
            // 通常由管理器根据维度 Y 坐标或上下文决定。这里我们提供标准映射，
            // 真实的维度流体替换可以在 MclConverterManager 中做上下文增强。
            dsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing").map(id)
        })

        // ==========================================
        // 2. 末地核心方块 (mcl_end)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.END_STONE, dsl.simple("mcl_end:end_stone"))
        registry.register(ChunkerVanillaBlockType.END_STONE_BRICKS, dsl.simple("mcl_end:end_bricks"))
        registry.register(ChunkerVanillaBlockType.DRAGON_EGG, dsl.simple("mcl_end:dragon_egg"))

        // 紫珀块系列
        registry.register(ChunkerVanillaBlockType.PURPUR_BLOCK, dsl.simple("mcl_end:purpur_block"))
        registry.register(ChunkerVanillaBlockType.PURPUR_PILLAR, dsl.log("mcl_end:purpur_pillar"))

        // 末地烛 (End Rod)
        registry.register(ChunkerVanillaBlockType.END_ROD, BlockMapper { id ->
            val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
            val param2 = when (facing) {
                FacingDirection.DOWN -> 20
                FacingDirection.UP -> 0
                FacingDirection.NORTH -> 4
                FacingDirection.SOUTH -> 8
                FacingDirection.WEST -> 12
                FacingDirection.EAST -> 16
            }.toByte()
            MclNode("mcl_end:end_rod", param2 = param2)
        })

        // 歌颂植物 (Chorus)
        registry.register(ChunkerVanillaBlockType.CHORUS_PLANT, dsl.simple("mcl_end:chorus_plant"))
        registry.register(ChunkerVanillaBlockType.CHORUS_FLOWER, dsl.simple("mcl_end:chorus_flower"))

        // 末地传送门框架
        registry.register(ChunkerVanillaBlockType.END_PORTAL_FRAME, BlockMapper { id ->
            val eye = id.getState(VanillaBlockStates.EYE) ?: com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.Bool.FALSE
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal.NORTH
            // Mineclonia 中框架逻辑较为简单，param2 处理方向
            val param2 = when (facing) {
                com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal.NORTH -> 0
                com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal.EAST -> 1
                com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal.SOUTH -> 2
                com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.FacingDirectionHorizontal.WEST -> 3
            }.toByte()
            // 如果有眼，Mineclonia 会通过 NodeMeta 处理，节点名保持一致
            MclNode("mcl_portals:endframe", param2 = param2)
        })

        // ==========================================
        // 3. 附属方块 (下界与末地材质的楼梯/台阶/墙)
        // ==========================================
        
        // 下界材质
        registerDimensionSubsets("quartzblock", "quartzblock", "quartz", hasWall = false)
        registerDimensionSubsets("quartz_smooth", "quartz_smooth", "smooth_quartz", hasWall = false)
        registerDimensionSubsets("nether_brick", "nether_brick", "nether_brick", hasWall = true)
        registerDimensionSubsets("red_nether_brick", "red_nether_brick", "red_nether_brick", hasWall = true)
        registerDimensionSubsets("blackstone", "blackstone", "blackstone", hasWall = true)
        registerDimensionSubsets("polished_blackstone", "polished_blackstone", "polished_blackstone", hasWall = true)
        registerDimensionSubsets("polished_blackstone_brick", "polished_blackstone_brick", "polished_blackstone_brick", hasWall = true)

        // 末地材质
        registerDimensionSubsets("end_bricks", "end_bricks", "end_stone_brick", hasWall = true)
        registerDimensionSubsets("purpur_block", "purpur_block", "purpur", hasWall = false)
        
        // 普通过渡
        registry.register(ChunkerVanillaBlockType.NETHER_BRICK_FENCE, dsl.simple("mcl_fences:nether_brick_fence"))
    }

    private fun registerDimensionSubsets(mclBase: String, mclStairSlab: String, chunkerName: String, hasWall: Boolean) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        try {
            val stairType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_STAIRS")
            registry.register(stairType, dsl.stair("mcl_stairs:stair_$mclStairSlab"))
        } catch (_: Exception) {}

        try {
            val slabType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_SLAB")
            registry.register(slabType, dsl.slab(
                "mcl_stairs:slab_$mclStairSlab",
                "mcl_stairs:slab_${mclStairSlab}_top",
                "mcl_stairs:slab_${mclStairSlab}_double"
            ))
        } catch (_: Exception) {}

        if (hasWall) {
            try {
                val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_WALL")
                registry.register(wallType, dsl.simple("mcl_walls:$mclBase"))
            } catch (_: Exception) {}
        }
    }
}