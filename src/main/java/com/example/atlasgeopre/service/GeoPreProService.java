package com.example.atlasgeopre.service;

import com.example.atlasgeopre.models.CoordinateParam;
import com.example.atlasgeopre.models.ResampleParam;
import com.example.atlasgeopre.models.TiffMetaData;

import java.awt.image.BufferedImage;
import java.util.Map;
public interface GeoPreProService {

    /**
     * 获取tiff文件元数据
     *
     * @param id 文件路径
     * @return 元数据
     */
    TiffMetaData getMetadata(String id);

    /**
     * 更换坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    Map<String, String> changeCoordination(CoordinateParam param);

    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    Map<String, String> resampleImage(ResampleParam param);


    /**
     * 格式转换方法
     *
     * @param filePath     文件路径
     * @param targetFormat 目标格式
     * @return 返回结果
     */
    Map<String, String> changeFormat(String filePath, String outputPath, String targetFormat);

    BufferedImage get3DTile(int z, int x, int y);

}
