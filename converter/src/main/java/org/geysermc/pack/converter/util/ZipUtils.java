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

package org.geysermc.pack.converter.util;

import org.geysermc.pack.converter.PackConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Adjusted ZipUtils class to better suit the usage
 * From https://stackoverflow.com/a/15970455/5299903
 */
public class ZipUtils {

    private final List<String> fileList = new ArrayList<>();
    private final PackConverter packConverter;
    private final File sourceFolder;

    public ZipUtils(PackConverter packConverter, File sourceFolder) {
        this.packConverter = packConverter;
        this.sourceFolder = sourceFolder;
    }

    public void zipIt(LogListener listener, String zipFile) {
        byte[] buffer = new byte[1024];
        String source = sourceFolder.getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            listener.debug("Output to zip " + zipFile);
            FileInputStream in = null;

            for (String file: this.fileList) {
                listener.debug("File added " + file);
                ZipEntry ze = new ZipEntry(file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(sourceFolder + File.separator + file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } finally {
                    if (in != null)
                        in.close();
                }
            }

            zos.closeEntry();
            listener.debug("Folder successfully compressed");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void generateFileList() {
        generateFileList(sourceFolder);
    }

    public void generateFileList(File node) {
        // Add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.getAbsolutePath()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            if (subNote != null) {
                for (String filename : subNote) {
                    generateFileList(new File(node, filename));
                }
            }
        }
    }

    private String generateZipEntry(String file) {
        return file.substring(sourceFolder.getAbsolutePath().length() + 1);
    }

    public static void openFileSystem(Path input, boolean compressed, PathConsumer inputConsumer) throws IOException {
        if (compressed) {
            Path tempDir = Files.createTempDirectory("extracted_zip_");
            try {
                unzip(input, tempDir);
                inputConsumer.accept(tempDir);
            } finally {
                deleteDirectory(tempDir);
            }
        } else {
            inputConsumer.accept(input);
        }
    }

    private static void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newPath = destDir.resolve(entry.getName()).normalize();
                // 防范 Zip Slip 漏洞安全校验
                if (!newPath.startsWith(destDir)) {
                    throw new IOException("Entry is outside of the target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    Files.createDirectories(newPath.getParent());
                    Files.copy(zis, newPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private static void deleteDirectory(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                     .sorted(Comparator.reverseOrder())
                     .forEach(p -> {
                         try {
                             Files.delete(p);
                         } catch (IOException ignored) {}
                     });
            }
        } catch (IOException ignored) {}
    }

    public interface PathConsumer {

        void accept(Path path) throws IOException;
    }
}