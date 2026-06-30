/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import java.awt.image.BufferedImage;
import java.io.IOException;

public class TransformGraphics {
    private final TransformContext context;
    private final String output;
    private final float scale;

    private final BufferedImage outputImg;

    public static TransformGraphics of(TransformContext context, String output, int vanillaWidth, int vanillaHeight, float scale) {
        return new TransformGraphics(context, output, vanillaWidth, vanillaHeight, scale);
    }

    private TransformGraphics(TransformContext context, String output, int vanillaWidth, int vanillaHeight, float scale) {
        this.context = context;
        this.output = output;
        this.scale = scale;

        this.outputImg = new BufferedImage((int) (vanillaWidth * scale), (int) (vanillaHeight * scale), BufferedImage.TYPE_INT_ARGB);
    }

    public TransformGraphics addImage(SourceImage source) {
        this.outputImg.getGraphics().drawImage(source.texture, 0, 0, null);

        return this;
    }

    public TransformGraphics addImage(SourceImage source, int desX, int desY) {
        this.outputImg.getGraphics().drawImage(source.texture, desX, desY, null);

        return this;
    }

    public TransformGraphics addImage(SourceImage source, int srcX, int srcY, int width, int height, int desX, int desY) {
        this.outputImg.getGraphics().drawImage(ImageUtil.crop(
                source.texture, source.x(srcX), source.y(srcY),
                source.x(width), source.y(height)
        ), (int) (desX * scale), (int) (desY * scale), null);

        return this;
    }

    public void finish() throws IOException {
        context.offer(KeyUtil.key(Key.MINECRAFT_NAMESPACE, this.output), this.outputImg, "PNG");
    }

    public static class SourceImage {
        private final BufferedImage texture;
        private final int vanillaWidth, vanillaHeight;

        public SourceImage(BufferedImage texture, int vanillaWidth, int vanillaHeight) {
            this.texture = texture;
            this.vanillaWidth = vanillaWidth;
            this.vanillaHeight = vanillaHeight;
        }

        public float x(int srcX) {
            return srcX * (this.texture.getWidth() / (float) vanillaWidth);
        }

        public float y(int srcY) {
            return srcY * (this.texture.getHeight() / (float) vanillaHeight);
        }
    }
}
