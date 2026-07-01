/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Oleg V. Khaschansky
 */
package org.apache.harmony.awt.gl.color;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class ColorConverter {
    private ColorScaler scaler = new ColorScaler();

    public void loadScalingData(ColorSpace cs) {
        scaler.loadScalingData(cs);
    }

    private static android.graphics.ColorSpace getAndroidColorSpace(ColorSpace cs) {
        if (cs == null) {
            return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);
        }
        if (cs.isCS_sRGB()) {
            return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);
        }

        int type = cs.getType();
        switch (type) {
            case ColorSpace.TYPE_RGB:
                if (cs == LUTColorConverter.LINEAR_RGB_CS) {
                    return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_SRGB);
                }
                return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);
                
            case ColorSpace.TYPE_GRAY:
                if (cs == LUTColorConverter.LINEAR_GRAY_CS) {
                    return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB);
                }
                return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);
                
            case ColorSpace.TYPE_XYZ:
                return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.CIE_XYZ);
                
            default:
                return android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB);
        }
    }

    public void translateColor(ICC_Transform t, BufferedImage src, BufferedImage dst) {
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM = dst.getColorModel();

        ColorSpace srcCS = srcCM.getColorSpace();
        ColorSpace dstCS = dstCM.getColorSpace();

        android.graphics.ColorSpace aSrcCS = getAndroidColorSpace(srcCS);
        android.graphics.ColorSpace aDstCS = getAndroidColorSpace(dstCS);
        android.graphics.ColorSpace.Connector connector = android.graphics.ColorSpace.connect(aSrcCS, aDstCS);

        int w = src.getWidth();
        int h = src.getHeight();

        WritableRaster srcRaster = src.getRaster();
        WritableRaster dstRaster = dst.getRaster();

        int srcNumColorCaps = srcCM.getNumColorComponents();
        int dstNumColorCaps = dstCM.getNumColorComponents();

        boolean srcHasAlpha = srcCM.hasAlpha();
        boolean dstHasAlpha = dstCM.hasAlpha();

        // 提前分配所有缓存，避免在百万次循环中触发 GC
        float[] srcPixel = new float[srcCM.getNumComponents()];
        float[] dstPixel = new float[dstCM.getNumComponents()];
        float[] rgb = new float[3];
        Object srcData = null;
        Object dstData = null;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 复用 srcData 和 srcPixel，零对象分配
                srcData = srcRaster.getDataElements(x, y, srcData);
                srcCM.getNormalizedComponents(srcData, srcPixel, 0);

                if (srcNumColorCaps >= 3) {
                    rgb[0] = srcPixel[0];
                    rgb[1] = srcPixel[1];
                    rgb[2] = srcPixel[2];
                } else if (srcNumColorCaps == 1) {
                    rgb[0] = srcPixel[0];
                    rgb[1] = srcPixel[0];
                    rgb[2] = srcPixel[0];
                } else {
                    rgb[0] = srcPixel[0];
                    rgb[1] = srcPixel[1 % srcNumColorCaps];
                    rgb[2] = srcPixel[2 % srcNumColorCaps];
                }

                // Android 的 transform(float[]) 支持原地修改 (in-place modification)
                connector.transform(rgb);

                if (dstNumColorCaps >= 3) {
                    dstPixel[0] = rgb[0];
                    dstPixel[1] = rgb[1];
                    dstPixel[2] = rgb[2];
                } else if (dstNumColorCaps == 1) {
                    dstPixel[0] = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
                } else {
                    dstPixel[0] = rgb[0];
                    if (dstNumColorCaps > 1) dstPixel[1] = rgb[1];
                    if (dstNumColorCaps > 2) dstPixel[2] = rgb[2];
                }

                if (dstHasAlpha) {
                    if (srcHasAlpha) {
                        dstPixel[dstNumColorCaps] = srcPixel[srcNumColorCaps];
                    } else {
                        dstPixel[dstNumColorCaps] = 1.0f;
                    }
                }

                // 复用 dstData，零对象分配
                dstData = dstCM.getDataElements(dstPixel, 0, dstData);
                dstRaster.setDataElements(x, y, dstData);
            }
        }
    }

    public float[][] translateColor(ICC_Transform t,
            float buffer[][],
            ColorSpace srcCS,
            ColorSpace dstCS,
            int nPixels) {

        android.graphics.ColorSpace aSrcCS = getAndroidColorSpace(srcCS);
        android.graphics.ColorSpace aDstCS = getAndroidColorSpace(dstCS);
        android.graphics.ColorSpace.Connector connector = android.graphics.ColorSpace.connect(aSrcCS, aDstCS);

        float[] rgb = new float[3];
        int dstChannels = aDstCS.getComponentCount();

        for (int i = 0; i < nPixels; i++) {
            float[] pixel = buffer[i];

            if (pixel.length >= 3) {
                rgb[0] = pixel[0];
                rgb[1] = pixel[1];
                rgb[2] = pixel[2];
            } else if (pixel.length == 1) {
                rgb[0] = pixel[0];
                rgb[1] = pixel[0];
                rgb[2] = pixel[0];
            } else {
                rgb[0] = pixel[0];
                rgb[1] = pixel[1 % pixel.length];
                rgb[2] = pixel[2 % pixel.length];
            }

            // 原地转换
            connector.transform(rgb);

            if (pixel.length < dstChannels) {
                float[] newPixel = new float[Math.max(dstChannels, pixel.length)];
                System.arraycopy(pixel, 0, newPixel, 0, pixel.length);
                pixel = newPixel;
                buffer[i] = pixel;
            }

            if (dstChannels >= 3) {
                pixel[0] = rgb[0];
                pixel[1] = rgb[1];
                pixel[2] = rgb[2];
            } else if (dstChannels == 1) {
                pixel[0] = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
            } else {
                pixel[0] = rgb[0];
                if (pixel.length > 1) pixel[1] = rgb[1];
                if (pixel.length > 2) pixel[2] = rgb[2];
            }
        }

        return buffer;
    }

    public void translateColor(ICC_Transform t, Raster src, WritableRaster dst) {
        ColorSpace srcCS = null;
        ColorSpace dstCS = null;
        if (t != null) {
            try {
                srcCS = new java.awt.color.ICC_ColorSpace(t.getSrc());
                dstCS = new java.awt.color.ICC_ColorSpace(t.getDst());
            } catch (Exception e) {}
        }

        android.graphics.ColorSpace aSrcCS = getAndroidColorSpace(srcCS);
        android.graphics.ColorSpace aDstCS = getAndroidColorSpace(dstCS);
        android.graphics.ColorSpace.Connector connector = android.graphics.ColorSpace.connect(aSrcCS, aDstCS);

        int w = src.getWidth();
        int h = src.getHeight();

        int srcBands = src.getNumBands();
        int dstBands = dst.getNumBands();

        float[] srcPixel = new float[srcBands];
        float[] dstPixel = new float[dstBands];
        float[] rgb = new float[3];

        int minX = src.getMinX();
        int minY = src.getMinY();
        int maxX = minX + w;
        int maxY = minY + h;

        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                src.getPixel(x, y, srcPixel);

                if (srcBands >= 3) {
                    rgb[0] = srcPixel[0] / 255.0f;
                    rgb[1] = srcPixel[1] / 255.0f;
                    rgb[2] = srcPixel[2] / 255.0f;
                } else {
                    float grayVal = srcPixel[0] / 255.0f;
                    rgb[0] = grayVal;
                    rgb[1] = grayVal;
                    rgb[2] = grayVal;
                }

                connector.transform(rgb); // 原地转换

                if (dstBands >= 3) {
                    dstPixel[0] = rgb[0] * 255.0f;
                    dstPixel[1] = rgb[1] * 255.0f;
                    dstPixel[2] = rgb[2] * 255.0f;
                } else {
                    dstPixel[0] = (0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2]) * 255.0f;
                }

                if (dstBands > 3 && srcBands > 3) {
                    dstPixel[3] = srcPixel[3];
                } else if (dstBands > 3) {
                    dstPixel[3] = 255.0f;
                }

                dst.setPixel(x, y, dstPixel);
            }
        }
    }

    public short[] translateColor(ICC_Transform t, short src[], short dst[]) {
        ColorSpace srcCS = null;
        ColorSpace dstCS = null;
        if (t != null) {
            try {
                srcCS = new java.awt.color.ICC_ColorSpace(t.getSrc());
                dstCS = new java.awt.color.ICC_ColorSpace(t.getDst());
            } catch (Exception e) {}
        }

        int nSrcChannels = t != null ? t.getNumInputChannels() : 3;
        int nDstChannels = t != null ? t.getNumOutputChannels() : 3;

        int nPixels = src.length / nSrcChannels;
        if (dst == null || dst.length < nPixels * nDstChannels) {
            dst = new short[nPixels * nDstChannels];
        }

        android.graphics.ColorSpace aSrcCS = getAndroidColorSpace(srcCS);
        android.graphics.ColorSpace aDstCS = getAndroidColorSpace(dstCS);
        android.graphics.ColorSpace.Connector connector = android.graphics.ColorSpace.connect(aSrcCS, aDstCS);

        float[] rgb = new float[3];

        for (int i = 0; i < nPixels; i++) {
            int srcIdx = i * nSrcChannels;
            int dstIdx = i * nDstChannels;

            if (nSrcChannels >= 3) {
                rgb[0] = (src[srcIdx] & 0xFFFF) / 65535.0f;
                rgb[1] = (src[srcIdx + 1] & 0xFFFF) / 65535.0f;
                rgb[2] = (src[srcIdx + 2] & 0xFFFF) / 65535.0f;
            } else {
                float grayVal = (src[srcIdx] & 0xFFFF) / 65535.0f;
                rgb[0] = grayVal;
                rgb[1] = grayVal;
                rgb[2] = grayVal;
            }

            connector.transform(rgb); // 原地转换

            if (nDstChannels >= 3) {
                dst[dstIdx] = (short) Math.round(rgb[0] * 65535.0f);
                dst[dstIdx + 1] = (short) Math.round(rgb[1] * 65535.0f);
                dst[dstIdx + 2] = (short) Math.round(rgb[2] * 65535.0f);
            } else {
                float grayVal = (0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2]) * 65535.0f;
                dst[dstIdx] = (short) Math.round(grayVal);
            }

            if (nDstChannels > 3 && nSrcChannels > 3) {
                dst[dstIdx + 3] = src[srcIdx + 3];
            } else if (nDstChannels > 3) {
                dst[dstIdx + 3] = (short) 65535;
            }
        }

        return dst;
    }

    private NativeImageFormat createImageFormat(BufferedImage bi) {
        int nRows = bi.getHeight();
        int nCols = bi.getWidth();
        int nComps = bi.getColorModel().getNumColorComponents();
        short imgData[] = new short[nRows*nCols*nComps];
        return new NativeImageFormat(
                imgData, nComps, nRows, nCols);
    }

    private NativeImageFormat createImageFormat(
            ICC_Transform t, short arr[], int nCols, boolean in
    ) {
        int nComponents = in ? t.getNumInputChannels() : t.getNumOutputChannels();

        if (arr == null || arr.length < nCols*nComponents) {
            arr = new short[nCols*nComponents];
        }

        if (nCols == 0)
            nCols = arr.length / nComponents;

        return new NativeImageFormat(arr, nComponents, 1, nCols);
    }
}