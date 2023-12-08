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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/imagePreprocessing")
@CrossOrigin("*")
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
     * 更换shp坐标系
     *
     * @param outPath    输出路径
     * @param targetEPSG 目标epsg
     * @return
     */
    @PostMapping("/shpProjecttion")
    @ApiOperation("更换shp坐标系")
    public ResponseData changeShpCoor(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("targetEPSG") String targetEPSG
    ) {

        CoordinateParam param = new CoordinateParam();
        param.setFilePath(filePath);
        param.setOutPath(outPath);
        param.setTargetEPSG(targetEPSG);
        Map<String, String> result = geoPreProService.changeShpCoordination(param);
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
        if (image == null) {
            return null;
        }
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

    @GetMapping("/api/imageNote")
    @ApiOperation("影像注记")
    public ResponseEntity<ByteArrayResource> getImageNote(
            @RequestParam("tilematrix") int z,
            @RequestParam("tilerow") int y,
            @RequestParam("tilecol") int x
    ) throws IOException {
        // 使用TileService类读取和渲染瓦片数据
        BufferedImage image = geoPreProService.getImageNote(z, x, y);
        if (image == null) {
            return null;
        }
        // 将图像数据转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        // 返回字节数组
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .contentLength(bytes.length)
                .body(resource);
    }

    @GetMapping("/getTileMaps")
    @ApiOperation("获取瓦片检索范围-{最小x坐标，最大y坐标，最大x坐标，最小y坐标，层级(17或者19)}")
    public ResponseData getTileMaps(
            @RequestParam("minX") Double minX,
            @RequestParam("maxY") Double maxY,
            @RequestParam("maxX") Double maxX,
            @RequestParam("minY") Double minY,
            @RequestParam("level") Integer level) {

        return ResponseData.success(geoPreProService.getTileFiles(new double[]{minX, maxY, maxX, minY}, level));

    }


    @PostMapping("/pixelMask")
    @ApiOperation("像素掩膜")
    public ResponseData pixelMask(
            @RequestParam("filePath") String filePath,
            @RequestParam("outPath") String outPath,
            @RequestParam("shpfiles") String shpfiles
    ) {
        geoPreProService.pixelMask(filePath, outPath, shpfiles);
        return ResponseData.success();
    }

    @RequestMapping("/down")
    @ApiOperation("断点下载")
    public void downLoadFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置编码格式
        response.setCharacterEncoding("utf-8");
        //获取文件路径
        String fileName = request.getParameter("fileName");
//        String drive = request.getParameter("drive");
        //完整路径(路径拼接待优化-前端传输优化-后端从新格式化  )
        String pathAll = "D:\\data\\cc\\" + fileName;
        Optional<String> pathFlag = Optional.of(pathAll);
        File file = null;
        //根据文件名，读取file流
        file = new File(pathAll);
//        System.out.println("文件路径:" + file.getAbsoluteFile());
        if (!file.exists()) {
            System.out.println("文件不存在");
            return;
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            //分片下载
            long fSize = file.length();//获取长度
            response.setContentType("application/blob");
            String file_Name = URLEncoder.encode(file.getName(), "UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + file_Name);
            //根据前端传来的Range  判断支不支持分片下载
            response.addHeader("Accept-Range", "bytes");
            //获取文件大小
            //response.setHeader("fSize",String.valueOf(fSize));
            response.addHeader("fName", file_Name);
            //定义断点
            long pos = 0, last = fSize, sum = 0;
            //判断前端需不需要分片下载
            if (null != request.getHeader("Range")) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String numRange = request.getHeader("Range").replaceAll("bytes=", "");
                String[] strRange = numRange.split("-");
                if (strRange.length == 2) {
                    pos = Long.parseLong(strRange[0].trim());
                    last = Long.parseLong(strRange[1].trim());
                    //若结束字节超出文件大小 取文件大小
                    if (last > fSize) {
                        last = fSize;
                    }
                } else {
                    //若只给一个长度  开始位置一直到结束
                    pos = Long.parseLong(numRange.replaceAll("-", "").trim());
                }
            }
            long rangeLenght = last - pos;
            response.setHeader("Access-Control-Expose-Headers", "Content-Range");
            String contentRange = "bytes " + pos + "-" + last + "/" + fSize;
            response.addHeader("Content-Range", contentRange);
            response.addHeader("Content-Length", String.valueOf(rangeLenght));
            os = new BufferedOutputStream(response.getOutputStream());
            is = new BufferedInputStream(Files.newInputStream(file.toPath()));
            is.skip(pos);//跳过已读的文件(重点，跳过之前已经读过的文件)
            byte[] buffer = new byte[1024];
            int lenght = 0;
            System.out.println(pos + "-" + last);
            while (sum < rangeLenght) {
                lenght = is.read(buffer, 0, (rangeLenght - sum) <= buffer.length ? (int) (rangeLenght - sum) : buffer.length);
                sum = sum + lenght;
                os.write(buffer, 0, lenght);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

}
