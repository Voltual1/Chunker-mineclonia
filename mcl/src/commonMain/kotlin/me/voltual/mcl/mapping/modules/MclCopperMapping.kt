package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry

object MclCopperMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 1. 基础铜矿与原铜块
        registry.register(ChunkerVanillaBlockType.COPPER_ORE, dsl.simple("mcl_copper:stone_with_copper"))
        registry.register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, dsl.simple("mcl_deepslate:deepslate_with_copper"))
        registry.register(ChunkerVanillaBlockType.RAW_COPPER_BLOCK, dsl.simple("mcl_copper:block_raw"))

        // 2. 处理所有氧化等级和涂蜡状态
        val states = listOf("", "EXPOSED_", "WEATHERED_", "OXIDIZED_")
        
        for (s in states) {
            val sLow = s.lowercase().replace("_", "")
            val mclSuffix = if (sLow.isEmpty()) "" else "_$sLow"
            
            // 循环处理 正常版 和 涂蜡(WAXED_)版
            for (isWaxed in listOf(false, true)) {
                val prefix = if (isWaxed) "WAXED_$s" else s
                val waxedSuffix = if (isWaxed) "_preserved" else ""

                // 铜块
                registry.register(enumValueOf("${prefix}COPPER_BLOCK"), dsl.simple("mcl_copper:block$mclSuffix$waxedSuffix"))
                
                // 切制铜块 (Stairs/Slabs 已在 MclCoreMapping 处理，这里处理完整块)
                registry.register(enumValueOf("${prefix}CUT_COPPER"), dsl.simple("mcl_copper:block${mclSuffix}_cut$waxedSuffix"))
                
                // 錾刻铜块
                registry.register(enumValueOf("${prefix}CHISELED_COPPER"), dsl.simple("mcl_copper:block${mclSuffix}_chiseled$waxedSuffix"))
                
                // 铜格栅 (Grate)
                registry.register(enumValueOf("${prefix}COPPER_GRATE"), dsl.simple("mcl_copper:block${mclSuffix}_grate$waxedSuffix"))
                
                // 铜灯泡 (Bulb)
                registry.register(enumValueOf("${prefix}COPPER_BULB"), dsl.copperBulb("mcl_copper:bulb$mclSuffix"))
                
                // 铜门与活板门
                registry.register(enumValueOf("${prefix}COPPER_DOOR"), dsl.door("mcl_copper:door$mclSuffix$waxedSuffix"))
                registry.register(enumValueOf("${prefix}COPPER_TRAPDOOR"), dsl.trapdoor("mcl_copper:trapdoor$mclSuffix$waxedSuffix"))
                
                // 铜灯笼 (Lantern)
                registry.register(enumValueOf("${prefix}COPPER_LANTERN"), dsl.lantern("mcl_lanterns:copper_lantern$mclSuffix$waxedSuffix"))
                
                // 铜锁链 (Chain)
                registry.register(enumValueOf("${prefix}COPPER_CHAIN"), dsl.log("mcl_lanterns:copper_chain$mclSuffix$waxedSuffix"))
                
                // 铜栏杆 (Bars)
                registry.register(enumValueOf("${prefix}COPPER_BARS"), dsl.simple("mcl_panes:copper_bar$mclSuffix$waxedSuffix"))
            }
        }
        
        // 特殊：铜火把 (Copper Torch)
        registry.register(ChunkerVanillaBlockType.COPPER_TORCH, dsl.simple("mcl_torches:copper_torch"))
        registry.register(ChunkerVanillaBlockType.COPPER_WALL_TORCH, dsl.wallTorch("mcl_torches:copper_torch_wall", "mcl_torches:copper_torch_wall"))
    }

    private inline fun <reified T : Enum<T>> enumValueOf(name: String): T = 
        java.lang.Enum.valueOf(T::class.java, name)
}