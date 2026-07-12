package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.core.MclNode
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclBedMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 1. 注册 16 色的床
        registerBeds()

        // 2. 重生锚 (Respawn Anchor)
        registry.register(ChunkerVanillaBlockType.RESPAWN_ANCHOR, BlockMapper { id ->
            val charges = id.getState(VanillaBlockStates.RESPAWN_ANCHOR_CHARGES) ?: RespawnAnchorCharges._0
            val nodeName = if (charges == RespawnAnchorCharges._0) {
                "mcl_beds:respawn_anchor"
            } else {
                "mcl_beds:respawn_anchor_charged_${charges.ordinal}"
            }
            MclNode(nodeName)
        })
    }

    private fun registerBeds() {
        val registry = MclMappingRegistry
        
        // 颜色名称映射 (Minecraft 枚举名 -> Mineclonia 节点颜色名)
        val bedColors = mapOf(
            "WHITE" to "white",
            "ORANGE" to "orange",
            "MAGENTA" to "magenta",
            "LIGHT_BLUE" to "light_blue",
            "YELLOW" to "yellow",
            "LIME" to "lime",
            "PINK" to "pink",
            "GRAY" to "gray",
            "LIGHT_GRAY" to "silver", // 修正：淡灰色对应 silver
            "CYAN" to "cyan",
            "PURPLE" to "purple",
            "BLUE" to "blue",
            "BROWN" to "brown",
            "GREEN" to "green",
            "RED" to "red",
            "BLACK" to "black"
        )

        for ((mcPrefix, mclColor) in bedColors) {
            val enumName = "${mcPrefix}_BED"
            try {
                val blockType = ChunkerVanillaBlockType.valueOf(enumName)
                registry.register(blockType, BlockMapper { id ->
                    val part = id.getState(VanillaBlockStates.BED_PART) ?: BedPart.FOOT
                    val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
                    
                    // Mineclonia: bottom 是足部，top 是头部
                    val suffix = if (part == BedPart.HEAD) "_top" else "_bottom"
                    
                    /**
                     * 修正方向映射逻辑：
                     * 根据反馈：Z轴正常 (0=North, 2=South)，但 X轴反了。
                     * 原始逻辑：EAST=1, WEST=3
                     * 修正逻辑：EAST=3, WEST=1
                     */
                    val param2 = when (facing) {
                        FacingDirectionHorizontal.NORTH -> 0
                        FacingDirectionHorizontal.SOUTH -> 2
                        FacingDirectionHorizontal.EAST -> 3 // 对调修正
                        FacingDirectionHorizontal.WEST -> 1 // 对调修正
                    }.toByte()

                    MclNode("mcl_beds:bed_${mclColor}${suffix}", param2 = param2)
                })
            } catch (e: IllegalArgumentException) {
                // 忽略不存在的枚举
            }
        }
    }
}