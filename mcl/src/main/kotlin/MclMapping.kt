package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.ChunkerBlockIdentifier
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*

/**
 * Mineclonia 颜色数据类，用于计算蜡烛等的 param2 调色板索引
 */
data class MclDyeColor(val name: String, val palette_index: Int)

object mcl_dyes {
    val colors = mapOf(
        "white" to MclDyeColor("white", 0),
        "silver" to MclDyeColor("silver", 1),
        "grey" to MclDyeColor("grey", 2),
        "black" to MclDyeColor("black", 3),
        "purple" to MclDyeColor("purple", 4),
        "blue" to MclDyeColor("blue", 5),
        "light_blue" to MclDyeColor("light_blue", 6),
        "cyan" to MclDyeColor("cyan", 7),
        "green" to MclDyeColor("green", 8),
        "lime" to MclDyeColor("lime", 9),
        "yellow" to MclDyeColor("yellow", 10),
        "brown" to MclDyeColor("brown", 11),
        "orange" to MclDyeColor("orange", 12),
        "red" to MclDyeColor("red", 13),
        "magenta" to MclDyeColor("magenta", 14),
        "pink" to MclDyeColor("pink", 15)
    )
}

/**
 * 方块映射器接口
 */
fun interface BlockMapper {
    fun map(identifier: ChunkerBlockIdentifier): MclNode?
}

/**
 * Mineclonia 映射注册表
 */
object MclMappingRegistry {
    private val mappers = mutableMapOf<ChunkerVanillaBlockType, MutableList<BlockMapper>>()

    fun register(type: ChunkerVanillaBlockType, mapper: BlockMapper) {
        mappers.computeIfAbsent(type) { mutableListOf() }.add(mapper)
    }

    fun convert(identifier: ChunkerBlockIdentifier): MclNode {
        val type = identifier.type
        if (type is ChunkerVanillaBlockType) {
            val list = mappers[type]
            if (list != null) {
                for (mapper in list) {
                    val result = mapper.map(identifier)
                    if (result != null) return result
                }
            }
        }
        return MclNode("mcl_core:cobble") // 默认回退方块
    }
}

/**
 * Chunker 状态到 Mineclonia (Minetest facedir/param2) 转换 DSL
 */
object MclMappingDsl {

    // 1. 简单无状态映射
    fun simple(targetName: String) = BlockMapper { _ ->
        MclNode(targetName)
    }

    // 2. 水平朝向映射 (facedir)
    fun directional(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 3. 楼梯映射 (Stairs)
    fun stair(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val baseDir = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }
        val param2 = if (half == Half.TOP) (baseDir + 20) else baseDir
        MclNode(targetName, param2 = param2.toByte())
    }

    // 4. 台阶映射 (Slabs)
    fun slab(bottomTarget: String, topTarget: String, doubleTarget: String) = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.SLAB_TYPE) ?: SlabType.BOTTOM
        when (type) {
            SlabType.BOTTOM -> MclNode(bottomTarget)
            SlabType.TOP -> MclNode(topTarget)
            SlabType.DOUBLE -> MclNode(doubleTarget)
        }
    }

    // 5. 树干/轴向映射 (Log Axis)
    fun log(targetName: String) = BlockMapper { id ->
        val axis = id.getState(VanillaBlockStates.AXIS) ?: Axis.Y
        val param2 = when (axis) {
            Axis.Y -> 0
            Axis.Z -> 4
            Axis.X -> 12
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 6. 挂载朝向映射 (wallmounted)
    fun wallmounted(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
        val param2 = when (facing) {
            FacingDirection.DOWN -> 0
            FacingDirection.UP -> 1
            FacingDirection.NORTH -> 2
            FacingDirection.SOUTH -> 3
            FacingDirection.WEST -> 4
            FacingDirection.EAST -> 5
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 7. 液体映射
    fun liquid(sourceName: String, flowingName: String) = BlockMapper { id ->
        val flowing = id.getState(VanillaBlockStates.FLOWING) ?: Bool.FALSE
        val level = id.getState(VanillaBlockStates.LIQUID_LEVEL) ?: LiquidLevel._0
        val param2 = level.ordinal.toByte()
        if (flowing == Bool.TRUE) {
            MclNode(flowingName, param2 = param2)
        } else {
            MclNode(sourceName, param2 = param2)
        }
    }

    // 8. 矿石亮灭状态映射
    fun litOre(normal: String, lit: String) = BlockMapper { id ->
        val litState = id.getState(VanillaBlockStates.LIT) ?: Bool.FALSE
        if (litState == Bool.TRUE) MclNode(lit) else MclNode(normal)
    }

    // 9. 墙装红石火把
    fun wallTorch(offName: String, onName: String) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) ?: Bool.FALSE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (lit == Bool.TRUE) onName else offName
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 3
            FacingDirectionHorizontal.WEST -> 4
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 5
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 10. 按钮映射
    fun button(basename: String) = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val suffix = if (powered == Bool.TRUE) "_on" else "_off"
        val facing = id.getState(VanillaBlockStates.ATTACHMENT_TYPE) ?: AttachmentType.FLOOR
        val direction = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            AttachmentType.FLOOR -> when (direction) {
                FacingDirectionHorizontal.SOUTH -> 10
                FacingDirectionHorizontal.WEST -> 13
                FacingDirectionHorizontal.NORTH -> 8
                FacingDirectionHorizontal.EAST -> 15
            }
            AttachmentType.CEILING -> when (direction) {
                FacingDirectionHorizontal.SOUTH -> 15
                FacingDirectionHorizontal.WEST -> 8
                FacingDirectionHorizontal.NORTH -> 13
                FacingDirectionHorizontal.EAST -> 10
            }
            AttachmentType.WALL -> when (direction) {
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 5
            }
        }.toByte()
        MclNode("mcl_buttons:button_${basename}${suffix}", param2 = param2)
    }

    // 11. 压力板映射
    fun pressurePlate(basename: String) = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val suffix = if (powered == Bool.TRUE) "_on" else "_off"
        MclNode("mcl_pressureplates:pressure_plate_${basename}${suffix}")
    }

    // 12. 红石中继器映射
    fun repeater() = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val locked = id.getState(VanillaBlockStates.LOCKED) ?: Bool.FALSE
        val delay = id.getState(VanillaBlockStates.DELAY) ?: Delay._1
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val baseDir = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }
        val nodeName: String
        val param2: Byte
        if (locked == Bool.TRUE) {
            nodeName = if (powered == Bool.TRUE) "mcl_repeaters:repeater_on_locked" else "mcl_repeaters:repeater_off_locked"
            param2 = ((delay.ordinal + 1) * 4 + baseDir).toByte()
        } else {
            val state = if (powered == Bool.TRUE) "on" else "off"
            nodeName = "mcl_repeaters:repeater_${state}_${delay.ordinal + 1}"
            param2 = baseDir.toByte()
        }
        MclNode(nodeName, param2 = param2)
    }

    // 13. 红石比较器映射
    fun comparator() = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val mode = id.getState(VanillaBlockStates.MODE_COMPARATOR) ?: ComparatorMode.COMPARE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val baseDir = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }
        val state = if (powered == Bool.TRUE) "on" else "off"
        val modeStr = if (mode == ComparatorMode.COMPARE) "comp" else "sub"
        val nodeName = "mcl_comparators:comparator_${state}_${modeStr}"
        val param2 = (4 * power.ordinal + baseDir).toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 14. 阳光检测器映射
    fun daylightDetector() = BlockMapper { id ->
        val inverted = id.getState(VanillaBlockStates.INVERTED) ?: Bool.FALSE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0
        val nodeName = if (inverted == Bool.TRUE) "mcl_daylight_detector:daylight_detector_inverted" else "mcl_daylight_detector:daylight_detector"
        MclNode(nodeName, param2 = power.ordinal.toByte())
    }

    // 15. 红石粉导线映射
    fun redstoneWire() = BlockMapper { id ->
        val east = id.getState(VanillaBlockStates.REDSTONE_EAST) ?: RedstoneConnection.NONE
        val west = id.getState(VanillaBlockStates.REDSTONE_WEST) ?: RedstoneConnection.NONE
        val north = id.getState(VanillaBlockStates.REDSTONE_NORTH) ?: RedstoneConnection.NONE
        val south = id.getState(VanillaBlockStates.REDSTONE_SOUTH) ?: RedstoneConnection.NONE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0

        var wireflags = 0
        if (north != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x1
            if (north == RedstoneConnection.UP) wireflags = wireflags or 0x10
        }
        if (east != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x2
            if (east == RedstoneConnection.UP) wireflags = wireflags or 0x20
        }
        if (south != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x4
            if (south == RedstoneConnection.UP) wireflags = wireflags or 0x40
        }
        if (west != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x8
            if (west == RedstoneConnection.UP) wireflags = wireflags or 0x80
        }
        val nodeName = if (wireflags == 0) "mcl_redstone:redstone" else "mcl_redstone:wire_" + String.format("%02x", wireflags)
        MclNode(nodeName, param2 = power.ordinal.toByte())
    }

    // 16. 活塞映射
    fun piston(sticky: Boolean) = BlockMapper { id ->
        val extended = id.getState(VanillaBlockStates.EXTENDED) ?: Bool.FALSE
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        val base = if (sticky) "mcl_pistons:piston_sticky" else "mcl_pistons:piston"
        val state = if (extended == Bool.TRUE) "_on" else "_off"
        val param2 = when (facing) {
            FacingDirection.DOWN -> 15
            FacingDirection.UP -> 1
            FacingDirection.NORTH -> 2
            FacingDirection.SOUTH -> 0
            FacingDirection.WEST -> 1
            FacingDirection.EAST -> 3
        }.toByte()
        MclNode("$base$state", param2 = param2)
    }

    // 17. 活塞臂推杆
    fun pistonHead() = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.PISTON_TYPE) ?: PistonType.NORMAL
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        val baseName = if (type == PistonType.STICKY) "mcl_pistons:piston_pusher_sticky" else "mcl_pistons:piston_pusher"
        val param2 = when (facing) {
            FacingDirection.DOWN -> 15
            FacingDirection.UP -> 1
            FacingDirection.NORTH -> 2
            FacingDirection.SOUTH -> 0
            FacingDirection.WEST -> 1
            FacingDirection.EAST -> 3
        }.toByte()
        MclNode(baseName, param2 = param2)
    }

    // 18. 颅骨/生物头颅
    fun head(baseName: String, wall: Boolean) = BlockMapper { id ->
        val suffix = if (wall) "_wall" else ""
        val nodeName = "mcl_heads:$baseName$suffix"
        val param2 = if (wall) {
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            when (facing) {
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 5
            }.toByte()
        } else {
            val rotation = id.getState(VanillaBlockStates.ROTATION) ?: Rotation._0
            (rotation.ordinal * 10).toByte()
        }
        MclNode(nodeName, param2 = param2)
    }

    // 19. 铁砧
    fun anvil(nodeName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 20. 门映射 (适配木门/铁门/铜门)
    fun door(customBase: String) = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val open = id.getState(VanillaBlockStates.OPEN) == Bool.TRUE
        val hinge = id.getState(VanillaBlockStates.DOOR_HINGE) ?: HingeSide.LEFT
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val style = if (hinge == HingeSide.RIGHT) "2" else "1"
        val part = if (half == Half.TOP) "t" else "b"
        var param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }
        if (open) {
            param2 = if (hinge == HingeSide.LEFT) (param2 + 1) % 4 else (param2 + 3) % 4
        }
        MclNode("${customBase}_${part}_${style}", param2 = param2.toByte())
    }

    // 21. 活板门映射
    fun trapdoor(customBase: String) = BlockMapper { id ->
        val open = id.getState(VanillaBlockStates.OPEN) == Bool.TRUE
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (open) "${customBase}_open" else customBase
        var param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }
        if (half == Half.TOP) {
            param2 += 20
        }
        MclNode(nodeName, param2 = param2.toByte())
    }

    // 22. 双层高大植物
    fun doublePlant(basename: String) = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val suffix = if (half == Half.TOP) "_top" else ""
        MclNode("mcl_flowers:$basename$suffix")
    }

    // 23. 床
    fun bed(color: String) = BlockMapper { id ->
        val part = id.getState(VanillaBlockStates.BED_PART) ?: BedPart.FOOT
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val suffix = if (part == BedPart.HEAD) "top" else "bottom"
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode("mcl_beds:bed_${color}_$suffix", param2 = param2)
    }

    // 24. 旗帜
    fun banner(color: String, wall: Boolean) = BlockMapper { id ->
        val nodeName = if (wall) "mcl_banners:hanging_banner" else "mcl_banners:standing_banner"
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = if (wall) {
            when (facing) {
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 5
            }.toByte()
        } else {
            0.toByte()
        }
        MclNode(nodeName, param2 = param2)
    }

    // 25. 小麦作物
    fun wheatCrop() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
        val nodeName = if (age == Age_7._7) "mcl_farming:wheat" else "mcl_farming:wheat_${age.ordinal}"
        MclNode(nodeName)
    }

    // 26. 甜菜根作物
    fun beetrootCrop() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_3) ?: Age_3._0
        val nodeName = if (age == Age_3._3) "mcl_farming:beetroot" else "mcl_farming:beetroot_${age.ordinal}"
        MclNode(nodeName)
    }

    // 27. 胡萝卜作物
    fun carrotCrop() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
        val nodeName = if (age == Age_7._7) "mcl_farming:carrot" else "mcl_farming:carrot_${age.ordinal + 1}"
        MclNode(nodeName)
    }

    // 28. 马铃薯作物
    fun potatoCrop() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_7) ?: Age_7._0
        val nodeName = if (age == Age_7._7) "mcl_farming:potato" else "mcl_farming:potato_${age.ordinal + 1}"
        MclNode(nodeName)
    }

    // 29. 可可豆作物
    fun cocoaCrop() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_2) ?: Age_2._0
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode("mcl_cocoas:cocoa_${age.ordinal + 1}", param2 = param2)
    }

    // 30. 甜浆果灌木
    fun sweetBerryBush() = BlockMapper { id ->
        val age = id.getState(VanillaBlockStates.AGE_3) ?: Age_3._0
        MclNode("mcl_farming:sweet_berry_bush_${age.ordinal}")
    }

    // 31. 巨型蘑菇块
    fun mushroomBlock(color: String) = BlockMapper { id ->
        val north = id.getState(VanillaBlockStates.NORTH) == Bool.TRUE
        val east = id.getState(VanillaBlockStates.EAST) == Bool.TRUE
        val south = id.getState(VanillaBlockStates.SOUTH) == Bool.TRUE
        val west = id.getState(VanillaBlockStates.WEST) == Bool.TRUE
        val up = id.getState(VanillaBlockStates.UP) == Bool.TRUE
        val down = id.getState(VanillaBlockStates.DOWN) == Bool.TRUE

        val t = if (up) '0' else '1'
        val b = if (down) '0' else '1'
        val r = if (east) '0' else '1'
        val l = if (west) '0' else '1'
        val bk = if (north) '0' else '1'
        val f = if (south) '0' else '1'

        val bin = "$t$b$r$l$bk$f"
        MclNode("mcl_mushrooms:${color}_mushroom_block_cap_$bin")
    }

    // 32. 滴水石柱
    fun pointedDripstone() = BlockMapper { id ->
        val direction = id.getState(VanillaBlockStates.VERTICAL_DIRECTION) ?: VerticalDirection.UP
        val thickness = id.getState(VanillaBlockStates.DRIPSTONE_THICKNESS) ?: DripstoneThickness.TIP
        val dirStr = if (direction == VerticalDirection.DOWN) "top" else "bottom"
        val thicknessStr = when (thickness) {
            DripstoneThickness.TIP_MERGE -> "tip_merge"
            DripstoneThickness.TIP -> "tip"
            DripstoneThickness.FRUSTUM -> "frustum"
            DripstoneThickness.MIDDLE -> "middle"
            DripstoneThickness.BASE -> "base"
        }
        MclNode("mcl_dripstone:dripstone_${dirStr}_${thicknessStr}")
    }

    // 33. 发光浆果藤蔓 (洞穴藤蔓)
    fun caveVines() = BlockMapper { id ->
        val berries = id.getState(VanillaBlockStates.BERRIES) == Bool.TRUE
        val nodeName = if (berries) "mcl_lush_caves:cave_vines_lit" else "mcl_lush_caves:cave_vines"
        MclNode(nodeName)
    }

    // 34. 大垂滴叶
    fun bigDripleaf() = BlockMapper { id ->
        val tilt = id.getState(VanillaBlockStates.TILT) ?: Tilt.NONE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = when (tilt) {
            Tilt.NONE -> "mcl_lush_caves:dripleaf_big"
            Tilt.PARTIAL, Tilt.UNSTABLE -> "mcl_lush_caves:dripleaf_big_tipped_half"
            Tilt.FULL -> "mcl_lush_caves:dripleaf_big_tipped_full"
        }
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 35. 小垂滴叶
    fun smallDripleaf() = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (half == Half.TOP) "mcl_lush_caves:dripleaf_small" else "mcl_lush_caves:dripleaf_small_stem"
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 36. 悬挂苔藓
    fun hangingMoss() = BlockMapper { id ->
        val tip = id.getState(VanillaBlockStates.TIP) == Bool.TRUE
        val nodeName = if (tip) "mcl_pale_oak:hanging_moss_tip" else "mcl_pale_oak:hanging_moss"
        MclNode(nodeName)
    }

    // 37. 试炼刷怪笼
    fun trialSpawner() = BlockMapper { id ->
        val state = id.getState(VanillaBlockStates.TRIAL_SPAWNER_STATE) ?: TrialSpawnerState.INACTIVE
        val ominous = id.getState(VanillaBlockStates.OMINOUS) == Bool.TRUE
        val active = state == TrialSpawnerState.ACTIVE || state == TrialSpawnerState.WAITING_FOR_PLAYERS || state == TrialSpawnerState.EJECTING_REWARD
        val prefix = if (ominous) "mcl_trial_spawners:ominous_trialspawner" else "mcl_trial_spawners:trialspawner"
        val suffix = if (active) "_on" else ""
        MclNode("$prefix$suffix")
    }

    // 38. 宝库 (Vault)
    fun vault() = BlockMapper { id ->
        val state = id.getState(VanillaBlockStates.VAULT_STATE) ?: VaultState.INACTIVE
        val ominous = id.getState(VanillaBlockStates.OMINOUS) == Bool.TRUE
        val base = if (ominous) "mcl_vaults:ominous_vault" else "mcl_vaults:vault"
        val suffix = when (state) {
            VaultState.INACTIVE -> ""
            VaultState.ACTIVE, VaultState.UNLOCKING -> "_on"
            VaultState.EJECTING -> "_ejecting"
        }
        MclNode("$base$suffix")
    }

    // 39. 熔炉系列
    fun furnaceLike(normal: String, active: String) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (lit) active else normal
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 40. 饰面罐 (Decorated Pot)
    fun decoratedPot() = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode("mcl_pottery_sherds:pot", param2 = param2)
    }

    // 41. 雕版书架
    fun chiseledBookshelf() = BlockMapper { id ->
        val s0 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_0_OCCUPIED) == Bool.TRUE
        val s1 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_1_OCCUPIED) == Bool.TRUE
        val s2 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_2_OCCUPIED) == Bool.TRUE
        val s3 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_3_OCCUPIED) == Bool.TRUE
        val s4 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_4_OCCUPIED) == Bool.TRUE
        val s5 = id.getState(VanillaBlockStates.CHISELED_BOOKSHELF_SLOT_5_OCCUPIED) == Bool.TRUE

        var bits = 0
        if (s0) bits = bits or (1 shl 0)
        if (s1) bits = bits or (1 shl 1)
        if (s2) bits = bits or (1 shl 2)
        if (s3) bits = bits or (1 shl 3)
        if (s4) bits = bits or (1 shl 4)
        if (s5) bits = bits or (1 shl 5)

        val nodeName = if (bits == 0) "mcl_books:chiseled_bookshelf" else "mcl_books:chiseled_bookshelf_" + String.format("%02x", bits)
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 42. 箱子系列
    fun chestLike(baseName: String) = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.CHEST_TYPE) ?: ChestType.SINGLE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        val suffix = when (type) {
            ChestType.SINGLE -> "_small"
            ChestType.LEFT -> "_left"
            ChestType.RIGHT -> "_right"
        }
        MclNode("$baseName$suffix", param2 = param2)
    }

    // 43. 投掷器与发射器
    fun dispenserLike(baseName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        when (facing) {
            FacingDirection.UP -> MclNode("${baseName}_up")
            FacingDirection.DOWN -> MclNode("${baseName}_down")
            FacingDirection.NORTH -> MclNode(baseName, param2 = 2)
            FacingDirection.SOUTH -> MclNode(baseName, param2 = 0)
            FacingDirection.EAST -> MclNode(baseName, param2 = 3)
            FacingDirection.WEST -> MclNode(baseName, param2 = 1)
        }
    }

    // 44. 漏斗
    fun hopper() = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL_DOWN) ?: FacingDirectionHorizontalDown.DOWN
        val enabled = id.getState(VanillaBlockStates.ENABLED) == Bool.TRUE
        val state = if (enabled) "" else "_disabled"
        when (facing) {
            FacingDirectionHorizontalDown.DOWN -> MclNode("mcl_hoppers:hopper$state")
            FacingDirectionHorizontalDown.NORTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 2)
            FacingDirectionHorizontalDown.SOUTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 0)
            FacingDirectionHorizontalDown.EAST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 3)
            FacingDirectionHorizontalDown.WEST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 1)
        }
    }

    // 45. 门门禁书架 (雕版书架)
    fun shelf(basename: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode("mcl_books:shelf_${basename}", param2 = param2)
    }

    // 46. 堆肥桶
    fun composter() = BlockMapper { id ->
        val level = id.getState(VanillaBlockStates.COMPOSTER_LEVEL) ?: ComposterLevel._0
        val nodeName = when (level) {
            ComposterLevel._0 -> "mcl_composters:composter"
            ComposterLevel._8 -> "mcl_composters:composter_ready"
            else -> "mcl_composters:composter_${level.ordinal}"
        }
        MclNode(nodeName)
    }

    // 47. 围栏门 (Gate)
    fun gate(targetName: String) = BlockMapper { id ->
        val open = id.getState(VanillaBlockStates.OPEN) == Bool.TRUE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (open) "${targetName}_open" else targetName
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.WEST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.EAST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 48. 铜灯 (Copper Bulb)
    fun copperBulb(exposure: String, waxed: Boolean) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val powered = id.getState(VanillaBlockStates.POWERED) == Bool.TRUE
        val state = when {
            lit && powered -> "on_powered"
            lit -> "on"
            powered -> "off_powered"
            else -> "off"
        }
        val waxSuffix = if (waxed) "_preserved" else ""
        val nodeName = "mcl_copper:bulb${exposure}_${state}${waxSuffix}"
        MclNode(nodeName)
    }

    // 49. 避雷针 (Lightning Rod)
    fun lightningRod(exposure: String, waxed: Boolean) = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) == Bool.TRUE
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
        val powerSuffix = if (powered) "_powered" else ""
        val waxSuffix = if (waxed) "_preserved" else ""
        val nodeName = "mcl_lightning_rods:rod${exposure}${powerSuffix}${waxSuffix}"
        val param2 = when (facing) {
            FacingDirection.DOWN -> 20
            FacingDirection.UP -> 0
            FacingDirection.NORTH -> 8
            FacingDirection.SOUTH -> 4
            FacingDirection.WEST -> 16
            FacingDirection.EAST -> 12
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 50. 木桶 (Barrel)
    fun barrel() = BlockMapper { id ->
        val open = id.getState(VanillaBlockStates.OPEN) == Bool.TRUE
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        val baseName = if (open) "mcl_barrels:barrel_open" else "mcl_barrels:barrel_closed"
        val param2 = when (facing) {
            FacingDirection.DOWN -> 20
            FacingDirection.UP -> 0
            FacingDirection.NORTH -> 8
            FacingDirection.SOUTH -> 4
            FacingDirection.WEST -> 16
            FacingDirection.EAST -> 12
        }.toByte()
        MclNode(baseName, param2 = param2)
    }

    // 51. 蜡烛
    fun candle(color: String?) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val count = id.getState(VanillaBlockStates.CANDLES) ?: Candles._1
        val num = count.ordinal + 1
        val prefix = if (lit) "mcl_candles:candle_lit_" else "mcl_candles:candle_"
        val nodeName = "$prefix$num"
        val param2 = if (color != null) {
            val colorDef = mcl_dyes.colors[color]
            if (colorDef != null) colorDef.palette_index.toByte() else 0.toByte()
        } else {
            0.toByte()
        }
        MclNode(nodeName, param2 = param2)
    }

    // 52. 蛋糕蜡烛
    fun candleCake(color: String?) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val nodeName = if (lit) "mcl_candles:candle_cake_lit" else "mcl_candles:candle_cake"
        val param2 = if (color != null) {
            val colorDef = mcl_dyes.colors[color]
            if (colorDef != null) colorDef.palette_index.toByte() else 0.toByte()
        } else {
            0.toByte()
        }
        MclNode(nodeName, param2 = param2)
    }
}

/**
 * 初始化并向注册表绑定所有的 Chunker 方块映射逻辑
 */
object MclMappingInitializer {

    fun initialize() {
        MclMappingRegistry.apply {
            // 自然与石质方块
            register(ChunkerVanillaBlockType.AIR, MclMappingDsl.simple("air"))
            register(ChunkerVanillaBlockType.STONE, MclMappingDsl.simple("mcl_core:stone"))
            register(ChunkerVanillaBlockType.GRANITE, MclMappingDsl.simple("mcl_core:granite"))
            register(ChunkerVanillaBlockType.POLISHED_GRANITE, MclMappingDsl.simple("mcl_core:granite_smooth"))
            register(ChunkerVanillaBlockType.DIORITE, MclMappingDsl.simple("mcl_core:diorite"))
            register(ChunkerVanillaBlockType.POLISHED_DIORITE, MclMappingDsl.simple("mcl_core:diorite_smooth"))
            register(ChunkerVanillaBlockType.ANDESITE, MclMappingDsl.simple("mcl_core:andesite"))
            register(ChunkerVanillaBlockType.POLISHED_ANDESITE, MclMappingDsl.simple("mcl_core:andesite_smooth"))
            register(ChunkerVanillaBlockType.GRASS_BLOCK, MclMappingDsl.simple("mcl_core:dirt_with_grass"))
            register(ChunkerVanillaBlockType.DIRT, MclMappingDsl.simple("mcl_core:dirt"))
            register(ChunkerVanillaBlockType.COARSE_DIRT, MclMappingDsl.simple("mcl_core:coarse_dirt"))
            register(ChunkerVanillaBlockType.PODZOL, MclMappingDsl.simple("mcl_core:podzol"))
            register(ChunkerVanillaBlockType.COBBLESTONE, MclMappingDsl.simple("mcl_core:cobble"))
            register(ChunkerVanillaBlockType.BEDROCK, MclMappingDsl.simple("mcl_core:bedrock"))
            register(ChunkerVanillaBlockType.SAND, MclMappingDsl.simple("mcl_core:sand"))
            register(ChunkerVanillaBlockType.RED_SAND, MclMappingDsl.simple("mcl_core:redsand"))
            register(ChunkerVanillaBlockType.GRAVEL, MclMappingDsl.simple("mcl_core:gravel"))
            register(ChunkerVanillaBlockType.CLAY, MclMappingDsl.simple("mcl_core:clay"))
            register(ChunkerVanillaBlockType.BRICKS, MclMappingDsl.simple("mcl_core:brick_block"))

            // 液体 (水、岩浆等)
            register(ChunkerVanillaBlockType.WATER, MclMappingDsl.liquid("mcl_core:water_source", "mcl_core:water_flowing"))
            register(ChunkerVanillaBlockType.LAVA, MclMappingDsl.liquid("mcl_core:lava_source", "mcl_core:lava_flowing"))

            // 深层板岩与凝灰岩 (Deepslate & Tuff)
            register(ChunkerVanillaBlockType.DEEPSLATE, MclMappingDsl.log("mcl_deepslate:deepslate"))
            register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE, MclMappingDsl.simple("mcl_deepslate:deepslate_cobbled"))
            register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_cobbled_deepslate"))
            register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_SLAB, MclMappingDsl.slab("mcl_stairs:slab_cobbled_deepslate", "mcl_stairs:slab_cobbled_deepslate_top", "mcl_deepslate:deepslate_cobbled"))
            register(ChunkerVanillaBlockType.COBBLED_DEEPSLATE_WALL, MclMappingDsl.simple("mcl_deepslate:deepslatecobbledwall"))
            register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE, MclMappingDsl.simple("mcl_deepslate:deepslate_polished"))
            register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_deepslate_polished"))
            register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_SLAB, MclMappingDsl.slab("mcl_stairs:slab_deepslate_polished", "mcl_stairs:slab_deepslate_polished_top", "mcl_deepslate:deepslate_polished"))
            register(ChunkerVanillaBlockType.POLISHED_DEEPSLATE_WALL, MclMappingDsl.simple("mcl_deepslate:deepslatepolishedwall"))
            register(ChunkerVanillaBlockType.DEEPSLATE_TILES, MclMappingDsl.simple("mcl_deepslate:deepslate_tiles"))
            register(ChunkerVanillaBlockType.DEEPSLATE_TILE_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_deepslate_tiles"))
            register(ChunkerVanillaBlockType.DEEPSLATE_TILE_SLAB, MclMappingDsl.slab("mcl_stairs:slab_deepslate_tiles", "mcl_stairs:slab_deepslate_tiles_top", "mcl_deepslate:deepslate_tiles"))
            register(ChunkerVanillaBlockType.DEEPSLATE_TILE_WALL, MclMappingDsl.simple("mcl_deepslate:deepslatetileswall"))
            register(ChunkerVanillaBlockType.DEEPSLATE_BRICKS, MclMappingDsl.simple("mcl_deepslate:deepslate_bricks"))
            register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_deepslate_bricks"))
            register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_SLAB, MclMappingDsl.slab("mcl_stairs:slab_deepslate_bricks", "mcl_stairs:slab_deepslate_bricks_top", "mcl_deepslate:deepslate_bricks"))
            register(ChunkerVanillaBlockType.DEEPSLATE_BRICK_WALL, MclMappingDsl.simple("mcl_deepslate:deepslatebrickswall"))
            register(ChunkerVanillaBlockType.CHISELED_DEEPSLATE, MclMappingDsl.simple("mcl_deepslate:deepslate_chiseled"))
            register(ChunkerVanillaBlockType.CRACKED_DEEPSLATE_BRICKS, MclMappingDsl.simple("mcl_deepslate:deepslate_bricks_cracked"))
            register(ChunkerVanillaBlockType.CRACKED_DEEPSLATE_TILES, MclMappingDsl.simple("mcl_deepslate:deepslate_tiles_cracked"))

            register(ChunkerVanillaBlockType.TUFF, MclMappingDsl.simple("mcl_deepslate:tuff"))
            register(ChunkerVanillaBlockType.TUFF_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_tuff"))
            register(ChunkerVanillaBlockType.TUFF_SLAB, MclMappingDsl.slab("mcl_stairs:slab_tuff", "mcl_stairs:slab_tuff_top", "mcl_deepslate:tuff"))
            register(ChunkerVanillaBlockType.TUFF_WALL, MclMappingDsl.simple("mcl_deepslate:tuffwall"))
            register(ChunkerVanillaBlockType.POLISHED_TUFF, MclMappingDsl.simple("mcl_deepslate:tuff_polished"))
            register(ChunkerVanillaBlockType.POLISHED_TUFF_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_tuff_polished"))
            register(ChunkerVanillaBlockType.POLISHED_TUFF_SLAB, MclMappingDsl.slab("mcl_stairs:slab_tuff_polished", "mcl_stairs:slab_tuff_polished_top", "mcl_deepslate:tuff_polished"))
            register(ChunkerVanillaBlockType.POLISHED_TUFF_WALL, MclMappingDsl.simple("mcl_deepslate:tuffpolishedwall"))
            register(ChunkerVanillaBlockType.CHISELED_TUFF, MclMappingDsl.simple("mcl_deepslate:tuff_chiseled"))
            register(ChunkerVanillaBlockType.TUFF_BRICKS, MclMappingDsl.simple("mcl_deepslate:tuff_bricks"))
            register(ChunkerVanillaBlockType.TUFF_BRICK_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_tuff_bricks"))
            register(ChunkerVanillaBlockType.TUFF_BRICK_SLAB, MclMappingDsl.slab("mcl_stairs:slab_tuff_bricks", "mcl_stairs:slab_tuff_bricks_top", "mcl_deepslate:tuff_bricks"))
            register(ChunkerVanillaBlockType.TUFF_BRICK_WALL, MclMappingDsl.simple("mcl_deepslate:tuffbrickswall"))
            register(ChunkerVanillaBlockType.CHISELED_TUFF_BRICKS, MclMappingDsl.simple("mcl_deepslate:tuff_chiseled_bricks"))

            // 矿石系列 (以普通与深层板岩分类)
            register(ChunkerVanillaBlockType.COAL_ORE, MclMappingDsl.simple("mcl_core:stone_with_coal"))
            register(ChunkerVanillaBlockType.DEEPSLATE_COAL_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_coal"))
            register(ChunkerVanillaBlockType.IRON_ORE, MclMappingDsl.simple("mcl_core:stone_with_iron"))
            register(ChunkerVanillaBlockType.DEEPSLATE_IRON_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_iron"))
            register(ChunkerVanillaBlockType.GOLD_ORE, MclMappingDsl.simple("mcl_core:stone_with_gold"))
            register(ChunkerVanillaBlockType.DEEPSLATE_GOLD_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_gold"))
            register(ChunkerVanillaBlockType.DIAMOND_ORE, MclMappingDsl.simple("mcl_core:stone_with_diamond"))
            register(ChunkerVanillaBlockType.DEEPSLATE_DIAMOND_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_diamond"))
            register(ChunkerVanillaBlockType.EMERALD_ORE, MclMappingDsl.simple("mcl_core:stone_with_emerald"))
            register(ChunkerVanillaBlockType.DEEPSLATE_EMERALD_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_emerald"))
            register(ChunkerVanillaBlockType.LAPIS_ORE, MclMappingDsl.simple("mcl_core:stone_with_lapis"))
            register(ChunkerVanillaBlockType.DEEPSLATE_LAPIS_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_lapis"))
            register(ChunkerVanillaBlockType.REDSTONE_ORE, MclMappingDsl.litOre("mcl_core:stone_with_redstone", "mcl_core:stone_with_redstone_lit"))
            register(ChunkerVanillaBlockType.DEEPSLATE_REDSTONE_ORE, MclMappingDsl.litOre("mcl_deepslate:deepslate_with_redstone", "mcl_deepslate:deepslate_with_redstone_lit"))
            register(ChunkerVanillaBlockType.COPPER_ORE, MclMappingDsl.simple("mcl_copper:stone_with_copper"))
            register(ChunkerVanillaBlockType.DEEPSLATE_COPPER_ORE, MclMappingDsl.simple("mcl_deepslate:deepslate_with_copper"))

            // 木头与植物变体 (以橡木 Oak 为例，全系注册)
            register(ChunkerVanillaBlockType.OAK_LOG, MclMappingDsl.log("mcl_trees:tree_oak"))
            register(ChunkerVanillaBlockType.STRIPPED_OAK_LOG, MclMappingDsl.log("mcl_trees:stripped_oak"))
            register(ChunkerVanillaBlockType.OAK_WOOD, MclMappingDsl.log("mcl_trees:bark_oak"))
            register(ChunkerVanillaBlockType.STRIPPED_OAK_WOOD, MclMappingDsl.log("mcl_trees:bark_stripped_oak"))
            register(ChunkerVanillaBlockType.OAK_PLANKS, MclMappingDsl.simple("mcl_trees:wood_oak"))
            register(ChunkerVanillaBlockType.OAK_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_oak"))
            register(ChunkerVanillaBlockType.OAK_SLAB, MclMappingDsl.slab("mcl_stairs:slab_oak", "mcl_stairs:slab_oak_top", "mcl_trees:wood_oak"))
            register(ChunkerVanillaBlockType.OAK_FENCE, MclMappingDsl.simple("mcl_fences:oak_fence"))
            register(ChunkerVanillaBlockType.OAK_FENCE_GATE, MclMappingDsl.gate("mcl_fences:oak_fence_gate"))

            // 下界与其他特殊木种
            register(ChunkerVanillaBlockType.CRIMSON_PLANKS, MclMappingDsl.simple("mcl_crimson:wood_crimson"))
            register(ChunkerVanillaBlockType.WARPED_PLANKS, MclMappingDsl.simple("mcl_crimson:wood_warped"))
            register(ChunkerVanillaBlockType.CRIMSON_STEM, MclMappingDsl.log("mcl_crimson:tree_crimson"))
            register(ChunkerVanillaBlockType.WARPED_STEM, MclMappingDsl.log("mcl_crimson:tree_warped"))

            // 铜氧化状态 (Exposed, Weathered, Oxidized)
            register(ChunkerVanillaBlockType.COPPER_BLOCK, MclMappingDsl.simple("mcl_copper:block"))
            register(ChunkerVanillaBlockType.EXPOSED_COPPER, MclMappingDsl.simple("mcl_copper:block_exposed"))
            register(ChunkerVanillaBlockType.WEATHERED_COPPER, MclMappingDsl.simple("mcl_copper:block_weathered"))
            register(ChunkerVanillaBlockType.OXIDIZED_COPPER, MclMappingDsl.simple("mcl_copper:block_oxidized"))
            register(ChunkerVanillaBlockType.WAXED_COPPER_BLOCK, MclMappingDsl.simple("mcl_copper:block_preserved"))
            register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER, MclMappingDsl.simple("mcl_copper:block_exposed_preserved"))
            register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER, MclMappingDsl.simple("mcl_copper:block_weathered_preserved"))
            register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER, MclMappingDsl.simple("mcl_copper:block_oxidized_preserved"))

            register(ChunkerVanillaBlockType.CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_cut"))
            register(ChunkerVanillaBlockType.EXPOSED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_exposed_cut"))
            register(ChunkerVanillaBlockType.WEATHERED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_weathered_cut"))
            register(ChunkerVanillaBlockType.OXIDIZED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_oxidized_cut"))
            register(ChunkerVanillaBlockType.WAXED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_cut_preserved"))
            register(ChunkerVanillaBlockType.WAXED_EXPOSED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_exposed_cut_preserved"))
            register(ChunkerVanillaBlockType.WAXED_WEATHERED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_weathered_cut_preserved"))
            register(ChunkerVanillaBlockType.WAXED_OXIDIZED_CUT_COPPER, MclMappingDsl.simple("mcl_copper:block_oxidized_cut_preserved"))

            register(ChunkerVanillaBlockType.COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_grate"))
            register(ChunkerVanillaBlockType.EXPOSED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_exposed_grate"))
            register(ChunkerVanillaBlockType.WEATHERED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_weathered_grate"))
            register(ChunkerVanillaBlockType.OXIDIZED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_oxidized_grate"))
            register(ChunkerVanillaBlockType.WAXED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_grate_preserved"))
            register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_exposed_grate_preserved"))
            register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_weathered_grate_preserved"))
            register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_GRATE, MclMappingDsl.simple("mcl_copper:block_oxidized_grate_preserved"))

            // 铜灯 (Copper Bulbs)
            register(ChunkerVanillaBlockType.COPPER_BULB, MclMappingDsl.copperBulb("", false))
            register(ChunkerVanillaBlockType.EXPOSED_COPPER_BULB, MclMappingDsl.copperBulb("_exposed", false))
            register(ChunkerVanillaBlockType.WEATHERED_COPPER_BULB, MclMappingDsl.copperBulb("_weathered", false))
            register(ChunkerVanillaBlockType.OXIDIZED_COPPER_BULB, MclMappingDsl.copperBulb("_oxidized", false))
            register(ChunkerVanillaBlockType.WAXED_COPPER_BULB, MclMappingDsl.copperBulb("", true))
            register(ChunkerVanillaBlockType.WAXED_EXPOSED_COPPER_BULB, MclMappingDsl.copperBulb("_exposed", true))
            register(ChunkerVanillaBlockType.WAXED_WEATHERED_COPPER_BULB, MclMappingDsl.copperBulb("_weathered", true))
            register(ChunkerVanillaBlockType.WAXED_OXIDIZED_COPPER_BULB, MclMappingDsl.copperBulb("_oxidized", true))

            // 避雷针 (Lightning Rods)
            register(ChunkerVanillaBlockType.LIGHTNING_ROD, MclMappingDsl.lightningRod("", false))
            register(ChunkerVanillaBlockType.EXPOSED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_exposed", false))
            register(ChunkerVanillaBlockType.WEATHERED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_weathered", false))
            register(ChunkerVanillaBlockType.OXIDIZED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_oxidized", false))
            register(ChunkerVanillaBlockType.WAXED_LIGHTNING_ROD, MclMappingDsl.lightningRod("", true))
            register(ChunkerVanillaBlockType.WAXED_EXPOSED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_exposed", true))
            register(ChunkerVanillaBlockType.WAXED_WEATHERED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_weathered", true))
            register(ChunkerVanillaBlockType.WAXED_OXIDIZED_LIGHTNING_ROD, MclMappingDsl.lightningRod("_oxidized", true))

            // 功能性红石方块与结构方块
            register(ChunkerVanillaBlockType.CHEST, MclMappingDsl.chestLike("mcl_chests:chest"))
            register(ChunkerVanillaBlockType.TRAPPED_CHEST, MclMappingDsl.chestLike("mcl_chests:trapped_chest"))
            register(ChunkerVanillaBlockType.ENDER_CHEST, MclMappingDsl.directional("mcl_chests:ender_chest_small"))
            register(ChunkerVanillaBlockType.BARREL, MclMappingDsl.barrel())
            register(ChunkerVanillaBlockType.DISPENSER, MclMappingDsl.dispenserLike("mcl_dispensers:dispenser"))
            register(ChunkerVanillaBlockType.DROPPER, MclMappingDsl.dispenserLike("mcl_dispensers:dropper"))
            register(ChunkerVanillaBlockType.HOPPER, MclMappingDsl.hopper())
            register(ChunkerVanillaBlockType.FURNACE, MclMappingDsl.furnaceLike("mcl_furnaces:furnace", "mcl_furnaces:furnace_active"))
            register(ChunkerVanillaBlockType.BLAST_FURNACE, MclMappingDsl.furnaceLike("mcl_blast_furnace:blast_furnace", "mcl_blast_furnace:blast_furnace_active"))
            register(ChunkerVanillaBlockType.SMOKER, MclMappingDsl.furnaceLike("mcl_smoker:smoker", "mcl_smoker:smoker_active"))
            register(ChunkerVanillaBlockType.JUKEBOX, MclMappingDsl.simple("mcl_jukebox:jukebox"))
            register(ChunkerVanillaBlockType.NOTE_BLOCK, MclMappingDsl.simple("mcl_noteblock:noteblock"))
            register(ChunkerVanillaBlockType.TARGET, MclMappingDsl.simple("mcl_target:target_off"))
            register(ChunkerVanillaBlockType.LEVER, MclMappingDsl.wallmounted("mcl_lever:lever_off"))
            register(ChunkerVanillaBlockType.OBSERVER, MclMappingDsl.directional("mcl_observers:observer_off"))
            register(ChunkerVanillaBlockType.REPEATER, MclMappingDsl.repeater())
            register(ChunkerVanillaBlockType.COMPARATOR, MclMappingDsl.comparator())
            register(ChunkerVanillaBlockType.DAYLIGHT_DETECTOR, MclMappingDsl.daylightDetector())
            register(ChunkerVanillaBlockType.REDSTONE_WIRE, MclMappingDsl.redstoneWire())
            register(ChunkerVanillaBlockType.REDSTONE_BLOCK, MclMappingDsl.simple("mcl_redstone_torch:redstoneblock"))
            register(ChunkerVanillaBlockType.REDSTONE_TORCH, MclMappingDsl.litOre("mcl_redstone_torch:redstone_torch_off", "mcl_redstone_torch:redstone_torch_on"))
            register(ChunkerVanillaBlockType.REDSTONE_WALL_TORCH, MclMappingDsl.wallTorch("mcl_redstone_torch:redstone_torch_off_wall", "mcl_redstone_torch:redstone_torch_on_wall"))

            // 活塞结构
            register(ChunkerVanillaBlockType.PISTON, MclMappingDsl.piston(false))
            register(ChunkerVanillaBlockType.STICKY_PISTON, MclMappingDsl.piston(true))
            register(ChunkerVanillaBlockType.PISTON_HEAD, MclMappingDsl.pistonHead())

            // 铁砧与工作台
            register(ChunkerVanillaBlockType.ANVIL, MclMappingDsl.anvil("mcl_anvils:anvil"))
            register(ChunkerVanillaBlockType.CHIPPED_ANVIL, MclMappingDsl.anvil("mcl_anvils:anvil_damage_1"))
            register(ChunkerVanillaBlockType.DAMAGED_ANVIL, MclMappingDsl.anvil("mcl_anvils:anvil_damage_2"))
            register(ChunkerVanillaBlockType.CRAFTING_TABLE, MclMappingDsl.simple("mcl_crafting_table:crafting_table"))
            register(ChunkerVanillaBlockType.LOOM, MclMappingDsl.directional("mcl_loom:loom"))
            register(ChunkerVanillaBlockType.CARTOGRAPHY_TABLE, MclMappingDsl.simple("mcl_cartography_table:cartography_table"))
            register(ChunkerVanillaBlockType.FLETCHING_TABLE, MclMappingDsl.simple("mcl_fletching_table:fletching_table"))
            register(ChunkerVanillaBlockType.SMITHING_TABLE, MclMappingDsl.simple("mcl_smithing_table:table"))
            register(ChunkerVanillaBlockType.STONECUTTER, MclMappingDsl.directional("mcl_stonecutter:stonecutter"))
            register(ChunkerVanillaBlockType.GRINDSTONE, MclMappingDsl.directional("mcl_grindstone:grindstone"))
            register(ChunkerVanillaBlockType.LECTERN, MclMappingDsl.directional("mcl_lectern:lectern"))

            // 矿车铁轨
            register(ChunkerVanillaBlockType.RAIL, MclMappingDsl.simple("mcl_core:rail"))
            register(ChunkerVanillaBlockType.POWERED_RAIL, MclMappingDsl.litOre("mcl_redstone:golden_rail_off", "mcl_redstone:golden_rail_on"))
            register(ChunkerVanillaBlockType.DETECTOR_RAIL, MclMappingDsl.litOre("mcl_redstone:detector_rail_off", "mcl_redstone:detector_rail_on"))
            register(ChunkerVanillaBlockType.ACTIVATOR_RAIL, MclMappingDsl.litOre("mcl_redstone:activator_rail_off", "mcl_redstone:activator_rail_on"))

            // 饰面罐、雕孔书架与堆肥桶
            register(ChunkerVanillaBlockType.DECORATED_POT, MclMappingDsl.decoratedPot())
            register(ChunkerVanillaBlockType.CHISELED_BOOKSHELF, MclMappingDsl.chiseledBookshelf())
            register(ChunkerVanillaBlockType.COMPOSTER, MclMappingDsl.composter())

            // 蜡烛与蜡烛蛋糕 (以部分常规颜色为例)
            register(ChunkerVanillaBlockType.CANDLE, MclMappingDsl.candle(null))
            register(ChunkerVanillaBlockType.WHITE_CANDLE, MclMappingDsl.candle("white"))
            register(ChunkerVanillaBlockType.RED_CANDLE, MclMappingDsl.candle("red"))
            register(ChunkerVanillaBlockType.BLUE_CANDLE, MclMappingDsl.candle("blue"))
            register(ChunkerVanillaBlockType.CANDLE_CAKE, MclMappingDsl.candleCake(null))
            register(ChunkerVanillaBlockType.WHITE_CANDLE_CAKE, MclMappingDsl.candleCake("white"))
            register(ChunkerVanillaBlockType.RED_CANDLE_CAKE, MclMappingDsl.candleCake("red"))

            // 头颅与颅骨 (Heads & Skulls)
            register(ChunkerVanillaBlockType.SKELETON_SKULL, MclMappingDsl.head("skeleton", false))
            register(ChunkerVanillaBlockType.SKELETON_WALL_SKULL, MclMappingDsl.head("skeleton", true))
            register(ChunkerVanillaBlockType.WITHER_SKELETON_SKULL, MclMappingDsl.head("wither_skeleton", false))
            register(ChunkerVanillaBlockType.WITHER_SKELETON_WALL_SKULL, MclMappingDsl.head("wither_skeleton", true))
            register(ChunkerVanillaBlockType.ZOMBIE_HEAD, MclMappingDsl.head("zombie", false))
            register(ChunkerVanillaBlockType.ZOMBIE_WALL_HEAD, MclMappingDsl.head("zombie", true))
            register(ChunkerVanillaBlockType.PLAYER_HEAD, MclMappingDsl.head("steve", false))
            register(ChunkerVanillaBlockType.PLAYER_WALL_HEAD, MclMappingDsl.head("steve", true))
            register(ChunkerVanillaBlockType.CREEPER_HEAD, MclMappingDsl.head("creeper", false))
            register(ChunkerVanillaBlockType.CREEPER_WALL_HEAD, MclMappingDsl.head("creeper", true))
            register(ChunkerVanillaBlockType.DRAGON_HEAD, MclMappingDsl.head("dragon", false))
            register(ChunkerVanillaBlockType.DRAGON_WALL_HEAD, MclMappingDsl.head("dragon", true))
            register(ChunkerVanillaBlockType.PIGLIN_HEAD, MclMappingDsl.head("piglin", false))
            register(ChunkerVanillaBlockType.PIGLIN_WALL_HEAD, MclMappingDsl.head("piglin", true))

            // 门与活板门 (适配所有木种和金属)
            register(ChunkerVanillaBlockType.OAK_DOOR, MclMappingDsl.door("mcl_doors:door_oak"))
            register(ChunkerVanillaBlockType.SPRUCE_DOOR, MclMappingDsl.door("mcl_doors:door_spruce"))
            register(ChunkerVanillaBlockType.BIRCH_DOOR, MclMappingDsl.door("mcl_doors:door_birch"))
            register(ChunkerVanillaBlockType.JUNGLE_DOOR, MclMappingDsl.door("mcl_doors:door_jungle"))
            register(ChunkerVanillaBlockType.ACACIA_DOOR, MclMappingDsl.door("mcl_doors:door_acacia"))
            register(ChunkerVanillaBlockType.CHERRY_DOOR, MclMappingDsl.door("mcl_doors:door_cherry_blossom"))
            register(ChunkerVanillaBlockType.DARK_OAK_DOOR, MclMappingDsl.door("mcl_doors:door_dark_oak"))
            register(ChunkerVanillaBlockType.PALE_OAK_DOOR, MclMappingDsl.door("mcl_doors:door_pale_oak"))
            register(ChunkerVanillaBlockType.MANGROVE_DOOR, MclMappingDsl.door("mcl_doors:door_mangrove"))
            register(ChunkerVanillaBlockType.BAMBOO_DOOR, MclMappingDsl.door("mcl_doors:door_bamboo"))
            register(ChunkerVanillaBlockType.IRON_DOOR, MclMappingDsl.door("mcl_doors:iron_door"))
            register(ChunkerVanillaBlockType.COPPER_DOOR, MclMappingDsl.door("mcl_copper:door"))
            register(ChunkerVanillaBlockType.EXPOSED_COPPER_DOOR, MclMappingDsl.door("mcl_copper:door_exposed"))
            register(ChunkerVanillaBlockType.WEATHERED_COPPER_DOOR, MclMappingDsl.door("mcl_copper:door_weathered"))
            register(ChunkerVanillaBlockType.OXIDIZED_COPPER_DOOR, MclMappingDsl.door("mcl_copper:door_oxidized"))

            register(ChunkerVanillaBlockType.OAK_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_oak"))
            register(ChunkerVanillaBlockType.SPRUCE_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_spruce"))
            register(ChunkerVanillaBlockType.BIRCH_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_birch"))
            register(ChunkerVanillaBlockType.JUNGLE_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_jungle"))
            register(ChunkerVanillaBlockType.ACACIA_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_acacia"))
            register(ChunkerVanillaBlockType.CHERRY_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_cherry_blossom"))
            register(ChunkerVanillaBlockType.DARK_OAK_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_dark_oak"))
            register(ChunkerVanillaBlockType.PALE_OAK_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_pale_oak"))
            register(ChunkerVanillaBlockType.MANGROVE_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_mangrove"))
            register(ChunkerVanillaBlockType.BAMBOO_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:trapdoor_bamboo"))
            register(ChunkerVanillaBlockType.IRON_TRAPDOOR, MclMappingDsl.trapdoor("mcl_doors:iron_trapdoor"))
            register(ChunkerVanillaBlockType.COPPER_TRAPDOOR, MclMappingDsl.trapdoor("mcl_copper:trapdoor"))
            register(ChunkerVanillaBlockType.EXPOSED_COPPER_TRAPDOOR, MclMappingDsl.trapdoor("mcl_copper:trapdoor_exposed"))
            register(ChunkerVanillaBlockType.WEATHERED_COPPER_TRAPDOOR, MclMappingDsl.trapdoor("mcl_copper:trapdoor_weathered"))
            register(ChunkerVanillaBlockType.OXIDIZED_COPPER_TRAPDOOR, MclMappingDsl.trapdoor("mcl_copper:trapdoor_oxidized"))

            // 双层高大植物
            register(ChunkerVanillaBlockType.SUNFLOWER, MclMappingDsl.doublePlant("sunflower"))
            register(ChunkerVanillaBlockType.LILAC, MclMappingDsl.doublePlant("lilac"))
            register(ChunkerVanillaBlockType.ROSE_BUSH, MclMappingDsl.doublePlant("rose_bush"))
            register(ChunkerVanillaBlockType.PEONY, MclMappingDsl.doublePlant("peony"))
            register(ChunkerVanillaBlockType.TALL_GRASS, MclMappingDsl.doublePlant("double_grass"))
            register(ChunkerVanillaBlockType.LARGE_FERN, MclMappingDsl.doublePlant("double_fern"))

            // 农业与作物阶段转换
            register(ChunkerVanillaBlockType.WHEAT, MclMappingDsl.wheatCrop())
            register(ChunkerVanillaBlockType.BEETROOTS, MclMappingDsl.beetrootCrop())
            register(ChunkerVanillaBlockType.CARROTS, MclMappingDsl.carrotCrop())
            register(ChunkerVanillaBlockType.POTATOES, MclMappingDsl.potatoCrop())
            register(ChunkerVanillaBlockType.COCOA, MclMappingDsl.cocoaCrop())
            register(ChunkerVanillaBlockType.SWEET_BERRY_BUSH, MclMappingDsl.sweetBerryBush())

            // 巨型蘑菇
            register(ChunkerVanillaBlockType.BROWN_MUSHROOM_BLOCK, MclMappingDsl.mushroomBlock("brown"))
            register(ChunkerVanillaBlockType.RED_MUSHROOM_BLOCK, MclMappingDsl.mushroomBlock("red"))
            register(ChunkerVanillaBlockType.MUSHROOM_STEM, MclMappingDsl.simple("mcl_mushrooms:brown_mushroom_block_stem"))

            // 滴水石柱与植物
            register(ChunkerVanillaBlockType.DRIPSTONE_BLOCK, MclMappingDsl.simple("mcl_dripstone:dripstone_block"))
            register(ChunkerVanillaBlockType.POINTED_DRIPSTONE, MclMappingDsl.pointedDripstone())
            register(ChunkerVanillaBlockType.CAVE_VINES_BODY, MclMappingDsl.caveVines())
            register(ChunkerVanillaBlockType.CAVE_VINES_HEAD, MclMappingDsl.caveVines())
            register(ChunkerVanillaBlockType.BIG_DRIPLEAF, MclMappingDsl.bigDripleaf())
            register(ChunkerVanillaBlockType.BIG_DRIPLEAF_STEM, MclMappingDsl.directional("mcl_lush_caves:dripleaf_big_stem"))
            register(ChunkerVanillaBlockType.SMALL_DRIPLEAF, MclMappingDsl.smallDripleaf())

            // 苍白花园植物 (Pale Garden)
            register(ChunkerVanillaBlockType.PALE_MOSS_BLOCK, MclMappingDsl.simple("mcl_pale_oak:pale_moss"))
            register(ChunkerVanillaBlockType.PALE_MOSS_CARPET, MclMappingDsl.simple("mcl_pale_oak:pale_moss_carpet"))
            register(ChunkerVanillaBlockType.PALE_HANGING_MOSS, MclMappingDsl.hangingMoss())
            register(ChunkerVanillaBlockType.OPEN_EYEBLOSSOM, MclMappingDsl.simple("mcl_flowers:eyeblossom_open"))
            register(ChunkerVanillaBlockType.CLOSED_EYEBLOSSOM, MclMappingDsl.simple("mcl_flowers:eyeblossom"))

            // 树脂方块 (Resin)
            register(ChunkerVanillaBlockType.RESIN_BLOCK, MclMappingDsl.simple("mcl_pale_oak:block_of_resin"))
            register(ChunkerVanillaBlockType.RESIN_BRICKS, MclMappingDsl.simple("mcl_pale_oak:resin_brick_block"))
            register(ChunkerVanillaBlockType.RESIN_BRICK_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_resin_brick"))
            register(ChunkerVanillaBlockType.RESIN_BRICK_SLAB, MclMappingDsl.slab("mcl_stairs:slab_resin_brick", "mcl_stairs:slab_resin_brick_top", "mcl_pale_oak:resin_brick_block"))
            register(ChunkerVanillaBlockType.RESIN_BRICK_WALL, MclMappingDsl.simple("mcl_pale_oak:resinbrickwall"))
            register(ChunkerVanillaBlockType.CHISELED_RESIN_BRICKS, MclMappingDsl.simple("mcl_pale_oak:chiseled_resin_brick"))

            // 试炼场与宝库结构 (Trial & Vault)
            register(ChunkerVanillaBlockType.TRIAL_SPAWNER, MclMappingDsl.trialSpawner())
            register(ChunkerVanillaBlockType.VAULT, MclMappingDsl.vault())
            register(ChunkerVanillaBlockType.HEAVY_CORE, MclMappingDsl.simple("mcl_tools:heavy_core"))
        }
    }
}