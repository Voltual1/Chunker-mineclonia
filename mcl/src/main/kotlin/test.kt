package me.voltual.mcl

import com.hivemc.chunker.conversion.intermediate.column.chunk.identifier.type.block.ChunkerVanillaBlockType.*

fun initMclMappings() {
    // 注册简单方块
    MclMappingRegistry.register(STONE, MclMappingDsl.simple("mcl_core:stone"))
    MclMappingRegistry.register(DIRT, MclMappingDsl.simple("mcl_core:dirt"))
    
    // 注册原木 (带 Axis 状态)
    MclMappingRegistry.register(OAK_LOG, MclMappingDsl.log("mcl_trees:tree_oak"))
    MclMappingRegistry.register(SPRUCE_LOG, MclMappingDsl.log("mcl_trees:tree_spruce"))

    // 注册楼梯 (带 Facing 和 Half 状态)
    MclMappingRegistry.register(OAK_STAIRS, MclMappingDsl.stair("mcl_stairs:stair_oak"))

    // 注册台阶 (Slabs)
    MclMappingRegistry.register(
        OAK_SLAB, 
        MclMappingDsl.slab("mcl_stairs:slab_oak", "mcl_stairs:slab_oak_top", "mcl_stairs:slab_oak_double")
    )
}