package com.example.atlasgeopre.service.impl;

import com.example.atlasgeopre.common.config.CacheLoader;
import com.example.atlasgeopre.common.config.GDALInitializer;
import com.example.atlasgeopre.models.CoordinateParam;
import com.example.atlasgeopre.models.ResampleParam;
import com.example.atlasgeopre.models.TiffMetaData;
import com.example.atlasgeopre.service.GeoPreProService;
import com.example.atlasgeopre.tools.*;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.ogr.*;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

@Service
public class GeoPreProServiceImpl implements GeoPreProService {
    @Value("${url.tilePath}")
    private String tilePath;

    @Value("${url.notePath}")
    private String notePath;

    /**
     * 获取tiff文件元数据
     *
     * @param filePath 文件路径
     * @return 元数据
     */
    @Override
    public TiffMetaData getMetadata(String filePath) {
//        System.setProperty("PROJ_LIB", "D:\\Program Files\\Java\\release-1930-x64-gdal-3-6-3-mapserver-8-0-0\\bin\\proj7\\share");
//        File file = new File(imagePath);
//        System.out.println("PROJ_LIB: " + System.getenv("PROJ_LIB"));
        GDALInitializer.initialize();
        File file = new File(filePath);
        // 获取文件的修改时间
        long modifiedTime = file.lastModified();
        TiffMetaData tiffMetaData = new TiffMetaData();
        // 获取文件后缀
        tiffMetaData.setFileSuffix(getFileExtension(file));
        // 获取文件大小（以KB为单位）
        tiffMetaData.setFileSize(getFileSize(file));
        // 获取文件路径
        tiffMetaData.setImagePath(filePath);
        // 将修改时间转换为日期对象
        tiffMetaData.setModificationTime(new Date(modifiedTime));
        // 获取名称
        tiffMetaData.setImageName(file.getName());
        Dataset imageSet = gdal.Open(filePath);
        //获取空间仿射变换参数
        double[] geotransform = imageSet.GetGeoTransform();
        System.out.println(Arrays.toString(geotransform));
        //判断数据的坐标系是投影坐标系还是地理坐标系
        if (imageSet.GetProjection().contains("PROJCS")) {
            //投影坐标系需要做左上角顶点的坐标转换，以及分辨率转换
            // 创建坐标转换对象
            SpatialReference projSRS = new SpatialReference(imageSet.GetProjection());
            SpatialReference latLonSRS = new SpatialReference();
            latLonSRS.SetWellKnownGeogCS("WGS84"); // 设置地理坐标系为WGS 84
            // 创建坐标转换
            CoordinateTransformation transform = new CoordinateTransformation(projSRS, latLonSRS);

            // 创建一个点要素
            Geometry point = new Geometry(ogr.wkbPoint);
            point.AddPoint(imageSet.GetGeoTransform()[0], imageSet.GetGeoTransform()[3]);
            // 进行坐标转换
            point.Transform(transform);
            // 计算一度纬度的等效长度（以米为单位）
            double oneDegreeLatLength = 111320.0; // 在赤道上的纬度
            // 将空间分辨率从米转换为度
            double resolutionInMetersx = geotransform[1]; // 图像的空间分辨率（以米为单位）
            double resolutionInMetersy = geotransform[5]; // 图像的空间分辨率（以米为单位）

            double resolutionInDegreesx = resolutionInMetersx / oneDegreeLatLength;
            double resolutionInDegreesy = resolutionInMetersy / oneDegreeLatLength;

            tiffMetaData.setAffineTransformation(new double[]{point.GetY(), point.GetX(), resolutionInDegreesx, resolutionInDegreesy, geotransform[2], geotransform[4]});

        } else {
            tiffMetaData.setAffineTransformation(new double[]{geotransform[0], geotransform[3], geotransform[1], geotransform[5], geotransform[2], geotransform[4]});
        }

        //获取图像的压缩方式
        tiffMetaData.setCompressMode(imageSet.GetMetadataItem("COMPRESSION"));

        //获取色彩模式
        tiffMetaData.setColorPattern(getColorPatten(imageSet));

        //获取宽度和高度
        tiffMetaData.setImageHeight(imageSet.getRasterYSize());
        tiffMetaData.setImageWidth(imageSet.getRasterXSize());

        // 获取坐标系
        SpatialReference spatialRef = new SpatialReference(imageSet.GetProjection());
        // 获取EPSG代码
        String epsgCode = spatialRef.GetAttrValue("AUTHORITY", 1);
        if (epsgCode == null) {
            spatialRef.delete();
            return tiffMetaData;
        }
        //获取epsg代码
        tiffMetaData.setEpsg("EPSG:" + epsgCode);
        //获取元数据坐标系
        switch (epsgCode) {
            case "4326":
                tiffMetaData.setProjection("WGS_1984(WGS84坐标系)");
                break;
            case "3857":
                tiffMetaData.setProjection("WGS_1984(Web墨卡托投影)");
                break;
            case "4490":
                tiffMetaData.setProjection("China_2000");
                break;
        }
        spatialRef.delete();
        return tiffMetaData;
    }


    /**
     * 更换坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    @Override
    public Map<String, String> changeCoordination(CoordinateParam param) {
        GDALInitializer.initialize();

//        GDAL代码，存在与postgis冲突的环境变量
//         打开源图像数据集
        Dataset sourceDataset = gdal.Open(param.getFilePath(), gdalconst.GA_ReadOnly);
        SpatialReference spatialRef = new SpatialReference(sourceDataset.GetProjection());
        // 获取坐标系名称

        String coordinateSystem = spatialRef.GetAttrValue("DATUM");
        // 获取EPSG代码

        //实例化exe文件的执行对象
//        ExeExecution exeExecution = new ExeExecution();
//        exeExecution.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceEPSG, param.getTargetEPSG());
        ProgressReporter progressReporter = new ProgressReporter();
        GdalOptionTools.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceDataset.GetProjection(), param.getTargetEPSG(), progressReporter);
        spatialRef.delete();
        sourceDataset.delete();
        if (progressReporter.getSchedule() >= 100) {
            Map<String, String> result = new HashMap<>();
            result.put("sourceCoordinateSystem", coordinateSystem);
            if (param.getTargetEPSG().equals("EPSG:4490")) {
                result.put("newCoordinateSystem", "China_2000");
            } else if (param.getTargetEPSG().equals("EPSG:3857")) {
                result.put("newCoordinateSystem", "WGS_1984(Web墨卡托投影)");
            } else if (param.getTargetEPSG().equals("EPSG:4326")) {
                result.put("newCoordinateSystem", "WGS_1984(WGS84坐标系)");
            }
            result.put("ouputPath", param.getOutPath());
            return result;
        } else {
            return null;
        }
    }

    /**
     * 更换shp坐标系
     *
     * @param param 坐标系参数
     * @return 输出后文件路径
     */
    @Override
    public Map<String, String> changeShpCoordination(CoordinateParam param) {
        GDALInitializer.initialize();
        // 为了支持中文路径，请添加下面这句代码
        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        // 为了使属性表字段支持中文，请添加下面这句
        gdal.SetConfigOption("SHAPE_ENCODING", "");
        DataSource shapefileDataset = ogr.Open(param.getFilePath());
        if (shapefileDataset == null) {
            System.err.println("Failed to open Shapefile.");
            return null;
        }
        // 指定新Shapefile文件的路径
        String newShapefilePath = param.getOutPath();
        SpatialReference targetSRS = new SpatialReference();
        targetSRS.ImportFromEPSG(32616); // EPSG code for WGS84 (地理坐标系)
        // 创建OGR驱动对象
        Driver driver = ogr.GetDriverByName("ESRI Shapefile");

        // 创建新的数据源
        DataSource newDataSource = driver.CreateDataSource(newShapefilePath);


        Map<String, String> result = new HashMap<>();

        // 获取Shapefile的图层
        int layerCount = shapefileDataset.GetLayerCount();
        for (int layerindex = 0; layerindex < layerCount; layerindex++) {
            // 获取第一个图层
            Layer layer = shapefileDataset.GetLayerByIndex(layerindex);

            // 创建新的图层
            Layer newLayer = newDataSource.CreateLayer(layer.GetName(), layer.GetSpatialRef(), ogr.wkbPolygon);
            // 创建源和目标坐标系
            SpatialReference sourceSRS = new SpatialReference(layer.GetSpatialRef().ExportToWkt());
            result.put("sourceCoordinateSystem", layer.GetSpatialRef().ExportToWkt());
            result.put("newCoordinateSystem", "WGS_1984(WGS84坐标系)");
            // 创建坐标系转换对象
            CoordinateTransformation coordinateTransformation = new org.gdal.osr.CoordinateTransformation(sourceSRS, targetSRS);
            // 遍历图层中的要素并进行坐标转换
            layer.ResetReading();
            org.gdal.ogr.Feature feature;
            while ((feature = layer.GetNextFeature()) != null) {
                Geometry geometry = feature.GetGeometryRef();
                geometry.TransformTo(targetSRS);
                // 创建新要素
                org.gdal.ogr.Feature newFeature = new org.gdal.ogr.Feature(newLayer.GetLayerDefn());
                // 设置新要素的几何信息
                newFeature.SetGeometry(geometry);
                // 写入新要素到新图层
                newLayer.CreateFeature(newFeature);
                // 释放资源
                geometry.delete();
                newFeature.delete();
                feature.delete();
            }
            // 清理资源
            sourceSRS.delete();
            layer.delete();
            newLayer.delete();
            coordinateTransformation.delete();
        }
        // 关闭数据集
        shapefileDataset.delete();
        result.put("ouputPath", param.getOutPath());
        newDataSource.delete();
        return result;
    }


    /**
     * 重采样
     *
     * @param param 重采样参数
     * @return 采样后文件路径
     */
    @Override
    public Map<String, String> resampleImage(ResampleParam param) {
        GDALInitializer.initialize();

        //实例化exe文件的执行对象
        //ExeExecution.doResampleOperation(param.getFilePath(), param.getOutPath(), param.getReSizeX(), param.getReSizeY(), param.getResampleMethod());

        ProgressReporter progressReporter = new ProgressReporter();
        GdalOptionTools.doResampleOperation(param.getFilePath(), param.getOutPath(), param.getReSizeX(), param.getReSizeY(), param.getResampleMethod(), progressReporter);

        if (progressReporter.getSchedule() >= 100) {
            //设置返回值参数
            Map<String, String> result = new HashMap<>();
            result.put("newWidth", String.valueOf(param.getReSizeX()));
            result.put("newHeight", String.valueOf(param.getReSizeY()));
            result.put("outputPath", param.getOutPath());

            return result;
        } else {
            return null;
        }

    }


    /**
     * 获取文件后缀
     *
     * @param file 文件
     * @return 后缀
     */
    public static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取文件大小
     *
     * @param file 文件
     * @return 大小
     */
    private static long getFileSize(File file) {
        return file.length() / 1024; // 文件大小以KB为单位
    }

    /**
     * 获取色彩模式
     *
     * @param dataset 数据集
     * @return 结果
     */
    private static String getColorPatten(Dataset dataset) {

        StringBuilder pattn = new StringBuilder();

        if (dataset != null) {
            int bandSize = dataset.getRasterCount();

            for (int i = 1; i <= bandSize; i++) {
                Band band = dataset.GetRasterBand(i);

                String bandName = "波段" + i + ":";

                //获取色彩模式
                int color = band.GetColorInterpretation();

                if (color == gdalconst.GCI_GrayIndex) {
                    pattn.append(bandName).append("灰度图像").append("\n");
                } else if (color == gdalconst.GCI_PaletteIndex) {
                    pattn.append(bandName).append("调色板索引图像").append("\n");

                } else if (color == gdalconst.GCI_RedBand) {
                    pattn.append(bandName).append("红色波段图像").append("\n");

                } else if (color == gdalconst.GCI_GreenBand) {
                    pattn.append(bandName).append("绿色波段图像").append("\n");

                } else if (color == gdalconst.GCI_BlueBand) {
                    pattn.append(bandName).append("蓝色波段图像").append("\n");

                } else if (color == gdalconst.GCI_AlphaBand) {
                    pattn.append(bandName).append("透明通道图像").append("\n");
                }
            }
        }

        return pattn.toString();
    }

    /**
     * gif图片转换要求
     *
     * @param targetFormat 目标格式
     */
    public static String gifChange(String inputpath, String outputFilePath, String targetFormat) throws IOException {
        // 加载 GIF 图像
        File gifFile = new File(inputpath);
        ImageInputStream input = ImageIO.createImageInputStream(gifFile);
        int dotIndex = gifFile.getName().lastIndexOf('.');
        String fileName = gifFile.getName().substring(0, dotIndex);

        // 获取 GIF 图像的 ImageReader
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        ImageReader reader = null;
        while (readers.hasNext()) {
            reader = readers.next();
            if (reader.getFormatName().equalsIgnoreCase("gif")) {
                break;
            }
        }

        if (reader != null) {
            reader.setInput(input);
        }
        int numFrames = 0;
        if (reader != null) {
            numFrames = reader.getNumImages(true);
        }
        // 确保父路径存在
        File outParent = new File(outputFilePath + File.separator + fileName);
        if (!outParent.exists()) {
            outParent.mkdir();
        }
        // 读取 GIF 图像的每一帧并保存为 PNG 图像
        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            // 获取当前帧的图像数据
            BufferedImage frame = reader.read(frameIndex);

            if (targetFormat.equals("PNG")) {
                File outputFile = new File(outputFilePath + File.separator + fileName + File.separator + fileName + "_Frame_" + frameIndex + ".png");
                ImageIO.write(frame, "png", outputFile);
            } else {
                File outputFile = new File(outputFilePath + File.separator + fileName + File.separator + fileName + "_frame_" + frameIndex + ".jpg");
                ImageIO.write(frame, "jpeg", outputFile);
            }
        }

        //将输出的文件打包成压缩包
        try {
            FileOutputStream fos = new FileOutputStream(outputFilePath + File.separator + fileName + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            File directory = new File(outputFilePath + File.separator + fileName);
            ZipFileTools.compressDirectory(directory, zos);
            zos.close();
            fos.close();
            System.out.println(outputFilePath + File.separator + fileName + ".zip");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 关闭 ImageInputStream 和 ImageReader
        if (reader != null) {
            reader.dispose();
        }
        input.close();
        return outputFilePath + File.separator + fileName + ".zip";
    }

    /**
     * 格式转换方法
     *
     * @param filePath     文件路径
     * @param targetFormat 目标格式
     * @return 返回结果
     */
    @Override
    public Map<String, String> changeFormat(String filePath, String outputPath, String targetFormat) {
        GDALInitializer.initialize();
        Map<String, String> res = new HashMap<>();
        File file = new File(filePath);
        int dotIndex = file.getName().lastIndexOf('.');
        String fileName = file.getName().substring(0, dotIndex);
        File outParent = new File(outputPath + File.separator + fileName);
        if (!outParent.exists()) {
            outParent.mkdir();
        }

//        if (GeoPreProServiceImpl.getFileExtension(file).equals("tif") && targetFormat.equals("AAIGrid")) {
//            Dataset imageSet = gdal.Open(filePath, gdalconst.GA_ReadOnly);
//            ExeExecution.doChangeFormat(filePath, outputPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), imageSet.getRasterCount());
//            imageSet.delete();
//
//        } else {
//            ExeExecution.doChangeFormat(filePath, outputPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), 0);
//        }

        ProgressReporter progressReporter = new ProgressReporter();
        GdalOptionTools.doChangeFormat(filePath, outputPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), progressReporter);

        if (progressReporter.getSchedule() >= 100) {
            //将输出的文件打包成压缩包
            try {
                FileOutputStream fos = new FileOutputStream(outputPath + File.separator + fileName + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);
                File directory = new File(outputPath + File.separator + fileName);
                ZipFileTools.compressDirectory(directory, zos);
                zos.close();
                fos.close();
                System.out.println(outputPath + File.separator + fileName + ".zip");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            res.put("targetFormat", targetFormat);
            res.put("outputPath", outputPath + File.separator + fileName + ".zip");
            return res;
        } else {
            return null;
        }
    }

    /**
     * 获取地球加载瓦片地图
     *
     * @param z 窗口当前缩放层级
     * @param x 瓦片列号
     * @param y 瓦片行号
     * @return 瓦片数据字节流
     */
    @Override
    public BufferedImage get3DTile(int z, int x, int y) {
        final String key = String.valueOf(z) + String.valueOf(x) + String.valueOf(y);
        if (CacheLoader.tryGetCacheTile(key) == null) {
            if (z < 18) {
                //18层级以下的数据调用GeographicTilingScheme格式的数据
                File file = new File(tilePath + z + File.separator + x + File.separator + y + ".jpg");
                if (file.exists()) {
                    try {
                        return ImageIO.read(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;
            } else {
                //18层级以上的调用91位图自定义原始瓦片数据，需要先做行列号转换
                int[] cor = getChang91rowCol(x, y, z);
                //File.separator表示文件层级的分割符号
                File file = new File(tilePath + (z + 1) + File.separator + cor[0] + File.separator + cor[1] + ".jpg");
                if (file.exists()) {
                    try {
                        return ImageIO.read(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                return null;
            }
        }
        return CacheLoader.tryGetCacheTile(key);
    }

    /**
     * 将瓦片行列号的格式从GeographicTilingScheme，转换为91位图的自定义原始瓦片
     * 两者区别在于：
     * GeographicTilingScheme纬度的切片范围是（-90.0，90.0），0层级瓦片为东西两张瓦片
     * 91位图自定义的原始瓦片纬度范围为（-180.0，180.0），且0层级瓦片为一张图
     * 两者都是为了将256*256的图片表示的区域尽可能表示为正方形区域
     *
     * @param x    瓦片在zoom层级下的列号
     * @param y    瓦片在zoom层级下的行号
     * @param zoom 窗口当前缩放层级
     * @return 转换后的行列号数组
     */
    public static int[] getChang91rowCol(int x, int y, int zoom) {
        int cols = (int) Math.pow(2, (zoom + 1));
        int rows = (int) Math.pow(2, zoom);

        double xcol = 360.0 / cols * x - 180.0;
        double yrow = 90.0 - (180.0 / rows) * y;

        int cols91 = (int) Math.pow(2, zoom + 1);
        int rows91 = (int) Math.pow(2, zoom + 1);

        int col = (int) Math.round((xcol + 180.0) * cols91 / 360.0);
        int row = (int) Math.round((180.0 - yrow) * rows91 / 360.0);

        return new int[]{row, col};
    }


    /**
     * 获取影像注记
     *
     * @param z
     * @param x
     * @param y
     * @return
     */
    @Override
    public BufferedImage getImageNote(int z, int x, int y) {
        File file = new File(notePath + File.separator + z + File.separator + x + File.separator + y + ".png");
        if (file.exists()) {
            try {
                return ImageIO.read(file);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * 获取切片文件Map
     *
     * @param area 区域的四至范围
     * @return
     */
    @Override
    public int[] getTileFiles(double[] area, int level) {
        //        return TileFileTask.getTileFiles3(geTileArea, tilePath);
        return TIleToGeo.geTileArea(area, level);
    }
}
