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
import org.geysermc.pack.converter.util.KeyUtil;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.texture.Texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

@AutoService(TextureTransformer.class)
public class EnderDragonTransformer implements TextureTransformer {
    public static final List<DragonData> DRAGONS = List.of(
            new DragonData("entity/enderdragon/dragon.png", "entity/dragon/dragon.png"),
            new DragonData("entity/enderdragon/dragon_exploding.png", "entity/dragon/dragon_exploding.png")
    );

    @Override
    public void transform(@NotNull TransformContext context) throws IOException {
        for (DragonData dragonData : DRAGONS) {
            Texture javaTexture = context.poll(KeyUtil.key(Key.MINECRAFT_NAMESPACE, dragonData.javaKey()));
            if (javaTexture == null) continue;

            BufferedImage image = this.readImage(javaTexture);

            float scale = image.getHeight() / 256f;

            TransformGraphics.SourceImage regularSource = new TransformGraphics.SourceImage(image, 256, 256);

            TransformGraphics.of(context, dragonData.bedrockKey(), 256, 256, scale)
                    .addImage(regularSource) // Dragon
                    .addImage(regularSource, 0, 88, 56, 112, 56, 88) // More chicken- dragon wings? aw...
                    .finish();
        }
    }

    public record DragonData(String javaKey, String bedrockKey) {}
}
