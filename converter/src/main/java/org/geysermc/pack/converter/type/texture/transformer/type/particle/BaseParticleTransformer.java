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

package org.geysermc.pack.converter.type.texture.transformer.type.particle;

import com.google.auto.service.AutoService;
import net.kyori.adventure.key.Key;
import org.geysermc.pack.converter.type.texture.transformer.TextureTransformer;
import org.geysermc.pack.converter.type.texture.transformer.TransformContext;
import org.geysermc.pack.converter.util.ImageUtil;
import org.geysermc.pack.converter.util.KeyUtil;
import org.geysermc.pack.converter.util.Spritesheet;
import org.jetbrains.annotations.NotNull;
import team.unnamed.creative.texture.Texture;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@AutoService(TextureTransformer.class)
public class BaseParticleTransformer implements TextureTransformer {
    private static final String PATH = "particle";
    private static final String OUTPUT = "particles.png";

    private static final List<TextureData> TEXTURES = List.of(
            new AtlasTextureData("generic", 8),
            new AtlasTextureData("splash", 8),
            new MultiTextureData("particle/bubble", "entity/fishing_hook", "particle/bubble_gray", null, "particle/flash"),
            new MultiTextureData("particle/flame", "particle/lava", "particle/soul_fire_flame", null), // TODO: Soul lava?
            new MultiTextureData("particle/note", "particle/critical_hit", "particle/enchanted_hit"),
            new MultiTextureData("particle/heart", "particle/angry", "particle/glint", null), // TODO: Villager?
            new MultiTextureData(null, null, "particle/glow"),
            new MultiTextureData("particle/drip_hang", "particle/drip_fall", "particle/drip_land", null, "particle/copper_fire_flame", null), // TODO: Copper lava??
            new AtlasTextureData("effect", 8),
            new AtlasTextureData("spell", 8),
            new AtlasTextureData("explosion", 16),
            new AtlasTextureData("glitter", 8),
            new AtlasTextureData("spark", 8),
            new MultiTextureData(new String[] { null }), // TODO: What are these textures?
            new MultiTextureData(null, "particle/sga_a", "particle/sga_b", "particle/sga_c", "particle/sga_d", "particle/sga_e", "particle/sga_f", "particle/sga_g", "particle/sga_h", "particle/sga_i", "particle/sga_j", "particle/sga_k", "particle/sga_l", "particle/sga_m", "particle/sga_n", "particle/sga_o"),
            new MultiTextureData("particle/sga_p", "particle/sga_q", "particle/sga_r", "particle/sga_s", "particle/sga_t", "particle/sga_u", "particle/sga_v", "particle/sga_w", "particle/sga_x", "particle/sga_y", "particle/sga_z")
    );

    private static final Map<String, Integer> PARTICLE_SCALES = Map.of(
            "particle/flash.png", 4
    );

    @Override
    public void transform(@NotNull TransformContext context) throws IOException {
        // Create a grayscale bubble image
        Texture bubbleTexture = context.peek(KeyUtil.key(Key.MINECRAFT_NAMESPACE, PATH + "/bubble.png"));
        if (bubbleTexture != null) {
            BufferedImage bubble = this.readImage(bubbleTexture);
            context.offer(KeyUtil.key(Key.MINECRAFT_NAMESPACE, PATH + "/bubble_gray.png"), ImageUtil.grayscale(bubble), "png");
        }

        // 临时跳过粒子精灵图的生成以防卡死/OOM
        // this.createSpritesheet(context);
    }

    private void createSpritesheet(@NotNull TransformContext context) throws IOException {
    }

    interface TextureData {
        Texture[] textures(@NotNull TransformContext context);
    }

    record AtlasTextureData(@NotNull String javaName, int atlasCount) implements TextureData {
        @NotNull
        public Key textureKey(int atlas) {
            return KeyUtil.key(Key.MINECRAFT_NAMESPACE, PATH + "/" + javaName + "_" + atlas + ".png");
        }

        @Override
        public Texture[] textures(@NotNull TransformContext context) {
            Texture[] textures = new Texture[this.atlasCount];
            for (int atlas = 0; atlas < this.atlasCount; atlas++) {
                Texture texture = context.poll(this.textureKey(atlas));
                textures[atlas] = texture;
            }

            return textures;
        }
    }

    record MultiTextureData(String @NotNull... textureNames) implements TextureData {

        @Override
        public Texture[] textures(@NotNull TransformContext context) {
            Texture[] textures = new Texture[this.textureNames.length];
            for (int i = 0; i < this.textureNames.length; i++) {
                String textureName = this.textureNames[i];
                if (textureName == null) {
                    continue;
                }

                Texture texture = context.poll(KeyUtil.key(Key.MINECRAFT_NAMESPACE, textureName + ".png"));
                if (texture == null) {
                    continue;
                }

                textures[i] = texture;
            }

            return textures;
        }
    }
}