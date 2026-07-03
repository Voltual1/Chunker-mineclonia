package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType

object MclCoreMapping : MclMappingModule {
    override fun register() {
        MclMappingRegistry.apply {
            register(ChunkerVanillaBlockType.AIR, MclMappingDsl.simple("air"))
            register(ChunkerVanillaBlockType.STONE, MclMappingDsl.simple("mcl_core:stone"))
            register(ChunkerVanillaBlockType.GRANITE, MclMappingDsl.simple("mcl_core:granite"))
            register(ChunkerVanillaBlockType.POLISHED_GRANITE, MclMappingDsl.simple("mcl_core:granite_smooth"))
            register(ChunkerVanillaBlockType.DIORITE, MclMappingDsl.simple("mcl_core:diorite"))
            register(ChunkerVanillaBlockType.POLISHED_DIORITE, MclMappingDsl.simple("mcl_core:diorite_smooth"))
            register(ChunkerVanillaBlockType.ANDESITE, MclMappingDsl.simple("mcl_core:andesite"))
            register(ChunkerVanillaBlockType.POLISHED_ANDESITE, MclMappingDsl.simple("mcl_core:andesite_smooth"))
            register(ChunkerVanillaBlockType.GRASS_BLOCK, MclMappingDsl.simple("mcl_core:dirt_with_grass"))
            register(ChunkerVanillaBlockType.DIRT, MclMappingDsl.simple("mcl_core:dirt"))
            register(ChunkerVanillaBlockType.COARSE_DIRT, MclMappingDsl.simple("mcl_core:coarse_dirt"))
            register(ChunkerVanillaBlockType.PODZOL, MclMappingDsl.simple("mcl_core:podzol"))
            register(ChunkerVanillaBlockType.COBBLESTONE, MclMappingDsl.simple("mcl_core:cobble"))
            register(ChunkerVanillaBlockType.BEDROCK, MclMappingDsl.simple("mcl_core:bedrock"))
            register(ChunkerVanillaBlockType.SAND, MclMappingDsl.simple("mcl_core:sand"))
            register(ChunkerVanillaBlockType.RED_SAND, MclMappingDsl.simple("mcl_core:redsand"))
            register(ChunkerVanillaBlockType.GRAVEL, MclMappingDsl.simple("mcl_core:gravel"))
            register(ChunkerVanillaBlockType.CLAY, MclMappingDsl.simple("mcl_core:clay"))
            register(ChunkerVanillaBlockType.BRICKS, MclMappingDsl.simple("mcl_core:brick_block"))

            register(ChunkerVanillaBlockType.WATER, MclMappingDsl.liquid("mcl_core:water_source", "mcl_core:water_flowing"))
            register(ChunkerVanillaBlockType.LAVA, MclMappingDsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing"))
        }
    }
}