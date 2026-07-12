package me.voltual.mcl.mapping

import me.voltual.mcl.core.MclBlockEntityData
import me.voltual.mcl.core.MclInventory
import me.voltual.mcl.core.MclItemStack
import me.voltual.mcl.mapping.MclItemRegistry

import com.google.gson.JsonElement
import com.hivemc.chunker.conversion.intermediate.column.blockentity.*
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.FurnaceBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.ChestBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.TrappedChestBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.ShulkerBoxBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.sign.SignBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.banner.ChunkerBannerPattern
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerDyeColor
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerVanillaEntityType

object MclBlockEntityRegistry {
    private val converters = mutableMapOf<Class<out BlockEntity>, (BlockEntity) -> MclBlockEntityData>()

    private val entityTypeToMcl = mapOf(
        ChunkerVanillaEntityType.ZOMBIE to "mobs_mc:zombie",
        ChunkerVanillaEntityType.PIGLIN to "mobs_mc:piglin",
        ChunkerVanillaEntityType.PIGLIN_BRUTE to "mobs_mc:piglin_brute",
        ChunkerVanillaEntityType.ZOMBIFIED_PIGLIN to "mobs_mc:zombified_piglin",
        ChunkerVanillaEntityType.WOLF to "mobs_mc:wolf",
        ChunkerVanillaEntityType.SKELETON to "mobs_mc:skeleton",
        ChunkerVanillaEntityType.WITHER_SKELETON to "mobs_mc:witherskeleton",
        ChunkerVanillaEntityType.STRAY to "mobs_mc:stray",
        ChunkerVanillaEntityType.SPIDER to "mobs_mc:spider",
        ChunkerVanillaEntityType.CAVE_SPIDER to "mobs_mc:cave_spider",
        ChunkerVanillaEntityType.CREEPER to "mobs_mc:creeper",
        ChunkerVanillaEntityType.WITCH to "mobs_mc:witch",
        ChunkerVanillaEntityType.BLAZE to "mobs_mc:blaze",
        ChunkerVanillaEntityType.GHAST to "mobs_mc:ghast",
        ChunkerVanillaEntityType.ENDERMAN to "mobs_mc:enderman",
        ChunkerVanillaEntityType.ENDERMITE to "mobs_mc:endermite",
        ChunkerVanillaEntityType.SHULKER to "mobs_mc:shulker",
        ChunkerVanillaEntityType.SILVERFISH to "mobs_mc:silverfish",
        ChunkerVanillaEntityType.VEX to "mobs_mc:vex",
        ChunkerVanillaEntityType.EVOKER to "mobs_mc:evoker",
        ChunkerVanillaEntityType.ILLUSIONER to "mobs_mc:illusioner",
        ChunkerVanillaEntityType.VINDICATOR to "mobs_mc:vindicator",
        ChunkerVanillaEntityType.RAVAGER to "mobs_mc:ravager",
        ChunkerVanillaEntityType.COW to "mobs_mc:cow",
        ChunkerVanillaEntityType.PIG to "mobs_mc:pig",
        ChunkerVanillaEntityType.SHEEP to "mobs_mc:sheep",
        ChunkerVanillaEntityType.CHICKEN to "mobs_mc:chicken",
        ChunkerVanillaEntityType.RABBIT to "mobs_mc:rabbit",
        ChunkerVanillaEntityType.POLAR_BEAR to "mobs_mc:polar_bear",
        ChunkerVanillaEntityType.LLAMA to "mobs_mc:llama",
        ChunkerVanillaEntityType.TRADER_LLAMA to "mobs_mc:trader_llama",
        ChunkerVanillaEntityType.DONKEY to "mobs_mc:donkey",
        ChunkerVanillaEntityType.MULE to "mobs_mc:mule",
        ChunkerVanillaEntityType.HORSE to "mobs_mc:horse",
        ChunkerVanillaEntityType.SKELETON_HORSE to "mobs_mc:skeleton_horse",
        ChunkerVanillaEntityType.ZOMBIE_HORSE to "mobs_mc:zombie_horse",
        ChunkerVanillaEntityType.BAT to "mobs_mc:bat",
        ChunkerVanillaEntityType.PARROT to "mobs_mc:parrot",
        ChunkerVanillaEntityType.OCELOT to "mobs_mc:ocelot",
        ChunkerVanillaEntityType.CAT to "mobs_mc:cat",
        ChunkerVanillaEntityType.SQUID to "mobs_mc:squid",
        ChunkerVanillaEntityType.GLOW_SQUID to "mobs_mc:glow_squid",
        ChunkerVanillaEntityType.COD to "mobs_mc:cod",
        ChunkerVanillaEntityType.SALMON to "mobs_mc:salmon",
        ChunkerVanillaEntityType.PUFFERFISH to "mobs_mc:pufferfish",
        ChunkerVanillaEntityType.TROPICAL_FISH to "mobs_mc:tropical_fish",
        ChunkerVanillaEntityType.AXOLOTL to "mobs_mc:axolotl",
        ChunkerVanillaEntityType.WANDERING_TRADER to "mobs_mc:wandering_trader",
        ChunkerVanillaEntityType.VILLAGER to "mobs_mc:villager",
        ChunkerVanillaEntityType.ZOMBIE_VILLAGER to "mobs_mc:villager_zombie",
        ChunkerVanillaEntityType.WITHER to "mobs_mc:wither",
        ChunkerVanillaEntityType.ENDER_DRAGON to "mobs_mc:ender_dragon"
    )

    init {
        register(ChestBlockEntity::class.java) { be -> convertChest(be) }
        register(TrappedChestBlockEntity::class.java) { be -> convertChest(be) }
        register(ShulkerBoxBlockEntity::class.java) { be -> convertChest(be) }

        register(FurnaceBlockEntity::class.java) { be -> convertFurnace(be as FurnaceBlockEntity) }
        register(SignBlockEntity::class.java) { be -> convertSign(be as SignBlockEntity) }
        register(JukeboxBlockEntity::class.java) { be -> convertJukebox(be as JukeboxBlockEntity) }
        register(SpawnerBlockEntity::class.java) { be -> convertSpawner(be as SpawnerBlockEntity) }
        register(LecternBlockEntity::class.java) { be -> convertLectern(be as LecternBlockEntity) }
        register(BannerBlockEntity::class.java) { be -> convertBanner(be as BannerBlockEntity) }
    }

    fun <T : BlockEntity> register(clazz: Class<T>, converter: (BlockEntity) -> MclBlockEntityData) {
        converters[clazz] = converter
    }

    fun convert(blockEntity: BlockEntity): MclBlockEntityData? {
        val converter = converters[blockEntity::class.java] ?: return null
        return converter(blockEntity)
    }

    private fun convertChest(be: BlockEntity): MclBlockEntityData {
        val size = 27 
        val items = MutableList(size) { MclItemStack("", 0) }

        val chestItems = when (be) {
            is ChestBlockEntity -> be.items
            is TrappedChestBlockEntity -> be.items
            is ShulkerBoxBlockEntity -> be.items
            else -> emptyMap()
        }

        for ((slotByte, chunkerItem) in chestItems) {
            val slot = slotByte.toInt()
            if (slot in 0 until size) {
                items[slot] = MclItemRegistry.fromChunker(chunkerItem)
            }
        }

        return MclBlockEntityData(
            fields = mapOf(
                "infotext" to "Container",
                "formspec" to "size[11.75,10.425]list[context;main;0.375,0.75;9,3;]list[current_player;main;0.375,5.1;9,3;9]list[current_player;main;0.375,9.05;9,1;]"
            ),
            inventories = mapOf("main" to MclInventory(9, items))
        )
    }

    private fun convertFurnace(furnace: FurnaceBlockEntity): MclBlockEntityData {
        val srcItem = MclItemRegistry.fromChunker(furnace.items[0])
        val fuelItem = mclItemFromChunkerOrEmpty(furnace.items[1])
        val dstItem = mclItemFromChunkerOrEmpty(furnace.items[2])

        return MclBlockEntityData(
            fields = mapOf(
                "infotext" to if (furnace.burnTime > 0) "Furnace (active)" else "Furnace out of fuel",
                "src_totaltime" to furnace.cookTimeTotal.toString(),
                "src_time" to furnace.cookTime.toString(),
                "fuel_totaltime" to furnace.burnTime.toString(),
                "fuel_time" to "0"
            ),
            inventories = mapOf(
                "src" to MclInventory(1, listOf(srcItem)),
                "fuel" to MclInventory(1, listOf(fuelItem)),
                "dst" to MclInventory(1, listOf(dstItem))
            )
        )
    }

    private fun convertSign(sign: SignBlockEntity): MclBlockEntityData {
        val textBuilder = StringBuilder()
        for (lineElement in sign.front.lines) {
            val lineText = extractTextFromJson(lineElement)
            if (lineText.isNotEmpty()) textBuilder.append(lineText).append("\n")
        }
        val text = textBuilder.toString().trim()

        return MclBlockEntityData(
            fields = mapOf(
                "text" to text,
                "infotext" to "\"$text\"",
                "formspec" to "field[text;;${text}]"
            )
        )
    }

    private fun convertJukebox(jukebox: JukeboxBlockEntity): MclBlockEntityData {
        val record = jukebox.record
        val fields = mutableMapOf("infotext" to "Jukebox")
        val inventories = mutableMapOf<String, MclInventory>()

        if (record != null && !record.identifier.isAir) {
            val mclRecord = MclItemRegistry.fromChunker(record)
            fields["infotext"] = "Jukebox (Playing: ${mclRecord.name})"
            inventories["music"] = MclInventory(1, listOf(mclRecord))
        }

        return MclBlockEntityData(fields, inventories)
    }

    private fun convertSpawner(spawner: SpawnerBlockEntity): MclBlockEntityData {
        val entityType = spawner.entityType
        val mclMobName = entityTypeToMcl[entityType] ?: "mobs_mc:pig"

        return MclBlockEntityData(
            fields = mapOf(
                "Mob" to mclMobName,
                "MaxMobsInArea" to "4",
                "PlayerDistance" to "15",
                "infotext" to "Monster Spawner ($mclMobName)",
                "has_timer" to "true",
                "timer_delay" to "2"
            )
        )
    }

    private fun convertLectern(lectern: LecternBlockEntity): MclBlockEntityData {
        val fields = mutableMapOf<String, String>()
        val book = lectern.book
        
        if (book != null && !book.identifier.isAir) {
            val mclBook = MclItemRegistry.fromChunker(book)
            fields["book_item"] = "${mclBook.name} ${mclBook.count} ${mclBook.wear}"
            
            fields["page"] = (lectern.page + 1).toString()
            fields["pages"] = "15" 
            fields["infotext"] = "Lectern with book"
        }

        return MclBlockEntityData(fields = fields)
    }

    private fun convertBanner(banner: BannerBlockEntity): MclBlockEntityData {
        val fields = mutableMapOf<String, String>()
        val inventories = mutableMapOf<String, MclInventory>()

        val blockType = banner.type
        val blockName = if (blockType is ChunkerVanillaBlockType) blockType.name else ""

        val colorsList = listOf(
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
        )
        val mcColor = colorsList.firstOrNull { blockName.startsWith(it) } ?: "WHITE"
        val mclColor = when (mcColor) {
            "GRAY" -> "grey"
            "LIGHT_GRAY" -> "silver"
            else -> mcColor.lowercase()
        }

        val bannerItemName = "mcl_banners:banner_item_$mclColor"
        val itemStack = MclItemStack(bannerItemName, 1, 0)
        
        // ==========================================
        // 【核心花纹图案转换系统】
        // ==========================================
        val patterns = banner.patterns
        if (patterns.isNotEmpty()) {
            val serializedLayers = serializeLayersToLua(patterns)
            
            // 将序列化后的 layers 字符串安全写入旗帜 ItemStack 的 metadata
            itemStack.metadata = mapOf("layers" to serializedLayers)
            
            // 写入 block meta 的 layers 字段以供 ABM / Entities 渲染
            fields["layers"] = serializedLayers
        }

        inventories["banner"] = MclInventory(1, listOf(itemStack))

        val isWallBanner = blockName.contains("WALL")
        if (!isWallBanner) {
            fields["rotation_level"] = "0"
        } else {
            fields["rotation_level"] = "8"
        }

        return MclBlockEntityData(
            fields = fields,
            inventories = inventories
        )
    }

    /**
     * 将 Chunker 的 Banner 图案列表完美编译为 Minetest 特有的序列化 Lua Table 格式。
     * 格式示例: { { ["color"] = "unicolor_grey", ["pattern"] = "circle" }, ... }
     */
    private fun serializeLayersToLua(patterns: List<it.unimi.dsi.fastutil.Pair<ChunkerDyeColor, ChunkerBannerPattern>>): String {
        val sb = StringBuilder()
        sb.append("{ ")
        for (i in patterns.indices) {
            val pair = patterns[i]
            val dye = pair.left()
            val pattern = pair.right()

            // 转换颜色名称以对齐 unicolor 格式
            val mclColor = when (dye) {
                ChunkerDyeColor.GRAY -> "grey"
                ChunkerDyeColor.LIGHT_GRAY -> "silver"
                else -> dye.name.lowercase()
            }
            val unicolor = "unicolor_$mclColor"

            // 转换图案 ID
            val mclPattern = mapChunkerPatternToMcl(pattern)

            sb.append("{ ")
            sb.append("[\"color\"] = \"$unicolor\", ")
            sb.append("[\"pattern\"] = \"$mclPattern\"")
            sb.append(" }")
            if (i < patterns.size - 1) {
                sb.append(", ")
            }
        }
        sb.append(" }")
        return sb.toString()
    }

    /**
     * 依据 Mineclonia 的 mcl_banners_patterncraft.lua 完美的 Pattern ID 转换词典
     */
    private fun mapChunkerPatternToMcl(pattern: ChunkerBannerPattern): String {
        return when (pattern) {
            ChunkerBannerPattern.BASE -> "base"
            ChunkerBannerPattern.SQUARE_BOTTOM_LEFT -> "square_bottom_left"
            ChunkerBannerPattern.SQUARE_BOTTOM_RIGHT -> "square_bottom_right"
            ChunkerBannerPattern.SQUARE_TOP_LEFT -> "square_top_left"
            ChunkerBannerPattern.SQUARE_TOP_RIGHT -> "square_top_right"
            ChunkerBannerPattern.STRIPE_BOTTOM -> "stripe_bottom"
            ChunkerBannerPattern.STRIPE_TOP -> "stripe_top"
            ChunkerBannerPattern.STRIPE_LEFT -> "stripe_left"
            ChunkerBannerPattern.STRIPE_RIGHT -> "stripe_right"
            ChunkerBannerPattern.STRIPE_CENTER -> "stripe_center"
            ChunkerBannerPattern.STRIPE_MIDDLE -> "stripe_middle"
            ChunkerBannerPattern.STRIPE_DOWNRIGHT -> "stripe_downright"
            ChunkerBannerPattern.STRIPE_DOWNLEFT -> "stripe_downleft"
            ChunkerBannerPattern.STRIPE_SMALL -> "small_stripes"
            ChunkerBannerPattern.CROSS -> "cross"
            ChunkerBannerPattern.STRAIGHT_CROSS -> "straight_cross"
            ChunkerBannerPattern.TRIANGLE_BOTTOM -> "triangle_bottom"
            ChunkerBannerPattern.TRIANGLE_TOP -> "triangle_top"
            ChunkerBannerPattern.TRIANGLES_BOTTOM -> "triangles_bottom"
            ChunkerBannerPattern.TRIANGLES_TOP -> "triangles_top"
            ChunkerBannerPattern.DIAGONAL_LEFT -> "diagonal_left"
            ChunkerBannerPattern.DIAGONAL_RIGHT -> "diagonal_right"
            ChunkerBannerPattern.DIAGONAL_LEFT_MIRROR -> "diagonal_up_left"
            ChunkerBannerPattern.DIAGONAL_RIGHT_MIRROR -> "diagonal_up_right"
            ChunkerBannerPattern.CIRCLE_MIDDLE -> "circle"
            ChunkerBannerPattern.RHOMBUS_MIDDLE -> "rhombus"
            ChunkerBannerPattern.HALF_VERTICAL -> "half_vertical"
            ChunkerBannerPattern.HALF_HORIZONTAL -> "half_horizontal"
            ChunkerBannerPattern.HALF_VERTICAL_MIRROR -> "half_vertical_right"
            ChunkerBannerPattern.HALF_HORIZONTAL_MIRROR -> "half_horizontal_bottom"
            ChunkerBannerPattern.BORDER -> "border"
            ChunkerBannerPattern.CURLY_BORDER -> "curly_border"
            ChunkerBannerPattern.GRADIENT -> "gradient"
            ChunkerBannerPattern.GRADIENT_UP -> "gradient_up"
            ChunkerBannerPattern.BRICKS -> "bricks"
            ChunkerBannerPattern.GLOBE -> "globe"
            ChunkerBannerPattern.CREEPER -> "creeper"
            ChunkerBannerPattern.SKULL -> "skull"
            ChunkerBannerPattern.FLOWER -> "flower"
            ChunkerBannerPattern.MOJANG -> "thing"
            ChunkerBannerPattern.PIGLIN -> "piglin"
            ChunkerBannerPattern.FLOW -> "flow"
            ChunkerBannerPattern.GUSTER -> "guster"
        }
    }

    private fun extractTextFromJson(element: JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        if (element.isJsonPrimitive) return element.asString
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("text")) return obj.get("text").asString
        }
        return ""
    }

    private fun mclItemFromChunkerOrEmpty(chunkerItem: com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack?): MclItemStack {
        if (chunkerItem == null) return MclItemStack("", 0)
        return MclItemRegistry.fromChunker(chunkerItem)
    }
}