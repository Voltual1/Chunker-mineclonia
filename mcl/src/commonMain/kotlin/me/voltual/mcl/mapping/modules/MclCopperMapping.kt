package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclCopperMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        registry.register(ChunkerVanillaBlockType.COPPER_ORE, dsl.simple("mcl_copper:stone_with_copper"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, dsl.simple("mcl_deepslate:deepslate_with_copper"))
        registry.register(ChunkerVanillaBlockType.RAW_COPPER_BLOCK, dsl.simple("mcl_copper:block_raw"))
        registry.register(ChunkerVanillaBlockType.COPPER_BLOCK, dsl.simple("mcl_copper:block"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BLOCK, dsl.simple("mcl_copper:block_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized_preserved"))

        registerCutCopperSet("", "copper", "")
        registerCutCopperSet("EXPOSED_", "copper_exposed", "_exposed")
        registerCutCopperSet("WEATHERED_", "copper_weathered", "_weathered")
        registerCutCopperSet("OXIDIZED_", "copper_oxidized", "_oxidized")
        registerCutCopperSet("WAXED_", "copper", "", true)
        registerCutCopperSet("WAXED_EXPOSED_", "copper_exposed", "_exposed", true)
        registerCutCopperSet("WAXED_WEATHERED_", "copper_weathered", "_weathered", true)
        registerCutCopperSet("WAXED_OXIDIZED_", "copper_oxidized", "_oxidized", true)

        registry.register(ChunkerVanillaBlockType.CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled"))
        registry.register(ChunkerVanillaBlockType.WAXED_CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_GRATE, dsl.simple("mcl_copper:block_grate"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_GRATE, dsl.simple("mcl_copper:block_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_BULB, dsl.copperBulb("mcl_copper:bulb"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_DOOR, dsl.door("mcl_copper:door"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_DOOR, dsl.door("mcl_copper:door_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_BARS, dsl.simple("mcl_panes:copper_bar"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized"))
        registry.register(ChunkerVanillaBlockType.WAXED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized_preserved"))

        // ==========================================
        // 【关键修复】：将落地铜火把改为 floorMounted
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_TORCH, dsl.floorMounted("mcl_torches:copper_torch"))
        registry.register(ChunkerVanillaBlockType.COPPER_WALL_TORCH, dsl.wallTorch("mcl_torches:copper_torch_wall", "mcl_torches:copper_torch_wall"))
    }

    private fun registerCutCopperSet(chunkerPrefix: String, stairSlabBase: String, mclBlockBase: String, isWaxed: Boolean = false) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val waxedSuffix = if (isWaxed) "_preserved" else ""

        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER"), dsl.simple("mcl_copper:block${mclBlockBase}_cut$waxedSuffix"))
        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER_STAIRS"), dsl.stair("mcl_stairs:stair_${stairSlabBase}_cut$waxedSuffix"))
        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER_SLAB"), dsl.slab(
            "mcl_stairs:slab_${stairSlabBase}_cut$waxedSuffix",
            "mcl_stairs:slab_${stairSlabBase}_cut_top$waxedSuffix",
            "mcl_stairs:slab_${stairSlabBase}_cut_double$waxedSuffix"
        ))
    }

    private inline fun <reified T : Enum<T>> enumValueOf(name: String): T = 
        java.lang.Enum.valueOf(T::class.java, name)
}