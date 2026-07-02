package org.mineclonia.engine.buffer

import kotlinx.io.Sink
import kotlinx.io.Source

interface IStreamCodec {
    fun isFormatMatched(source: Source): Boolean
    fun deriveTransformKey(metaSource: Source, identifier: String): ByteArray
    fun transformStream(input: Source, output: Sink, transformKey: ByteArray): Boolean
}