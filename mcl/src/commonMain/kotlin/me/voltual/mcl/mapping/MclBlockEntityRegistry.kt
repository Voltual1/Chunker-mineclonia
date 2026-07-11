package me.voltual.mcl.mapping

import me.voltual.mcl.core.MclBlockEntityData
import me.voltual.mcl.core.MclInventory
import me.voltual.mcl.core.MclItemStack
import me.voltual.mcl.mapping.MclItemRegistry

import com.google.gson.JsonElement
import com.hivemc.chunker.conversion.intermediate.column.blockentity.*
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.FurnaceBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.ChestBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.blockentity.sign.SignBlockEntity
import com.hivemc.chunker.conversion.intermediate.column.entity.type.ChunkerEntityType

/**
 * 方块实体转换注册表
 */
object MclBlockEntityRegistry {
    private val converters = mutableMapOf<Class<out BlockEntity>, (BlockEntity) -> MclBlockEntityData>()

    init {
        // 注册已实现的转换器
    register(ChestBlockEntity::class.java, ::convertChest)
    // 修复：确保陷阱箱的数据也能通过 ChestBlockEntity 转换器无损导出
    register(com.hivemc.chunker.conversion.intermediate.column.blockentity.container.randomizable.TrappedChestBlockEntity::class.java, ::convertChest)    
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
     * 执行方块实体转换，供 MclConverterManager 调用
     */
    fun convert(blockEntity: BlockEntity): MclBlockEntityData? {
        val converter = converters[blockEntity::class.java] ?: return null
        return converter(blockEntity)
    }

    /**
     * 1. 箱子转换 (Chest)
     */
    private fun convertChest(chest: ChestBlockEntity): MclBlockEntityData {
        val size = 27 // Mineclonia 标准单箱子
        val items = MutableList(size) { MclItemStack("", 0) }

        for ((slotByte, chunkerItem) in chest.items) {
            val slot = slotByte.toInt()
            if (slot in 0 until size) {
                items[slot] = MclItemRegistry.fromChunker(chunkerItem)
            }
        }

        return MclBlockEntityData(
            fields = mapOf(
                "infotext" to "Chest",
                "formspec" to "size[8,9]list[current_name;main;0,0;9,3;]list[current_player;main;0,5;8,4;]"
            ),
            inventories = mapOf("main" to MclInventory(9, items))
        )
    }

    /**
     * 2. 熔炉转换 (Furnace)
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
     * 3. 告示牌转换 (Sign)
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
     * 4. 唱片机转换 (Jukebox)
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
     * 5. 刷怪笼转换 (Spawner)
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