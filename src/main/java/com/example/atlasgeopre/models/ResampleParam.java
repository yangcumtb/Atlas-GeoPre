package com.example.atlasgeopre.models;

import lombok.Data;

/**
 * 重采样参数设置
 */

@Data
public class ResampleParam {

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 输出图像路径
     */
    private String outPath;

    /**
     * 重采样方法
     * -r :
     * near: nearest neighbour resampling (default, fastest algorithm, worst interpolation quality).最邻近重采样（默认，最快的算法，最差的插值质量）。
     * bilinear: bilinear resampling.双线性重采样。
     * cubic: cubic resampling.立体重采样。
     * cubicspline: cubic spline resampling.立体样条重采样。
     * lanczos: Lanczos windowed sinc resampling.Lanczos 窗口化 sinc 重采样
     * average: average resampling, computes the weighted average of all non-NODATA contributing pixels.平均值重采样，计算所有非NODATA贡献的像素的加权平均值。
     * mode: mode resampling, selects the value which appears most often of all the sampled points. In the case of ties, the first value identified as the mode will be selected.众数重采样
     * max: maximum resampling, selects the maximum value from all non-NODATA contributing pixels.最大值重采样
     * min: minimum resampling, selects the minimum value from all non-NODATA contributing pixels.最小值重采样
     * med: median resampling, selects the median value of all non-NODATA contributing pixels.中位数重采样
     * q1: first quartile resampling, selects the first quartile value of all non-NODATA contributing pixels.第一四分位数重采样
     * q3: third quartile resampling, selects the third quartile value of all non-NODATA contributing pixels.第三四分位数重采样
     */
    private String resampleMethod;

    //重采样后x像素值
    private int reSizeX;

    //重采样后y像素值
    private int reSizeY;

}
