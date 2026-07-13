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
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode(targetName, param2 = param2)
    }

    // 3. 楼梯映射 (Stairs)
    fun stair(targetName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val baseDir = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.NORTH -> 2
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
            FacingDirectionHorizontal.SOUTH -> 3
            FacingDirectionHorizontal.NORTH -> 2
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
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.NORTH -> 2
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

    // 12. 箱子逻辑
    fun chest(baseNode: String) = BlockMapper { id ->
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
        MclNode("$baseNode$suffix", param2 = param2)
    }

    // 13. 潜影盒
    fun shulkerBox(mclColor: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.UP
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

    // 15. 红石比较器映射
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
            FacingDirection.SOUTH -> 0
            FacingDirection.WEST -> 1
            FacingDirection.NORTH -> 2
            FacingDirection.EAST -> 3
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
            FacingDirection.SOUTH -> 0
            FacingDirection.WEST -> 1
            FacingDirection.NORTH -> 2
            FacingDirection.EAST -> 3
        }.toByte()
        MclNode(baseName, param2 = param2)
    }

    // 20. 围栏门 (Gate)
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

    // 21. 双层高大植物
    fun doublePlant(basename: String) = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val suffix = if (half == Half.TOP) "_top" else ""
        MclNode("mcl_flowers:$basename$suffix")
    }

    // 22. 大垂滴叶
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

    // 23. 小垂滴叶
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

    // 24. 熔炉类激活态映射
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

    // 25. 投掷器、发射器等动力容器
    fun dispenserLike(baseName: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
        when (facing) {
            FacingDirection.UP -> MclNode("${baseName}_up")
            FacingDirection.DOWN -> MclNode("${baseName}_down")
            FacingDirection.SOUTH -> MclNode(baseName, param2 = 0)
            FacingDirection.WEST -> MclNode(baseName, param2 = 1)
            FacingDirection.NORTH -> MclNode(baseName, param2 = 2)
            FacingDirection.EAST -> MclNode(baseName, param2 = 3)
        }
    }

    // 26. 漏斗朝向与启停状态
    fun hopper() = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL_DOWN) ?: FacingDirectionHorizontalDown.DOWN
        val enabled = id.getState(VanillaBlockStates.ENABLED) == Bool.TRUE
        val state = if (enabled) "" else "_disabled"
        when (facing) {
            FacingDirectionHorizontalDown.DOWN -> MclNode("mcl_hoppers:hopper$state")
            FacingDirectionHorizontalDown.SOUTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 0)
            FacingDirectionHorizontalDown.WEST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 1)
            FacingDirectionHorizontalDown.NORTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 2)
            FacingDirectionHorizontalDown.EAST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 3)
        }
    }

    // 27. 木质多重部件双叶门逻辑 (Wood Doors)
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

    // 28. 木质活板门逻辑 (Wood Trapdoors)
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
            if (param2 == 21) param2 = 23 else if (param2 == 23) param2 = 21
        }
        MclNode(nodeName, param2 = param2.toByte())
    }

    // 29. 灯笼逻辑 (Lanterns)
    fun lantern(mclBase: String) = BlockMapper { id ->
        val hanging = id.getState(VanillaBlockStates.HANGING) == Bool.TRUE
        val suffix = if (hanging) "_ceiling" else "_floor"
        val param2: Byte = if (hanging) 0 else 1
        MclNode("$mclBase$suffix", param2 = param2)
    }

    // 30. 铜灯泡逻辑 (Copper Bulbs)
    fun copperBulb(mclBase: String) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val powered = id.getState(VanillaBlockStates.POWERED) == Bool.TRUE
        val state = if (lit) "_on" else "_off"
        val pSuffix = if (powered) "_powered" else ""
        MclNode("$mclBase$state$pSuffix")
    }
    
    // 31. 巨型蘑菇块逻辑 (Mushroom Blocks)
    // Mineclonia 格式: mcl_mushrooms:[color]_mushroom_block_cap_XXXXXX
    // 顺序: Up, Down, East(+X), West(-X), North(+Z), South(-Z)
    fun mushroomBlock(color: String) = BlockMapper { id ->
        val up = if (id.getState(VanillaBlockStates.UP) == Bool.TRUE) "1" else "0"
        val down = if (id.getState(VanillaBlockStates.DOWN) == Bool.TRUE) "1" else "0"
        val east = if (id.getState(VanillaBlockStates.EAST) == Bool.TRUE) "1" else "0"
        val west = if (id.getState(VanillaBlockStates.WEST) == Bool.TRUE) "1" else "0"
        val north = if (id.getState(VanillaBlockStates.NORTH) == Bool.TRUE) "1" else "0"
        val south = if (id.getState(VanillaBlockStates.SOUTH) == Bool.TRUE) "1" else "0"
        val bin = "$up$down$east$west$north$south"
        MclNode("mcl_mushrooms:${color}_mushroom_block_cap_$bin")
    }

    // 32. 堆肥桶逻辑 (Composter)
    fun composter() = BlockMapper { id ->
        val level = id.getState(VanillaBlockStates.COMPOSTER_LEVEL) ?: ComposterLevel._0
        val nodeName = when (level) {
            ComposterLevel._0 -> "mcl_composters:composter"
            ComposterLevel._8 -> "mcl_composters:composter_ready"
            else -> "mcl_composters:composter_${level.ordinal}"
        }
        MclNode(nodeName)
    }
    
    // 33. 营火逻辑 (Campfires)
    fun campfire(isSoul: Boolean) = BlockMapper { id ->
        val lit = id.getState(VanillaBlockStates.LIT) == Bool.TRUE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        
        // 确定基础节点名
        val baseName = if (isSoul) "mcl_campfires:soul_campfire" else "mcl_campfires:campfire"
        val nodeName = if (lit) "${baseName}_lit" else baseName
        
        // 朝向转换
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        
        MclNode(nodeName, param2 = param2)
    }

    // 34. 铁砧逻辑 (Anvils)
    fun anvil(damageLevel: Int) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        
        // Mineclonia: 0=undamaged, 1=slightly damaged, 2=very damaged
        val nodeName = when (damageLevel) {
            0 -> "mcl_anvils:anvil"
            1 -> "mcl_anvils:anvil_damage_1"
            else -> "mcl_anvils:anvil_damage_2"
        }
        
        // 朝向转换
        val param2 = when (facing) {
            FacingDirectionHorizontal.SOUTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.NORTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        
        MclNode(nodeName, param2 = param2)
    }

    // 35. 蘑菇柄逻辑 (Mushroom Stem)
    // 根据上下左右前后的表皮暴露状态转换到 Mineclonia 对应的 Stem 节点。
    // 如果六面都是表皮 (UP, DOWN, NORTH, SOUTH, EAST, WEST 都为 TRUE)，
    // 我们映射到 stem_full (在红、褐两类中，由于 Chunker 的 MUSHROOM_STEM 没有颜色，默认回退到 brown)
    fun mushroomStem() = BlockMapper { id ->
        val up = id.getState(VanillaBlockStates.UP) == Bool.TRUE
        val down = id.getState(VanillaBlockStates.DOWN) == Bool.TRUE
        val north = id.getState(VanillaBlockStates.NORTH) == Bool.TRUE
        val south = id.getState(VanillaBlockStates.SOUTH) == Bool.TRUE
        val east = id.getState(VanillaBlockStates.EAST) == Bool.TRUE
        val west = id.getState(VanillaBlockStates.WEST) == Bool.TRUE

        if (up && down && north && south && east && west) {
            MclNode("mcl_mushrooms:brown_mushroom_block_stem_full")
        } else {
            MclNode("mcl_mushrooms:brown_mushroom_block_stem")
        }
    }
    
    // 36. 砂轮逻辑 (Grindstone)
    // 根据 face (floor, wall, ceiling) 和 facing (n,s,e,w) 计算 Mineclonia 的特殊 param2
    fun grindstone() = BlockMapper { id ->
        val face = id.getState(VanillaBlockStates.GRINDSTONE_ATTACHMENT_TYPE) ?: GrindstoneAttachmentType.FLOOR
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        
        val param2: Byte = when (face) {
            GrindstoneAttachmentType.WALL -> when (facing) {
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
                FacingDirectionHorizontal.EAST -> 5
            }
            GrindstoneAttachmentType.FLOOR -> {
                if (facing == FacingDirectionHorizontal.NORTH || facing == FacingDirectionHorizontal.SOUTH) 0 else 1
            }
            GrindstoneAttachmentType.CEILING -> {
                if (facing == FacingDirectionHorizontal.NORTH || facing == FacingDirectionHorizontal.SOUTH) 20 else 21
            }
            else -> 0
        }.toByte()
        
        MclNode("mcl_grindstone:grindstone", param2 = param2)
    }
    
    // 37. 指向滴水石逻辑 (Pointed Dripstone)
    fun pointedDripstone() = BlockMapper { id ->
        val direction = id.getState(VanillaBlockStates.VERTICAL_DIRECTION) ?: VerticalDirection.UP
        val thickness = id.getState(VanillaBlockStates.DRIPSTONE_THICKNESS) ?: DripstoneThickness.TIP
        // vertical_direction: UP 为石笋 (Mineclonia 对应 bottom), DOWN 为钟乳石 (Mineclonia 对应 top)
        val dirStr = if (direction == VerticalDirection.UP) "bottom" else "top"
        val thickStr = when (thickness) {
            DripstoneThickness.TIP_MERGE -> "tip_merge"
            DripstoneThickness.TIP -> "tip"
            DripstoneThickness.FRUSTUM -> "frustum"
            DripstoneThickness.MIDDLE -> "middle"
            DripstoneThickness.BASE -> "base"
        }
        MclNode("mcl_dripstone:dripstone_${dirStr}_$thickStr")
    }

    // 38. 潜声脉络逻辑 (Sculk Vein)
    // 选取第一个存在的面进行 wallmounted 映射
    fun sculkVein() = BlockMapper { id ->
        val param2: Byte = when {
            id.getState(VanillaBlockStates.DOWN) == Bool.TRUE -> 1
            id.getState(VanillaBlockStates.UP) == Bool.TRUE -> 0
            id.getState(VanillaBlockStates.NORTH) == Bool.TRUE -> 2
            id.getState(VanillaBlockStates.SOUTH) == Bool.TRUE -> 3
            id.getState(VanillaBlockStates.WEST) == Bool.TRUE -> 4
            id.getState(VanillaBlockStates.EAST) == Bool.TRUE -> 5
            else -> 1
        }.toByte()
        MclNode("mcl_sculk:vein", param2 = param2)
    }
    
    // 39. 耕地水分逻辑 (Farmland)
    // moisture 如果为 _0 则是干耕地 (soil)；如果大于 _0 则为湿耕地 (soil_wet)
    fun farmland() = BlockMapper { id ->
        val moisture = id.getState(VanillaBlockStates.MOISTURE) ?: Moisture._0
        if (moisture == Moisture._0) {
            MclNode("mcl_farming:soil")
        } else {
            MclNode("mcl_farming:soil_wet")
        }
    }

    // 40. 幽匿尖啸体状态映射 (Sculk Shrieker)
    // can_summon 和 shrieking 作为状态传递，param2 代表是否激活
    fun sculkShrieker() = BlockMapper { id ->
        val shrieking = id.getState(VanillaBlockStates.SHRIEKING) == Bool.TRUE
        val param2: Byte = if (shrieking) 1 else 0
        MclNode("mcl_sculk:shrieker", param2 = param2)
    }
    
    // 41.发光地衣映射
    fun glowLichen() = BlockMapper { id ->
        val north = id.getState(VanillaBlockStates.WALL_NORTH) == Bool.TRUE
        val south = id.getState(VanillaBlockStates.WALL_SOUTH) == Bool.TRUE
        val east = id.getState(VanillaBlockStates.WALL_EAST) == Bool.TRUE
        val west = id.getState(VanillaBlockStates.WALL_WEST) == Bool.TRUE
        val up = id.getState(VanillaBlockStates.UP) == Bool.TRUE
        val down = id.getState(VanillaBlockStates.DOWN) == Bool.TRUE

        var cnt = 0
        if (north) cnt++
        if (south) cnt++
        if (east) cnt++
        if (west) cnt++
        if (up) cnt++
        if (down) cnt++

        if (cnt <= 1) {
            // 单面附着，使用 wallmounted 的 param2 表示
            val param2 = when {
                down -> 1  // 附着在地面 (下表面向上看) -> Minetest wallmounted 1 (挂在底面)
                up -> 0    // 附着在顶面 -> Minetest wallmounted 0
                east -> 2  // 附着在西面 -> 墙挂朝东 Minetest wallmounted 2
                west -> 3  // 附着在东面 -> 墙挂朝西 Minetest wallmounted 3
                north -> 4 // 附着在南面 -> 墙挂朝北 Minetest wallmounted 4
                south -> 5 // 附着在北面 -> 墙挂朝南 Minetest wallmounted 5
                else -> 1  // 默认挂在底面
            }.toByte()
            MclNode("mcl_core:glow_lichen", param2 = param2)
        } else {
            // 多面附着，使用多字母组合后缀拼接 (如 mcl_core:glow_lichen_nwed)
            val sb = java.lang.StringBuilder("mcl_core:glow_lichen_")
            if (north) sb.append("n")
            if (west) sb.append("w")
            if (south) sb.append("s")
            if (east) sb.append("e")
            if (up) sb.append("u")
            if (down) sb.append("d")
            MclNode(sb.toString(), param2 = 0)
        }
    }
    
    //藤蔓
    fun vine() = BlockMapper { id ->
        val north = id.getState(VanillaBlockStates.NORTH) == Bool.TRUE
        val south = id.getState(VanillaBlockStates.SOUTH) == Bool.TRUE
        val east = id.getState(VanillaBlockStates.EAST) == Bool.TRUE
        val west = id.getState(VanillaBlockStates.WEST) == Bool.TRUE
        val up = id.getState(VanillaBlockStates.UP) == Bool.TRUE

        // 默认转换为对应挂载墙面的 wallmounted param2
        val param2 = when {
            up -> 1 // 顶面
            east -> 2
            west -> 3
            north -> 4
            south -> 5
            else -> 1
        }.toByte()
        MclNode("mcl_core:vine", param2 = param2)
    }
}