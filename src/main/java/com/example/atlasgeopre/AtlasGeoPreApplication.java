package com.example.atlasgeopre;

import com.example.atlasgeopre.common.config.CacheLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class AtlasGeoPreApplication implements ApplicationRunner {

    @Value("${url.cacheTilePath}")
    private String cacheTilePath;

    @Value("${url.imageNotePath}")
    private String imageNotePath;

    public static void main(String[] args) {
        SpringApplication.run(AtlasGeoPreApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        /**
         * 做瓦片数据的缓存工作
         */
        CacheLoader.preloadTilesToCache(cacheTilePath);

        /**
         * 影像注记的缓存
         */
        CacheLoader.preloadImageNoteToCache(imageNotePath);

    }

}
