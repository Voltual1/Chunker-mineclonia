package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.core.MclNode
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclCandleMapping : MclMappingModule {
    
    // 颜色到 Mineclonia 调色板索引的映射 (基于 mcl_dyes.lua)
    private val colorToPaletteIndex = mapOf(
        "WHITE" to 0, "ORANGE" to 12, "MAGENTA" to 14, "LIGHT_BLUE" to 6,
        "YELLOW" to 10, "LIME" to 9, "PINK" to 15, "GRAY" to 2,
        "LIGHT_GRAY" to 1, "CYAN" to 7, "PURPLE" to 4, "BLUE" to 5,
        "BROWN" to 11, "GREEN" to 8, "RED" to 13, "BLACK" to 3
    )

    override fun register() {
        val registry = MclMappingRegistry

        // 1. 注册普通蜡烛 (CANDLE, WHITE_CANDLE ... BLACK_CANDLE)
        val candleColors = listOf(
            "" to ChunkerVanillaBlockType.CANDLE,
            "WHITE" to ChunkerVanillaBlockType.WHITE_CANDLE,
            "ORANGE" to ChunkerVanillaBlockType.ORANGE_CANDLE,
            "MAGENTA" to ChunkerVanillaBlockType.MAGENTA_CANDLE,
            "LIGHT_BLUE" to ChunkerVanillaBlockType.LIGHT_BLUE_CANDLE,
            "YELLOW" to ChunkerVanillaBlockType.YELLOW_CANDLE,
            "LIME" to ChunkerVanillaBlockType.LIME_CANDLE,
            "PINK" to ChunkerVanillaBlockType.PINK_CANDLE,
            "GRAY" to ChunkerVanillaBlockType.GRAY_CANDLE,
            "LIGHT_GRAY" to ChunkerVanillaBlockType.LIGHT_GRAY_CANDLE,
            "CYAN" to ChunkerVanillaBlockType.CYAN_CANDLE,
            "PURPLE" to ChunkerVanillaBlockType.PURPLE_CANDLE,
            "BLUE" to ChunkerVanillaBlockType.BLUE_CANDLE,
            "BROWN" to ChunkerVanillaBlockType.BROWN_CANDLE,
            "GREEN" to ChunkerVanillaBlockType.GREEN_CANDLE,
            "RED" to ChunkerVanillaBlockType.RED_CANDLE,
            "BLACK" to ChunkerVanillaBlockType.BLACK_CANDLE
        )

        for ((colorName, blockType) in candleColors) {
            registry.register(blockType, BlockMapper { id ->
                val count = id.getState(VanillaBlockStates.CANDLES) ?: Candles._1
                val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
                
                // 确定基础节点名: mcl_candles:candle_[lit_]<数量>
                val num = count.ordinal + 1
                val baseName = if (lit) "mcl_candles:candle_lit_$num" else "mcl_candles:candle_$num"
                
                // 获取调色板索引 (param2)
                // 注意：Mineclonia 蜡烛的 param2 = palette_index (未染色通常为0)
                val paletteIndex = colorToPaletteIndex[colorName] ?: 0
                
                MclNode(baseName, param2 = paletteIndex.toByte())
            })
        }

        // 2. 注册蛋糕蜡烛 (CANDLE_CAKE, WHITE_CANDLE_CAKE ... BLACK_CANDLE_CAKE)
        val cakeCandleColors = listOf(
            "" to ChunkerVanillaBlockType.CANDLE_CAKE,
            "WHITE" to ChunkerVanillaBlockType.WHITE_CANDLE_CAKE,
            "ORANGE" to ChunkerVanillaBlockType.ORANGE_CANDLE_CAKE,
            "MAGENTA" to ChunkerVanillaBlockType.MAGENTA_CANDLE_CAKE,
            "LIGHT_BLUE" to ChunkerVanillaBlockType.LIGHT_BLUE_CANDLE_CAKE,
            "YELLOW" to ChunkerVanillaBlockType.YELLOW_CANDLE_CAKE,
            "LIME" to ChunkerVanillaBlockType.LIME_CANDLE_CAKE,
            "PINK" to ChunkerVanillaBlockType.PINK_CANDLE_CAKE,
            "GRAY" to ChunkerVanillaBlockType.GRAY_CANDLE_CAKE,
            "LIGHT_GRAY" to ChunkerVanillaBlockType.LIGHT_GRAY_CANDLE_CAKE,
            "CYAN" to ChunkerVanillaBlockType.CYAN_CANDLE_CAKE,
            "PURPLE" to ChunkerVanillaBlockType.PURPLE_CANDLE_CAKE,
            "BLUE" to ChunkerVanillaBlockType.BLUE_CANDLE_CAKE,
            "BROWN" to ChunkerVanillaBlockType.BROWN_CANDLE_CAKE,
            "GREEN" to ChunkerVanillaBlockType.GREEN_CANDLE_CAKE,
            "RED" to ChunkerVanillaBlockType.RED_CANDLE_CAKE,
            "BLACK" to ChunkerVanillaBlockType.BLACK_CANDLE_CAKE
        )

        for ((colorName, blockType) in cakeCandleColors) {
            registry.register(blockType, BlockMapper { id ->
                val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
                val baseName = if (lit) "mcl_candles:candle_cake_lit" else "mcl_candles:candle_cake"
                
                val paletteIndex = colorToPaletteIndex[colorName] ?: 0
                MclNode(baseName, param2 = paletteIndex.toByte())
            })
        }
    }
}