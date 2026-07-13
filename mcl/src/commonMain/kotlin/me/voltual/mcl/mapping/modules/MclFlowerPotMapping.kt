package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl

object MclFlowerPotMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 1. 空花盆
        registry.register(ChunkerVanillaBlockType.FLOWER_POT, dsl.simple("mcl_flowerpots:flower_pot"))

        // 2. 基础花卉盆栽 (对应 mcl_flowers)
        registry.register(ChunkerVanillaBlockType.POTTED_DANDELION, dsl.simple("mcl_flowerpots:flower_pot_dandelion"))
        registry.register(ChunkerVanillaBlockType.POTTED_POPPY, dsl.simple("mcl_flowerpots:flower_pot_poppy"))
        registry.register(ChunkerVanillaBlockType.POTTED_BLUE_ORCHID, dsl.simple("mcl_flowerpots:flower_pot_blue_orchid"))
        registry.register(ChunkerVanillaBlockType.POTTED_ALLIUM, dsl.simple("mcl_flowerpots:flower_pot_allium"))
        registry.register(ChunkerVanillaBlockType.POTTED_AZURE_BLUET, dsl.simple("mcl_flowerpots:flower_pot_azure_bluet"))
        registry.register(ChunkerVanillaBlockType.POTTED_OXEYE_DAISY, dsl.simple("mcl_flowerpots:flower_pot_oxeye_daisy"))
        registry.register(ChunkerVanillaBlockType.POTTED_CORNFLOWER, dsl.simple("mcl_flowerpots:flower_pot_cornflower"))
        registry.register(ChunkerVanillaBlockType.POTTED_LILY_OF_THE_VALLEY, dsl.simple("mcl_flowerpots:flower_pot_lily_of_the_valley"))
        registry.register(ChunkerVanillaBlockType.POTTED_WITHER_ROSE, dsl.simple("mcl_flowerpots:flower_pot_wither_rose"))

        // 郁金香系列 (Minecraft 使用 RED_TULIP, MineClonia 使用 tulip_red)
        registry.register(ChunkerVanillaBlockType.POTTED_RED_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_red"))
        registry.register(ChunkerVanillaBlockType.POTTED_ORANGE_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_orange"))
        registry.register(ChunkerVanillaBlockType.POTTED_WHITE_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_white"))
        registry.register(ChunkerVanillaBlockType.POTTED_PINK_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_pink"))

        // 3. 树苗盆栽 (对应 mcl_trees)
        // MineClonia 树苗盆栽通常命名为 flower_pot_<木材名>
        registry.register(ChunkerVanillaBlockType.POTTED_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_SPRUCE_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_spruce"))
        registry.register(ChunkerVanillaBlockType.POTTED_BIRCH_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_birch"))
        registry.register(ChunkerVanillaBlockType.POTTED_JUNGLE_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_jungle"))
        registry.register(ChunkerVanillaBlockType.POTTED_ACACIA_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_acacia"))
        registry.register(ChunkerVanillaBlockType.POTTED_DARK_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_dark_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_PALE_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_pale_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_MANGROVE_PROPAGULE, dsl.simple("mcl_flowerpots:flower_pot_mangrove"))
        registry.register(ChunkerVanillaBlockType.POTTED_CHERRY_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_cherry_blossom"))

        // 4. 其他植物
        registry.register(ChunkerVanillaBlockType.POTTED_FERN, dsl.simple("mcl_flowerpots:flower_pot_fern"))
        registry.register(ChunkerVanillaBlockType.POTTED_DEAD_BUSH, dsl.simple("mcl_flowerpots:flower_pot_dead_bush"))
        registry.register(ChunkerVanillaBlockType.POTTED_CACTUS, dsl.simple("mcl_flowerpots:flower_pot_cactus"))
        registry.register(ChunkerVanillaBlockType.POTTED_BAMBOO, dsl.simple("mcl_flowerpots:flower_pot_bamboo"))
        
        // 蘑菇
        registry.register(ChunkerVanillaBlockType.POTTED_BROWN_MUSHROOM, dsl.simple("mcl_flowerpots:flower_pot_brown_mushroom"))
        registry.register(ChunkerVanillaBlockType.POTTED_RED_MUSHROOM, dsl.simple("mcl_flowerpots:flower_pot_red_mushroom"))

        // 繁茂洞穴与下界
        registry.register(ChunkerVanillaBlockType.POTTED_AZALEA_BUSH, dsl.simple("mcl_flowerpots:flower_pot_azalea"))
        registry.register(ChunkerVanillaBlockType.POTTED_FLOWERING_AZALEA_BUSH, dsl.simple("mcl_flowerpots:flower_pot_azalea_flowering"))
        registry.register(ChunkerVanillaBlockType.POTTED_CRIMSON_FUNGUS, dsl.simple("mcl_flowerpots:flower_pot_crimson_fungus"))
        registry.register(ChunkerVanillaBlockType.POTTED_WARPED_FUNGUS, dsl.simple("mcl_flowerpots:flower_pot_warped_fungus"))
        registry.register(ChunkerVanillaBlockType.POTTED_CRIMSON_ROOTS, dsl.simple("mcl_flowerpots:flower_pot_crimson_roots"))
        registry.register(ChunkerVanillaBlockType.POTTED_WARPED_ROOTS, dsl.simple("mcl_flowerpots:flower_pot_warped_roots"))
        
        // 苍白橡木特殊植物
        registry.register(ChunkerVanillaBlockType.POTTED_CLOSED_EYEBLOSSOM, dsl.simple("mcl_flowerpots:flower_pot_closed_eyeblossom"))
        registry.register(ChunkerVanillaBlockType.POTTED_OPEN_EYEBLOSSOM, dsl.simple("mcl_flowerpots:flower_pot_open_eyeblossom"))
    }
}