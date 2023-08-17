package com.example.atlasgeopre.tools;

import com.example.atlasgeopre.common.config.GDALInitializer;
import org.gdal.gdal.*;
import org.gdal.gdalconst.gdalconstConstants;

import java.util.Vector;

/**
 * 通过Vector构建命令。但对bil等envi格式的文件不支持
 * String out = outputPath + "\\" + imageName + "\\" + imageName + suffix;
 * // 打开输入数据集
 * Vector warpOptions = new Vector();    // 创建向量以存储warp操作的参数列表
 * warpOptions.add("-of");
 * warpOptions.add("PNG");
 * warpOptions.add("-co");               // 设置输出文件的压缩选项
 * warpOptions.add("COMPRESS=DEFLATE");  // 使用DEFLATE压缩方法
 * warpOptions.add("-co");
 * warpOptions.add("COPY_SRC_OVERVIEWS=YES");
 * // 创建金字塔
 * warpOptions.add("-r");
 * warpOptions.add("average");
 * warpOptions.add("-levels");
 * warpOptions.add("4");
 * warpOptions.add("-ovr");
 * 设置输出文件格式为.ovr
 * TranslateOptions translateOptions = new TranslateOptions(warpOptions);
 * Dataset outputDataset = gdal.Translate(out, inputDataset, translateOptions, progressReporter);
 * 释放资源
 * outputDataset.delete();
 * gdal.GDALDestroyDriverManager();
 */


public class GdalOptionTools {

    /**
     * 格式转换
     *
     * @param inputPath        输入
     * @param outputPath       输出
     * @param imageName        文件名
     * @param format           文件格式
     * @param suffix           格式后缀
     * @param progressReporter 进度
     */
    public static void doChangeFormat(String inputPath, String outputPath, String imageName, String format, String suffix, ProgressReporter progressReporter) {

        GDALInitializer.initialize();
        Dataset inputDataset = gdal.Open(inputPath, gdalconstConstants.GA_ReadOnly);
        Band band = inputDataset.GetRasterBand(1);
        int dataType = band.getDataType();

        Driver driver = inputDataset.GetDriver();
        String driverName = driver.getShortName();
        boolean needParam = driverName.equals("GTIFF") && (dataType == gdalconstConstants.GDT_Float32 || dataType == gdalconstConstants.GDT_Float64);

        int bandCount = inputDataset.getRasterCount();
        if (format.equals("AAIGrid")) {
            //遍历循环波段所有波段
            for (int i = 1; i <= bandCount; i++) {
                String out = outputPath + "/" + imageName + "/" + imageName + "_band" + String.valueOf(i) + suffix;
                Vector aAIGridwarpOptions = new Vector();    // 创建向量以存储warp操作的参数列表
                aAIGridwarpOptions.add("-b");
                aAIGridwarpOptions.add(String.valueOf(i));
                aAIGridwarpOptions.add("-of");
                aAIGridwarpOptions.add("AAIGrid");
                TranslateOptions translateOptions = new TranslateOptions(aAIGridwarpOptions);
                Dataset outputDataset = gdal.Translate(out, inputDataset, translateOptions, progressReporter);
                outputDataset.delete();
            }

        } else {
            Vector warpOptions = new Vector();
            String out = outputPath + "/" + imageName + "/" + imageName + suffix;
            // 设置命令参数
            switch (suffix) {
                case ".bsq":
                    warpOptions.add("-of");
                    warpOptions.add("ENVI");
                    warpOptions.add("-co");
                    warpOptions.add("INTERLEAVE=BSQ");
                    break;
                case ".bil":
                    warpOptions.add("-of");
                    warpOptions.add("ENVI");
                    warpOptions.add("-co");
                    warpOptions.add("INTERLEAVE=BIL");
                    break;
                case ".bip":
                    warpOptions.add("-of");
                    warpOptions.add("ENVI");
                    warpOptions.add("-co");
                    warpOptions.add("INTERLEAVE=BIP");
                    break;
                default:
                    if (needParam) {
                        warpOptions.add("-ot");
                        warpOptions.add("UInt16");
                        warpOptions.add(("-scale"));
                        warpOptions.add("0");
                        warpOptions.add("1");
                        warpOptions.add("0");
                        warpOptions.add("65535");
                    }
                    warpOptions.add("-of");
                    warpOptions.add(format);
                    break;
            }

            TranslateOptions translateOptions = new TranslateOptions(warpOptions);
            gdal.Translate(out, inputDataset, translateOptions, progressReporter);
        }
        inputDataset.delete();
        gdal.GDALDestroyDriverManager();
    }


    /**
     * 坐标系转换
     *
     * @param inputPath        输入
     * @param outputPath       输出
     * @param sourceEPSG       原坐标代码
     * @param targetEPSG       目标坐标代码
     * @param progressReporter 进度
     */
    public static void doChangeCoordinate(String inputPath, String outputPath, String sourceEPSG, String targetEPSG, ProgressReporter progressReporter) {
        GDALInitializer.initialize();
        Dataset inputDataset = gdal.Open(inputPath, gdalconstConstants.GA_ReadOnly);

        Dataset[] inputs = new Dataset[]{inputDataset};


        Vector warpOptions = new Vector();
        warpOptions.add("-s_srs");
        warpOptions.add(sourceEPSG);
        warpOptions.add("-t_srs");
        warpOptions.add(targetEPSG);

        WarpOptions warpOptions1 = new WarpOptions(warpOptions);
        gdal.Warp(outputPath, inputs, warpOptions1, progressReporter);

        inputDataset.delete();
        gdal.GDALDestroyDriverManager();
    }

    /**
     * 重采样
     *
     * @param inputFile        输入
     * @param outputFile       输出
     * @param width            宽
     * @param height           高
     * @param method           方法
     * @param progressReporter 进度
     */
    public static void doResampleOperation(String inputFile, String outputFile, int width, int height, String method, ProgressReporter progressReporter) {
        GDALInitializer.initialize();
        Dataset inputDataset = gdal.Open(inputFile, gdalconstConstants.GA_ReadOnly);

        Dataset[] inputs = new Dataset[]{inputDataset};

        Vector warpOptions = new Vector();
        warpOptions.add("-r");
        warpOptions.add(method);
        warpOptions.add("-ts");
        warpOptions.add(String.valueOf(width));
        warpOptions.add(String.valueOf(height));

        WarpOptions warpOptions1 = new WarpOptions(warpOptions);
        gdal.Warp(outputFile, inputs, warpOptions1, progressReporter);

        inputDataset.delete();
        gdal.GDALDestroyDriverManager();
    }

}
