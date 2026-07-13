package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.core.MclNode

object MclVegetationMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        // ==========================================
        // 1. 基础农业作物 (mcl_farming)
        // ==========================================
        // 小麦
        registry.register(ChunkerVanillaBlockType.WHEAT, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
            val nodeName = if (age == Age_7._7) "mcl_farming:wheat" else "mcl_farming:wheat_${age.ordinal + 1}"
            MclNode(nodeName)
        })

        // 胡萝卜
        registry.register(ChunkerVanillaBlockType.CARROTS, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
            val nodeName = if (age == Age_7._7) "mcl_farming:carrot" else "mcl_farming:carrot_${age.ordinal + 1}"
            MclNode(nodeName)
        })

        // 马铃薯
        registry.register(ChunkerVanillaBlockType.POTATOES, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
            val nodeName = if (age == Age_7._7) "mcl_farming:potato" else "mcl_farming:potato_${age.ordinal + 1}"
            MclNode(nodeName)
        })

        // 甜菜根 (修正: Chunker 源码中名为 BEETROOTS)
        registry.register(ChunkerVanillaBlockType.BEETROOTS, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_3) ?: Age_3._0
            val nodeName = if (age == Age_3._3) "mcl_farming:beetroot" else "mcl_farming:beetroot_${age.ordinal}"
            MclNode(nodeName)
        })

        // 甜浆果
        registry.register(ChunkerVanillaBlockType.SWEET_BERRY_BUSH, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_3) ?: Age_3._0
            MclNode("mcl_farming:sweet_berry_bush_${age.ordinal}")
        })

        // 可可豆
        registry.register(ChunkerVanillaBlockType.COCOA, BlockMapper { id ->
            val age = id.getState(VanillaBlockStates.AGE_2) ?: Age_2._0
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            val param2 = when (facing) {
                FacingDirectionHorizontal.NORTH -> 0
                FacingDirectionHorizontal.EAST -> 1
                FacingDirectionHorizontal.SOUTH -> 2
                FacingDirectionHorizontal.WEST -> 3
            }.toByte()
            MclNode("mcl_cocoas:cocoa_${age.ordinal + 1}", param2 = param2)
        })

        // 瓜藤系列
        registry.register(ChunkerVanillaBlockType.PUMPKIN_STEM, dsl.simple("mcl_farming:pumpkintige_unconnect"))
        registry.register(ChunkerVanillaBlockType.ATTACHED_PUMPKIN_STEM, dsl.simple("mcl_farming:pumpkintige_unconnect"))
        registry.register(ChunkerVanillaBlockType.MELON_STEM, dsl.simple("mcl_farming:melontige_unconnect"))
        registry.register(ChunkerVanillaBlockType.ATTACHED_MELON_STEM, dsl.simple("mcl_farming:melontige_unconnect"))

        // ==========================================
        // 2. 天然花卉与地表装饰 (mcl_flowers)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.DANDELION, dsl.simple("mcl_flowers:dandelion"))
        registry.register(ChunkerVanillaBlockType.POPPY, dsl.simple("mcl_flowers:poppy"))
        registry.register(ChunkerVanillaBlockType.BLUE_ORCHID, dsl.simple("mcl_flowers:blue_orchid"))
        registry.register(ChunkerVanillaBlockType.ALLIUM, dsl.simple("mcl_flowers:allium"))
        registry.register(ChunkerVanillaBlockType.AZURE_BLUET, dsl.simple("mcl_flowers:azure_bluet"))
        registry.register(ChunkerVanillaBlockType.OXEYE_DAISY, dsl.simple("mcl_flowers:oxeye_daisy"))
        registry.register(ChunkerVanillaBlockType.CORNFLOWER, dsl.simple("mcl_flowers:cornflower"))
        registry.register(ChunkerVanillaBlockType.WITHER_ROSE, dsl.simple("mcl_flowers:wither_rose"))
        registry.register(ChunkerVanillaBlockType.LILY_OF_THE_VALLEY, dsl.simple("mcl_flowers:lily_of_the_valley"))

        registry.register(ChunkerVanillaBlockType.RED_TULIP, dsl.simple("mcl_flowers:tulip_red"))
        registry.register(ChunkerVanillaBlockType.ORANGE_TULIP, dsl.simple("mcl_flowers:tulip_orange"))
        registry.register(ChunkerVanillaBlockType.WHITE_TULIP, dsl.simple("mcl_flowers:tulip_white"))
        registry.register(ChunkerVanillaBlockType.PINK_TULIP, dsl.simple("mcl_flowers:tulip_pink"))

        registry.register(ChunkerVanillaBlockType.SUNFLOWER, dsl.doublePlant("sunflower"))
        registry.register(ChunkerVanillaBlockType.LILAC, dsl.doublePlant("lilac"))
        registry.register(ChunkerVanillaBlockType.ROSE_BUSH, dsl.doublePlant("rose_bush"))
        registry.register(ChunkerVanillaBlockType.PEONY, dsl.doublePlant("peony"))
        registry.register(ChunkerVanillaBlockType.TALL_GRASS, dsl.doublePlant("double_grass"))
        registry.register(ChunkerVanillaBlockType.LARGE_FERN, dsl.doublePlant("double_fern"))

        registry.register(ChunkerVanillaBlockType.SHORT_GRASS, dsl.simple("mcl_flowers:tallgrass"))
        registry.register(ChunkerVanillaBlockType.FERN, dsl.simple("mcl_flowers:fern"))
        registry.register(ChunkerVanillaBlockType.DEAD_BUSH, dsl.simple("mcl_core:deadbush"))
        registry.register(ChunkerVanillaBlockType.LILY_PAD, dsl.simple("mcl_flowers:waterlily"))
        
        registry.register(ChunkerVanillaBlockType.PINK_PETALS, BlockMapper { id ->
            val count = id.getState(VanillaBlockStates.FLOWER_AMOUNT) ?: Flowers._1
            val num = count.ordinal + 1
            MclNode("mcl_flowers:pink_petals_$num")
        })

        // ==========================================
        // 3. 洞穴与奇幻生态 (mcl_lush_caves & mcl_crimson)
        // ==========================================
        registry.register(ChunkerVanillaBlockType.MOSS_BLOCK, dsl.simple("mcl_lush_caves:moss"))
        registry.register(ChunkerVanillaBlockType.MOSS_CARPET, dsl.simple("mcl_lush_caves:moss_carpet"))
        registry.register(ChunkerVanillaBlockType.AZALEA, dsl.simple("mcl_lush_caves:azalea"))
        registry.register(ChunkerVanillaBlockType.FLOWERING_AZALEA, dsl.simple("mcl_lush_caves:azalea_flowering"))
        registry.register(ChunkerVanillaBlockType.ROOTED_DIRT, dsl.simple("mcl_lush_caves:rooted_dirt"))
        registry.register(ChunkerVanillaBlockType.HANGING_ROOTS, dsl.simple("mcl_lush_caves:hanging_roots"))

        registry.register(ChunkerVanillaBlockType.SPORE_BLOSSOM, dsl.simple("mcl_lush_caves:spore_blossom"))
        
        // 修正: 映射具体的 Body 和 Head 到 Mineclonia 的 cave_vines
        registry.register(ChunkerVanillaBlockType.CAVE_VINES_BODY, BlockMapper { id ->
            val berries = id.getState(VanillaBlockStates.BERRIES) == Bool.TRUE
            MclNode(if (berries) "mcl_lush_caves:cave_vines_lit" else "mcl_lush_caves:cave_vines")
        })
        registry.register(ChunkerVanillaBlockType.CAVE_VINES_HEAD, BlockMapper { id ->
            val berries = id.getState(VanillaBlockStates.BERRIES) == Bool.TRUE
            MclNode(if (berries) "mcl_lush_caves:cave_vines_lit" else "mcl_lush_caves:cave_vines")
        })

        registry.register(ChunkerVanillaBlockType.BIG_DRIPLEAF, dsl.bigDripleaf())
        registry.register(ChunkerVanillaBlockType.BIG_DRIPLEAF_STEM, dsl.simple("mcl_lush_caves:dripleaf_big_stem"))
        registry.register(ChunkerVanillaBlockType.SMALL_DRIPLEAF, dsl.smallDripleaf())

        registry.register(ChunkerVanillaBlockType.CRIMSON_FUNGUS, dsl.simple("mcl_crimson:crimson_fungus"))
        registry.register(ChunkerVanillaBlockType.WARPED_FUNGUS, dsl.simple("mcl_crimson:warped_fungus"))
        registry.register(ChunkerVanillaBlockType.CRIMSON_ROOTS, dsl.simple("mcl_crimson:crimson_roots"))
        registry.register(ChunkerVanillaBlockType.WARPED_ROOTS, dsl.simple("mcl_crimson:warped_roots"))
        registry.register(ChunkerVanillaBlockType.NETHER_SPROUTS, dsl.simple("mcl_crimson:nether_sprouts"))
        registry.register(ChunkerVanillaBlockType.SHROOMLIGHT, dsl.simple("mcl_crimson:shroomlight"))
        
        registry.register(ChunkerVanillaBlockType.WEEPING_VINES, dsl.simple("mcl_crimson:weeping_vines"))
        registry.register(ChunkerVanillaBlockType.WEEPING_VINES_PLANT, dsl.simple("mcl_crimson:weeping_vines"))
        registry.register(ChunkerVanillaBlockType.TWISTING_VINES, dsl.simple("mcl_crimson:twisting_vines"))
        registry.register(ChunkerVanillaBlockType.TWISTING_VINES_PLANT, dsl.simple("mcl_crimson:twisting_vines"))
                
        // 巨型蘑菇块与蘑菇柄
        registry.register(ChunkerVanillaBlockType.BROWN_MUSHROOM_BLOCK, dsl.mushroomBlock("brown"))
        registry.register(ChunkerVanillaBlockType.RED_MUSHROOM_BLOCK, dsl.mushroomBlock("red"))
        registry.register(ChunkerVanillaBlockType.MUSHROOM_STEM, dsl.mushroomStem())
        
        registry.register(ChunkerVanillaBlockType.AZALEA_LEAVES, dsl.simple("mcl_trees:leaves_azalea"))
        registry.register(ChunkerVanillaBlockType.FLOWERING_AZALEA_LEAVES, dsl.simple("mcl_trees:leaves_azalea_flowering"))
        
        registry.register(ChunkerVanillaBlockType.BAMBOO_SAPLING, dsl.simple("mcl_bamboo:bamboo_shoot"))

// 竹子主干 (Bamboo) -> 根据 leaves 状态动态映射
registry.register(ChunkerVanillaBlockType.BAMBOO, BlockMapper { id ->
    val leaves = id.getState(VanillaBlockStates.BAMBOO_LEAVES) ?: BambooLeafSize.NONE
    val age = id.getState(VanillaBlockStates.AGE_1) ?: Age_1._0

    // 根据 Minecraft 的 age 区分粗细 (Mineclonia: big 或 small)
    val size = if (age == Age_1._1) "big" else "small"

    val nodeName = when (leaves) {
        BambooLeafSize.NONE -> "mcl_bamboo:bamboo_$size"
        BambooLeafSize.SMALL -> "mcl_bamboo:bamboo_${size}_leafsmall"
        BambooLeafSize.LARGE -> "mcl_bamboo:bamboo_${size}_leafbig"
    }

    // Mineclonia 的竹子需要 param2（1到4的随机朝向以打破单一视觉，默认为 0 保证稳定）
    MclNode(nodeName, param2 = 0)
})

// 仙人掌 -> 映射到 mcl_core:cactus
registry.register(ChunkerVanillaBlockType.CACTUS, dsl.simple("mcl_core:cactus"))

// 甘蔗 -> 映射到 mcl_core:reeds
registry.register(ChunkerVanillaBlockType.SUGAR_CANE, dsl.simple("mcl_core:reeds"))

        // 1. 蜂箱与蜂巢动态映射 (Beehive & Bee Nest)
        registry.register(ChunkerVanillaBlockType.BEEHIVE, BlockMapper { id ->
            val honeyLevel = id.getState(VanillaBlockStates.HONEY_LEVEL) ?: HoneyLevel._0
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            
            val nodeName = if (honeyLevel == HoneyLevel._0) "mcl_beehives:beehive" else "mcl_beehives:beehive_${honeyLevel.ordinal}"
            val param2 = when (facing) {
                FacingDirectionHorizontal.SOUTH -> 0
                FacingDirectionHorizontal.WEST -> 1
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 3
            }.toByte()
            MclNode(nodeName, param2 = param2)
        })

        registry.register(ChunkerVanillaBlockType.BEE_NEST, BlockMapper { id ->
            val honeyLevel = id.getState(VanillaBlockStates.HONEY_LEVEL) ?: HoneyLevel._0
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            
            val nodeName = if (honeyLevel == HoneyLevel._0) "mcl_beehives:bee_nest" else "mcl_beehives:bee_nest_${honeyLevel.ordinal}"
            val param2 = when (facing) {
                FacingDirectionHorizontal.SOUTH -> 0
                FacingDirectionHorizontal.WEST -> 1
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 3
            }.toByte()
            MclNode(nodeName, param2 = param2)
        })

        // 2. 地表普通小蘑菇
        registry.register(ChunkerVanillaBlockType.BROWN_MUSHROOM, dsl.simple("mcl_mushrooms:mushroom_brown"))
        registry.register(ChunkerVanillaBlockType.RED_MUSHROOM, dsl.simple("mcl_mushrooms:mushroom_red"))
    }
}