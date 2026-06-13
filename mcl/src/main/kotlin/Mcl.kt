package me.voltual.mcl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hivemc.chunker.conversion.intermediate.column.blockentity.*
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.FurnaceBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.ChestBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.sign.SignBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemStack
import com.hivemc.chunker.conversion.intermediate.column.chunk.itemstack.ChunkerItemProperty
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerItemStackIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.item.ChunkerVanillaItemType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import java.util.logging.Logger

/**
 * Mineclonia 物品栈的内部表示
 */
data class MclItemStack(
    val name: String,
    val count: Int,
    val wear: Int = 0
)

/**
 * Mineclonia 物品栏的内部表示
 */
data class MclInventory(
    val width: Int,
    val items: List<MclItemStack>
)

/**
 * 转换后的 Mineclonia 方块实体数据
 */
data class MclBlockEntityData(
    val fields: Map<String, String> = emptyMap(),
    val inventories: Map<String, MclInventory> = emptyMap()
)

/**
 * 物品转换器，负责将 Chunker 的物品转换为 Mineclonia 的物品名
 */
object MclItemRegistry {
    private val logger = Logger.getLogger("MclItemRegistry")

    /**
     * 将 Chunker 的物品标识符转换为 Mineclonia 的节点/物品名称
     */
    fun getItemName(identifier: ChunkerItemStackIdentifier): String {
        val type = identifier.itemStackType
        return when (type) {
            is ChunkerVanillaItemType -> {
                // 这里的映射逻辑可以根据 conversions.h 进一步扩展
                "mcl_core:${type.name.lowercase()}"
            }
            is ChunkerVanillaBlockType -> {
                "mcl_core:${type.name.lowercase()}"
            }
            else -> {
                logger.warning("Unknown item type: ${type::class.java.name}")
                "mcl_core:cobble" // 默认回退物品
            }
        }
    }

    /**
     * 将 ChunkerItemStack 转换为 MclItemStack
     */
    fun fromChunker(itemStack: ChunkerItemStack?): MclItemStack {
        if (itemStack == null || itemStack.identifier.isAir) {
            return MclItemStack("", 0)
        }
        val name = getItemName(itemStack.identifier)
        val amount = itemStack.get(ChunkerItemProperty.AMOUNT).orElse(1)
        val durability = itemStack.get(ChunkerItemProperty.DURABILITY).orElse(0)
        return MclItemStack(name, amount, durability)
    }
}

/**
 * 方块实体转换注册表
 */
object MclBlockEntityRegistry {
    private val converters = mutableMapOf<Class<out BlockEntity>, (BlockEntity) -> MclBlockEntityData>()

    init {
        register(ChestBlockEntity::class.java, ::convertChest)
        register(FurnaceBlockEntity::class.java, ::convertFurnace)
        register(SignBlockEntity::class.java, ::convertSign)
        register(JukeboxBlockEntity::class.java, ::convertJukebox)
        register(SpawnerBlockEntity::class.java, ::convertSpawner)
    }

    fun <T : BlockEntity> register(clazz: Class<T>, converter: (T) -> MclBlockEntityData) {
        @Suppress("UNCHECKED_CAST")
        converters[clazz] = converter as (BlockEntity) -> MclBlockEntityData
    }

    /**
     * 执行方块实体转换
     */
    fun convert(blockEntity: BlockEntity): MclBlockEntityData? {
        val converter = converters[blockEntity::class.java] ?: return null
        return converter(blockEntity)
    }

    /**
     * 1. 箱子转换 (Chest)
     */
    private fun convertChest(chest: ChestBlockEntity): MclBlockEntityData {
        val size = 27 // Mineclonia 单箱子为 27 格
        val items = MutableList(size) { MclItemStack("", 0) }

        // 填充物品
        for ((slotByte, chunkerItem) in chest.items) {
            val slot = slotByte.toInt()
            if (slot in 0 until size) {
                items[slot] = MclItemRegistry.fromChunker(chunkerItem)
            }
        }

        val fields = mapOf(
            "infotext" to "Chest",
            "formspec" to "size[8,9]list[current_name;main;0,0;9,3;]list[current_player;main;0,5;8,4;]"
        )

        val inventories = mapOf(
            "main" to MclInventory(9, items)
        )

        return MclBlockEntityData(fields, inventories)
    }

    /**
     * 2. 熔炉转换 (Furnace)
     */
    private fun convertFurnace(furnace: FurnaceBlockEntity): MclBlockEntityData {
        // Minecraft 熔炉槽位: 0=原料(src), 1=燃料(fuel), 2=产物(dst)
        val srcItem = MclItemRegistry.fromChunker(furnace.items[0])
        val fuelItem = MclItemRegistry.fromChunker(furnace.items[1])
        val dstItem = MclItemRegistry.fromChunker(furnace.items[2])

        val fields = mutableMapOf(
            "infotext" to if (furnace.burnTime > 0) "Furnace (active)" else "Furnace out of fuel",
            "src_totaltime" to furnace.cookTimeTotal.toString(),
            "src_time" to furnace.cookTime.toString(),
            "fuel_totaltime" to furnace.burnTime.toString(), // 对应燃烧总时间
            "fuel_time" to "0" // 当前燃烧进度
        )

        val inventories = mapOf(
            "src" to MclInventory(1, listOf(srcItem)),
            "fuel" to MclInventory(1, listOf(fuelItem)),
            "dst" to MclInventory(1, listOf(dstItem))
        )

        return MclBlockEntityData(fields, inventories)
    }

    /**
     * 3. 告示牌转换 (Sign)
     */
    private fun convertSign(sign: SignBlockEntity): MclBlockEntityData {
        // 提取正面文字
        val textBuilder = StringBuilder()
        val frontLines = sign.front.lines
        for (lineElement in frontLines) {
            val lineText = extractTextFromJson(lineElement)
            if (lineText.isNotEmpty()) {
                textBuilder.append(lineText).append("\n")
            }
        }
        val text = textBuilder.toString().trim()

        val fields = mapOf(
            "text" to text,
            "infotext" to "\"$text\"",
            "formspec" to "field[text;;${text}]"
        )

        return MclBlockEntityData(fields)
    }

    /**
     * 4. 唱片机转换 (Jukebox)
     */
    private fun convertJukebox(jukebox: JukeboxBlockEntity): MclBlockEntityData {
        val record = jukebox.record
        val fields = mutableMapOf<String, String>()
        val inventories = mutableMapOf<String, MclInventory>()

        if (record != null && !record.identifier.isAir) {
            val mclRecord = MclItemRegistry.fromChunker(record)
            fields["infotext"] = "Jukebox (Playing: ${mclRecord.name})"
            inventories["music"] = MclInventory(1, listOf(mclRecord))
        } else {
            fields["infotext"] = "Jukebox"
        }

        return MclBlockEntityData(fields, inventories)
    }

    /**
     * 5. 刷怪笼转换 (Spawner)
     */
    private fun convertSpawner(spawner: SpawnerBlockEntity): MclBlockEntityData {
        val entityType = spawner.entityType
        val entityName = entityType?.let { "mcl_mobs:${it.toString().lowercase()}" } ?: "mcl_mobs:zombie"

        val fields = mapOf(
            "entity_name" to entityName,
            "delay" to spawner.delay.toString(),
            "infotext" to "Monster Spawner ($entityName)"
        )

        return MclBlockEntityData(fields)
    }

    /**
     * 辅助方法：从 Chunker 的 JsonText 中提取纯文本
     */
    private fun extractTextFromJson(element: JsonElement?): String {
        if (element == null || element.isJsonNull) return ""
        if (element.isJsonPrimitive) {
            return element.asString
        }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("text")) {
                return obj.get("text").asString
            }
        }
        return ""
    }
}