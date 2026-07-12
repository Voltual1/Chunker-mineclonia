package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclCopperMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // ==========================================
        // 1. 基础铜矿与原铜块
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_ORE, dsl.simple("mcl_copper:stone_with_copper"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, dsl.simple("mcl_deepslate:deepslate_with_copper"))
        registry.register(ChunkerVanillaBlockType.RAW_COPPER_BLOCK, dsl.simple("mcl_copper:block_raw"))

        // ==========================================
        // 2. 铜块变体 (Copper Blocks)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_BLOCK, dsl.simple("mcl_copper:block"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BLOCK, dsl.simple("mcl_copper:block_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized_preserved"))

        // ==========================================
        // 3. 切制铜块、楼梯、台阶 (Cut Copper Series) - 修复后缀顺序
        // ==========================================
        // --- 正常版 ---
        registerCutCopperSet("", "copper")
        registerCutCopperSet("EXPOSED_", "copper_exposed")
        registerCutCopperSet("WEATHERED_", "copper_weathered")
        registerCutCopperSet("OXIDIZED_", "copper_oxidized")

        // --- 涂蜡版 ---
        registerCutCopperSet("WAXED_", "copper", true)
        registerCutCopperSet("WAXED_EXPOSED_", "copper_exposed", true)
        registerCutCopperSet("WAXED_WEATHERED_", "copper_weathered", true)
        registerCutCopperSet("WAXED_OXIDIZED_", "copper_oxidized", true)

        // ==========================================
        // 4. 錾刻铜块 (Chiseled Copper Blocks)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled"))

        registry.register(ChunkerVanillaBlockType.WAXED_CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled_preserved"))

        // ==========================================
        // 5. 铜格栅 (Copper Grates)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_GRATE, dsl.simple("mcl_copper:block_grate"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_GRATE, dsl.simple("mcl_copper:block_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate_preserved"))

        // ==========================================
        // 6. 铜灯泡 (Copper Bulbs)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_BULB, dsl.copperBulb("mcl_copper:bulb"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized_preserved"))

        // ==========================================
        // 7. 铜门 (Copper Doors)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_DOOR, dsl.door("mcl_copper:door"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_DOOR, dsl.door("mcl_copper:door_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized_preserved"))

        // ==========================================
        // 8. 铜活板门 (Copper Trapdoors)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized_preserved"))

        // ==========================================
        // 9. 铜灯笼与锁链 (Lanterns & Chains)
        // ==========================================
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

        // ==========================================
        // 10. 铜栏杆 (Copper Bars)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_BARS, dsl.simple("mcl_panes:copper_bar"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized_preserved"))

        // ==========================================
        // 11. 其他铜制品 (Rods & Torches)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized"))

        registry.register(ChunkerVanillaBlockType.WAXED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized_preserved"))

        registry.register(ChunkerVanillaBlockType.COPPER_TORCH, dsl.simple("mcl_torches:copper_torch"))
        registry.register(ChunkerVanillaBlockType.COPPER_WALL_TORCH, dsl.wallTorch("mcl_torches:copper_torch_wall", "mcl_torches:copper_torch_wall"))
        
        //磁石
        registry.register(ChunkerVanillaBlockType.LODESTONE, dsl.simple("mcl_compass:lodestone"))
    }

    /**
     * 辅助函数：注册切制铜及其楼梯、台阶。
     * 精准修复：针对 Preserved 变体，_preserved 后缀必须在绝对末尾。
     */
    private fun registerCutCopperSet(chunkerPrefix: String, mclBase: String, isWaxed: Boolean = false) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val waxedSuffix = if (isWaxed) "_preserved" else ""

        // 方块
        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER"), dsl.simple("mcl_copper:block_${mclBase}_cut$waxedSuffix"))

        // 楼梯 (格式: mcl_stairs:stair_copper_xxx_cut[_preserved])
        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER_STAIRS"), dsl.stair("mcl_stairs:stair_${mclBase}_cut$waxedSuffix"))

        // 台阶 (格式: mcl_stairs:slab_copper_xxx_cut[_top/double][_preserved])
        // 关键修复：_preserved 必须放在最后
        registry.register(enumValueOf("${chunkerPrefix}CUT_COPPER_SLAB"), dsl.slab(
            "mcl_stairs:slab_${mclBase}_cut$waxedSuffix",
            "mcl_stairs:slab_${mclBase}_cut_top$waxedSuffix",
            "mcl_stairs:slab_${mclBase}_cut_double$waxedSuffix"
        ))
    }

    private inline fun <reified T : Enum<T>> enumValueOf(name: String): T = 
        java.lang.Enum.valueOf(T::class.java, name)
}