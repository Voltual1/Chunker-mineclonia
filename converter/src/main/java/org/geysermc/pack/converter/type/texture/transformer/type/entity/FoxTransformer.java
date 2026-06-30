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

package org.geysermc.pack.converter.type.texture.transformer.type.entity;

import com.google.auto.service.AutoService;
import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.type.texture.transformer.TextureTransformer;
import org.geysermc.pack.converter.type.texture.transformer.TransformContext;
import org.geysermc.pack.converter.type.texture.transformer.TransformGraphics;
import org.geysermc.pack.converter.util.ImageUtil;
import org.geysermc.pack.converter.util.KeyUtil;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.texture.Texture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@AutoService(TextureTransformer.class)
public class FoxTransformer implements TextureTransformer {
    public static final List<FoxData> FOXES = List.of(
            new FoxData("entity/fox/fox.png", "entity/fox/fox_sleep.png"),
            new FoxData("entity/fox/fox_snow.png", "entity/fox/fox_snow_sleep.png", "entity/fox/artic_fox.png")
    );

    @Override
    public void transform(@NotNull TransformContext context) throws IOException {
        for (FoxData foxData : FOXES) {
            if (!context.isTexturePresent(KeyUtil.key(foxData.javaKey())) || !context.isTexturePresent(KeyUtil.key(foxData.javaSleepingKey()))) continue;

            Texture javaTexture = context.pollOrPeekVanilla(KeyUtil.key(Key.MINECRAFT_NAMESPACE, foxData.javaKey()));
            if (javaTexture == null) {
                context.warn("Unable to find fox texture %s.".formatted(foxData.javaKey()));
                continue;
            }

            Texture javaSleepingTexture = context.pollOrPeekVanilla(KeyUtil.key(Key.MINECRAFT_NAMESPACE, foxData.javaSleepingKey()));
            if (javaSleepingTexture == null) {
                context.warn("Unable to find fox texture %s.".formatted(foxData.javaSleepingKey()));
                continue;
            }

            BufferedImage regularFox = this.readImage(javaTexture);
            BufferedImage sleepingFox = this.readImage(javaSleepingTexture);

            float scale = regularFox.getHeight() / 32f;

            TransformGraphics.SourceImage regularSource = new TransformGraphics.SourceImage(regularFox, 48, 32);
            TransformGraphics.SourceImage sleepingSource = new TransformGraphics.SourceImage(sleepingFox, 48, 32);

            TransformGraphics.of(context, foxData.bedrockKey(), 64, 32, scale)
                    .addImage(regularSource, 1, 5, 28, 12, 0, 0) // Regular head
                    .addImage(sleepingSource, 1, 5, 28, 12, 0, 12) // Sleepy head
                    .addImage(regularSource, 8, 1, 5, 3, 0, 0) // Ear 1
                    .addImage(regularSource, 15, 1, 5, 3, 16, 0) // Ear 2
                    .addImage(regularSource, 30, 0, 18, 14, 28, 0) // Tail
                    .addImage(regularSource, 6, 18, 14, 5, 0, 24) // Nose
                    .addImage(regularSource, 4, 24, 8, 8, 14, 24) // Leg 1
                    .addImage(regularSource, 13, 24, 8, 8, 22, 24) // Leg 2
                    .addImage(regularSource, 24, 21, 24, 11, 30, 21) // Body 1
                    .addImage(regularSource, 30, 15, 12, 6, 36, 15) // Body 2
                    .finish();
        }
    }

    public record FoxData(String javaKey, String javaSleepingKey, String bedrockKey) {
        public FoxData(String javaKey, String javaSleepingKey) {
            this(javaKey, javaSleepingKey, javaKey);
        }
    }
}
