package com.example.atlasgeopre.models;

import lombok.Data;

@Data
public class CoordinateParam {

    /**
     * 影像文件路径
     */
    private String filePath;

    /**
     * 输出图像路径
     */
    private String outPath;

    /**
     * 目标坐标系的EPSG代码，示例值：”EPSG:4326“
     */
    private String targetEPSG;

}
