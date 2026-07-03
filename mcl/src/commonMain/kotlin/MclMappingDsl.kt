package me.voltual.mcl

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

    // 12. 红石中继器映射
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

    // 13. 红石比较器映射
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
            FacingDirection.NORTH -> 0
            FacingDirection.EAST -> 1
            FacingDirection.SOUTH -> 2
            FacingDirection.WEST -> 3
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
            FacingDirection.NORTH -> 0
            FacingDirection.EAST -> 1
            FacingDirection.SOUTH -> 2
            FacingDirection.WEST -> 3
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
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 5
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode("mcl_beds:bed_${color}_$suffix", param2 = param2)
    }

    // 24. 旗帜
    fun banner(color: String, wall: Boolean) = BlockMapper { id ->
        val nodeName = if (wall) "mcl_banners:hanging_banner" else "mcl_banners:standing_banner"
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = if (wall) {
            when (facing) {
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.EAST -> 5
                FacingDirectionHorizontal.SOUTH -> 3
                FacingDirectionHorizontal.WEST -> 4
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 35. 小垂滴叶
    fun smallDripleaf() = BlockMapper { id ->
        val half = id.getState(VanillaBlockStates.HALF) ?: Half.BOTTOM
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val nodeName = if (half == Half.TOP) "mcl_lush_caves:dripleaf_small" else "mcl_lush_caves:dripleaf_small_stem"
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 40. 饰面罐 (Decorated Pot)
    fun decoratedPot() = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
        }.toByte()
        MclNode(nodeName, param2 = param2)
    }

    // 42. 箱子系列
    fun chestLike(baseName: String) = BlockMapper { id ->
        val type = id.getState(VanillaBlockStates.CHEST_TYPE) ?: ChestType.SINGLE
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirection.NORTH -> MclNode(baseName, param2 = 0)
            FacingDirection.EAST -> MclNode(baseName, param2 = 1)
            FacingDirection.SOUTH -> MclNode(baseName, param2 = 2)
            FacingDirection.WEST -> MclNode(baseName, param2 = 3)
        }
    }

    // 44. 漏斗
    fun hopper() = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL_DOWN) ?: FacingDirectionHorizontalDown.DOWN
        val enabled = id.getState(VanillaBlockStates.ENABLED) == Bool.TRUE
        val state = if (enabled) "" else "_disabled"
        when (facing) {
            FacingDirectionHorizontalDown.DOWN -> MclNode("mcl_hoppers:hopper$state")
            FacingDirectionHorizontalDown.NORTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 0)
            FacingDirectionHorizontalDown.EAST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 1)
            FacingDirectionHorizontalDown.SOUTH -> MclNode("mcl_hoppers:hopper_side$state", param2 = 2)
            FacingDirectionHorizontalDown.WEST -> MclNode("mcl_hoppers:hopper_side$state", param2 = 3)
        }
    }

    // 45. 门门禁书架 (雕版书架)
    fun shelf(basename: String) = BlockMapper { id ->
        val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
        val param2 = when (facing) {
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirectionHorizontal.NORTH -> 0
            FacingDirectionHorizontal.EAST -> 1
            FacingDirectionHorizontal.SOUTH -> 2
            FacingDirectionHorizontal.WEST -> 3
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
            FacingDirection.NORTH -> 4
            FacingDirection.EAST -> 16
            FacingDirection.SOUTH -> 8
            FacingDirection.WEST -> 12
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
            FacingDirection.NORTH -> 4
            FacingDirection.EAST -> 16
            FacingDirection.SOUTH -> 8
            FacingDirection.WEST -> 12
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