package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl

object MclWoodMapping : MclMappingModule {
    override fun register() {
        // 1. 橡木 (Oak) - 基础前缀为 mcl_trees 和 mcl_core
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

        // 7. 红树林 (Mangrove) - 注意：红树林在 Mineclonia 有独立模块前缀
        registerWoodSet("MANGROVE", "mangrove", "mcl_mangrove", hasLeaves = true)

        // 8. 樱花 (Cherry) - 对应 Mineclonia 的 cherry_blossom
        registerWoodSet("CHERRY", "cherry_blossom", "mcl_cherry_blossom", hasLeaves = true)
        
        // 9. 苍白橡木 (Pale Oak)
        registerWoodSet("PALE_OAK", "pale_oak", "mcl_pale_oak", hasLeaves = true)

        // 10. 竹子 (Bamboo) - 特殊处理，因为没有 Log 概念
        registerBamboo()

        // 11. 绯红 (Crimson) & 诡异 (Warped) 下界木材套装注册
        registerNetherWood("CRIMSON", "crimson")
        registerNetherWood("WARPED", "warped")
    }

    /**
     * 注册标准木材套装 (Log, Planks, Stairs, Slabs, Fences, Doors, etc.)
     */
    private fun registerWoodSet(
        chunkerPrefix: String, 
        mclName: String, 
        modName: String, 
        hasLeaves: Boolean
    ) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // 原木 (Log)
        safeRegister(registry, "${chunkerPrefix}_LOG") { dsl.log("$modName:tree_$mclName") }
        // 木块 (Wood / Bark)
        safeRegister(registry, "${chunkerPrefix}_WOOD") { dsl.log("$modName:bark_$mclName") }
        // 去皮原木 (Stripped Log)
        safeRegister(registry, "STRIPPED_${chunkerPrefix}_LOG") { dsl.log("$modName:stripped_$mclName") }
        // 去皮木块 (Stripped Wood)
        safeRegister(registry, "STRIPPED_${chunkerPrefix}_WOOD") { dsl.log("$modName:bark_stripped_$mclName") }

        // 木板 (Planks)
        safeRegister(registry, "${chunkerPrefix}_PLANKS") { dsl.simple("mcl_trees:wood_$mclName") }

        // 楼梯 & 台阶
        safeRegister(registry, "${chunkerPrefix}_STAIRS") { dsl.stair("mcl_stairs:stair_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_SLAB") {
            dsl.slab(
                "mcl_stairs:slab_$mclName", 
                "mcl_stairs:slab_${mclName}_top", 
                "mcl_stairs:slab_${mclName}_double"
            )
        }

        // 栅栏 & 栅栏门
        safeRegister(registry, "${chunkerPrefix}_FENCE") { dsl.simple("mcl_fences:${mclName}_fence") }
        safeRegister(registry, "${chunkerPrefix}_FENCE_GATE") { dsl.gate("mcl_fences:${mclName}_fence_gate") }

        // 门 & 活板门
        safeRegister(registry, "${chunkerPrefix}_DOOR") { dsl.door("mcl_doors:door_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_TRAPDOOR") { dsl.trapdoor("mcl_doors:trapdoor_$mclName") }

        // 按钮 & 压力板
        safeRegister(registry, "${chunkerPrefix}_BUTTON") { dsl.button(mclName) }
        safeRegister(registry, "${chunkerPrefix}_PRESSURE_PLATE") { dsl.pressurePlate(mclName) }

        // 告示牌
        safeRegister(registry, "${chunkerPrefix}_SIGN") { dsl.simple("mcl_signs:standing_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_WALL_SIGN") { dsl.directional("mcl_signs:wall_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_HANGING_SIGN") { dsl.simple("mcl_signs:hanging_sign_$mclName") }
        safeRegister(registry, "${chunkerPrefix}_WALL_HANGING_SIGN") { dsl.directional("mcl_signs:hanging_sign_wall_$mclName") }

        // 树叶 & 树苗
        if (hasLeaves) {
            safeRegister(registry, "${chunkerPrefix}_LEAVES") { dsl.simple("$modName:leaves_$mclName") }
            
            val saplingEnumName = if (chunkerPrefix == "MANGROVE") "MANGROVE_PROPAGULE" else "${chunkerPrefix}_SAPLING"
            safeRegister(registry, saplingEnumName) { dsl.simple("mcl_trees:sapling_$mclName") }
        }
    }

    private fun registerBamboo() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        
        registry.register(ChunkerVanillaBlockType.BAMBOO_PLANKS, dsl.simple("mcl_bamboo:bamboo_plank"))
        registry.register(ChunkerVanillaBlockType.BAMBOO_MOSAIC, dsl.simple("mcl_bamboo:bamboo_plank_mosaic"))
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
        registry.register(ChunkerVanillaBlockType.BAMBOO_WALL_HANGING_SIGN, dsl.directional("mcl_signs:hanging_sign_bamboo_wall"))
    }

    /**
     * 极速精准对齐下界两套木材（Warped 诡异 与 Crimson 绯红）
     */
    private fun registerNetherWood(chunkerPrefix: String, mclName: String) {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val mod = "mcl_trees"

        // 对应真实挖掘日志精准注册
        registry.register(enumValueOf("${chunkerPrefix}_STEM"), dsl.log("$mod:tree_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_HYPHAE"), dsl.log("$mod:wood_$mclName"))
        registry.register(enumValueOf("STRIPPED_${chunkerPrefix}_STEM"), dsl.log("$mod:stripped_$mclName"))
        registry.register(enumValueOf("STRIPPED_${chunkerPrefix}_HYPHAE"), dsl.log("$mod:bark_stripped_$mclName"))
        
        // 特殊：下界木板名在 Mineclonia 中叫 bark_<color>
        registry.register(enumValueOf("${chunkerPrefix}_PLANKS"), dsl.simple("$mod:bark_$mclName"))

        // 其他功能性木质方块
        registry.register(enumValueOf("${chunkerPrefix}_FENCE"), dsl.simple("mcl_fences:${mclName}_fence"))
        registry.register(enumValueOf("${chunkerPrefix}_FENCE_GATE"), dsl.gate("mcl_fences:${mclName}_fence_gate"))
        registry.register(enumValueOf("${chunkerPrefix}_DOOR"), dsl.door("mcl_doors:door_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_TRAPDOOR"), dsl.trapdoor("mcl_doors:trapdoor_$mclName"))
        
        // 楼梯/台阶 (在 Mineclonia 中，诡异木楼梯包含了“木板楼梯”和由“木块”制成的“树皮楼梯”)
        registry.register(enumValueOf("${chunkerPrefix}_STAIRS"), dsl.stair("mcl_stairs:stair_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_SLAB"), dsl.slab(
            "mcl_stairs:slab_$mclName", 
            "mcl_stairs:slab_${mclName}_top", 
            "mcl_stairs:slab_${mclName}_double"
        ))

        // 按钮 & 压力板
        registry.register(enumValueOf("${chunkerPrefix}_BUTTON"), dsl.button(mclName))
        registry.register(enumValueOf("${chunkerPrefix}_PRESSURE_PLATE"), dsl.pressurePlate(mclName))

        // 告示牌与吊挂牌系列（修复 wall 前缀顺序颠倒的缺陷）
        registry.register(enumValueOf("${chunkerPrefix}_SIGN"), dsl.simple("mcl_signs:standing_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_WALL_SIGN"), dsl.directional("mcl_signs:wall_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_HANGING_SIGN"), dsl.simple("mcl_signs:hanging_sign_$mclName"))
        registry.register(enumValueOf("${chunkerPrefix}_WALL_HANGING_SIGN"), dsl.directional("mcl_signs:hanging_sign_wall_$mclName"))
    }

    /**
     * 安全注册辅助方法：捕获由于 Chunker 版本差异导致的不存在的枚举项崩溃
     */
    private inline fun safeRegister(
        registry: MclMappingRegistry, 
        enumName: String, 
        mapperBuilder: () -> me.voltual.mcl.mapping.BlockMapper
    ) {
        try {
            val enumType = enumValueOf<ChunkerVanillaBlockType>(enumName)
            registry.register(enumType, mapperBuilder())
        } catch (_: IllegalArgumentException) {
            // Chunker 库中当前运行版本没有这一项，优雅忽略
        }
    }
}