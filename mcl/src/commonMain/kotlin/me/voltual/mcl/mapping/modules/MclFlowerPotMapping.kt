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

        // 2. 基础花卉盆栽 (mcl_flowers)
        registry.register(ChunkerVanillaBlockType.POTTED_DANDELION, dsl.simple("mcl_flowerpots:flower_pot_dandelion"))
        registry.register(ChunkerVanillaBlockType.POTTED_POPPY, dsl.simple("mcl_flowerpots:flower_pot_poppy"))
        registry.register(ChunkerVanillaBlockType.POTTED_BLUE_ORCHID, dsl.simple("mcl_flowerpots:flower_pot_blue_orchid"))
        registry.register(ChunkerVanillaBlockType.POTTED_ALLIUM, dsl.simple("mcl_flowerpots:flower_pot_allium"))
        registry.register(ChunkerVanillaBlockType.POTTED_AZURE_BLUET, dsl.simple("mcl_flowerpots:flower_pot_azure_bluet"))
        registry.register(ChunkerVanillaBlockType.POTTED_OXEYE_DAISY, dsl.simple("mcl_flowerpots:flower_pot_oxeye_daisy"))
        registry.register(ChunkerVanillaBlockType.POTTED_CORNFLOWER, dsl.simple("mcl_flowerpots:flower_pot_cornflower"))
        registry.register(ChunkerVanillaBlockType.POTTED_LILY_OF_THE_VALLEY, dsl.simple("mcl_flowerpots:flower_pot_lily_of_the_valley"))
        registry.register(ChunkerVanillaBlockType.POTTED_WITHER_ROSE, dsl.simple("mcl_flowerpots:flower_pot_wither_rose"))

        // 郁金香系列
        registry.register(ChunkerVanillaBlockType.POTTED_RED_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_red"))
        registry.register(ChunkerVanillaBlockType.POTTED_ORANGE_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_orange"))
        registry.register(ChunkerVanillaBlockType.POTTED_WHITE_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_white"))
        registry.register(ChunkerVanillaBlockType.POTTED_PINK_TULIP, dsl.simple("mcl_flowerpots:flower_pot_tulip_pink"))

        // 3. 树苗盆栽 (修正 Mangrove 命名)
        registry.register(ChunkerVanillaBlockType.POTTED_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_SPRUCE_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_spruce"))
        registry.register(ChunkerVanillaBlockType.POTTED_BIRCH_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_birch"))
        registry.register(ChunkerVanillaBlockType.POTTED_JUNGLE_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_jungle"))
        registry.register(ChunkerVanillaBlockType.POTTED_ACACIA_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_acacia"))
        registry.register(ChunkerVanillaBlockType.POTTED_DARK_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_dark_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_PALE_OAK_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_pale_oak"))
        registry.register(ChunkerVanillaBlockType.POTTED_CHERRY_SAPLING, dsl.simple("mcl_flowerpots:flower_pot_sapling_cherry_blossom"))
        
        // 特殊：红树林盆栽在 MineClonia 中称为 propagule
        registry.register(ChunkerVanillaBlockType.POTTED_MANGROVE_PROPAGULE, dsl.simple("mcl_flowerpots:flower_pot_propagule"))

        // 4. 蘑菇与真菌
        registry.register(ChunkerVanillaBlockType.POTTED_BROWN_MUSHROOM, dsl.simple("mcl_flowerpots:flower_pot_mushroom_brown"))
        registry.register(ChunkerVanillaBlockType.POTTED_RED_MUSHROOM, dsl.simple("mcl_flowerpots:flower_pot_mushroom_red"))
        registry.register(ChunkerVanillaBlockType.POTTED_CRIMSON_FUNGUS, dsl.simple("mcl_flowerpots:flower_pot_crimson_fungus"))
        registry.register(ChunkerVanillaBlockType.POTTED_WARPED_FUNGUS, dsl.simple("mcl_flowerpots:flower_pot_warped_fungus"))

        // 5. 其他装饰植物 (修正 Dead Bush 命名)
        registry.register(ChunkerVanillaBlockType.POTTED_DEAD_BUSH, dsl.simple("mcl_flowerpots:flower_pot_deadbush"))
        registry.register(ChunkerVanillaBlockType.POTTED_FERN, dsl.simple("mcl_flowerpots:flower_pot_fern"))
        registry.register(ChunkerVanillaBlockType.POTTED_CACTUS, dsl.simple("mcl_flowerpots:flower_pot_cactus"))
        registry.register(ChunkerVanillaBlockType.POTTED_BAMBOO, dsl.simple("mcl_flowerpots:flower_pot_bamboo"))
        registry.register(ChunkerVanillaBlockType.POTTED_AZALEA_BUSH, dsl.simple("mcl_flowerpots:flower_pot_azalea"))
        registry.register(ChunkerVanillaBlockType.POTTED_FLOWERING_AZALEA_BUSH, dsl.simple("mcl_flowerpots:flower_pot_azalea_flowering"))
        
        // 苍白橡木眼花
        registry.register(ChunkerVanillaBlockType.POTTED_CLOSED_EYEBLOSSOM, dsl.simple("mcl_flowerpots:flower_pot_closed_eyeblossom"))
        registry.register(ChunkerVanillaBlockType.POTTED_OPEN_EYEBLOSSOM, dsl.simple("mcl_flowerpots:flower_pot_open_eyeblossom"))
    }
}