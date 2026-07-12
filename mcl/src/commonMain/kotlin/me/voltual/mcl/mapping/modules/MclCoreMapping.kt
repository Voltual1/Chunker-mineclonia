package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.core.MclNode

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
        
        // 平滑石头及其派生 (Smooth Stone)
        registry.register(ChunkerVanillaBlockType.SMOOTH_STONE, dsl.simple("mcl_core:stone_smooth"))
        registry.register(ChunkerVanillaBlockType.SMOOTH_STONE_SLAB, dsl.slab(
            "mcl_stairs:slab_stone",
            "mcl_stairs:slab_stone_top",
            "mcl_stairs:slab_stone_double"
        ))
        // 修正：使用 STONE_STAIRS 映射到 Mineclonia 的平滑石头楼梯
        registry.register(ChunkerVanillaBlockType.STONE_STAIRS, dsl.stair("mcl_stairs:stair_stone"))
        
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
        registry.register(ChunkerVanillaBlockType.TERRACOTTA, dsl.simple("mcl_colorblocks:hardened_clay"))

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

        // 蛋糕映射逻辑
        registry.register(ChunkerVanillaBlockType.CAKE, BlockMapper { id ->
            val bites = id.getState(VanillaBlockStates.BITES) ?: Bites._0
            val nodeName = when (bites) {
                Bites._0 -> "mcl_cake:cake"
                Bites._1 -> "mcl_cake:cake_6"
                Bites._2 -> "mcl_cake:cake_5"
                Bites._3 -> "mcl_cake:cake_4"
                Bites._4 -> "mcl_cake:cake_3"
                Bites._5 -> "mcl_cake:cake_2"
                Bites._6 -> "mcl_cake:cake_1"
                else -> "mcl_cake:cake"
            }
            MclNode(nodeName)
        })

        // ==========================================
        // 2. 液体与冰雪
        // ==========================================
        registry.register(ChunkerVanillaBlockType.WATER, dsl.liquid("mcl_core:water_source", "mcl_core:water_flowing"))
        registry.register(ChunkerVanillaBlockType.LAVA, dsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing"))
        registry.register(ChunkerVanillaBlockType.POWDER_SNOW, dsl.simple("mcl_powder_snow:powder_snow"))
        
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
        // 4. 玻璃与玻璃板
        // ==========================================
        registry.register(ChunkerVanillaBlockType.GLASS, dsl.simple("mcl_core:glass"))
        registry.register(ChunkerVanillaBlockType.GLASS_PANE, dsl.simple("mcl_panes:pane_natural"))
        registry.register(ChunkerVanillaBlockType.IRON_BARS, dsl.simple("mcl_panes:bar"))

        // ==========================================
        // 5. 泥块、泥砖与红树根系列
        // ==========================================
        registry.register(ChunkerVanillaBlockType.MUD, dsl.simple("mcl_mud:mud"))
        registry.register(ChunkerVanillaBlockType.PACKED_MUD, dsl.simple("mcl_mud:packed_mud"))
        registry.register(ChunkerVanillaBlockType.MUD_BRICKS, dsl.simple("mcl_mud:mud_bricks"))
        registry.register(ChunkerVanillaBlockType.MUDDY_MANGROVE_ROOTS, dsl.log("mcl_mangrove:mangrove_mud_roots"))

        registry.register(ChunkerVanillaBlockType.MUD_BRICK_STAIRS, dsl.stair("mcl_stairs:stair_mud_brick"))
        registry.register(ChunkerVanillaBlockType.MUD_BRICK_SLAB, dsl.slab(
            "mcl_stairs:slab_mud_brick",
            "mcl_stairs:slab_mud_brick_top",
            "mcl_stairs:slab_mud_brick_double"
        ))
        registry.register(ChunkerVanillaBlockType.MUD_BRICK_WALL, dsl.simple("mcl_walls:mudbrick"))

        // ==========================================
        // 6. 深层板岩及其变种
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

        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COAL_ORE, dsl.simple("mcl_deepslate:deepslate_with_coal"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_IRON_ORE, dsl.simple("mcl_deepslate:deepslate_with_iron"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_GOLD_ORE, dsl.simple("mcl_deepslate:deepslate_with_gold"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_REDSTONE_ORE, dsl.litOre("mcl_deepslate:deepslate_with_redstone", "mcl_deepslate:deepslate_with_redstone_lit"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_LAPIS_ORE, dsl.simple("mcl_deepslate:deepslate_with_lapis"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_EMERALD_ORE, dsl.simple("mcl_deepslate:deepslate_with_emerald"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_DIAMOND_ORE, dsl.simple("mcl_deepslate:deepslate_with_diamond"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, dsl.simple("mcl_deepslate:deepslate_with_copper"))

        registry.register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_STAIRS, dsl.stair("mcl_stairs:stair_deepslate_cobbled"))
        registry.register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_SLAB, dsl.slab("mcl_stairs:slab_deepslate_cobbled", "mcl_stairs:slab_deepslate_cobbled_top", "mcl_stairs:slab_deepslate_cobbled_double"))
        registry.register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_WALL, dsl.simple("mcl_deepslate:deepslatecobbledwall"))

        registry.register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_STAIRS, dsl.stair("mcl_stairs:stair_deepslate_polished"))
        registry.register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_SLAB, dsl.slab("mcl_stairs:slab_deepslate_polished", "mcl_stairs:slab_deepslate_polished_top", "mcl_stairs:slab_deepslate_polished_double"))
        registry.register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_WALL, dsl.simple("mcl_deepslate:deepslatepolishedwall"))

        registry.register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_STAIRS, dsl.stair("mcl_stairs:stair_deepslate_bricks"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_SLAB, dsl.slab("mcl_stairs:slab_deepslate_bricks", "mcl_stairs:slab_deepslate_bricks_top", "mcl_stairs:slab_deepslate_bricks_double"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_WALL, dsl.simple("mcl_deepslate:deepslatebrickswall"))

        registry.register(ChunkerVanillaBlockType.DEEPSLATE_TILE_STAIRS, dsl.stair("mcl_stairs:stair_deepslate_tiles"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_TILE_SLAB, dsl.slab("mcl_stairs:slab_deepslate_tiles", "mcl_stairs:slab_deepslate_tiles_top", "mcl_stairs:slab_deepslate_tiles_double"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_TILE_WALL, dsl.simple("mcl_deepslate:deepslatetileswall"))

        // ==========================================
        // 7. 凝灰岩及其变种
        // ==========================================
        registry.register(ChunkerVanillaBlockType.TUFF, dsl.simple("mcl_deepslate:tuff"))
        registry.register(ChunkerVanillaBlockType.POLISHED_TUFF, dsl.simple("mcl_deepslate:tuff_polished"))
        registry.register(ChunkerVanillaBlockType.TUFF_BRICKS, dsl.simple("mcl_deepslate:tuff_bricks"))
        registry.register(ChunkerVanillaBlockType.CHISELED_TUFF, dsl.simple("mcl_deepslate:tuff_chiseled"))
        registry.register(ChunkerVanillaBlockType.CHISELED_TUFF_BRICKS, dsl.simple("mcl_deepslate:tuff_chiseled_bricks"))

        registerStoneSet("tuff", "tuff", "tuff", hasWall = true)
        registerStoneSet("tuff_polished", "tuff_polished", "polished_tuff", hasWall = true)
        registerStoneSet("tuff_brick", "tuff_brick", "tuff_brick", hasWall = true)

        // ==========================================
        // 8. 苍白橡树树脂变体
        // ==========================================
        registry.register(ChunkerVanillaBlockType.RESIN_BLOCK, dsl.simple("mcl_pale_oak:block_of_resin"))
        registry.register(ChunkerVanillaBlockType.RESIN_BRICKS, dsl.simple("mcl_pale_oak:resin_brick_block"))
        registry.register(ChunkerVanillaBlockType.CHISELED_RESIN_BRICKS, dsl.simple("mcl_pale_oak:chiseled_resin_brick"))
        
        registry.register(ChunkerVanillaBlockType.RESIN_BRICK_STAIRS, dsl.stair("mcl_stairs:slab_resin_brick"))
        registry.register(ChunkerVanillaBlockType.RESIN_BRICK_SLAB, dsl.slab("mcl_stairs:slab_resin_brick", "mcl_stairs:slab_resin_brick_top", "mcl_stairs:slab_resin_brick_double"))
        registry.register(ChunkerVanillaBlockType.RESIN_BRICK_WALL, dsl.simple("mcl_pale_oak:resinbrick"))

        // ==========================================
        // 9. 楼梯、台阶与墙体集合注册
        // ==========================================
        registerStoneSet("cobble", "cobble", "cobblestone", hasWall = true)
        registerStoneSet("mossycobble", "mossycobble", "mossy_cobblestone", hasWall = true)
        
        // 已经通过手动注册处理了 Smooth Stone 相关的变体，这里保留 registerStoneSet 自动处理通用部分
        // 注意：Minecraft 的 STONE_SLAB 通常也指 Smooth Stone Slab
        registry.register(ChunkerVanillaBlockType.STONE_SLAB, dsl.slab(
            "mcl_stairs:slab_stone",
            "mcl_stairs:slab_stone_top",
            "mcl_stairs:slab_stone_double"
        ))
        
        registerStoneSet("stonebrick", "stonebrick", "stone_brick", hasWall = true)
        registerStoneSet("stonebrickmossy", "stonebrickmossy", "mossy_stone_brick", hasWall = true)
        registerStoneSet("granite", "granite", "granite", hasWall = true)
        registerStoneSet("granite_smooth", "granite_smooth", "polished_granite", hasWall = false)
        registerStoneSet("diorite", "diorite", "diorite", hasWall = true)
        registerStoneSet("diorite_smooth", "diorite_smooth", "polished_diorite", hasWall = false)
        registerStoneSet("andesite", "andesite", "andesite", hasWall = true)
        registerStoneSet("andesite_smooth", "andesite_smooth", "polished_andesite", hasWall = false)
        registerStoneSet("sandstone", "sandstone", "sandstone", hasWall = true)
        registerStoneSet("redsandstone", "redsandstone", "red_sandstone", hasWall = true)
        
        registry.register(ChunkerVanillaBlockType.BRICK_STAIRS, dsl.stair("mcl_stairs:stair_brick_block"))
        registry.register(ChunkerVanillaBlockType.BRICK_SLAB, dsl.slab(
            "mcl_stairs:slab_brick_block",
            "mcl_stairs:slab_brick_block_top",
            "mcl_stairs:slab_brick_block_double"
        ))
        registry.register(ChunkerVanillaBlockType.BRICK_WALL, dsl.simple("mcl_walls:brick"))

        // ==========================================
        // 10. 箱子系列
        // ==========================================
        registry.register(ChunkerVanillaBlockType.CHEST, dsl.chest("mcl_chests:chest"))
        registry.register(ChunkerVanillaBlockType.TRAPPED_CHEST, dsl.chest("mcl_chests:trapped_chest"))
        registry.register(ChunkerVanillaBlockType.ENDER_CHEST, dsl.directional("mcl_chests:ender_chest_small"))

        // ==========================================
        // 11. 16色彩色方块与羊毛
        // ==========================================
        registerColoredSets()
        
        registry.register(ChunkerVanillaBlockType.SPAWNER, dsl.simple("mcl_mobspawners:spawner"))
    }

    private fun registerStoneSet(
        mclBase: String,
        mclStairSlab: String,
        chunkerName: String,
        hasWall: Boolean,
        suffix: String = ""
    ) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        try {
            val stairType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_STAIRS")
            registry.register(stairType, dsl.stair("mcl_stairs:stair_$mclStairSlab"))
        } catch (_: IllegalArgumentException) {}

        try {
            val slabType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_SLAB")
            registry.register(slabType, dsl.slab(
                "mcl_stairs:slab_$mclStairSlab",
                "mcl_stairs:slab_${mclStairSlab}_top",
                "mcl_stairs:slab_${mclStairSlab}_double"
            ))
        } catch (_: IllegalArgumentException) {}

        if (hasWall) {
            try {
                val wallType = enumValueOf<ChunkerVanillaBlockType>("${chunkerName.uppercase()}_WALL")
                registry.register(wallType, dsl.simple("mcl_walls:$mclBase"))
            } catch (_: IllegalArgumentException) {}
        }
    }

    private fun registerColoredSets() {
        val colorMap = mapOf(
            "WHITE" to Pair("white", "white"),
            "ORANGE" to Pair("orange", "orange"),
            "MAGENTA" to Pair("magenta", "magenta"),
            "LIGHT_BLUE" to Pair("light_blue", "lightblue"),
            "YELLOW" to Pair("yellow", "yellow"),
            "LIME" to Pair("lime", "green"),
            "PINK" to Pair("pink", "pink"),
            "GRAY" to Pair("grey", "dark_grey"),
            "LIGHT_GRAY" to Pair("silver", "grey"),
            "CYAN" to Pair("cyan", "cyan"),
            "PURPLE" to Pair("purple", "violet"),
            "BLUE" to Pair("blue", "blue"),
            "BROWN" to Pair("brown", "brown"),
            "GREEN" to Pair("green", "dark_green"),
            "RED" to Pair("red", "red"),
            "BLACK" to Pair("black", "black")
        )

        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        for ((mcName, colors) in colorMap) {
            val dColor = colors.first   
            val sColor = colors.second  
            
            registry.register(enumValueOf("${mcName}_WOOL"), dsl.simple("mcl_wool:$dColor"))
            registry.register(enumValueOf("${mcName}_CARPET"), dsl.simple("mcl_wool:${dColor}_carpet"))
            registry.register(enumValueOf("${mcName}_CONCRETE"), dsl.simple("mcl_colorblocks:concrete_$dColor"))
            registry.register(enumValueOf("${mcName}_CONCRETE_POWDER"), dsl.simple("mcl_colorblocks:concrete_powder_$dColor"))
            registry.register(enumValueOf("${mcName}_TERRACOTTA"), dsl.simple("mcl_colorblocks:hardened_clay_$dColor"))
            registry.register(enumValueOf("${mcName}_GLAZED_TERRACOTTA"), dsl.directional("mcl_colorblocks:glazed_terracotta_$dColor"))
            registry.register(enumValueOf("${mcName}_STAINED_GLASS"), dsl.simple("mcl_core:glass_$dColor"))
            registry.register(enumValueOf("${mcName}_STAINED_GLASS_PANE"), dsl.simple("mcl_panes:pane_$dColor"))
            registry.register(enumValueOf("${mcName}_SHULKER_BOX"), dsl.shulkerBox(sColor))
        }
        registry.register(ChunkerVanillaBlockType.SHULKER_BOX, dsl.shulkerBox("violet"))
    }
}