package me.voltual.mcl.mapping.modules

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.VanillaBlockStates
import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.states.vanilla.types.*
import me.voltual.mcl.mapping.BlockMapper
import me.voltual.mcl.mapping.MclMappingModule
import me.voltual.mcl.mapping.MclMappingRegistry
import me.voltual.mcl.mapping.MclMappingDsl
import me.voltual.mcl.core.MclNode

object MclRedstoneMapping : MclMappingModule {
    override fun register() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl

        registry.register(ChunkerVanillaBlockType.REDSTONE_WIRE, dsl.redstoneWire())
        registry.register(ChunkerVanillaBlockType.REDSTONE_BLOCK, dsl.simple("mcl_redstone_torch:redstoneblock"))
        registry.register(ChunkerVanillaBlockType.REDSTONE_LAMP, dsl.furnaceLike("mcl_redstone_lamp:lamp_off", "mcl_redstone_lamp:lamp_on"))
        
        // ==========================================
        // 【关键修复】：将落地红石火把改为 litFloorMounted
        // ==========================================
        registry.register(ChunkerVanillaBlockType.REDSTONE_TORCH, dsl.litFloorMounted("mcl_redstone_torch:redstone_torch_off", "mcl_redstone_torch:redstone_torch_on"))
        registry.register(ChunkerVanillaBlockType.REDSTONE_WALL_TORCH, dsl.wallTorch("mcl_redstone_torch:redstone_torch_off_wall", "mcl_redstone_torch:redstone_torch_on_wall"))

        registry.register(ChunkerVanillaBlockType.REPEATER, dsl.repeater())
        registry.register(ChunkerVanillaBlockType.COMPARATOR, dsl.comparator())
        registry.register(ChunkerVanillaBlockType.TARGET, dsl.litOre("mcl_target:target_off", "mcl_target:target_on"))
        registry.register(ChunkerVanillaBlockType.DAYLIGHT_DETECTOR, dsl.daylightDetector())

        registry.register(ChunkerVanillaBlockType.OBSERVER, BlockMapper { id ->
            val facing = id.getState(VanillaBlockStates.FACING_ALL) ?: FacingDirection.NORTH
            val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
            val suffix = if (powered == Bool.TRUE) "_on" else "_off"
            
            val (nodeBase, param2) = when (facing) {
                FacingDirection.DOWN -> "mcl_observers:observer_down" to 0
                FacingDirection.UP -> "mcl_observers:observer_up" to 0
                FacingDirection.NORTH -> "mcl_observers:observer" to 2
                FacingDirection.SOUTH -> "mcl_observers:observer" to 0
                FacingDirection.EAST -> "mcl_observers:observer" to 1
                FacingDirection.WEST -> "mcl_observers:observer" to 3
            }
            MclNode("$nodeBase$suffix", param2 = (param2 as Int).toByte())
        })

        registry.register(ChunkerVanillaBlockType.LEVER, BlockMapper { id ->
            val powered = id.getState(VanillaBlockStates.POWERED) ?: Bool.FALSE
            val attach = id.getState(VanillaBlockStates.ATTACHMENT_TYPE) ?: AttachmentType.WALL
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            val state = if (powered == Bool.TRUE) "_on" else "_off"
            
            val param2 = when (attach) {
                AttachmentType.FLOOR -> if (facing == FacingDirectionHorizontal.NORTH || facing == FacingDirectionHorizontal.SOUTH) 8 else 10
                AttachmentType.CEILING -> if (facing == FacingDirectionHorizontal.NORTH || facing == FacingDirectionHorizontal.SOUTH) 15 else 13
                AttachmentType.WALL -> when (facing) {
                    FacingDirectionHorizontal.NORTH -> 4
                    FacingDirectionHorizontal.SOUTH -> 5
                    FacingDirectionHorizontal.EAST -> 3
                    FacingDirectionHorizontal.WEST -> 2
                }
            }.toByte()
            MclNode("mcl_lever:lever$state", param2 = param2)
        })

        registry.register(ChunkerVanillaBlockType.STONE_BUTTON, dsl.button("stone"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_BUTTON, dsl.button("polished_blackstone"))
        registry.register(ChunkerVanillaBlockType.STONE_PRESSURE_PLATE, dsl.pressurePlate("stone"))
        registry.register(ChunkerVanillaBlockType.POLISHED_BLACKSTONE_PRESSURE_PLATE, dsl.pressurePlate("polished_blackstone"))
        registry.register(ChunkerVanillaBlockType.LIGHT_WEIGHTED_PRESSURE_PLATE, dsl.pressurePlate("light"))
        registry.register(ChunkerVanillaBlockType.HEAVY_WEIGHTED_PRESSURE_PLATE, dsl.pressurePlate("heavy"))

        registry.register(ChunkerVanillaBlockType.PISTON, dsl.piston(false))
        registry.register(ChunkerVanillaBlockType.STICKY_PISTON, dsl.piston(true))
        registry.register(ChunkerVanillaBlockType.PISTON_HEAD, dsl.pistonHead())

        registry.register(ChunkerVanillaBlockType.DISPENSER, dsl.dispenserLike("mcl_dispensers:dispenser"))
        registry.register(ChunkerVanillaBlockType.DROPPER, dsl.dispenserLike("mcl_dispensers:dropper"))
        registry.register(ChunkerVanillaBlockType.HOPPER, dsl.hopper())
        registry.register(ChunkerVanillaBlockType.NOTE_BLOCK, dsl.simple("mcl_noteblock:noteblock"))

        registry.register(ChunkerVanillaBlockType.LECTERN, BlockMapper { id ->
            val facing = id.getState(VanillaBlockStates.FACING_HORIZONTAL) ?: FacingDirectionHorizontal.NORTH
            val hasBook = id.getState(VanillaBlockStates.HAS_BOOK) == Bool.TRUE
            val powered = id.getState(VanillaBlockStates.POWERED) == Bool.TRUE

            val nodeName = if (hasBook) "mcl_lectern:lectern_with_book" else "mcl_lectern:lectern"
            
            var param2 = when (facing) {
                FacingDirectionHorizontal.SOUTH -> 0
                FacingDirectionHorizontal.EAST -> 1
                FacingDirectionHorizontal.NORTH -> 2
                FacingDirectionHorizontal.WEST -> 3
            }

            if (powered) {
                param2 += 128
            }

            MclNode(nodeName, param2 = param2.toByte())
        })
        
        registry.register(ChunkerVanillaBlockType.IRON_TRAPDOOR, dsl.trapdoor("mcl_doors:iron_trapdoor"))
        registry.register(ChunkerVanillaBlockType.IRON_DOOR, dsl.door("mcl_doors:iron_door"))

        registerCopperTrapdoors()
    }

    private fun registerCopperTrapdoors() {
        val registry = MclMappingRegistry
        val dsl = MclMappingDsl
        val prefixes = listOf("", "EXPOSED_", "WEATHERED_", "OXIDIZED_")
        
        for (prefix in prefixes) {
            val mcBase = "${prefix}COPPER_TRAPDOOR"
            val mclBase = "mcl_copper:${prefix.lowercase()}copper_trapdoor"
            
            registry.register(enumValueOf(mcBase), dsl.trapdoor(mclBase))
            registry.register(enumValueOf("WAXED_$mcBase"), dsl.trapdoor(mclBase))
        }
    }
}