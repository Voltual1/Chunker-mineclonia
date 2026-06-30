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

package org.geysermc.pack.converter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class IccProfileStore {
    // 留空占位符，请在此处填入对应文件的 Base64 编码
    public static String SRGB_BASE64 = "";
    public static String GRAY_BASE64 = "";
    public static String CIEXYZ_BASE64 = "";
    public static String PYCC_BASE64 = "";
    public static String LINEAR_RGB_BASE64 = "";

    public static void install(File cacheDir) {
        Map<String, String> profiles = new HashMap<>();
        profiles.put("sRGB.pf", SRGB_BASE64);
        profiles.put("GRAY.pf", GRAY_BASE64);
        profiles.put("CIEXYZ.pf", CIEXYZ_BASE64);
        profiles.put("PYCC.pf", PYCC_BASE64);
        profiles.put("LINEAR_RGB.pf", LINEAR_RGB_BASE64);

        for (Map.Entry<String, String> entry : profiles.entrySet()) {
            String fileName = entry.getKey();
            String base64 = entry.getValue();
            if (base64 == null || base64.isEmpty()) {
                continue;
            }
            File file = new File(cacheDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] decoded = Base64.getDecoder().decode(base64);
                fos.write(decoded);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 设置系统属性，使 Android AWT 可以找到解压出来的配置档
        System.setProperty("java.iccprofile.path", cacheDir.getAbsolutePath());
    }
}