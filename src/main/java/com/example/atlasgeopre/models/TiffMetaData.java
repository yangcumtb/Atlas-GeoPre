package com.example.atlasgeopre.models;


import lombok.Data;

import java.util.Date;

@Data
public class TiffMetaData {

    //影像存储路径
    private String imagePath;

    //影像名称
    private String imageName;

    //文件后缀
    private String fileSuffix;

    //文件大小
    private long fileSize;

    //图像宽度
    private Integer imageWidth;

    //图像高度
    private Integer imageHeight;

    //色彩模式
    private String colorPattern;

    //压缩方式
    private String compressMode;

    //修改时间
    private Date modificationTime;

    //空间参数：水平像素大小
    private double[] affineTransformation;

    //空间坐标系
    private String projection;

    //epsg代码
    private String epsg;


}
