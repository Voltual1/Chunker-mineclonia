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

object MclBlockEntityRegistry {
    private val converters = mutableMapOf<Class<out BlockEntity>, (BlockEntity) -> MclBlockEntityData>()

    init {
        // 利用统一签名的 Lambda 进行注册，彻底解决 Kotlin 泛型逆变冲突报错
        register(ChestBlockEntity::class.java) { be -> convertChest(be) }
        register(TrappedChestBlockEntity::class.java) { be -> convertChest(be) }
        register(ShulkerBoxBlockEntity::class.java) { be -> convertChest(be) }

        register(FurnaceBlockEntity::class.java) { be -> convertFurnace(be as FurnaceBlockEntity) }
        register(SignBlockEntity::class.java) { be -> convertSign(be as SignBlockEntity) }
        register(JukeboxBlockEntity::class.java) { be -> convertJukebox(be as JukeboxBlockEntity) }
        register(SpawnerBlockEntity::class.java) { be -> convertSpawner(be as SpawnerBlockEntity) }
    }

    fun <T : BlockEntity> register(clazz: Class<T>, converter: (BlockEntity) -> MclBlockEntityData) {
        converters[clazz] = converter
    }

    fun convert(blockEntity: BlockEntity): MclBlockEntityData? {
        val converter = converters[blockEntity::class.java] ?: return null
        return converter(blockEntity)
    }

    /**
     * 1. 统一接收 BlockEntity 作为参数，并通过 Smart Cast 提取 `items` 属性
     */
    private fun convertChest(be: BlockEntity): MclBlockEntityData {
        val size = 27 
        val items = MutableList(size) { MclItemStack("", 0) }

        // 处理 Chest, TrappedChest, ShulkerBox 的共享继承逻辑
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

    /**
     * 2. 熔炉转换
     */
    private fun convertFurnace(furnace: FurnaceBlockEntity): MclBlockEntityData {
        val srcItem = MclItemRegistry.fromChunker(furnace.items[0])
        val fuelItem = MclItemRegistry.fromChunker(furnace.items[1])
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

    /**
     * 3. 告示牌转换
     */
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

    /**
     * 4. 唱片机转换
     */
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

    /**
     * 5. 刷怪笼转换
     */
    private fun convertSpawner(spawner: SpawnerBlockEntity): MclBlockEntityData {
        val entityType = spawner.entityType
        val entityName = entityType?.let { "mcl_mobs:${it.toString().lowercase()}" } ?: "mcl_mobs:zombie"

        return MclBlockEntityData(
            fields = mapOf(
                "entity_name" to entityName,
                "delay" to spawner.delay.toString(),
                "infotext" to "Monster Spawner ($entityName)"
            )
        )
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