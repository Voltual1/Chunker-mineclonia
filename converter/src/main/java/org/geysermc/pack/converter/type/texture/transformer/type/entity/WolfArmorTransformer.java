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
public class WolfArmorTransformer implements TextureTransformer {
    private static final Key JAVA_ARMOR = KeyUtil.key("entity/equipment/wolf_body/armadillo_scute.png");
    private static final Key JAVA_OVERLAY = KeyUtil.key("entity/equipment/wolf_body/armadillo_scute_overlay.png");
    private static final Key JAVA_LOW_CRACK = KeyUtil.key("entity/wolf/wolf_armor_crackiness_low.png");
    private static final Key JAVA_MEDIUM_CRACK = KeyUtil.key("entity/wolf/wolf_armor_crackiness_medium.png");
    private static final Key JAVA_HIGH_CRACK = KeyUtil.key("entity/wolf/wolf_armor_crackiness_high.png");

    private static final Key BEDROCK_ARMOR = KeyUtil.key("entity/wolf/wolf_armor.png");
    private static final Key BEDROCK_OVERLAY = KeyUtil.key("entity/wolf/wolf_armor_dyed.tga");
    private static final Key BEDROCK_LOW_CRACK = KeyUtil.key("entity/wolf/wolf_armor_cracked_low.png");
    private static final Key BEDROCK_MEDIUM_CRACK = KeyUtil.key("entity/wolf/wolf_armor_cracked_medium.png");
    private static final Key BEDROCK_HIGH_CRACK = KeyUtil.key("entity/wolf/wolf_armor_cracked_high.png");

    @Override
    public void transform(@NotNull TransformContext context) throws IOException {
        if (context.isTexturePresent(JAVA_OVERLAY)) {
            Texture armorTexture = context.peekOrVanilla(JAVA_ARMOR);

            if (armorTexture == null) {
                context.warn("Unable to find wolf armor texture file from vanilla files.");
                return;
            }

            BufferedImage armorImage = this.readImage(armorTexture);

            Texture overlay = context.poll(JAVA_OVERLAY);
            BufferedImage overlayImage = this.readImage(overlay);

            // Scale to the base image so ensure we don't go out of bounds in the image
            float resizeX = (float) armorImage.getWidth() / overlayImage.getWidth();
            float resizeY = (float) armorImage.getHeight() / overlayImage.getHeight();
            overlayImage = ImageUtil.scale(overlayImage, resizeX, resizeY);

            for (int x = 0; x < armorImage.getWidth(); x++) {
                for (int y = 0; y < armorImage.getHeight(); y++) {
                    Color c = new Color(overlayImage.getRGB(x, y), true);

                    if (c.getAlpha() == 255) {
                        Color newCol = new Color(c.getRed(), c.getGreen(), c.getBlue(), 2);

                        armorImage.setRGB(x, y, ImageUtil.colorToARGB(newCol));
                    }
                }
            }

            context.offer(BEDROCK_OVERLAY, armorImage, "png");
        }

        handle(context, JAVA_ARMOR, BEDROCK_ARMOR);

        handle(context, JAVA_LOW_CRACK, BEDROCK_LOW_CRACK);
        handle(context, JAVA_MEDIUM_CRACK, BEDROCK_MEDIUM_CRACK);
        handle(context, JAVA_HIGH_CRACK, BEDROCK_HIGH_CRACK);
    }

    private void handle(TransformContext context, Key javaKey, Key bedrockKey) throws IOException {
        if (context.isTexturePresent(javaKey)) {
            Texture texture = context.poll(javaKey);
            if (texture == null) return;

            BufferedImage image = this.readImage(texture);

            context.offer(bedrockKey, ImageUtil.expandCanvas(
                    image, image.getWidth(), image.getWidth()
            ), "PNG");
        }
    }
}
