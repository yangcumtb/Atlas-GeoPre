package com.example.atlasgeopre.tools;

import org.gdal.gdal.ProgressCallback;

/**
 * @Author:zcx
 * @DATE:2023/5/16 13:18
 * @Description:gdal操作进度信息
 * @Version 1.0
 */
public class ProgressReporter extends ProgressCallback {
    //进度
    double schedule = 0.0;
    Integer id;

    public double getSchedule() {
        return schedule;
    }

    public void setSchedule(double schedule) {
        this.schedule = schedule;
    }

    public void mytask(Integer id) {
        this.id = id;
    }

    @Override
    public int run(double dfComplete, String pszMessage) {
        // 如果有进度信息，将其输出
//        if (pszMessage != null && !pszMessage.isEmpty()) {
//            System.out.println(pszMessage);
//        }
        // 将进度转化为百分数，并将其设置为status的值
//        System.out.println(id);
        setSchedule(dfComplete * 100);
//        System.out.printf("%.2f%n", dfComplete * 100);
        return 1;
    }
}
