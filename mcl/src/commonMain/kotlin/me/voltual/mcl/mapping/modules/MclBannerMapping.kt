package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.core.MclNode
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclBannerMapping : MclMappingModule {
    
    // 颜色对应映射 (Minecraft -> Mineclonia 颜色名)
    val colors = mapOf(
        "WHITE" to "white",
        "ORANGE" to "orange",
        "MAGENTA" to "magenta",
        "LIGHT_BLUE" to "light_blue",
        "YELLOW" to "yellow",
        "LIME" to "lime",
        "PINK" to "pink",
        "GRAY" to "grey",
        "LIGHT_GRAY" to "silver",
        "CYAN" to "cyan",
        "PURPLE" to "purple",
        "BLUE" to "blue",
        "BROWN" to "brown",
        "GREEN" to "green",
        "RED" to "red",
        "BLACK" to "black"
    )

    override fun register() {
        val registry = MclMappingRegistry

        for ((mcColor, mclColor) in colors) {
            // 1. 注册立地旗帜 (STANDING_BANNER)
            try {
                val standingType = ChunkerVanillaBlockType.valueOf("${mcColor}_BANNER")
                registry.register(standingType, BlockMapper { id ->
                    // 读取 16 角度旋转状态 (0-15)
                    val rotation = id.getState(VanillaBlockStates.ROTATION) ?: Rotation._0
                    
                    // Mineclonia 所有的立地旗帜统一转换为 mcl_banners:standing_banner
                    // 我们无需在此设置 param2，旋转数据将会被写入 BlockEntity 元数据中
                    MclNode("mcl_banners:standing_banner", param2 = 0)
                })
            } catch (_: IllegalArgumentException) {}

            // 2. 注册墙面旗帜 (WALL_BANNER)
            try {
                val wallType = ChunkerVanillaBlockType.valueOf("${mcColor}_WALL_BANNER")
                registry.register(wallType, BlockMapper { id ->
                    val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
                    
                    // 转换墙体 facedir param2。
                    // 参照 mcl_banners_init.lua 的挂墙逻辑，使用 wallmounted 处理：
                    // facedir_to_wallmounted 转换，这里我们直接根据朝向精准翻译为 Minetest param2：
                    val param2 = when (facing) {
                        FacingDirectionHorizontal.NORTH -> 4
                        FacingDirectionHorizontal.SOUTH -> 3
                        FacingDirectionHorizontal.WEST -> 2
                        FacingDirectionHorizontal.EAST -> 5
                    }.toByte()

                    MclNode("mcl_banners:hanging_banner", param2 = param2)
                })
            } catch (_: IllegalArgumentException) {}
        }
    }
}