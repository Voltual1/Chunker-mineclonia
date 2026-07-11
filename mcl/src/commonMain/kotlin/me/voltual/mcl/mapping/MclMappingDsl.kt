package me.voltual.mcl.mapping

import me.voltual.mcl.core.MclNode
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*

object MclMappingDsl {

    // 1. 简单无状态映射
    fun simple(targetName: String) = BlockMapper { _ ->
        MclNode(targetName)
    }

    // 2. 水平朝向映射 (facedir)
    fun directional(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 3. 楼梯映射 (Stairs)
    fun stair(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val baseDir = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.SOUTH -> 3
            FacingDirectionHorizontal.WEST -> 4
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
                FacingDirectionHorizontal.NORTH -> 8
                FacingDirectionHorizontal.EAST -> 9
                FacingDirectionHorizontal.SOUTH -> 10
                FacingDirectionHorizontal.WEST -> 11
            }
            AttachmentType.CEILING -> when (direction) {
                FacingDirectionHorizontal.NORTH -> 12
                FacingDirectionHorizontal.EAST -> 13
                FacingDirectionHorizontal.SOUTH -> 14
                FacingDirectionHorizontal.WEST -> 15
            }
            AttachmentType.WALL -> when (direction) {
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
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

    // 12. 修复后的箱子逻辑 (Chest / Trapped Chest)
    fun chest(baseNode: String) = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.CHEST_TYPE) ?: ChestType.SINGLE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        
        // Mineclonia Facedir 0-3
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()

        // 映射到 Mineclonia 实际渲染节点
        val suffix = when (type) {
            ChestType.SINGLE -> "_small"
            ChestType.LEFT -> "_left"
            ChestType.RIGHT -> "_right"
        }

        MclNode("$baseNode$suffix", param2 = param2)
    }

    // 13. 潜影盒特殊逻辑 (Shulker Box)
    fun shulkerBox(mclColor: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
        
        // Mineclonia 潜影盒在 init.lua 中的 shulker_box_rotations 映射
        // MC DOWN (0) -> MT 1 (floor)
        // MC UP (1)   -> MT 0 (ceiling)
        // MC NORTH (2)-> MT 4 (z-)
        // MC SOUTH (3)-> MT 5 (z+)
        // MC WEST (4) -> MT 3 (x-)
        // MC EAST (5) -> MT 2 (x+)
        val param2 = when (facing) {
            FacingDirection.DOWN -> 1
            FacingDirection.UP -> 0
            FacingDirection.NORTH -> 4
            FacingDirection.SOUTH -> 5
            FacingDirection.WEST -> 3
            FacingDirection.EAST -> 2
        }.toByte()

        MclNode("mcl_chests:${mclColor}_shulker_box_small", param2 = param2)
    }

    // 14. 红石中继器映射
    fun repeater() = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val locked = id.getState(VanillaBlockStates.LOCKED) ?: Bool.FALSE
        val delay = id.getState(VanillaBlockStates.DELAY) ?: Delay._1
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val baseDir = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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

    // 15. 红石比较器映射
    fun comparator() = BlockMapper { id ->
        val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
        val mode = id.getState(VanillaBlockStates.MODE_COMPARATOR) ?: ComparatorMode.COMPARE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val baseDir = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }
        val state = if (powered == Bool.TRUE) "on" else "off"
        val modeStr = if (mode == ComparatorMode.COMPARE) "comp" else "sub"
        val nodeName = "mcl_comparators:comparator_${state}_${modeStr}"
        val param2 = (4 * power.ordinal + baseDir).toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 16. 阳光检测器映射
    fun daylightDetector() = BlockMapper { id ->
        val inverted = id.getState(VanillaBlockStates.INVERTED) ?: Bool.FALSE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0
        val nodeName = if (inverted == Bool.TRUE) "mcl_daylight_detector:daylight_detector_inverted" else "mcl_daylight_detector:daylight_detector"
        MclNode(nodeName, param2 = power.ordinal.toByte())
    }

    // 17. 红石粉导线映射
    fun redstoneWire() = BlockMapper { id ->
        val north = id.getState(VanillaBlockStates.REDSTONE_NORTH) ?: RedstoneConnection.NONE
        val east = id.getState(VanillaBlockStates.REDSTONE_EAST) ?: RedstoneConnection.NONE
        val south = id.getState(VanillaBlockStates.REDSTONE_SOUTH) ?: RedstoneConnection.NONE
        val west = id.getState(VanillaBlockStates.REDSTONE_WEST) ?: RedstoneConnection.NONE
        val power = id.getState(VanillaBlockStates.POWER) ?: Power._0

        var wireflags = 0
        if (north != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x01
            if (north == RedstoneConnection.UP) wireflags = wireflags or 0x10
        }
        if (west != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x02
            if (west == RedstoneConnection.UP) wireflags = wireflags or 0x20
        }
        if (south != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x04
            if (south == RedstoneConnection.UP) wireflags = wireflags or 0x40
        }
        if (east != RedstoneConnection.NONE) {
            wireflags = wireflags or 0x08
            if (east == RedstoneConnection.UP) wireflags = wireflags or 0x80
        }

        val nodeName = if (wireflags == 0) "mcl_redstone:redstone" 
                       else "mcl_redstone:wire_" + wireflags.toString(16).padStart(2, '0')
        
        MclNode(nodeName, param2 = power.ordinal.toByte())
    }

    // 18. 活塞映射
    fun piston(sticky: Boolean) = BlockMapper { id ->
        val extended = id.getState(VanillaBlockStates.EXTENDED) ?: Bool.FALSE
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        val base = if (sticky) "mcl_pistons:piston_sticky" else "mcl_pistons:piston"
        val state = if (extended == Bool.TRUE) "_on" else "_off"
        
        val param2 = when (facing) {
            FacingDirection.NORTH -> 0
            FacingDirection.EAST -> 1
            FacingDirection.SOUTH -> 2
            FacingDirection.WEST -> 3
            FacingDirection.DOWN -> 20
            FacingDirection.UP -> 4
        }.toByte()
        MclNode("$base$state", param2 = param2)
    }

    // 19. 活塞臂推杆
    fun pistonHead() = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.PISTON_TYPE) ?: PistonType.NORMAL
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        val baseName = if (type == PistonType.STICKY) "mcl_pistons:piston_pusher_sticky" else "mcl_pistons:piston_pusher"
        val param2 = when (facing) {
            FacingDirection.DOWN -> 15
            FacingDirection.UP -> 1
            FacingDirection.NORTH -> 0
            FacingDirection.EAST -> 1
            FacingDirection.SOUTH -> 2
            FacingDirection.WEST -> 3
        }.toByte()
        MclNode(baseName, param2 = param2)
    }

    // 20. 门映射
    fun door(customBase: String) = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val open = id.getState(VanillaBlockStates.OPEN) == Bool.TRUE
        val hinge = id.getState(VanillaBlockStates.DOOR_HINGE) ?: HingeSide.LEFT
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val style = if (hinge == HingeSide.RIGHT) "2" else "1"
        val part = if (half == Half.TOP) "t" else "b"
        var param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }
        if (half == Half.TOP) {
            param2 += 20
        }
        MclNode(nodeName, param2 = param2.toByte())
    }

    // 22. 液体映射 (重复项已检查)
    fun liquidMapping(source: String, flowing: String) = BlockMapper { id ->
        val level = id.getState(VanillaBlockStates.LIQUID_LEVEL) ?: LiquidLevel._0
        val isFlowing = id.getState(VanillaBlockStates.FLOWING) == Bool.TRUE
        MclNode(if (isFlowing) flowing else source, param2 = level.ordinal.toByte())
    }
}