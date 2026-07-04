package me.voltual.mcl.mapping

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerItemStackIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.item.ChunkerVanillaItemType
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack
import me.voltual.mcl.core.MclItemStack
import me.voltual.mcl.mapping.MclMappingRegistry

/**
 * 物品转换注册表：负责将 Minecraft 物品标识符映射到 Mineclonia 物品名
 */
object MclItemRegistry {

    /**
     * 获取 Mineclonia 的物品全名 (mod:item)
     */
    fun getItemName(identifier: ChunkerItemStackIdentifier): String {
        val type = identifier.itemStackType
        
        // 1. 如果该物品本质上是一个方块 (BlockItem)
        if (type is ChunkerVanillaBlockType) {
            val blockId = com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier(type)
            return MclMappingRegistry.convert(blockId).name
        }

        // 2. 如果是纯物品 (Item)
        if (type is ChunkerVanillaItemType) {
            return itemMapping[type] ?: run {
                System.err.println("\u001B[31m[Item Debug] Missing explicit mapping for: $type\u001B[0m")
                "mcl_core:cobble"
            }
        }

        return "mcl_core:cobble"
    }

    /**
     * 将 Chunker 的物品栈转换为 Mineclonia 格式
     */
    fun fromChunker(itemStack: ChunkerItemStack?): MclItemStack {
        if (itemStack == null || itemStack.identifier.isAir) {
            return MclItemStack("", 0)
        }
        
        val name = getItemName(itemStack.identifier)
        val count = itemStack.get(ChunkerItemProperty.AMOUNT) ?: 1
        
        val mcDamage = itemStack.get(ChunkerItemProperty.DURABILITY) ?: 0
        var mtWear = 0
        if (mcDamage > 0) {
            val maxDurability = getVanillaMaxDurability(itemStack.identifier)
            mtWear = (mcDamage.toDouble() / maxDurability * 65535).toInt().coerceIn(0, 65535)
        }

        return MclItemStack(name, count, mtWear)
    }

    // ==========================================
    // 物品映射字典 (根据 Chunker 源码修正)
    // ==========================================
    private val itemMapping = mutableMapOf<ChunkerVanillaItemType, String>().apply {
        // 核心材料 (mcl_core)
        put(ChunkerVanillaItemType.STICK, "mcl_core:stick")
        put(ChunkerVanillaItemType.PAPER, "mcl_core:paper")
        put(ChunkerVanillaItemType.COAL, "mcl_core:coal_lump")
        put(ChunkerVanillaItemType.CHARCOAL, "mcl_core:charcoal_lump")
        put(ChunkerVanillaItemType.IRON_INGOT, "mcl_core:iron_ingot")
        put(ChunkerVanillaItemType.GOLD_INGOT, "mcl_core:gold_ingot")
        put(ChunkerVanillaItemType.IRON_NUGGET, "mcl_core:iron_nugget")
        put(ChunkerVanillaItemType.GOLD_NUGGET, "mcl_core:gold_nugget")
        put(ChunkerVanillaItemType.DIAMOND, "mcl_core:diamond")
        put(ChunkerVanillaItemType.EMERALD, "mcl_core:emerald")
        put(ChunkerVanillaItemType.LAPIS_LAZULI, "mcl_core:lapis") // Chunker 名是 LAPIS_LAZULI
        put(ChunkerVanillaItemType.QUARTZ, "mcl_nether:quartz")      // 修正: 对应 Chunker 的 QUARTZ
        put(ChunkerVanillaItemType.AMETHYST_SHARD, "mcl_amethyst:amethyst_shard")
        put(ChunkerVanillaItemType.RAW_IRON, "mcl_raw_ores:raw_iron")
        put(ChunkerVanillaItemType.RAW_GOLD, "mcl_raw_ores:raw_gold")
        put(ChunkerVanillaItemType.RAW_COPPER, "mcl_raw_ores:raw_copper")
        put(ChunkerVanillaItemType.COPPER_INGOT, "mcl_copper:copper_ingot")
        put(ChunkerVanillaItemType.NETHERITE_INGOT, "mcl_nether:netherite_ingot")
        put(ChunkerVanillaItemType.NETHERITE_SCRAP, "mcl_nether:netherite_scrap")
        put(ChunkerVanillaItemType.FLINT, "mcl_core:flint")
        put(ChunkerVanillaItemType.SUGAR, "mcl_core:sugar")
        put(ChunkerVanillaItemType.BOWL, "mcl_core:bowl")
        put(ChunkerVanillaItemType.BRICK, "mcl_core:brick")
        put(ChunkerVanillaItemType.CLAY_BALL, "mcl_core:clay_lump")

        // 基础食物
        put(ChunkerVanillaItemType.APPLE, "mcl_core:apple")
        put(ChunkerVanillaItemType.GOLDEN_APPLE, "mcl_core:apple_gold")
        put(ChunkerVanillaItemType.ENCHANTED_GOLDEN_APPLE, "mcl_core:apple_gold_enchanted")
        put(ChunkerVanillaItemType.BREAD, "mcl_farming:bread")
        put(ChunkerVanillaItemType.COOKIE, "mcl_farming:cookie")

        // 农产品
        put(ChunkerVanillaItemType.WHEAT, "mcl_farming:wheat_item")
        put(ChunkerVanillaItemType.WHEAT_SEEDS, "mcl_farming:wheat_seeds")
        put(ChunkerVanillaItemType.CARROT, "mcl_farming:carrot_item")
        put(ChunkerVanillaItemType.GOLDEN_CARROT, "mcl_farming:carrot_item_gold")
        put(ChunkerVanillaItemType.POTATO, "mcl_farming:potato_item")
        put(ChunkerVanillaItemType.BAKED_POTATO, "mcl_farming:potato_item_baked")
        put(ChunkerVanillaItemType.POISONOUS_POTATO, "mcl_farming:potato_item_poison")
        put(ChunkerVanillaItemType.BEETROOT, "mcl_farming:beetroot_item")
        put(ChunkerVanillaItemType.BEETROOT_SEEDS, "mcl_farming:beetroot_seeds")
        put(ChunkerVanillaItemType.MELON_SLICE, "mcl_farming:melon_item")
        put(ChunkerVanillaItemType.MELON_SEEDS, "mcl_farming:melon_seeds")
        put(ChunkerVanillaItemType.PUMPKIN_SEEDS, "mcl_farming:pumpkin_seeds")
        put(ChunkerVanillaItemType.SWEET_BERRIES, "mcl_farming:sweet_berry")
        put(ChunkerVanillaItemType.COCOA_BEANS, "mcl_cocoas:cocoa_beans")

        // 生物掉落
        put(ChunkerVanillaItemType.ROTTEN_FLESH, "mcl_mobitems:rotten_flesh")
        put(ChunkerVanillaItemType.BEEF, "mcl_mobitems:beef")
        put(ChunkerVanillaItemType.COOKED_BEEF, "mcl_mobitems:cooked_beef")
        put(ChunkerVanillaItemType.MUTTON, "mcl_mobitems:mutton")
        put(ChunkerVanillaItemType.COOKED_MUTTON, "mcl_mobitems:cooked_mutton")
        put(ChunkerVanillaItemType.CHICKEN, "mcl_mobitems:chicken")
        put(ChunkerVanillaItemType.COOKED_CHICKEN, "mcl_mobitems:cooked_chicken")
        put(ChunkerVanillaItemType.PORKCHOP, "mcl_mobitems:porkchop")
        put(ChunkerVanillaItemType.COOKED_PORKCHOP, "mcl_mobitems:cooked_porkchop")
        put(ChunkerVanillaItemType.RABBIT, "mcl_mobitems:rabbit")
        put(ChunkerVanillaItemType.COOKED_RABBIT, "mcl_mobitems:cooked_rabbit")
        put(ChunkerVanillaItemType.LEATHER, "mcl_mobitems:leather")
        put(ChunkerVanillaItemType.FEATHER, "mcl_mobitems:feather")
        put(ChunkerVanillaItemType.BONE, "mcl_mobitems:bone")
        put(ChunkerVanillaItemType.STRING, "mcl_mobitems:string")
        put(ChunkerVanillaItemType.INK_SAC, "mcl_mobitems:ink_sac")
        put(ChunkerVanillaItemType.GLOW_INK_SAC, "mcl_mobitems:glow_ink_sac")
        put(ChunkerVanillaItemType.BLAZE_ROD, "mcl_mobitems:blaze_rod")
        put(ChunkerVanillaItemType.BLAZE_POWDER, "mcl_mobitems:blaze_powder")
        put(ChunkerVanillaItemType.MAGMA_CREAM, "mcl_mobitems:magma_cream")
        put(ChunkerVanillaItemType.GHAST_TEAR, "mcl_mobitems:ghast_tear")
        put(ChunkerVanillaItemType.NETHER_STAR, "mcl_mobitems:nether_star")
        put(ChunkerVanillaItemType.SLIME_BALL, "mcl_mobitems:slimeball")
        put(ChunkerVanillaItemType.SHULKER_SHELL, "mcl_mobitems:shulker_shell")
        put(ChunkerVanillaItemType.SADDLE, "mcl_mobitems:saddle")
        put(ChunkerVanillaItemType.NAME_TAG, "mcl_mobitems:nametag")
        put(ChunkerVanillaItemType.GUNPOWDER, "mcl_mobitems:gunpowder")
        put(ChunkerVanillaItemType.RABBIT_HIDE, "mcl_mobitems:rabbit_hide")
        put(ChunkerVanillaItemType.RABBIT_FOOT, "mcl_mobitems:rabbit_foot")
        put(ChunkerVanillaItemType.NAUTILUS_SHELL, "mcl_mobitems:nautilus_shell")
        put(ChunkerVanillaItemType.HEART_OF_THE_SEA, "mcl_mobitems:heart_of_the_sea")

        // 药水工具
        put(ChunkerVanillaItemType.GLASS_BOTTLE, "mcl_potions:glass_bottle")
        put(ChunkerVanillaItemType.DRAGON_BREATH, "mcl_potions:dragon_breath")
        put(ChunkerVanillaItemType.FERMENTED_SPIDER_EYE, "mcl_potions:fermented_spider_eye")

        // 工具系列 (逻辑函数)
        registerToolSet("WOODEN", "wood")
        registerToolSet("STONE", "stone")
        registerToolSet("IRON", "iron")
        registerToolSet("GOLDEN", "gold")
        registerToolSet("DIAMOND", "diamond")
        registerToolSet("NETHERITE", "netherite")
        registerToolSet("COPPER", "copper")
        put(ChunkerVanillaItemType.SHEARS, "mcl_tools:shears")
        put(ChunkerVanillaItemType.FISHING_ROD, "mcl_fishing:fishing_rod")
        put(ChunkerVanillaItemType.FLINT_AND_STEEL, "mcl_fire:flint_and_steel")
        put(ChunkerVanillaItemType.MACE, "mcl_tools:mace")

        // 护甲系列
        registerArmorSet("LEATHER", "leather")
        registerArmorSet("CHAINMAIL", "chain")
        registerArmorSet("IRON", "iron")
        registerArmorSet("GOLDEN", "gold")
        registerArmorSet("DIAMOND", "diamond")
        registerArmorSet("NETHERITE", "netherite")
        registerArmorSet("COPPER", "copper")
        put(ChunkerVanillaItemType.ELYTRA, "mcl_armor:elytra")
        put(ChunkerVanillaItemType.SHIELD, "mcl_shields:shield")

        // 交通与船
        put(ChunkerVanillaItemType.MINECART, "mcl_minecarts:minecart")
        put(ChunkerVanillaItemType.CHEST_MINECART, "mcl_minecarts:chest_minecart")
        put(ChunkerVanillaItemType.FURNACE_MINECART, "mcl_minecarts:furnace_minecart")
        put(ChunkerVanillaItemType.TNT_MINECART, "mcl_minecarts:tnt_minecart")
        put(ChunkerVanillaItemType.HOPPER_MINECART, "mcl_minecarts:hopper_minecart")
        
        put(ChunkerVanillaItemType.OAK_BOAT, "mcl_boats:boat_oak")
        put(ChunkerVanillaItemType.SPRUCE_BOAT, "mcl_boats:boat_spruce")
        put(ChunkerVanillaItemType.BIRCH_BOAT, "mcl_boats:boat_birch")
        put(ChunkerVanillaItemType.JUNGLE_BOAT, "mcl_boats:boat_jungle")
        put(ChunkerVanillaItemType.ACACIA_BOAT, "mcl_boats:boat_acacia")
        put(ChunkerVanillaItemType.DARK_OAK_BOAT, "mcl_boats:boat_dark_oak")
        put(ChunkerVanillaItemType.MANGROVE_BOAT, "mcl_boats:boat_mangrove")
        put(ChunkerVanillaItemType.CHERRY_BOAT, "mcl_boats:boat_cherry_blossom")
        put(ChunkerVanillaItemType.BAMBOO_RAFT, "mcl_boats:boat_bamboo")

        // 染料
        registerDyes()
    }

    private fun MutableMap<ChunkerVanillaItemType, String>.registerToolSet(prefix: String, mcl: String) {
        put(enumValueOf("${prefix}_SWORD"), "mcl_tools:sword_$mcl")
        put(enumValueOf("${prefix}_PICKAXE"), "mcl_tools:pick_$mcl")
        put(enumValueOf("${prefix}_AXE"), "mcl_tools:axe_$mcl")
        put(enumValueOf("${prefix}_SHOVEL"), "mcl_tools:shovel_$mcl")
        put(enumValueOf("${prefix}_HOE"), "mcl_farming:hoe_$mcl")
    }

    private fun MutableMap<ChunkerVanillaItemType, String>.registerArmorSet(prefix: String, mcl: String) {
        put(enumValueOf("${prefix}_HELMET"), "mcl_armor:helmet_$mcl")
        put(enumValueOf("${prefix}_CHESTPLATE"), "mcl_armor:chestplate_$mcl")
        put(enumValueOf("${prefix}_LEGGINGS"), "mcl_armor:leggings_$mcl")
        put(enumValueOf("${prefix}_BOOTS"), "mcl_armor:boots_$mcl")
    }

    private fun MutableMap<ChunkerVanillaItemType, String>.registerDyes() {
        val dyes = listOf(
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
        )
        for (d in dyes) {
            put(enumValueOf("${d}_DYE"), "mcl_dyes:${d.lowercase()}")
        }
    }

    /**
     * 获取 Minecraft 原始耐用度
     */
    private fun getVanillaMaxDurability(identifier: ChunkerItemStackIdentifier): Int {
        val type = identifier.itemStackType
        if (type is ChunkerVanillaItemType) {
            val name = type.name
            return when {
                name.contains("NETHERITE") -> 2031
                name.contains("DIAMOND") -> 1561
                name.contains("IRON") -> 250
                name.contains("GOLD") -> 32
                name.contains("STONE") -> 131
                name.contains("WOOD") -> 59
                name.contains("ELYTRA") -> 432
                name.contains("SHIELD") -> 336
                name.contains("FISHING_ROD") -> 64
                name.contains("FLINT_AND_STEEL") -> 64
                name.contains("SHEARS") -> 238
                name.contains("BOW") -> 384
                name.contains("CROSSBOW") -> 326
                name.contains("TRIDENT") -> 250
                else -> 100
            }
        }
        return 100
    }
}