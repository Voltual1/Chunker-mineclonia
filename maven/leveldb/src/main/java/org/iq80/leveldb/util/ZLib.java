/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iq80.leveldb.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * A wrapper for java based ZLib
 */
public final class ZLib
{
    private static final ThreadLocal<Inflater> INFLATER = ThreadLocal.withInitial(Inflater::new);
    private static final ThreadLocal<Inflater> INFLATER_RAW = ThreadLocal.withInitial(() -> new Inflater(true));
    private static final ThreadLocal<Deflater> DEFLATER = ThreadLocal.withInitial(Deflater::new);
    private static final ThreadLocal<Deflater> DEFLATER_RAW = ThreadLocal.withInitial(() -> new Deflater(Deflater.DEFAULT_COMPRESSION, true));

    private ZLib()
    {
    }

    public static ByteBuffer uncompress(ByteBuffer compressed, boolean raw) throws IOException
    {
        Inflater inflater = (raw ? INFLATER_RAW : INFLATER).get();
        try {
            // 1. 初始化输入数据
            if (compressed.hasArray()) {
                inflater.setInput(compressed.array(), compressed.arrayOffset() + compressed.position(), compressed.remaining());
            }
            else {
                byte[] bytes = new byte[compressed.remaining()];
                int position = compressed.position();
                compressed.get(bytes);
                compressed.position(position);
                inflater.setInput(bytes);
            }

            // 2. 使用 Android 兼容的 byte[] 缓冲区进行解压
            // 初始分配一个合理的临时字节数组（例如 4KB 或根据压缩包大小估算）
            int bufferSize = Math.max(1024, compressed.remaining() * 2);
            byte[] outArray = new byte[bufferSize];
            int totalUncompressedBytes = 0;

            while (!inflater.finished()) {
                // 如果当前临时数组空间不够了，进行扩容
                if (totalUncompressedBytes >= outArray.length) {
                    byte[] newArray = new byte[outArray.length + Math.max(1024, compressed.remaining() * 2)];
                    System.arraycopy(outArray, 0, newArray, 0, totalUncompressedBytes);
                    outArray = newArray;
                }

                // 调用 Android 绝对支持的 inflate(byte[] b, int off, int len) 方法
                int count = inflater.inflate(outArray, totalUncompressedBytes, outArray.length - totalUncompressedBytes);

                if (count == 0) {
                    // 如果 inflater 没结束，但解压出了 0 字节，说明外层循环需要扩容数组
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        break;
                    }
                    // 强制触发下一次循环的扩容
                    byte[] newArray = new byte[outArray.length + 1024];
                    System.arraycopy(outArray, 0, newArray, 0, totalUncompressedBytes);
                    outArray = newArray;
                }
                else {
                    totalUncompressedBytes += count;
                }
            }

            // 3. 将解压出来的 byte[] 包装回所需的 ByteBuffer 中
            ByteBuffer buffer = ByteBuffer.wrap(outArray, 0, totalUncompressedBytes);
            return buffer;
        }
        catch (DataFormatException e) {
            throw new IOException(e);
        }
        finally {
            inflater.reset();
        }
    }

    public static int compress(byte[] input, int inputOffset, int length, byte[] output, int outputOffset, boolean raw)
            throws IOException
    {
        Deflater deflater = (raw ? DEFLATER_RAW : DEFLATER).get();
        try {
            deflater.setInput(input, inputOffset, length);
            deflater.finish();

            return deflater.deflate(output, outputOffset, output.length - outputOffset);
        }
        finally {
            deflater.reset();
        }
    }
}
