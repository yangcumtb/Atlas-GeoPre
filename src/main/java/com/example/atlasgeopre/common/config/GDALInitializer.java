package com.example.atlasgeopre.common.config;

import org.gdal.gdal.gdal;
import org.gdal.ogr.ogr;

/**
 * @Author:
 * @DATE:2023/7/6 10:08
 * @Description:
 * @Version 1.0
 */
public class GDALInitializer {
    public static void initialize() {
        // 在此处进行GDAL的初始化操作
        gdal.AllRegister();
    }

    public static void initializeogr() {
        ogr.RegisterAll();
    }
}
