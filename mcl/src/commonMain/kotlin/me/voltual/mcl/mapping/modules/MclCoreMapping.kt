package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl

object MclCoreMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // ==========================================
        // 1. 基础自然与通用岩石 (mcl_core)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.AIR, dsl.simple("air"))
        registry.register(ChunkerVanillaBlockType.STONE, dsl.simple("mcl_core:stone"))
        registry.register(ChunkerVanillaBlockType.COBBLESTONE, dsl.simple("mcl_core:cobble"))
        registry.register(ChunkerVanillaBlockType.MOSSY_COBBLESTONE, dsl.simple("mcl_core:mossycobble"))
        registry.register(ChunkerVanillaBlockType.STONE_BRICKS, dsl.simple("mcl_core:stonebrick"))
        registry.register(ChunkerVanillaBlockType.CHISELED_STONE_BRICKS, dsl.simple("mcl_core:stonebrickcarved"))
        registry.register(ChunkerVanillaBlockType.CRACKED_STONE_BRICKS, dsl.simple("mcl_core:stonebrickcracked"))
        registry.register(ChunkerVanillaBlockType.MOSSY_STONE_BRICKS, dsl.simple("mcl_core:stonebrickmossy"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_STONE, dsl.simple("mcl_core:stone_smooth"))
        
        registry.register(ChunkerVanillaBlockType.GRANITE, dsl.simple("mcl_core:granite"))
        registry.register(ChunkerVanillaBlockType.POLISHED_GRANITE, dsl.simple("mcl_core:granite_smooth"))
        registry.register(ChunkerVanillaBlockType.DIORITE, dsl.simple("mcl_core:diorite"))
        registry.register(ChunkerVanillaBlockType.POLISHED_DIORITE, dsl.simple("mcl_core:diorite_smooth"))
        registry.register(ChunkerVanillaBlockType.ANDESITE, dsl.simple("mcl_core:andesite"))
        registry.register(ChunkerVanillaBlockType.POLISHED_ANDESITE, dsl.simple("mcl_core:andesite_smooth"))
        
        registry.register(ChunkerVanillaBlockType.BEDROCK, dsl.simple("mcl_core:bedrock"))
        registry.register(ChunkerVanillaBlockType.GRAVEL, dsl.simple("mcl_core:gravel"))
        registry.register(ChunkerVanillaBlockType.CLAY, dsl.simple("mcl_core:clay"))
        registry.register(ChunkerVanillaBlockType.BRICKS, dsl.simple("mcl_core:brick_block"))

        // 沙子与沙石系列
        registry.register(ChunkerVanillaBlockType.SAND, dsl.simple("mcl_core:sand"))
        registry.register(ChunkerVanillaBlockType.SANDSTONE, dsl.simple("mcl_core:sandstone"))
        registry.register(ChunkerVanillaBlockType.CUT_SANDSTONE, dsl.simple("mcl_core:sandstonesmooth"))
        registry.register(ChunkerVanillaBlockType.CHISELED_SANDSTONE, dsl.simple("mcl_core:sandstonecarved"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_SANDSTONE, dsl.simple("mcl_core:sandstonesmooth2"))

        registry.register(ChunkerVanillaBlockType.RED_SAND, dsl.simple("mcl_core:redsand"))
        registry.register(ChunkerVanillaBlockType.RED_SANDSTONE, dsl.simple("mcl_core:redsandstone"))
        registry.register(ChunkerVanillaBlockType.CUT_RED_SANDSTONE, dsl.simple("mcl_core:redsandstonesmooth"))
        registry.register(ChunkerVanillaBlockType.CHISELED_RED_SANDSTONE, dsl.simple("mcl_core:redsandstonecarved"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_RED_SANDSTONE, dsl.simple("mcl_core:redsandstonesmooth2"))

        // 土壤与生态
        registry.register(ChunkerVanillaBlockType.DIRT, dsl.simple("mcl_core:dirt"))
        registry.register(ChunkerVanillaBlockType.COARSE_DIRT, dsl.simple("mcl_core:coarse_dirt"))
        registry.register(ChunkerVanillaBlockType.PODZOL, dsl.simple("mcl_core:podzol"))
        registry.register(ChunkerVanillaBlockType.MYCELIUM, dsl.simple("mcl_core:mycelium"))
        registry.register(ChunkerVanillaBlockType.DIRT_PATH, dsl.simple("mcl_core:grass_path"))
        registry.register(ChunkerVanillaBlockType.GRASS_BLOCK, dsl.simple("mcl_core:dirt_with_grass"))

        // 煤炭块与黑曜石
        registry.register(ChunkerVanillaBlockType.COAL_BLOCK, dsl.simple("mcl_core:coalblock"))
        registry.register(ChunkerVanillaBlockType.OBSIDIAN, dsl.simple("mcl_core:obsidian"))
        registry.register(ChunkerVanillaBlockType.CRYING_OBSIDIAN, dsl.simple("mcl_core:crying_obsidian"))

        // ==========================================
        // 2. 液体与冰雪 (mcl_core / mcl_nether / mcl_liquids)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.WATER, dsl.liquid("mcl_core:water_source", "mcl_core:water_flowing"))
        registry.register(ChunkerVanillaBlockType.LAVA, dsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing"))
        
        registry.register(ChunkerVanillaBlockType.ICE, dsl.simple("mcl_core:ice"))
        registry.register(ChunkerVanillaBlockType.PACKED_ICE, dsl.simple("mcl_core:packed_ice"))
        registry.register(ChunkerVanillaBlockType.BLUE_ICE, dsl.simple("mcl_core:blue_ice"))
        registry.register(ChunkerVanillaBlockType.SNOW_BLOCK, dsl.simple("mcl_core:snowblock"))
        registry.register(ChunkerVanillaBlockType.SNOW, dsl.simple("mcl_core:snow"))

        // ==========================================
        // 3. 矿石系列 (Ores)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.COAL_ORE, dsl.simple("mcl_core:stone_with_coal"))
        registry.register(ChunkerVanillaBlockType.IRON_ORE, dsl.simple("mcl_core:stone_with_iron"))
        registry.register(ChunkerVanillaBlockType.GOLD_ORE, dsl.simple("mcl_core:stone_with_gold"))
        registry.register(ChunkerVanillaBlockType.REDSTONE_ORE, dsl.litOre("mcl_core:stone_with_redstone", "mcl_core:stone_with_redstone_lit"))
        registry.register(ChunkerVanillaBlockType.LAPIS_ORE, dsl.simple("mcl_core:stone_with_lapis"))
        registry.register(ChunkerVanillaBlockType.EMERALD_ORE, dsl.simple("mcl_core:stone_with_emerald"))
        registry.register(ChunkerVanillaBlockType.DIAMOND_ORE, dsl.simple("mcl_core:stone_with_diamond"))

        // 金属与宝石块
        registry.register(ChunkerVanillaBlockType.GOLD_BLOCK, dsl.simple("mcl_core:goldblock"))
        registry.register(ChunkerVanillaBlockType.IRON_BLOCK, dsl.simple("mcl_core:ironblock"))
        registry.register(ChunkerVanillaBlockType.DIAMOND_BLOCK, dsl.simple("mcl_core:diamondblock"))
        registry.register(ChunkerVanillaBlockType.LAPIS_BLOCK, dsl.simple("mcl_core:lapisblock"))
        registry.register(ChunkerVanillaBlockType.EMERALD_BLOCK, dsl.simple("mcl_core:emeraldblock"))

        // ==========================================
        // 4. 玻璃与玻璃板 (mcl_core / mcl_panes)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.GLASS, dsl.simple("mcl_core:glass"))
        registry.register(ChunkerVanillaBlockType.GLASS_PANE, dsl.simple("mcl_panes:pane_natural"))
        registry.register(ChunkerVanillaBlockType.IRON_BARS, dsl.simple("mcl_panes:bar"))

        // ==========================================
        // 5. 泥块与泥砖 (mcl_mud)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.MUD, dsl.simple("mcl_mud:mud"))
        registry.register(ChunkerVanillaBlockType.PACKED_MUD, dsl.simple("mcl_mud:packed_mud"))
        registry.register(ChunkerVanillaBlockType.MUD_BRICKS, dsl.simple("mcl_mud:mud_bricks"))

        // ==========================================
        // 6. 深层泥板及其变种 (mcl_deepslate)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.DEEPSLATE, dsl.log("mcl_deepslate:deepslate"))
        registry.register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE, dsl.simple("mcl_deepslate:deepslate_cobbled"))
        registry.register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE, dsl.simple("mcl_deepslate:deepslate_polished"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_BRICKS, dsl.simple("mcl_deepslate:deepslate_bricks"))
        registry.register(ChunkerVanillaBlockType.CRACKED_DEEPSLATE_BRICKS, dsl.simple("mcl_deepslate:deepslate_bricks_cracked"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_TILES, dsl.simple("mcl_deepslate:deepslate_tiles"))
        registry.register(ChunkerVanillaBlockType.CRACKED_DEEPSLATE_TILES, dsl.simple("mcl_deepslate:deepslate_tiles_cracked"))
        registry.register(ChunkerVanillaBlockType.CHISELED_DEEPSLATE, dsl.simple("mcl_deepslate:deepslate_chiseled"))
        registry.register(ChunkerVanillaBlockType.REINFORCED_DEEPSLATE, dsl.log("mcl_deepslate:deepslate_reinforced"))

        // 深层矿石
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COAL_ORE, dsl.simple("mcl_deepslate:deepslate_with_coal"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_IRON_ORE, dsl.simple("mcl_deepslate:deepslate_with_iron"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_GOLD_ORE, dsl.simple("mcl_deepslate:deepslate_with_gold"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_REDSTONE_ORE, dsl.litOre("mcl_deepslate:deepslate_with_redstone", "mcl_deepslate:deepslate_with_redstone_lit"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_LAPIS_ORE, dsl.simple("mcl_deepslate:deepslate_with_lapis"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_EMERALD_ORE, dsl.simple("mcl_deepslate:deepslate_with_emerald"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_DIAMOND_ORE, dsl.simple("mcl_deepslate:deepslate_with_diamond"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, dsl.simple("mcl_deepslate:deepslate_with_copper"))

        // ==========================================
        // 7. 凝灰岩及其变种 (mcl_deepslate)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.TUFF, dsl.simple("mcl_deepslate:tuff"))
        registry.register(ChunkerVanillaBlockType.POLISHED_TUFF, dsl.simple("mcl_deepslate:tuff_polished"))
        registry.register(ChunkerVanillaBlockType.TUFF_BRICKS, dsl.simple("mcl_deepslate:tuff_bricks"))
        registry.register(ChunkerVanillaBlockType.CHISELED_TUFF, dsl.simple("mcl_deepslate:tuff_chiseled"))
        registry.register(ChunkerVanillaBlockType.CHISELED_TUFF_BRICKS, dsl.simple("mcl_deepslate:tuff_chiseled_bricks"))

        // ==========================================
        // 8. 楼梯、台阶与墙体集合注册 (Stairs, Slabs & Walls)
        // ==========================================
        registerStoneSet("cobble", "cobbles", "cobble", hasWall = true)
        registerStoneSet("mossycobble", "mossycobble", "mossy_cobblestone", hasWall = true)
        registerStoneSet("stone", "stone", "stone", hasWall = false)
        registerStoneSet("stonebrick", "stonebrick", "stone_brick", hasWall = true)
        registerStoneSet("stonebrickmossy", "stonebrickmossy", "mossy_stone_brick", hasWall = true)
        registerStoneSet("granite", "granite", "granite", hasWall = true)
        registerStoneSet("granite_smooth", "granite_smooth", "polished_granite", hasWall = false)
        registerStoneSet("diorite", "diorite", "diorite", hasWall = true)
        registerStoneSet("diorite_smooth", "diorite_smooth", "polished_diorite", hasWall = false)
        registerStoneSet("andesite", "andesite", "andesite", hasWall = true)
        registerStoneSet("andesite_smooth", "andesite_smooth", "polished_andesite", hasWall = false)
        registerStoneSet("brick", "brick", "brick", hasWall = true, suffix = "_block")
        registerStoneSet("sandstone", "sandstone", "sandstone", hasWall = true)
        registerStoneSet("redsandstone", "redsandstone", "red_sandstone", hasWall = true)
        registerStoneSet("mudbrick", "mudbrick", "mud_brick", hasWall = true)

        // 特殊矿物楼梯台阶 (mclx_stairs)
        registerMineralStairs()

        // ==========================================
        // 9. 16色彩色方块与羊毛 (Colorblocks & Wool)
        // ==========================================
        registerColoredSets()
        
        // ==========================================
// 箱子与陷阱箱映射 (Chests & Trapped Chests)
// ==========================================
registry.register(ChunkerVanillaBlockType.CHEST, dsl.chest("mcl_chests:chest"))
registry.register(ChunkerVanillaBlockType.TRAPPED_CHEST, dsl.chest("mcl_chests:trapped_chest"))
registry.register(ChunkerVanillaBlockType.ENDER_CHEST, dsl.directional("mcl_chests:ender_chest_small"))
    }

    /**
     * 注册一种岩石及其相关的楼梯、台阶、墙体变体
     */
    private fun registerStoneSet(
        mclBase: String,
        mclStairSlab: String,
        chunkerName: String,
        hasWall: Boolean,
        suffix: String = ""
    ) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 注册楼梯
        try {
            val stairType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_STAIRS")
            registry.register(stairType, dsl.stair("mcl_stairs:stair_$mclStairSlab"))
        } catch (_: IllegalArgumentException) {}

        // 注册台阶
        try {
            val slabType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_SLAB")
            registry.register(slabType, dsl.slab(
                "mcl_stairs:slab_$mclStairSlab",
                "mcl_stairs:slab_${mclStairSlab}_top",
                "mcl_stairs:slab_${mclStairSlab}_double"
            ))
        } catch (_: IllegalArgumentException) {}

        // 注册墙体
        if (hasWall) {
            try {
                val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_WALL")
                registry.register(wallType, dsl.simple("mcl_walls:$mclBase"))
            } catch (_: IllegalArgumentException) {}
        }
    }

    /**
     * 特殊金属、宝石楼梯台阶 (mclx_stairs)
     */
    private fun registerMineralStairs() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 青金石
        registry.register(ChunkerVanillaBlockType.LAPIS_BLOCK, dsl.simple("mcl_core:lapisblock"))
        // 金块楼梯台阶 (mclx_stairs_init.lua)
        // 铁块楼梯台阶 (mclx_stairs_init.lua)
    }

    /**
     * 循环处理 16 色系方块（羊毛、陶瓦、带釉陶瓦、混凝土、混凝土粉末、染色玻璃与玻璃板）
     */
    private fun registerColoredSets() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        val colors = listOf(
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
        )

        for (color in colors) {
            val upperColor = color.uppercase()

            // 1. 羊毛 (Wool)
            val woolType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_WOOL")
            registry.register(woolType, dsl.simple("mcl_wool:$color"))

            // 2. 地毯 (Carpet)
            val carpetType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_CARPET")
            registry.register(carpetType, dsl.simple("mcl_wool:carpet_$color"))

            // 3. 陶瓦 (Terracotta)
            val clayType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_TERRACOTTA")
            registry.register(clayType, dsl.simple("mcl_colorblocks:hardened_clay_$color"))

            // 4. 混凝土 (Concrete)
            val concreteType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_CONCRETE")
            registry.register(concreteType, dsl.simple("mcl_colorblocks:concrete_$color"))

            // 5. 混凝土粉末 (Concrete Powder)
            val powderType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_CONCRETE_POWDER")
            registry.register(powderType, dsl.simple("mcl_colorblocks:concrete_powder_$color"))

            // 6. 带釉陶瓦 (Glazed Terracotta) - 带有 Facedir 方向
            val glazedType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_GLAZED_TERRACOTTA")
            registry.register(glazedType, dsl.directional("mcl_colorblocks:glazed_terracotta_$color"))

            // 7. 染色玻璃 (Stained Glass)
            val glassType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_STAINED_GLASS")
            registry.register(glassType, dsl.simple("mcl_core:glass_$color"))

            // 8. 染色玻璃板 (Stained Glass Pane)
            val paneType = enumValueOf<ChunkerVanillaBlockType>("${upperColor}_STAINED_GLASS_PANE")
            registry.register(paneType, dsl.simple("mcl_panes:pane_${color}"))
        }
    }
}