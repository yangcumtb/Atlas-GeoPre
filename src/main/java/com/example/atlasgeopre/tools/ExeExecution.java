package com.example.atlasgeopre.tools;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class ExeExecution {

    public static String gdalCachPath = "/Users/yang/Documents/xxx项目预处理/data/mask/roadshp/roadshp";


    /**
     * @param inputFile  输入文件
     * @param outputFile 输出文件
     * @param width      采样宽度
     * @param height     采样高度
     * @param method     采样方法
     */
    public static void doResampleOperation(String inputFile, String outputFile, int width, int height, String method) {
        try {
            // 设置.exe文件路径
            //linux系统路径
            String exePath = "gdalwarp";
            // 设置命令参数
            String[] command = {
                    exePath,
                    "-r",
                    method,
                    "-ts",
                    String.valueOf(width),
                    String.valueOf(height),
                    inputFile,
                    outputFile
            };

            // 创建进程并执行命令
            System.out.println("正在执行重采样，采样方式：" + method);
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 更换坐标系
     *
     * @param inputPath  源文件
     * @param outputPath 输出文件
     * @param sourceEPSG 源坐标系
     * @param targetEPSG 目标坐标系
     */
    public void doChangeCoordinate(String inputPath, String outputPath, String sourceEPSG, String targetEPSG) {
        try {
            // 设置.exe文件路径
            //linux系统路径
            String exePath = "gdalwarp";

            // 设置命令参数
            String[] command = {
                    exePath,
                    "-s_srs",
                    sourceEPSG,
                    "-t_srs",
                    targetEPSG,
                    "\"" + inputPath + "\"",
                    "\"" + outputPath + "\"",
            };
            // 设置环境变量

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            Process process = processBuilder.start();

            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 格式转换方法
     *
     * @param inputPath  文件输入路径
     * @param outputPath 文件输出路径
     * @param format     文件格式
     * @param bandCount  波段总数（Grid格式需要逐波段输出）
     */
    public static String doChangeFormat(String inputPath, String outputPath, String imageName, String format, String suffix, Integer bandCount) {
        try {
            String exePath = "gdal_translate";
            String[] command;
            if (format.equals("AAIGrid")) {
                String ouput = outputPath + "\\" + imageName;
                // 对于GRID格式，要逐波段设置命令参数
                for (int i = 1; i <= bandCount; i++) {
                    command = new String[]{
                            exePath,
                            "-b",
                            String.valueOf(i),
                            "-of",
                            "AAIGrid",
                            "\"" + inputPath + "\"",
                            "\"" + outputPath + "\\" + imageName + "\\" + imageName + "_band" + String.valueOf(i) + suffix + "\"",
                    };
                    System.out.println("正在执行命令行：" + Arrays.toString(command));
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    Process process = processBuilder.start();
                    // 获取命令的输入流和输出流
                    InputStream inputStream = process.getInputStream();
                    InputStream errorStream = process.getErrorStream();
                    // 读取命令的输出
                    String output = readStream(inputStream);

                    // 读取命令的错误输出
                    String errorOutput = readStream(errorStream);
                    // 输出命令的输出和错误输出
                    System.out.println("命令输出:\n" + output);
                    System.out.println("错误输出:\n" + errorOutput);
                    // 等待命令执行完成
                    int exitCode = process.waitFor();
                    System.out.println("退出码: " + exitCode);
                }
                return ouput;
            } else {
                String ouput = outputPath + "\\" + imageName + "\\" + imageName + suffix;
                // 设置命令参数
                switch (suffix) {
                    case ".bsq":
                        command = new String[]{
                                exePath,
                                "-of",
                                "ENVI",
                                "-co",
                                "INTERLEAVE=BSQ",
                                "\"" + inputPath + "\"",
                                "\"" + outputPath + "\\" + imageName + "\\" + imageName + suffix + "\"",
                        };
                        break;
                    case ".bil":
                        command = new String[]{
                                exePath,
                                "-of",
                                "ENVI",
                                "-co",
                                "INTERLEAVE=BIL",
                                "\"" + inputPath + "\"",
                                "\"" + outputPath + "\\" + imageName + "\\" + imageName + suffix + "\"",
                        };
                        break;
                    case ".bip":
                        command = new String[]{
                                exePath,
                                "-of",
                                "ENVI",
                                "-co",
                                "INTERLEAVE=BIP",
                                "\"" + inputPath + "\"",
                                "\"" + outputPath + "\\" + imageName + "\\" + imageName + suffix + "\"",
                        };
                        break;
                    default:
                        command = new String[]{
                                exePath,
                                "-of",
                                format,
                                "\"" + inputPath + "\"",
                                "\"" + outputPath + "\\" + imageName + "\\" + imageName + suffix + "\"",
                        };
                        break;
                }
                System.out.println("正在执行命令行：" + Arrays.toString(command));
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process process = processBuilder.start();
                // 获取命令的输入流和输出流
                InputStream inputStream = process.getInputStream();
                InputStream errorStream = process.getErrorStream();

                // 读取命令的输出
                String output = readStream(inputStream);

                // 读取命令的错误输出
                String errorOutput = readStream(errorStream);
                // 输出命令的输出和错误输出
                System.out.println("命令输出:\n" + output);
                System.out.println("错误输出:\n" + errorOutput);
                // 等待命令执行完成
                int exitCode = process.waitFor();
                System.out.println("退出码: " + exitCode);
                return ouput;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return "";
    }

    // 读取流并返回字符串
    public static String readStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    public static String getOutBox(String inputfile) {
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

        String[] command = new String[]{
                "gdaltindex",
                filePath,
                inputfile
        };
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void pixelMask(String inputfile, String outputFile, String shpfile) {

        String[] command = new String[]{
                "gdalwarp",
                "-cutline",
                shpfile,
                "-dstnodata",
                "0",
                "-of",
                "GTiff",
                "-overwrite",
                inputfile,
                outputFile
        };

        System.out.println(String.valueOf(command));
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            // 获取命令的输入流和输出流
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // 读取命令的输出
            String output = readStream(inputStream);

            // 读取命令的错误输出
            String errorOutput = readStream(errorStream);
            // 输出命令的输出和错误输出
            System.out.println("命令输出:\n" + output);
            System.out.println("错误输出:\n" + errorOutput);
            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("退出码: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
