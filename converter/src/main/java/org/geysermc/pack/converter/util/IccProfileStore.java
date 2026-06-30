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
import java.io.InputStream;

public class IccProfileStore {

    private static final String[] PROFILE_NAMES = {
            "sRGB.pf",
            "GRAY.pf",
            "CIEXYZ.pf",
            "PYCC.pf",
            "LINEAR_RGB.pf"
    };

    public static void install(File cacheDir) {
        byte[] buffer = new byte[4096];
        for (String fileName : PROFILE_NAMES) {
            File destFile = new File(cacheDir, fileName);
            
            // 如果文件已存在且大小正常，无需重复写入
            if (destFile.exists() && destFile.length() > 0) {
                continue;
            }

            // 从 Classpath 中读取二进制配置档
            try (InputStream is = IccProfileStore.class.getResourceAsStream("/profiles/" + fileName)) {
                if (is == null) {
                    System.err.println("WARN: 无法在资源中找到 ICC 配置文件: " + fileName);
                    continue;
                }
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 设置系统属性，使 Android AWT 可以找到解压缩在 cacheDir 下的配置档
        System.setProperty("java.iccprofile.path", cacheDir.getAbsolutePath());
    }
}