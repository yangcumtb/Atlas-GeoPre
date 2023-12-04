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
import org.gdal.gdalconst.gdalconstConstants;
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
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

@Service
public class GeoPreProServiceImpl implements GeoPreProService {
    @Value("${url.tilePath}")
    private String tilePath;

    @Value("${url.tilePath2}")
    private String tilePath2;

    @Value("${url.tilePath3}")
    private String tilePath3;

    @Value("${url.tilePath4")
    private String tilePath4;

    @Value("${url.tilePath5}")
    private String tilePath5;

    @Value("${url.notePath}")
    private String notePath;

    @Value("${url.gdalCachPath}")
    private String gdalCachPath;

    @Value("${url.earthC}")
    private Double earthC;


    /**
     * 获取tiff文件元数据
     *
     * @param filePath 文件路径
     * @return 元数据
     */
    @Override
    public TiffMetaData getMetadata(String filePath) {
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

            tiffMetaData.setAffineTransformation(new double[]{point.GetY(), point.GetX(), resolutionInDegreesx, resolutionInDegreesy, geotransform[1], geotransform[5], geotransform[2], geotransform[4]});

        } else {
            /**
             * 计算米级分辨率
             */
            SpatialReference projSRS = new SpatialReference(imageSet.GetProjection());
            SpatialReference latLonSRS = new SpatialReference();

            latLonSRS.ImportFromEPSG(3857); // 设置地理坐标系为WGS 84
            CoordinateTransformation transform = new CoordinateTransformation(projSRS, latLonSRS);
            double[] leftUp = {geotransform[3], geotransform[0]};
            double[] rightDown = {geotransform[3] + geotransform[5] * imageSet.getRasterYSize(), geotransform[0] + geotransform[1] * imageSet.getRasterXSize()};
            double[] newleftUp = new double[3];
            double[] newrightdown = new double[3];
            transform.TransformPoint(newleftUp, leftUp[0], leftUp[1]);
            transform.TransformPoint(newrightdown, rightDown[0], rightDown[1]);

            double rexX = (newrightdown[0] - newleftUp[0]) / imageSet.getRasterXSize();

            double rexY = (newrightdown[1] - newleftUp[1]) / imageSet.getRasterYSize();

            tiffMetaData.setAffineTransformation(new double[]{geotransform[0], geotransform[3], geotransform[1], geotransform[5], rexX, rexY, geotransform[2], geotransform[4]});
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
                } else {
                    pattn.append(bandName).append("色彩未定义").append("\n");
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
//        File outParent = new File(outputPath + File.separator + fileName);
//        if (!outParent.exists()) {
//            outParent.mkdir();
//        }

//        if (GeoPreProServiceImpl.getFileExtension(file).equals("tif") && targetFormat.equals("AAIGrid")) {
//            Dataset imageSet = gdal.Open(filePath, gdalconst.GA_ReadOnly);
//            ExeExecution.doChangeFormat(filePath, outputPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), imageSet.getRasterCount());
//            imageSet.delete();
//
//        } else {
//            ExeExecution.doChangeFormat(filePath, outputPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), 0);
//        }
        // 获取当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String currentDate = dateFormat.format(new Date());
        // 创建日期路径
        String datePath = outputPath + File.separator + currentDate;
        File dateDirectory = new File(datePath);
        if (!dateDirectory.exists()) {
            dateDirectory.mkdirs();
        }
        // 文件名以"merge"为开始，后面加递增序号
        int sequence = 1;
        String fileoutPath = datePath + File.separator + fileName;

        // 检查文件是否存在，如果存在则增加递增序号
        while (new File(fileoutPath).exists()) {
            sequence++;
            String newfileName = fileName + "(" + sequence + ")";
            fileoutPath = datePath + File.separator + newfileName;
        }

        new File(fileoutPath).mkdir();

        ProgressReporter progressReporter = new ProgressReporter();
        GdalOptionTools.doChangeFormat(filePath, fileoutPath, fileName, targetFormat, FormatEum.getSuffixValue(targetFormat), progressReporter);

        if (progressReporter.getSchedule() >= 100) {
            //将输出的文件打包成压缩包
            try {
                FileOutputStream fos = new FileOutputStream(fileoutPath + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);
                File directory = new File(fileoutPath);
                ZipFileTools.compressDirectory(directory, zos);
                zos.close();
                fos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            res.put("targetFormat", targetFormat);
            res.put("outputPath", fileoutPath + ".zip");
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
                File file = new File(tilePath + File.separator + z + File.separator + x + File.separator + y + ".jpg");
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
                //由于数据分盘符挂载，需要确认根目录路径
                String finaltilePath = get19TilePath(cor);
                //File.separator表示文件层级的分割符号
                File file = new File(finaltilePath + File.separator + (z + 1) + File.separator + cor[0] + File.separator + cor[1] + ".jpg");
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
     * 19级别瓦片多目录判断根目录
     *
     * @param cor 行列号
     * @return
     */
    public String get19TilePath(int[] cor) {
        // 新建磁盘区间的起始列、结束列、起始行和结束行
        int[] columns = {445644, 471858, 436906, 480596, 419430, 445643, 419430, 463120,
                419430, 463120, 428168, 489334, 463121, 489334, 471859, 480596, 393216, 419429, 393216, 454380};
        int[] rows = {273794, 279619, 279620, 285444, 291271, 297095, 297096, 302920,
                302921, 308746, 308747, 314571, 314572, 320397, 320398, 326222, 250493, 256317, 256318, 262143};
        // 检查col和row是否在任何一个区间内
        for (int i = 0; i < columns.length; i += 2) {
            if (columns[i] <= cor[1] && cor[1] <= columns[i + 1] && rows[i] <= cor[0] && cor[0] <= rows[i + 1]) {
                return tilePath2;
            }
        }
        //甲方2范围
        int[] columns2 = {78643, 166023, 148548, 166023, 78643, 122332};
        int[] rows2 = {192238, 198063, 198064, 203888, 203889, 209714};
        // 检查col和row是否在任何一个区间内
        for (int i = 0; i < columns2.length; i += 2) {
            if (columns2[i] <= cor[1] && cor[1] <= columns2[i + 1] && rows2[i] <= cor[0] && cor[0] <= rows2[i + 1]) {
                return tilePath3;
            }
        }
        //甲方3范围
        int[] columns3 = {428168, 480596, 445644, 489334, 463121, 489334, 471859, 489334, 393216, 419429, 393216, 445643, 367001, 445643, 367001, 454381, 375739, 384476, 419430, 454381};
        int[] rows3 = {285445, 291270, 291271, 297095, 297096, 302920, 302921, 308746, 227191, 233015, 233016, 238841, 238842, 244666, 244667, 250492, 250493, 256317, 250493, 256317};
        // 检查col和row是否在任何一个区间内
        for (int i = 0; i < columns3.length; i += 2) {
            if (columns3[i] <= cor[1] && cor[1] <= columns3[i + 1] && rows3[i] <= cor[0] && cor[0] <= rows3[i + 1]) {
                return tilePath4;
            }
        }
        //甲方4范围
        // 新区间的起始列、结束列、起始行和结束行
        int[] columns4 = {78643, 157285, 463121, 480596, 463121, 480596, 445644, 471858, 445644, 471858, 445644, 454381, 375739, 410691, 445644, 454381, 375739, 393215, 367001, 393215};
        int[] rows4 = {198064, 203888, 192238, 198063, 198064, 203888, 203889, 209714, 209715, 215539, 215540, 221365, 221366, 227190, 221366, 227190, 227191, 233015, 233016, 238841};
        // 检查col和row是否在任何一个区间内
        for (int i = 0; i < columns4.length; i += 2) {
            if (columns4[i] <= cor[1] && cor[1] <= columns4[i + 1] && rows4[i] <= cor[0] && cor[0] <= rows4[i + 1]) {
                return tilePath5;
            }
        }
        return tilePath;
    }


    /**
     * 获取影像注记
     *
     * @param z 层级
     * @param x 列号
     * @param y 行号
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


    /**
     * 根据掩膜shp文件来处理
     *
     * @param inputFile 输入文件
     * @param outfile   输出文件
     * @param maskfiles 掩膜文件
     * @return
     */
    @Override
    public boolean pixelMask(String inputFile, String outfile, String maskfiles) {

        File file = new File(outfile);
        if (!new File(file.getParent()).exists()) {
            new File(file.getParent()).mkdir();
        }

        //maskfiles为多个shp文件，用“，”隔开
        String[] shpfiles = maskfiles.split(",");
        String outboxshp = GdalOptionTools.getOutBox(inputFile, gdalCachPath);

        String mergeshp = GdalOptionTools.mergeShp(shpfiles, gdalCachPath);
        String maskfile = GdalOptionTools.getMaskArea(outboxshp, mergeshp, gdalCachPath);
        GdalOptionTools.pixelMask(inputFile, outfile, maskfile);
        return true;
    }
}
