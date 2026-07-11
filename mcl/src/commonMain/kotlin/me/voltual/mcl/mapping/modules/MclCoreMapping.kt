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
        // 1. 基础自然与通用岩石
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
        
        registry.register(ChunkerVanillaBlockType.BEDROCK, dsl.simple("mcl_core:bedrock"))
        registry.register(ChunkerVanillaBlockType.GRAVEL, dsl.simple("mcl_core:gravel"))
        registry.register(ChunkerVanillaBlockType.CLAY, dsl.simple("mcl_core:clay"))
        registry.register(ChunkerVanillaBlockType.BRICKS, dsl.simple("mcl_core:brick_block"))
        registry.register(ChunkerVanillaBlockType.TERRACOTTA, dsl.simple("mcl_colorblocks:hardened_clay"))

        // 2. 土壤与生态
        registry.register(ChunkerVanillaBlockType.DIRT, dsl.simple("mcl_core:dirt"))
        registry.register(ChunkerVanillaBlockType.COARSE_DIRT, dsl.simple("mcl_core:coarse_dirt"))
        registry.register(ChunkerVanillaBlockType.PODZOL, dsl.simple("mcl_core:podzol"))
        registry.register(ChunkerVanillaBlockType.MYCELIUM, dsl.simple("mcl_core:mycelium"))
        registry.register(ChunkerVanillaBlockType.DIRT_PATH, dsl.simple("mcl_core:grass_path"))
        registry.register(ChunkerVanillaBlockType.GRASS_BLOCK, dsl.simple("mcl_core:dirt_with_grass"))

        // 3. 液体与冰雪
        registry.register(ChunkerVanillaBlockType.WATER, dsl.liquid("mcl_core:water_source", "mcl_core:water_flowing"))
        registry.register(ChunkerVanillaBlockType.LAVA, dsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing"))
        registry.register(ChunkerVanillaBlockType.ICE, dsl.simple("mcl_core:ice"))
        registry.register(ChunkerVanillaBlockType.SNOW_BLOCK, dsl.simple("mcl_core:snowblock"))

        // 4. 箱子系列 (修复物理后缀)
        registry.register(ChunkerVanillaBlockType.CHEST, dsl.chest("mcl_chests:chest"))
        registry.register(ChunkerVanillaBlockType.TRAPPED_CHEST, dsl.chest("mcl_chests:trapped_chest"))
        registry.register(ChunkerVanillaBlockType.ENDER_CHEST, dsl.directional("mcl_chests:ender_chest_small"))

        // 5. 彩色方块集注册 (16色循环)
        registerColoredSets()
    }

    private fun registerColoredSets() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // Minecraft Enum Color -> (Mineclonia Dye Color, Mineclonia Shulker Color)
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

        for ((mcName, colors) in colorMap) {
            val dColor = colors.first   // 标准染料名 (陶瓦/混凝土)
            val sColor = colors.second  // 潜影盒专用名
            
            // 羊毛
            registry.register(enumValueOf("${mcName}_WOOL"), dsl.simple("mcl_wool:$dColor"))
            registry.register(enumValueOf("${mcName}_CARPET"), dsl.simple("mcl_wool:carpet_$dColor"))
            
            // 混凝土与陶瓦
            registry.register(enumValueOf("${mcName}_CONCRETE"), dsl.simple("mcl_colorblocks:concrete_$dColor"))
            registry.register(enumValueOf("${mcName}_CONCRETE_POWDER"), dsl.simple("mcl_colorblocks:concrete_powder_$dColor"))
            registry.register(enumValueOf("${mcName}_TERRACOTTA"), dsl.simple("mcl_colorblocks:hardened_clay_$dColor"))
            registry.register(enumValueOf("${mcName}_GLAZED_TERRACOTTA"), dsl.directional("mcl_colorblocks:glazed_terracotta_$dColor"))

            // 玻璃
            registry.register(enumValueOf("${mcName}_STAINED_GLASS"), dsl.simple("mcl_core:glass_$dColor"))
            registry.register(enumValueOf("${mcName}_STAINED_GLASS_PANE"), dsl.simple("mcl_panes:pane_$dColor"))

            // 潜影盒 (基于 ShulkerBox DSL 处理 6 向朝向)
            registry.register(enumValueOf("${mcName}_SHULKER_BOX"), dsl.shulkerBox(sColor))
        }

        // 未染色潜影盒默认映射到紫色
        registry.register(ChunkerVanillaBlockType.SHULKER_BOX, dsl.shulkerBox("violet"))
    }
}