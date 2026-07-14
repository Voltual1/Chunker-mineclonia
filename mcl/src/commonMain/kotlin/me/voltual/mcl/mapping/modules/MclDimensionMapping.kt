package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.core.MclNode

object MclDimensionMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        registry.register(ChunkerVanillaBlockType.NETHERRACK, dsl.simple("mcl_nether:netherrack"))
        registry.register(ChunkerVanillaBlockType.GLOWSTONE, dsl.simple("mcl_nether:glowstone"))
        registry.register(ChunkerVanillaBlockType.MAGMA_BLOCK, dsl.simple("mcl_nether:magma"))
        registry.register(ChunkerVanillaBlockType.SOUL_SAND, dsl.simple("mcl_nether:soul_sand"))
        registry.register(ChunkerVanillaBlockType.SOUL_SOIL, dsl.simple("mcl_blackstone:soul_soil"))
        registry.register(ChunkerVanillaBlockType.NETHER_QUARTZ_ORE, dsl.simple("mcl_nether:quartz_ore"))
        registry.register(ChunkerVanillaBlockType.NETHER_GOLD_ORE, dsl.simple("mcl_blackstone:nether_gold"))
        registry.register(ChunkerVanillaBlockType.ANCIENT_DEBRIS, dsl.simple("mcl_nether:ancient_debris"))
        registry.register(ChunkerVanillaBlockType.NETHERITE_BLOCK, dsl.simple("mcl_nether:netheriteblock"))
        registry.register(ChunkerVanillaBlockType.NETHER_BRICKS, dsl.simple("mcl_nether:nether_brick"))
        registry.register(ChunkerVanillaBlockType.RED_NETHER_BRICKS, dsl.simple("mcl_nether:red_nether_brick"))
        registry.register(ChunkerVanillaBlockType.CHISELED_NETHER_BRICKS, dsl.simple("mcl_nether:chiseled_nether_brick"))
        registry.register(ChunkerVanillaBlockType.CRACKED_NETHER_BRICKS, dsl.simple("mcl_nether:cracked_nether_brick"))
        registry.register(ChunkerVanillaBlockType.NETHER_WART_BLOCK, dsl.simple("mcl_nether:nether_wart_block"))
        registry.register(ChunkerVanillaBlockType.QUARTZ_BLOCK, dsl.simple("mcl_nether:quartz_block"))
        registry.register(ChunkerVanillaBlockType.CHISELED_QUARTZ_BLOCK, dsl.simple("mcl_nether:quartz_chiseled"))
        registry.register(ChunkerVanillaBlockType.QUARTZ_PILLAR, dsl.log("mcl_nether:quartz_pillar"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_QUARTZ, dsl.simple("mcl_nether:quartz_smooth"))
        registry.register(ChunkerVanillaBlockType.QUARTZ_BRICKS, dsl.simple("mcl_blackstone:quartz_brick"))
        registry.register(ChunkerVanillaBlockType.BASALT, dsl.log("mcl_blackstone:basalt"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BASALT, dsl.log("mcl_blackstone:basalt_polished"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_BASALT, dsl.simple("mcl_blackstone:basalt_smooth"))
        registry.register(ChunkerVanillaBlockType.BLACKSTONE, dsl.simple("mcl_blackstone:blackstone"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE, dsl.simple("mcl_blackstone:blackstone_polished"))
        registry.register(ChunkerVanillaBlockType.CHISELED_POLISHED_BLACKSTONE, dsl.simple("mcl_blackstone:blackstone_chiseled_polished"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BRICKS, dsl.simple("mcl_blackstone:blackstone_brick_polished"))
        registry.register(ChunkerVanillaBlockType.CRACKED_POLISHED_BLACKSTONE_BRICKS, dsl.simple("mcl_blackstone:blackstone_brick_polished_cracked"))
        registry.register(ChunkerVanillaBlockType.GILDED_BLACKSTONE, dsl.simple("mcl_blackstone:blackstone_gilded"))

        registry.register(ChunkerVanillaBlockType.BLACKSTONE_STAIRS, dsl.stair("mcl_stairs:stair_blackstone"))
        registry.register(ChunkerVanillaBlockType.BLACKSTONE_SLAB, dsl.slab("mcl_stairs:slab_blackstone", "mcl_stairs:slab_blackstone_top", "mcl_stairs:slab_blackstone_double"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_STAIRS, dsl.stair("mcl_stairs:stair_blackstone_polished"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_SLAB, dsl.slab("mcl_stairs:slab_blackstone_polished", "mcl_stairs:slab_blackstone_polished_top", "mcl_stairs:slab_blackstone_polished_double"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BRICK_STAIRS, dsl.stair("mcl_stairs:stair_blackstone_brick_polished"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BRICK_SLAB, dsl.slab("mcl_stairs:slab_blackstone_brick_polished", "mcl_stairs:slab_blackstone_brick_polished_top", "mcl_stairs:slab_blackstone_brick_polished_double"))
        registry.register(ChunkerVanillaBlockType.BLACKSTONE_WALL, dsl.simple("mcl_blackstone:wall"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_WALL, dsl.simple("mcl_blackstone:polishedwall"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BRICK_WALL, dsl.simple("mcl_blackstone:polishedbrickwall"))

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
        registry.register(ChunkerVanillaBlockType.CRIMSON_NYLIUM, dsl.simple("mcl_crimson:crimson_nylium"))
        registry.register(ChunkerVanillaBlockType.WARPED_NYLIUM, dsl.simple("mcl_crimson:warped_nylium"))
        registry.register(ChunkerVanillaBlockType.WARPED_WART_BLOCK, dsl.simple("mcl_crimson:warped_wart_block"))
        registry.register(ChunkerVanillaBlockType.LAVA, BlockMapper { id -> dsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing").map(id) })

        registry.register(ChunkerVanillaBlockType.END_STONE, dsl.simple("mcl_end:end_stone"))
        registry.register(ChunkerVanillaBlockType.END_STONE_BRICKS, dsl.simple("mcl_end:end_bricks"))
        registry.register(ChunkerVanillaBlockType.DRAGON_EGG, dsl.simple("mcl_end:dragon_egg"))
        registry.register(ChunkerVanillaBlockType.PURPUR_BLOCK, dsl.simple("mcl_end:purpur_block"))
        registry.register(ChunkerVanillaBlockType.PURPUR_PILLAR, dsl.log("mcl_end:purpur_pillar"))

        // ==========================================
        // 【核心修复】：末地烛 (End Rod) 彻底修正方向
        // ==========================================
        registry.register(ChunkerVanillaBlockType.END_ROD, BlockMapper { id ->
            val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
            val param2 = when (facing) {
                FacingDirection.DOWN -> 20.toByte()
                FacingDirection.UP -> 0.toByte()
                FacingDirection.NORTH -> 4.toByte() // 指向 +Z (北)
                FacingDirection.SOUTH -> 8.toByte() // 指向 -Z (南)
                FacingDirection.EAST -> 12.toByte() // 修正：指向 +X (东)
                FacingDirection.WEST -> 16.toByte() // 修正：指向 -X (西)
            }
            MclNode("mcl_end:end_rod", param2 = param2)
        })

        registry.register(ChunkerVanillaBlockType.CHORUS_PLANT, dsl.simple("mcl_end:chorus_plant"))
        registry.register(ChunkerVanillaBlockType.CHORUS_FLOWER, dsl.simple("mcl_end:chorus_flower"))

        registry.register(ChunkerVanillaBlockType.END_PORTAL_FRAME, BlockMapper { id ->
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            val param2 = when (facing) {
                FacingDirectionHorizontal.NORTH -> 0
                FacingDirectionHorizontal.EAST -> 1
                FacingDirectionHorizontal.SOUTH -> 2
                FacingDirectionHorizontal.WEST -> 3
            }.toByte()
            MclNode("mcl_portals:end_portal_frame", param2 = param2)
        })

        registerDimensionSubsets("quartzblock", "quartzblock", "quartz", hasWall = false)
        registerDimensionSubsets("quartz_smooth", "quartz_smooth", "smooth_quartz", hasWall = false)
        registerDimensionSubsets("nether_brick", "nether_brick", "nether_brick", hasWall = false)
        registry.register(ChunkerVanillaBlockType.NETHER_BRICK_WALL, dsl.simple("mcl_walls:netherbrick"))
        registerDimensionSubsets("red_nether_brick", "red_nether_brick", "red_nether_brick", hasWall = false)
        registry.register(ChunkerVanillaBlockType.RED_NETHER_BRICK_WALL, dsl.simple("mcl_walls:rednetherbrick"))
        registerDimensionSubsets("end_bricks", "end_bricks", "end_stone_brick", hasWall = false)
        registry.register(ChunkerVanillaBlockType.END_STONE_BRICK_WALL, dsl.simple("mcl_walls:endbricks"))
        registerDimensionSubsets("purpur_block", "purpur_block", "purpur", hasWall = false)
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
            registry.register(slabType, dsl.slab("mcl_stairs:slab_$mclStairSlab", "mcl_stairs:slab_${mclStairSlab}_top", "mcl_stairs:slab_${mclStairSlab}_double"))
        } catch (_: Exception) {}
        if (hasWall) {
            try {
                val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_WALL")
                registry.register(wallType, dsl.simple("mcl_walls:$mclBase"))
            } catch (_: Exception) {}
        }
    }
}