package com.example.atlasgeopre.tools;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 处理瓦片和经纬坐标关系
 */
public class TIleToGeo {

    /**
     * 经纬度区域输出行列号范围
     *
     * @param geoArea minx，maxy，maxx，miny
     * @param level   瓦片层级
     * @return
     */
    public static int[] geTileArea(double[] geoArea, int level) {
        if (level < 18) {
            int mincol = (int) Math.round((geoArea[0] + 180.0) * Math.pow(2, level + 1) / 360.0);
            int minrow = (int) Math.round((90.0 - geoArea[1]) * Math.pow(2, level) / 180.0);
            int maxcol = mincol + (int) Math.round((geoArea[2] - geoArea[0]) * Math.pow(2, level + 1) / 360.0);
            int maxrow = minrow + (int) Math.round((geoArea[1] - geoArea[3]) * Math.pow(2, level) / 180.0);
            return new int[]{mincol, maxcol, minrow, maxrow, level};

        } else {
            int mincol = (int) Math.round((geoArea[0] + 180.0) * Math.pow(2, level) / 360.0);
            int minrow = (int) Math.round((180.0 - geoArea[1]) * Math.pow(2, level) / 360.0);
            int maxcol = mincol + (int) Math.round((geoArea[2] - geoArea[0]) * Math.pow(2, level) / 360.0);
            int maxrow = minrow + (int) Math.round((geoArea[1] - geoArea[3]) * Math.pow(2, level) / 360.0);
            return new int[]{mincol, maxcol, minrow, maxrow, level};
        }
    }

    /**
     * 根据瓦片范围，获取瓦片文件列表
     *
     * @param tileArea   瓦片编号区域
     * @param originPath 源路径
     * @return
     */
    public static HashMap<String, String> getTileFiles(int[] tileArea, String originPath) {
        originPath = originPath + File.separator + tileArea[4];
        HashMap<String, String> tileFiles = new HashMap<>();
        for (int row = tileArea[2]; row < tileArea[3]; row++) {
            for (int col = tileArea[0]; col < tileArea[1]; col++) {
                File file = new File(originPath + File.separator + col + File.separator + row + ".jpg");
                if (file.exists()) {
                    tileFiles.put(String.valueOf(col) + "_" + row, originPath + File.separator + col + File.separator + row + ".jpg");
                }
            }
        }
        return tileFiles;
    }

    /**
     * 根据行列号层级计算图像识别结果的坐标
     *
     * @param col     列号
     * @param row     行号
     * @param level   层级
     * @param picCoor 像素坐标，4行2列的二维数组
     * @return 返回结果，4行2列的二维数组，像素框转换后的经纬框范围
     */
    public static double[][] getGeoAreaFromtile(int col, int row, int level, double[][] picCoor) {
        //根据行列号以及level，计算左上角像素坐标
        double[][] res = new double[picCoor.length][picCoor[0].length];
        int n = (int) Math.pow(2, level);

        //根据行列号以及level，计算左上角像素坐标
        if (level < 18) {
            double ratio = (180.0 / n);
            double piexlRatio = ratio / 256;
            double y = 90.0 - ratio * row;
            double x = -180 + ratio * col;
            for (int i = 0; i < picCoor.length; i++) {
                double[] pointCoor = picCoor[i];
                double pointx = x + pointCoor[0] * piexlRatio;
                double pointy = y - pointCoor[1] * piexlRatio;
                res[i][0] = pointx;
                res[i][1] = pointy;
            }
            return res;
        }
        double ratio = (360.0 / n);
        double piexlRatio = ratio / 256;
        double y = 180.0 - ratio * row;
        double miny = 180.0 - ratio * (row + 1);
        double x = -180 + ratio * col;
        double maxx = -180.0 + ratio * (col + 1);
        for (int i = 0; i < picCoor.length; i++) {
            double[] pointCoor = picCoor[i];
            double pointx = x + pointCoor[0] * piexlRatio;
            double pointy = y - pointCoor[1] * piexlRatio;
            res[i][0] = pointx;
            res[i][1] = pointy;
        }

        return res;
    }
}
