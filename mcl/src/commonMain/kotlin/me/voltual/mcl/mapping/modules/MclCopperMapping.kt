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
        // 2. 铜块变体 (Copper Blocks) - 严格匹配 Chunker 源码命名
        // ==========================================
        // 正常铜块
        registry.register(ChunkerVanillaBlockType.COPPER_BLOCK, dsl.simple("mcl_copper:block"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized"))

        // 涂蜡铜块
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BLOCK, dsl.simple("mcl_copper:block_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER, dsl.simple("mcl_copper:block_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER, dsl.simple("mcl_copper:block_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER, dsl.simple("mcl_copper:block_oxidized_preserved"))

        // ==========================================
        // 3. 切制铜块 (Cut Copper Blocks)
        // ==========================================
        // 正常切制
        registry.register(ChunkerVanillaBlockType.CUT_COPPER, dsl.simple("mcl_copper:block_cut"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_CUT_COPPER, dsl.simple("mcl_copper:block_exposed_cut"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_CUT_COPPER, dsl.simple("mcl_copper:block_weathered_cut"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_CUT_COPPER, dsl.simple("mcl_copper:block_oxidized_cut"))

        // 涂蜡切制
        registry.register(ChunkerVanillaBlockType.WAXED_CUT_COPPER, dsl.simple("mcl_copper:block_cut_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_CUT_COPPER, dsl.simple("mcl_copper:block_exposed_cut_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_CUT_COPPER, dsl.simple("mcl_copper:block_weathered_cut_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_CUT_COPPER, dsl.simple("mcl_copper:block_oxidized_cut_preserved"))

        // ==========================================
        // 4. 錾刻铜块 (Chiseled Copper Blocks)
        // ==========================================
        // 正常錾刻
        registry.register(ChunkerVanillaBlockType.CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled"))

        // 涂蜡錾刻
        registry.register(ChunkerVanillaBlockType.WAXED_CHISELED_COPPER, dsl.simple("mcl_copper:block_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_CHISELED_COPPER, dsl.simple("mcl_copper:block_exposed_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_CHISELED_COPPER, dsl.simple("mcl_copper:block_weathered_chiseled_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_CHISELED_COPPER, dsl.simple("mcl_copper:block_oxidized_chiseled_preserved"))

        // ==========================================
        // 5. 铜格栅 (Copper Grates)
        // ==========================================
        // 正常格栅
        registry.register(ChunkerVanillaBlockType.COPPER_GRATE, dsl.simple("mcl_copper:block_grate"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate"))

        // 涂蜡格栅
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_GRATE, dsl.simple("mcl_copper:block_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_GRATE, dsl.simple("mcl_copper:block_exposed_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_GRATE, dsl.simple("mcl_copper:block_weathered_grate_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_GRATE, dsl.simple("mcl_copper:block_oxidized_grate_preserved"))

        // ==========================================
        // 6. 铜灯泡 (Copper Bulbs)
        // ==========================================
        // 正常灯泡
        registry.register(ChunkerVanillaBlockType.COPPER_BULB, dsl.copperBulb("mcl_copper:bulb"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized"))

        // 涂蜡灯泡
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BULB, dsl.copperBulb("mcl_copper:bulb_oxidized_preserved"))

        // ==========================================
        // 7. 铜门 (Copper Doors)
        // ==========================================
        // 正常门
        registry.register(ChunkerVanillaBlockType.COPPER_DOOR, dsl.door("mcl_copper:door"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized"))

        // 涂蜡门
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_DOOR, dsl.door("mcl_copper:door_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_DOOR, dsl.door("mcl_copper:door_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_DOOR, dsl.door("mcl_copper:door_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_DOOR, dsl.door("mcl_copper:door_oxidized_preserved"))

        // ==========================================
        // 8. 铜活板门 (Copper Trapdoors)
        // ==========================================
        // 正常活板门
        registry.register(ChunkerVanillaBlockType.COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized"))

        // 涂蜡活板门
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_TRAPDOOR, dsl.trapdoor("mcl_copper:trapdoor_oxidized_preserved"))

        // ==========================================
        // 9. 铜灯笼 (Copper Lanterns)
        // ==========================================
        // 正常灯笼
        registry.register(ChunkerVanillaBlockType.COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_oxidized"))

        // 涂蜡灯笼
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_LANTERN, dsl.lantern("mcl_lanterns:copper_lantern_oxidized_preserved"))

        // ==========================================
        // 10. 铜锁链 (Copper Chains)
        // ==========================================
        // 正常锁链
        registry.register(ChunkerVanillaBlockType.COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_oxidized"))

        // 涂蜡锁链
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_CHAIN, dsl.log("mcl_lanterns:copper_chain_oxidized_preserved"))

        // ==========================================
        // 11. 铜栏杆 (Copper Bars)
        // ==========================================
        // 正常栏杆
        registry.register(ChunkerVanillaBlockType.COPPER_BARS, dsl.simple("mcl_panes:copper_bar"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized"))

        // 涂蜡栏杆
        registry.register(ChunkerVanillaBlockType.WAXED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BARS, dsl.simple("mcl_panes:copper_bar_oxidized_preserved"))

        // ==========================================
        // 12. 铜避雷针 (Lightning Rods)
        // ==========================================
        // 正常避雷针
        registry.register(ChunkerVanillaBlockType.LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod"))
        registry.register(ChunkerVanillaBlockType.EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed"))
        registry.register(ChunkerVanillaBlockType.WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered"))
        registry.register(ChunkerVanillaBlockType.OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized"))

        // 涂蜡避雷针
        registry.register(ChunkerVanillaBlockType.WAXED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_EXPOSED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_exposed_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_WEATHERED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_weathered_preserved"))
        registry.register(ChunkerVanillaBlockType.WAXED_OXIDIZED_LIGHTNING_ROD, dsl.directional("mcl_lightning_rods:rod_oxidized_preserved"))

        // ==========================================
        // 13. 铜火把 (Copper Torches)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COPPER_TORCH, dsl.simple("mcl_torches:copper_torch"))
        registry.register(ChunkerVanillaBlockType.COPPER_WALL_TORCH, dsl.wallTorch("mcl_torches:copper_torch_wall", "mcl_torches:copper_torch_wall"))
    }
}