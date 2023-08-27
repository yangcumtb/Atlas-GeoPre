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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipOutputStream;

@Service
public class GeoPreProServiceImpl implements GeoPreProService {
    @Value("${url.tilePath}")
    private String tilePath;

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
        tiffMetaData.setAffineTransformation(imageSet.GetGeoTransform());


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
        String sourceEPSG = "EPSG:" + spatialRef.GetAttrValue("AUTHORITY", 1);

        //实例化exe文件的执行对象
//        ExeExecution exeExecution = new ExeExecution();
//        exeExecution.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceEPSG, param.getTargetEPSG());
        ProgressReporter progressReporter = new ProgressReporter();
        GdalOptionTools.doChangeCoordinate(param.getFilePath(), param.getOutPath(), sourceEPSG, param.getTargetEPSG(), progressReporter);

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
        File outParent = new File(outputFilePath + "/" + fileName);
        if (!outParent.exists()) {
            outParent.mkdir();
        }
        // 读取 GIF 图像的每一帧并保存为 PNG 图像
        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            // 获取当前帧的图像数据
            BufferedImage frame = reader.read(frameIndex);

            if (targetFormat.equals("PNG")) {
                File outputFile = new File(outputFilePath + "/" + fileName + "/" + fileName + "_Frame_" + frameIndex + ".png");
                ImageIO.write(frame, "png", outputFile);
            } else {
                File outputFile = new File(outputFilePath + "/" + fileName + "/" + fileName + "_frame_" + frameIndex + ".jpg");
                ImageIO.write(frame, "jpeg", outputFile);
            }
        }

        //将输出的文件打包成压缩包
        try {
            FileOutputStream fos = new FileOutputStream(outputFilePath + "/" + fileName + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);
            File directory = new File(outputFilePath + "/" + fileName);
            ZipFileTools.compressDirectory(directory, zos);
            zos.close();
            fos.close();
            System.out.println(outputFilePath + "/" + fileName + ".zip");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 关闭 ImageInputStream 和 ImageReader
        if (reader != null) {
            reader.dispose();
        }
        input.close();
        return outputFilePath + "/" + fileName + ".zip";
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
        File outParent = new File(outputPath + "/" + fileName);
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
                FileOutputStream fos = new FileOutputStream(outputPath + "/" + fileName + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);
                File directory = new File(outputPath + "/" + fileName);
                ZipFileTools.compressDirectory(directory, zos);
                zos.close();
                fos.close();
                System.out.println(outputPath + "/" + fileName + ".zip");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            res.put("targetFormat", targetFormat);
            res.put("outputPath", outputPath + "/" + fileName + ".zip");
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
}
