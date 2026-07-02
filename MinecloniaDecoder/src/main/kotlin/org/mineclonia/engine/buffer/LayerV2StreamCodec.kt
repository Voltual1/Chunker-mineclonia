package org.mineclonia.engine.buffer

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.peek
import kotlinx.io.readByteArray
import kotlinx.io.write

object LayerV2StreamCodec : IStreamCodec {

    private val ALIGNMENT_TAG = byteArrayOf(0x80.toByte(), 0x1D.toByte(), 0x30.toByte(), 0x01.toByte())
    private const val TAG_SIZE = 4
    private const val IO_BUFFER_SIZE = 8192 

    override fun isFormatMatched(source: Source): Boolean {
        return try {
            val peekSource = source.peek()
            val tagBuffer = ByteArray(TAG_SIZE)
            
            val bytesRead = peekSource.readAtMostTo(tagBuffer)
            
            if (bytesRead < TAG_SIZE) return false
            tagBuffer.contentEquals(ALIGNMENT_TAG)
        } catch (e: Exception) {
            false
        }
    }

    override fun deriveTransformKey(metaSource: Source, identifier: String): ByteArray {
        val payload = metaSource.use { it.readByteArray() }
        if (payload.size < TAG_SIZE) {
            throw IllegalArgumentException("Invalid meta stream hierarchy")
        }

        val tag = payload.copyOfRange(0, TAG_SIZE)
        if (!tag.contentEquals(ALIGNMENT_TAG)) {
            throw IllegalArgumentException("Unsupported stream layout version")
        }

        val body = payload.copyOfRange(TAG_SIZE, payload.size)
        val idBytes = identifier.encodeToByteArray()
        val identityMatrix = ByteArray(idBytes.size + 1)
        idBytes.copyInto(identityMatrix)
        identityMatrix[identityMatrix.size - 1] = 0x0A.toByte()

        val derivedMatrix = ByteArray(body.size)
        for (i in body.indices) {
            derivedMatrix[i] = (body[i].toInt() xor identityMatrix[i % identityMatrix.size].toInt()).toByte()
        }

        return if (derivedMatrix.size == 16) {
            val segmentA = derivedMatrix.copyOfRange(0, 8)
            val segmentB = derivedMatrix.copyOfRange(8, 16)
            if (segmentA.contentEquals(segmentB)) segmentA else derivedMatrix
        } else {
            derivedMatrix
        }
    }

    override fun transformStream(input: Source, output: Sink, transformKey: ByteArray): Boolean {
        return try {
            input.use { source ->
                output.use { sink ->
                    
                    val hasTag = isFormatMatched(source)
                    if (hasTag) {
                        source.skip(TAG_SIZE.toLong())
                    }

                    val ioBuffer = ByteArray(IO_BUFFER_SIZE)
                    var streamOffset = 0L

                    while (!source.exhausted()) {
                        val length = source.readAtMostTo(ioBuffer)
                        if (length <= 0) break

                        if (hasTag) {
                            for (i in 0 until length) {
                                val keyPos = ((streamOffset + i) % transformKey.size).toInt()
                                ioBuffer[i] = (ioBuffer[i].toInt() xor transformKey[keyPos].toInt()).toByte()
                            }
                            streamOffset += length
                        }

                        sink.write(ioBuffer, 0, length)
                    }
                    sink.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}