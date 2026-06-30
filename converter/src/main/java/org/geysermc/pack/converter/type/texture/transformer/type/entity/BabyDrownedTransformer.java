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
import org.geysermc.pack.converter.util.ImageUtil;
import org.geysermc.pack.converter.util.KeyUtil;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.texture.Texture;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@AutoService(TextureTransformer.class)
public class BabyDrownedTransformer implements TextureTransformer {
    private static final String DROWNED_BABY_TEXTURE = "drowned_baby";
    private static final String DROWNED_BABY_OUTER_LAYER_TEXTURE = "drowned_outer_layer_baby";

    private static final String TEXTURE_PATH = "entity/zombie/%s.png";

    @Override
    public void transform(@NotNull TransformContext context) throws IOException {
        Texture drownedTexture = context.poll(KeyUtil.key(Key.MINECRAFT_NAMESPACE, String.format(TEXTURE_PATH, DROWNED_BABY_TEXTURE)));
        Texture outerLayerTexture = context.poll(KeyUtil.key(Key.MINECRAFT_NAMESPACE, String.format(TEXTURE_PATH, DROWNED_BABY_OUTER_LAYER_TEXTURE)));

        if (drownedTexture == null || outerLayerTexture == null) {
            return;
        }

        context.debug("Converting baby drowned texture");

        BufferedImage baseImage = this.readImage(drownedTexture);
        BufferedImage overlayImage = ImageUtil.ensureMinWidth(this.readImage(outerLayerTexture), baseImage.getWidth());

        Graphics2D g = baseImage.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(overlayImage, 0, 0, null);
        g.dispose();

        context.offer(KeyUtil.key(Key.MINECRAFT_NAMESPACE, String.format(TEXTURE_PATH, DROWNED_BABY_TEXTURE)), baseImage, "png");
    }
}
