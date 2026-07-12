package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl

object MclWoodMapping : MclMappingModule {
    override fun register() {
        // 1. 橡木 (Oak) - 基础前缀为 mcl_trees
        registerWoodSet("OAK", "oak", "mcl_trees", hasLeaves = true)
        
        // 2. 云杉 (Spruce)
        registerWoodSet("SPRUCE", "spruce", "mcl_trees", hasLeaves = true)
        
        // 3. 白桦 (Birch)
        registerWoodSet("BIRCH", "birch", "mcl_trees", hasLeaves = true)
        
        // 4. 丛林木 (Jungle)
        registerWoodSet("JUNGLE", "jungle", "mcl_trees", hasLeaves = true)
        
        // 5. 金合欢 (Acacia)
        registerWoodSet("ACACIA", "acacia", "mcl_trees", hasLeaves = true)
        
        // 6. 深色橡木 (Dark Oak)
        registerWoodSet("DARK_OAK", "dark_oak", "mcl_trees", hasLeaves = true)

        // 7. 红树林 (Mangrove) - 根据真实日志独立重写注册，规避不规则命名
        registerMangroveSet()

        // 8. 樱花 (Cherry) - 对应 Mineclonia 的 cherry_blossom，根据真实日志独立重写注册
        registerCherryBlossomSet()
        
        // 9. 苍白橡木 (Pale Oak)
        registerWoodSet("PALE_OAK", "pale_oak", "mcl_pale_oak", hasLeaves = true)

        // 10. 竹子 (Bamboo) - 特殊处理，因为没有 Log 概念
        registerBamboo()

        // 11. 绯红 (Crimson) & 诡异 (Warped)
        registerNetherWood("CRIMSON", "crimson")
        registerNetherWood("WARPED", "warped")
    }

    /**
     * 注册标准常规木材套装
     */
    private fun registerWoodSet(
        chunkerPrefix: String, 
        mclName: String, 
        modName: String, 
        hasLeaves: Boolean
    ) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        safeRegister(registry, "${chunkerPrefix}_LOG") { dsl.log("$modName:tree_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_WOOD") { dsl.log("$modName:bark_$mclName") }
        safeRegister(registry, "STRIPPED_${chunkerPrefix}_LOG") { dsl.log("$modName:stripped_$mclName") }
        safeRegister(registry, "STRIPPED_${chunkerPrefix}_WOOD") { dsl.log("$modName:bark_stripped_$mclName") }

        safeRegister(registry, "${chunkerPrefix}_PLANKS") { dsl.simple("mcl_trees:wood_$mclName") }

        safeRegister(registry, "${chunkerPrefix}_STAIRS") { dsl.stair("mcl_stairs:stair_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_SLAB") {
            dsl.slab(
                "mcl_stairs:slab_$mclName", 
                "mcl_stairs:slab_${mclName}_top", 
                "mcl_stairs:slab_${mclName}_double"
            )
        }

        safeRegister(registry, "${chunkerPrefix}_FENCE") { dsl.simple("mcl_fences:${mclName}_fence") }
        safeRegister(registry, "${chunkerPrefix}_FENCE_GATE") { dsl.gate("mcl_fences:${mclName}_fence_gate") }
        val doorName = if (mclName == "oak") "mcl_doors:wooden_door" else "mcl_doors:door_$mclName"
        safeRegister(registry, "${chunkerPrefix}_DOOR") { dsl.door(doorName) }
        
        // 橡木活板门在 Mineclonia 中没有 _oak 后缀，其他有
        val trapdoorName = if (mclName == "oak") "mcl_doors:trapdoor" else "mcl_doors:trapdoor_$mclName"
        safeRegister(registry, "${chunkerPrefix}_TRAPDOOR") { dsl.trapdoor(trapdoorName) }
        
        safeRegister(registry, "${chunkerPrefix}_BUTTON") { dsl.button(mclName) }
        safeRegister(registry, "${chunkerPrefix}_PRESSURE_PLATE") { dsl.pressurePlate(mclName) }

        safeRegister(registry, "${chunkerPrefix}_SIGN") { dsl.simple("mcl_signs:standing_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_WALL_SIGN") { dsl.directional("mcl_signs:wall_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_HANGING_SIGN") { dsl.simple("mcl_signs:hanging_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_WALL_HANGING_SIGN") { dsl.directional("mcl_signs:hanging_sign_wall_$mclName") }

        if (hasLeaves) {
            safeRegister(registry, "${chunkerPrefix}_LEAVES") { dsl.simple("$modName:leaves_$mclName") }
            val saplingEnumName = if (chunkerPrefix == "MANGROVE") "MANGROVE_PROPAGULE" else "${chunkerPrefix}_SAPLING"
            safeRegister(registry, saplingEnumName) { dsl.simple("mcl_trees:sapling_$mclName") }
        }
    }

    /**
     * 根据真实游戏日志，完美对齐红树林 (Mangrove) 系列
     */
    private fun registerMangroveSet() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val mod = "mcl_trees"

        registry.register(ChunkerVanillaBlockType.MANGROVE_LOG, dsl.log("$mod:tree_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_WOOD, dsl.log("$mod:wood_mangrove"))
        registry.register(ChunkerVanillaBlockType.STRIPPED_MANGROVE_LOG, dsl.log("$mod:stripped_mangrove"))
        registry.register(ChunkerVanillaBlockType.STRIPPED_MANGROVE_WOOD, dsl.log("$mod:bark_stripped_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_LEAVES, dsl.simple("$mod:leaves_mangrove"))

        // 特殊：木板
        registry.register(ChunkerVanillaBlockType.MANGROVE_PLANKS, dsl.simple("$mod:bark_mangrove"))

        // 楼梯/台阶 (支持 bark 变体与普通变体)
        registry.register(ChunkerVanillaBlockType.MANGROVE_STAIRS, dsl.stair("mcl_stairs:stair_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_SLAB, dsl.slab(
            "mcl_stairs:slab_mangrove", 
            "mcl_stairs:slab_mangrove_top", 
            "mcl_stairs:slab_mangrove_double"
        ))

        registry.register(ChunkerVanillaBlockType.MANGROVE_FENCE, dsl.simple("mcl_fences:mangrove_fence"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_FENCE_GATE, dsl.gate("mcl_fences:mangrove_fence_gate"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_DOOR, dsl.door("mcl_doors:door_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_TRAPDOOR, dsl.trapdoor("mcl_doors:trapdoor_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_BUTTON, dsl.button("mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_PRESSURE_PLATE, dsl.pressurePlate("mangrove"))

        // 告示牌
        registry.register(ChunkerVanillaBlockType.MANGROVE_SIGN, dsl.simple("mcl_signs:standing_sign_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_WALL_SIGN, dsl.directional("mcl_signs:wall_sign_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_HANGING_SIGN, dsl.simple("mcl_signs:hanging_sign_mangrove"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_WALL_HANGING_SIGN, dsl.directional("mcl_signs:hanging_sign_wall_mangrove"))

        // 生态植物
        registry.register(ChunkerVanillaBlockType.MANGROVE_PROPAGULE, dsl.simple("mcl_mangrove:propagule"))
        registry.register(ChunkerVanillaBlockType.MANGROVE_ROOTS, dsl.simple("mcl_mangrove:mangrove_roots"))
    }

    /**
     * 根据真实游戏日志，完美对齐樱花 (Cherry) 系列
     */
    private fun registerCherryBlossomSet() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val mod = "mcl_trees"
        val mcl = "cherry_blossom"

        registry.register(ChunkerVanillaBlockType.CHERRY_LOG, dsl.log("$mod:tree_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_WOOD, dsl.log("$mod:wood_$mcl"))
        registry.register(ChunkerVanillaBlockType.STRIPPED_CHERRY_LOG, dsl.log("$mod:stripped_$mcl"))
        registry.register(ChunkerVanillaBlockType.STRIPPED_CHERRY_WOOD, dsl.log("$mod:bark_stripped_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_LEAVES, dsl.simple("$mod:leaves_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_SAPLING, dsl.simple("$mod:sapling_$mcl"))

        // 特殊：木板
        registry.register(ChunkerVanillaBlockType.CHERRY_PLANKS, dsl.simple("$mod:bark_$mcl"))

        // 楼梯/台阶
        registry.register(ChunkerVanillaBlockType.CHERRY_STAIRS, dsl.stair("mcl_stairs:stair_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_SLAB, dsl.slab(
            "mcl_stairs:slab_$mcl", 
            "mcl_stairs:slab_${mcl}_top", 
            "mcl_stairs:slab_${mcl}_double"
        ))

        registry.register(ChunkerVanillaBlockType.CHERRY_FENCE, dsl.simple("mcl_fences:${mcl}_fence"))
        registry.register(ChunkerVanillaBlockType.CHERRY_FENCE_GATE, dsl.gate("mcl_fences:${mcl}_fence_gate"))
        registry.register(ChunkerVanillaBlockType.CHERRY_DOOR, dsl.door("mcl_doors:door_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_TRAPDOOR, dsl.trapdoor("mcl_doors:trapdoor_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_BUTTON, dsl.button(mcl))
        registry.register(ChunkerVanillaBlockType.CHERRY_PRESSURE_PLATE, dsl.pressurePlate(mcl))

        // 告示牌
        registry.register(ChunkerVanillaBlockType.CHERRY_SIGN, dsl.simple("mcl_signs:standing_sign_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_WALL_SIGN, dsl.directional("mcl_signs:wall_sign_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_HANGING_SIGN, dsl.simple("mcl_signs:hanging_sign_$mcl"))
        registry.register(ChunkerVanillaBlockType.CHERRY_WALL_HANGING_SIGN, dsl.directional("mcl_signs:hanging_sign_wall_$mcl"))
    }

    private fun registerBamboo() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        
        // 核心竹块变体
        registry.register(ChunkerVanillaBlockType.BAMBOO_BLOCK, dsl.log("mcl_trees:tree_bamboo"))
        registry.register(ChunkerVanillaBlockType.STRIPPED_BAMBOO_BLOCK, dsl.log("mcl_trees:stripped_bamboo"))

        registry.register(ChunkerVanillaBlockType.BAMBOO_PLANKS, dsl.simple("mcl_trees:wood_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_MOSAIC, dsl.simple("mcl_bamboo:bamboo_mosaic"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_STAIRS, dsl.stair("mcl_stairs:stair_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_MOSAIC_STAIRS, dsl.stair("mcl_stairs:stair_bamboo_mosaic"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_SLAB, dsl.slab("mcl_stairs:slab_bamboo", "mcl_stairs:slab_bamboo_top", "mcl_stairs:slab_bamboo_double"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_MOSAIC_SLAB, dsl.slab("mcl_stairs:slab_bamboo_mosaic", "mcl_stairs:slab_bamboo_mosaic_top", "mcl_stairs:slab_bamboo_mosaic_double"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_FENCE, dsl.simple("mcl_fences:bamboo_fence"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_FENCE_GATE, dsl.gate("mcl_fences:bamboo_fence_gate"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_DOOR, dsl.door("mcl_doors:door_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_TRAPDOOR, dsl.trapdoor("mcl_doors:trapdoor_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_BUTTON, dsl.button("bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_PRESSURE_PLATE, dsl.pressurePlate("bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_SIGN, dsl.simple("mcl_signs:standing_sign_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_WALL_SIGN, dsl.directional("mcl_signs:wall_sign_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_HANGING_SIGN, dsl.simple("mcl_signs:hanging_sign_bamboo"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_WALL_HANGING_SIGN, dsl.directional("mcl_signs:hanging_sign_wall_bamboo"))
    }

    private fun registerNetherWood(chunkerPrefix: String, mclName: String) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val mod = "mcl_trees"

        registry.register(enumValueOf("${chunkerPrefix}_STEM"), dsl.log("$mod:tree_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_HYPHAE"), dsl.log("$mod:wood_$mclName"))
        registry.register(enumValueOf("STRIPPED_${chunkerPrefix}_STEM"), dsl.log("$mod:stripped_$mclName"))
        registry.register(enumValueOf("STRIPPED_${chunkerPrefix}_HYPHAE"), dsl.log("$mod:bark_stripped_$mclName"))
        
        registry.register(enumValueOf("${chunkerPrefix}_PLANKS"), dsl.simple("$mod:bark_$mclName"))

        registry.register(enumValueOf("${chunkerPrefix}_FENCE"), dsl.simple("mcl_fences:${mclName}_fence"))
        registry.register(enumValueOf("${chunkerPrefix}_FENCE_GATE"), dsl.gate("mcl_fences:${mclName}_fence_gate"))
        registry.register(enumValueOf("${chunkerPrefix}_DOOR"), dsl.door("mcl_doors:door_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_TRAPDOOR"), dsl.trapdoor("mcl_doors:trapdoor_$mclName"))
        
        registry.register(enumValueOf("${chunkerPrefix}_STAIRS"), dsl.stair("mcl_stairs:stair_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_SLAB"), dsl.slab(
            "mcl_stairs:slab_$mclName", 
            "mcl_stairs:slab_${mclName}_top", 
            "mcl_stairs:slab_${mclName}_double"
        ))

        registry.register(enumValueOf("${chunkerPrefix}_BUTTON"), dsl.button(mclName))
        registry.register(enumValueOf("${chunkerPrefix}_PRESSURE_PLATE"), dsl.pressurePlate(mclName))

        registry.register(enumValueOf("${chunkerPrefix}_SIGN"), dsl.simple("mcl_signs:standing_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_WALL_SIGN"), dsl.directional("mcl_signs:wall_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_HANGING_SIGN"), dsl.simple("mcl_signs:hanging_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_WALL_HANGING_SIGN"), dsl.directional("mcl_signs:hanging_sign_wall_$mclName"))
    }

    private inline fun safeRegister(
        registry: MclMappingRegistry, 
        enumName: String, 
        mapperBuilder: () -> me.voltual.mcl.mapping.BlockMapper
    ) {
        try {
            val enumType = enumValueOf<ChunkerVanillaBlockType>(enumName)
            registry.register(enumType, mapperBuilder())
        } catch (_: IllegalArgumentException) {}
    }
}