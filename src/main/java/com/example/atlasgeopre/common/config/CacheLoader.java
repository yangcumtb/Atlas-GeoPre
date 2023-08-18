package com.example.atlasgeopre.common.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CacheLoader {
    private static String cacheTilePath = "E:\\tile\\0,E:\\tile\\1,E:\\tile\\2,E:\\tile\\3,E:\\tile\\4,E:\\tile\\5,E:\\tile\\6";

    private static Cache<String, BufferedImage> cache = Caffeine.newBuilder()
            .maximumSize(500000) // 设置缓存的最大大小
            .build();

    @PreDestroy
    public void destroy() throws Exception {
        cache.invalidateAll();
    }

    /**
     * 预缓存前0-8级别数据
     */
    public static void preloadTilesToCache() throws IOException {
        // 执行瓦片数据的缓存预加载过程
        // 例如，遍历需要预加载的瓦片文件，将文件加载到缓存池中
        String[] cacheTilePaths = cacheTilePath.split(",");
        for (String cacheTile : cacheTilePaths) {
            File tileDirectory = new File(cacheTile);
            preloadFiles(tileDirectory);
        }
    }

    /**
     * 将文件写入缓存
     *
     * @param directory 文件目录
     */
    private static void preloadFiles(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    //文件的key用
                    String key = extractKeyFromFilePath(file.getAbsolutePath());
                    BufferedImage fileData = ImageIO.read(file);
                    if (fileData != null) {
                        cache.put(key, fileData);
                    }
                } else if (file.isDirectory()) {
                    preloadFiles(file); // 递归处理子目录
                }
            }
        }
    }

    /**
     * 根据key来获取已经缓存的瓦片
     *
     * @param key 层级+列号+行号，字符串拼接
     * @return
     */
    public static BufferedImage tryGetCacheTile(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 对瓦片文件所编号
     *
     * @param filePath 文件路径
     * @return
     */
    private static String extractKeyFromFilePath(String filePath) {
        String separator = File.separator;


        // 提取文件名，例如 "11.jpg"
        int lastIndex = filePath.lastIndexOf(separator);
        String fileName = (lastIndex >= 0) ? filePath.substring(lastIndex + 1) : filePath;

        // 去除文件扩展名，得到 "11"
        int dotIndex = fileName.lastIndexOf('.');
        String keyWithoutExtension = (dotIndex >= 0) ? fileName.substring(0, dotIndex) : fileName;

        // 分割路径
        String[] parts = filePath.split("\\\\");

        String level = "";
        String column = "";

        if (parts.length >= 3) { // 根据路径格式来确定位置
            level = parts[parts.length - 3];  //瓦片层级
            column = parts[parts.length - 2]; // 瓦片列号
        }

        // 组合行列号，得到瓦片的key
        return level + column + keyWithoutExtension;
    }

}
