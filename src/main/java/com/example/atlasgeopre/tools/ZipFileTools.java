package com.example.atlasgeopre.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFileTools {
    public static void compressDirectory(File directory, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[1024];
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectory(file, zos);
            } else {
                FileInputStream fis = new FileInputStream(file);
                zos.putNextEntry(new ZipEntry(file.getName()));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();
            }
        }

    }


}
