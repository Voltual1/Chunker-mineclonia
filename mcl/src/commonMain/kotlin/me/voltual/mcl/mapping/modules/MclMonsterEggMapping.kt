package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl

object MclMonsterEggMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 1. 基础虫蚀岩石
        registry.register(ChunkerVanillaBlockType.INFESTED_STONE, dsl.simple("mcl_monster_eggs:monster_egg_stone"))
        registry.register(ChunkerVanillaBlockType.INFESTED_COBBLESTONE, dsl.simple("mcl_monster_eggs:monster_egg_cobble"))
        
        // 2. 虫蚀石砖系列
        registry.register(ChunkerVanillaBlockType.INFESTED_STONE_BRICKS, dsl.simple("mcl_monster_eggs:monster_egg_stonebrick"))
        registry.register(ChunkerVanillaBlockType.INFESTED_CHISELED_STONE_BRICKS, dsl.simple("mcl_monster_eggs:monster_egg_stonebrickcarved"))
        registry.register(ChunkerVanillaBlockType.INFESTED_CRACKED_STONE_BRICKS, dsl.simple("mcl_monster_eggs:monster_egg_stonebrickcracked"))
        registry.register(ChunkerVanillaBlockType.INFESTED_MOSSY_STONE_BRICKS, dsl.simple("mcl_monster_eggs:monster_egg_stonebrickmossy"))

        // 3. 虫蚀深层板岩 (支持轴向 param2 facedir 转换)
        registry.register(ChunkerVanillaBlockType.INFESTED_DEEPSLATE, dsl.log("mcl_monster_eggs:monster_egg_deepslate"))
    }
}