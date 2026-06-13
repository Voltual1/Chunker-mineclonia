package me.voltual.mcl

import com.hivemc.chunker.conversion.encoding.base.writer.ColumnWriter
import com.hivemc.chunker.conversion.encoding.base.writer.WorldWriter
import com.hivemc.chunker.conversion.intermediate.world.ChunkerWorld

class MclWorldWriter(val manager: MclConverterManager) : WorldWriter {
    override fun writeWorld(chunkerWorld: ChunkerWorld): ColumnWriter {
        return MclColumnWriter(manager)
    }

    override fun flushWorld(chunkerWorld: ChunkerWorld) {
        manager.flush()
    }
}