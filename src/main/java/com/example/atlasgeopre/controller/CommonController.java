package com.example.atlasgeopre.controller;

import com.example.atlasgeopre.common.config.GDALInitializer;
import com.example.atlasgeopre.models.CoordinateParam;
import com.example.atlasgeopre.models.ResampleParam;
import com.example.atlasgeopre.models.ResponseData;
import com.example.atlasgeopre.models.TiffMetaData;
import com.example.atlasgeopre.service.GeoPreProService;
import com.example.atlasgeopre.service.impl.GeoPreProServiceImpl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/imagePreprocessing")
@Api("信息处理")
public class CommonController {

    @Autowired
    private GeoPreProService geoPreProService;

    /**
     * 获取影像元数据接口
     *
     * @param filePath 影像路径
     * @return 元数据
     */
    @GetMapping("/MetaData")
    @ApiOperation("获取影像元数据接口")
    public ResponseData getMetaData(String filePath) {
        try {
            TiffMetaData data = geoPreProService.getMetadata(filePath);
            return ResponseData.success(data);
        } catch (Exception e) {
            //如果文件无法读取，返回原因
            return ResponseData.error(e.getCause().toString());
        }
    }

    /**
     * 更换坐标系
     *
     * @param outPath    输出路径
     * @param targetEPSG 目标epsg
     * @return
     */
    @PostMapping("/Projecttion")
    @ApiOperation("更换坐标系")
    public ResponseData changeCoor(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("targetEPSG") String targetEPSG
    ) {

        CoordinateParam param = new CoordinateParam();
        param.setFilePath(filePath);
        param.setOutPath(outPath);
        param.setTargetEPSG(targetEPSG);
        Map<String, String> result = geoPreProService.changeCoordination(param);
        return ResponseData.success(result);
    }

    /**
     * 重采样
     *
     * @param filePath       文件路径
     * @param outPath        输出路径
     * @param resampleMethod 采样方法
     * @param reSizeX        采样后x像素值
     * @param reSizeY        采样后y像素值
     * @return
     */
    @PostMapping("/Resample")
    @ApiOperation("重采样")
    public ResponseData resample(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("resampleMethod") String resampleMethod,
            @RequestParam("reSizeX") Integer reSizeX,
            @RequestParam("reSizeY") Integer reSizeY
    ) {
        ResampleParam resampleParam = new ResampleParam();
        resampleParam.setResampleMethod(resampleMethod);
        resampleParam.setFilePath(filePath);
        resampleParam.setOutPath(outPath);
        resampleParam.setReSizeX(reSizeX);
        resampleParam.setReSizeY(reSizeY);

        Map<String, String> result = geoPreProService.resampleImage(resampleParam);
        return ResponseData.success(result);
    }

    /**
     * 转换影像格式，需要支持的格式：
     *
     * @param filePath     文件路径
     * @param outputPath   输出路径
     * @param targetFormat 目标格式
     * @return
     */
    @PostMapping("/ChangeFormat")
    @ApiOperation("转换影像格式")
    public ResponseData changeFormat(
            @RequestParam("filePath") String filePath,
            @RequestParam("outputPath") String outputPath,
            @RequestParam("targetFormat") String targetFormat
    ) throws IOException {
        File out = new File(outputPath);
        File input = new File(filePath);
        if (!out.exists()) {
            out.mkdir();
        }

        if (GeoPreProServiceImpl.getFileExtension(input).equals("gif")) {
            //对于gif文件，只能保存为png或者jpg
            String oupath = GeoPreProServiceImpl.gifChange(filePath, outputPath, targetFormat);
            if (oupath.equals("")) {
                return ResponseData.error("转换失败！");
            }
            Map<String, String> res = new HashMap<>();
            res.put("outputPath", oupath);
            return ResponseData.success(res);
        }
        Map<String, String> res = geoPreProService.changeFormat(filePath, outputPath, targetFormat);
        if (res == null) {
            return ResponseData.error("转换失败！");
        }
        return ResponseData.success(res);
    }


    @GetMapping("/api/readImage")
    @ApiOperation("底图瓦片")
    public ResponseEntity<ByteArrayResource> get3DTile(
            @RequestParam("col") Integer x,
            @RequestParam("row") Integer y,
            @RequestParam("lev") Integer z) throws IOException {
        // 使用TileService类读取和渲染瓦片数据
        BufferedImage image = geoPreProService.get3DTile(z, x, y);
        // 将图像数据转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] bytes = baos.toByteArray();
        // 返回字节数组
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .contentLength(bytes.length)
                .body(resource);
    }


    @PostMapping("/cs")
    public ResponseData cs() throws IOException, InterruptedException {
        GDALInitializer.initialize();
        Dataset hDataset = gdal.OpenShared("G:\\XQ\\fvc\\3.tif", gdalconstConstants.GA_ReadOnly);
        System.out.println("zbx:" + hDataset.GetProjection());
        System.out.println("成功");
//       try {
//           ProcessBuilder processBuilder = new ProcessBuilder("gdalinfo", "--version");
//           Process process = processBuilder.start();
//
//           BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//           String line;
//           while ((line = reader.readLine()) != null) {
//               System.out.println(line);
//           }
//
//           int exitCode = process.waitFor();
//           System.out.println("Exit Code: " + exitCode);
//
//       } catch (IOException | InterruptedException e) {
//           e.printStackTrace();
//       }
        gdal.GDALDestroyDriverManager();
        return ResponseData.success("成功");
    }

    public static void main(String[] args) {

    }
}
