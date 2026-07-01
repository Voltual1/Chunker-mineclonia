/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/PackConverter
 *
 */

package org.geysermc.pack.converter.type.texture.transformer;

import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.util.ImageUtil;
import org.geysermc.pack.converter.util.KeyUtil;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.texture.Texture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;

public interface TextureTransformer {
    int ORDER_FIRST = 0;
    int ORDER_EARLY = 25;
    int ORDER_NORMAL = 50;
    int ORDER_LATE = 75;
    int ORDER_LAST = 100;

    void transform(@NotNull TransformContext context) throws IOException;

    default BufferedImage readImage(@NotNull Texture texture) throws IOException {
        return ImageUtil.ensure32BitImage(ImageIO.read(new ByteArrayInputStream(texture.data().toByteArray())));
    }

    default int order() {
        return ORDER_NORMAL;
    }

    default void gridTransform(@NotNull TransformContext context, boolean poll, int rows, int columns, String bedrockOutput, String... javaInputs) throws IOException {
        gridTransform(
                context,
                poll,
                rows,
                columns,
                KeyUtil.key(Key.MINECRAFT_NAMESPACE, bedrockOutput),
                Arrays.stream(javaInputs).map(
                        str ->
                                str == null ? null : KeyUtil.key(Key.MINECRAFT_NAMESPACE, str)).toArray(Key[]::new));
    }

    // Adds images in rows and columns
    default void gridTransform(@NotNull TransformContext context, boolean poll, int rows, int columns, Key bedrockOutput, Key... javaInputs) throws IOException {
        // 临时跳过网格精灵图生成，防止转换器卡死
        return;
    }
}