package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld

class MclWorldWriter(val manager: MclConverterManager) : WorldWriter {

    override fun writeWorld(chunkerWorld: ChunkerWorld): ColumnWriter {
        // 可以在这里根据维度（chunkerWorld.dimension）做一些特殊处理
        // 目前我们的 manager 已经处理了坐标偏移
        return MclColumnWriter(manager)
    }

    override fun flushWorld(chunkerWorld: ChunkerWorld) {
        manager.flush()
    }
}