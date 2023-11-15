package com.example.atlasgeopre.tools;

import com.example.atlasgeopre.common.config.GDALInitializer;
import org.gdal.gdal.*;
import org.gdal.gdal.Driver;
import org.gdal.gdalconst.gdalconst;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.*;
import org.gdal.osr.SpatialReference;
import org.springframework.beans.factory.annotation.Value;

import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import static org.gdal.ogr.ogrConstants.wkbPolygon;

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
        Dataset inputDataset = gdal.Open(inputPath);
        Band band = inputDataset.GetRasterBand(1);
        int dataType = band.getDataType();

        Driver driver = inputDataset.GetDriver();
        String driverName = driver.getShortName();
        boolean needParam = driverName.equals("GTIFF") && (dataType == gdalconstConstants.GDT_Float32 || dataType == gdalconstConstants.GDT_Float64);

        int bandCount = inputDataset.getRasterCount();
        if (format.equals("AAIGrid")) {
            //遍历循环波段所有波段
            for (int i = 1; i <= bandCount; i++) {
                String out = outputPath + "/" + imageName + "_band" + String.valueOf(i) + suffix;
                Vector aAIGridwarpOptions = new Vector();    // 创建向量以存储warp操作的参数列表
                aAIGridwarpOptions.add("-b");
                aAIGridwarpOptions.add(String.valueOf(i));
                aAIGridwarpOptions.add("-of");
                aAIGridwarpOptions.add("AAIGrid");
                TranslateOptions translateOptions = new TranslateOptions(aAIGridwarpOptions);
                Dataset outputDataset = gdal.Translate(out, inputDataset, translateOptions, progressReporter);
                outputDataset.delete();
                translateOptions.delete();
            }

        } else {
            Vector warpOptions = new Vector();
            String out = outputPath + "/" + imageName + suffix;
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

            File outfie = new File(out);
            if (!new File(outfie.getParent()).exists()) {
                new File(outfie.getParent()).mkdir();
            }

            TranslateOptions translateOptions = new TranslateOptions(warpOptions);
            Dataset outputDataset = gdal.Translate(out, inputDataset, translateOptions, progressReporter);
            outputDataset.delete();
            translateOptions.delete();
        }
        band.delete();
        driver.delete();
        inputDataset.delete();
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
        Dataset inputDataset = gdal.Open(inputPath);

        Dataset[] inputs = new Dataset[]{inputDataset};


        Vector warpOptions = new Vector();
        warpOptions.add("-s_srs");
        warpOptions.add(sourceEPSG);
        warpOptions.add("-t_srs");
        warpOptions.add(targetEPSG);

        WarpOptions warpOptions1 = new WarpOptions(warpOptions);
        Dataset dataset = gdal.Warp(outputPath, inputs, warpOptions1, progressReporter);
        dataset.delete();
        inputDataset.delete();
        warpOptions1.delete();
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
        Dataset inputDataset = gdal.Open(inputFile);

        Dataset[] inputs = new Dataset[]{inputDataset};

        Vector warpOptions = new Vector();
        warpOptions.add("-r");
        warpOptions.add(method);
        warpOptions.add("-ts");
        warpOptions.add(String.valueOf(width));
        warpOptions.add(String.valueOf(height));

        WarpOptions warpOptions1 = new WarpOptions(warpOptions);
        Dataset outdata = gdal.Warp(outputFile, inputs, warpOptions1, progressReporter);
        outdata.delete();
        warpOptions1.delete();
        inputDataset.delete();
    }


    /**
     * 反算掩膜区域
     *
     * @param boxAreaPath   外部
     * @param innerAreaPath 内部
     */
    public static String getMaskArea(String boxAreaPath, String innerAreaPath, String gdalCachPath) {
        // 获取当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new Date());
        // 创建日期路径
        String datePath = gdalCachPath + File.separator + currentDate;
        File dateDirectory = new File(datePath);
        if (!dateDirectory.exists()) {
            dateDirectory.mkdirs();
        }
        // 文件名以"merge"为开始，后面加递增序号
        int sequence = 1;
        String fileName = "maskarea";  // 初始文件名
        String filePath = datePath + File.separator + fileName + ".shp";

        // 检查文件是否存在，如果存在则增加递增序号
        while (new File(filePath).exists()) {
            sequence++;
            fileName = "maskarea(" + sequence + ")";
            filePath = datePath + File.separator + fileName + ".shp";
        }


        GDALInitializer.initializeogr();
        GDALInitializer.initialize();
        // 打开第一个Shapefile
        DataSource dataSource1 = ogr.Open(innerAreaPath);

        // 打开第二个Shapefile
        DataSource dataSource2 = ogr.Open(boxAreaPath);

        // 获取第一个Shapefile的第一个图层
        Layer layer1 = dataSource1.GetLayer(0);

        // 获取第二个Shapefile的第一个图层
        Layer layer2 = dataSource2.GetLayer(0);


        // 创建一个新的数据源和图层用于存储交集结果
        DataSource outputDataSource = ogr.GetDriverByName("ESRI Shapefile").CreateDataSource(filePath);
        Layer outputLayer = outputDataSource.CreateLayer("intersection", null);

        // 进行两个Shapefile的交集操作
        layer1.SymDifference(layer2, outputLayer);

        // 释放资源
        layer1.delete();
        layer2.delete();
        outputLayer.delete();
        dataSource1.delete();
        dataSource2.delete();
        outputDataSource.delete();

        return filePath;
    }

    /**
     * gdalCachPath中生成合并文件
     *
     * @param shpfiles 用于掩膜的shp文件路径
     * @return
     */
    public static String mergeShp(String[] shpfiles, String gdalCachPath) {
        GDALInitializer.initializeogr();
        GDALInitializer.initialize();
        try {
            // 获取当前日期
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String currentDate = dateFormat.format(new Date());
            // 创建日期路径
            String datePath = gdalCachPath + File.separator + currentDate;
            File dateDirectory = new File(datePath);
            if (!dateDirectory.exists()) {
                dateDirectory.mkdirs();
            }
            // 文件名以"merge"为开始，后面加递增序号
            int sequence = 1;
            String fileName = "merge";  // 初始文件名
            String filePath = datePath + File.separator + fileName + ".shp";

            // 检查文件是否存在，如果存在则增加递增序号
            while (new File(filePath).exists()) {
                sequence++;
                fileName = "merge(" + sequence + ")";
                filePath = datePath + File.separator + fileName + ".shp";
            }
            SpatialReference spatialReference = new SpatialReference();
            spatialReference.ImportFromEPSG(4326);

            DataSource outputDataSource = ogr.GetDriverByName("ESRI Shapefile").CreateDataSource(filePath);
            Layer outputLayer = outputDataSource.CreateLayer("intersection", spatialReference);


            Geometry unionGeometry = new Geometry(wkbPolygon);  // 用于存储联合后的几何对象

            for (int i = 0; i < shpfiles.length; i++) {
                DataSource dataSource2 = ogr.Open(shpfiles[i]);
                Layer layer2 = dataSource2.GetLayer(0);
                // 遍历源图层的所有要素，并进行联合操作
                Feature sourceFeature;
                while ((sourceFeature = layer2.GetNextFeature()) != null) {
                    Geometry sourceGeometry = sourceFeature.GetGeometryRef();

                    // 将每个要素的几何对象与之前的联合几何对象进行联合
                    unionGeometry = unionGeometry.Union(sourceGeometry);
                    sourceGeometry.delete();
                }
                // 创建一个新要素，并将联合后的几何对象添加到输出图层
                layer2.delete();
                dataSource2.delete();
            }
            Feature outputFeature = new Feature(outputLayer.GetLayerDefn());
            outputFeature.SetGeometry(unionGeometry);
            outputLayer.CreateFeature(outputFeature);
            outputLayer.delete();
            unionGeometry.delete();
            outputFeature.delete();
            outputDataSource.delete();
            spatialReference.delete();
            return filePath;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 像素掩膜
     *
     * @param inputfile  掩膜文件
     * @param outputFile 输出路径
     * @param shpfile    掩膜shp
     */
    public static void pixelMask(String inputfile, String outputFile, String shpfile) {
        GDALInitializer.initialize();
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        Dataset dataset = gdal.Open(inputfile);

        Dataset[] inputs = new Dataset[]{dataset};

        Vector warpOptions = new Vector();
        warpOptions.add("-dstnodata");
        warpOptions.add("0");
        warpOptions.add("-of");
        warpOptions.add("GTiff");
        warpOptions.add("-overwrite");
        warpOptions.add("-cutline");
        warpOptions.add(shpfile);

        WarpOptions warpOptions1 = new WarpOptions(warpOptions);
        Dataset dataset1 = gdal.Warp(outputFile, inputs, warpOptions1);

        dataset.delete();
        dataset1.delete();
        warpOptions1.delete();
    }

    public static String getOutBox(String inputfile, String gdalCachPath) {
        // 获取当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new Date());
        // 创建日期路径
        String datePath = gdalCachPath + File.separator + currentDate;
        File dateDirectory = new File(datePath);
        if (!dateDirectory.exists()) {
            dateDirectory.mkdirs();
        }
        // 文件名以"merge"为开始，后面加递增序号
        int sequence = 1;
        String fileName = "outbox";  // 初始文件名
        String filePath = datePath + File.separator + fileName + ".shp";

        // 检查文件是否存在，如果存在则增加递增序号
        while (new File(filePath).exists()) {
            sequence++;
            fileName = "outbox(" + sequence + ")";
            filePath = datePath + File.separator + fileName + ".shp";
        }

        GDALInitializer.initializeogr();
        GDALInitializer.initialize();

        try {
            // 打开 TIFF 文件
            Dataset tiffDataset = gdal.Open(inputfile, gdalconst.GA_ReadOnly);

            if (tiffDataset != null) {
                // 获取 TIFF 文件的包围盒
                double[] adfGeoTransform = new double[6];
                tiffDataset.GetGeoTransform(adfGeoTransform);

                // 创建 Shapefile 数据源
                org.gdal.ogr.Driver shpDriver = ogr.GetDriverByName("ESRI Shapefile");
                DataSource shpDataSource = shpDriver.CreateDataSource(filePath);

                if (shpDataSource != null) {
                    // 创建一个包含包围盒的多边形
                    Layer shpLayer = shpDataSource.CreateLayer("BoundingBox", null, ogr.wkbPolygon);

                    FieldDefn fieldDefn = new FieldDefn("ID", ogrConstants.OFTInteger);
                    shpLayer.CreateField(fieldDefn);

                    Feature feature = new Feature(shpLayer.GetLayerDefn());
                    Geometry geometry = new Geometry(ogr.wkbPolygon);

                    // 创建一个线性环
                    Geometry ringGeometry = new Geometry(ogr.wkbLinearRing);
                    ringGeometry.AddPoint(adfGeoTransform[0], adfGeoTransform[3]);
                    ringGeometry.AddPoint(adfGeoTransform[0] + adfGeoTransform[1] * tiffDataset.getRasterXSize(), adfGeoTransform[3]);
                    ringGeometry.AddPoint(adfGeoTransform[0] + adfGeoTransform[1] * tiffDataset.getRasterXSize(), adfGeoTransform[3] + adfGeoTransform[5] * tiffDataset.getRasterYSize());
                    ringGeometry.AddPoint(adfGeoTransform[0], adfGeoTransform[3] + adfGeoTransform[5] * tiffDataset.getRasterYSize());
                    ringGeometry.AddPoint(adfGeoTransform[0], adfGeoTransform[3]);

                    geometry.AddGeometryDirectly(ringGeometry);
                    feature.SetGeometry(geometry);

                    feature.SetField("ID", 1);
                    shpLayer.CreateFeature(feature);

                    // 关闭数据源
                    ringGeometry.delete();
                    geometry.delete();
                    feature.delete();
                    shpLayer.delete();
                    shpDataSource.delete();
                } else {
                    System.err.println("Failed to create Shapefile data source.");
                }
                // 关闭 TIFF 数据集
                tiffDataset.delete();
                shpDriver.delete();

            } else {
                System.err.println("Failed to open TIFF file.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filePath;

    }
}
