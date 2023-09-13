package com.example.atlasgeopre.tools;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * @Author:
 * @DATE:2023/9/13 15:29
 * @Description:
 * @Version 1.0
 */
public class TileFileTask extends RecursiveAction {
    private final int[] tileArea; // 瓦片区域的坐标范围
    private final String originPath; // 瓦片文件的原始路径
    private final String fullPath; // 瓦片文件的完整路径
    private final ConcurrentHashMap<String, String> tileFiles; // 存储瓦片文件的ConcurrentHashMap
    private final int startRow; // 起始行
    private final int endRow; // 结束行
    private final int startCol; // 起始列
    private final int endCol; // 结束列

    private final int task_num;
    private final AtomicInteger taskCounter;


    public TileFileTask(int[] tileArea, String originPath, String fullPath, ConcurrentHashMap<String, String> tileFiles, int startRow, int endRow, int startCol, int endCol,int task_num,AtomicInteger taskCounter) {
        this.tileArea = tileArea;
        this.originPath = originPath;
        this.fullPath = fullPath;
        this.tileFiles = tileFiles;
        this.startRow = startRow;
        this.endRow = endRow;
        this.startCol = startCol;
        this.endCol = endCol;
        this.task_num=task_num;
        this.taskCounter = taskCounter;
    }
    @Override
    protected void compute() {
        // 如果任务的大小小于等于16，则顺序执行文件检查和添加操作
        if ((endRow - startRow) * (endCol - startCol) <= task_num) {
//            for (int row = startRow; row < endRow; row++) {
//                for (int col = startCol; col < endCol; col++) {
//                    File file = new File(fullPath + File.separator + col + File.separator + row + ".jpg");
//                    if (file.exists()) {
//                        String key = col + "_" + row;
//                        String filePath = fullPath + File.separator + col + File.separator + row + ".jpg";
//                        tileFiles.put(key, filePath);
//                    }
//                }
//            }
            IntStream.range(startRow,endRow).parallel().forEach(row -> IntStream.range(startCol, endCol).parallel().forEach(col -> {
                        File file = new File(fullPath + File.separator + col + File.separator + row + ".jpg");
                        if (file.exists()) {
                            String key = col + "_" + row;
                            String filePath = fullPath + File.separator + col + File.separator + row + ".jpg";
                            tileFiles.put(key, filePath);
                        }
                    }));
            //System.out.println((endRow - startRow) * (endCol - startCol));
            System.out.println("任务 " + taskCounter.incrementAndGet() + " executed.");
        } else {
            int midRow = (startRow + endRow) / 2;
            int midCol = (startCol + endCol) / 2;
            invokeAll(
                    new TileFileTask(tileArea, originPath, fullPath, tileFiles, startRow, midRow, startCol, midCol,task_num,taskCounter),
                    new TileFileTask(tileArea, originPath, fullPath, tileFiles, startRow, midRow, midCol, endCol,task_num,taskCounter),
                    new TileFileTask(tileArea, originPath, fullPath, tileFiles, midRow, endRow, startCol, midCol,task_num,taskCounter),
                    new TileFileTask(tileArea, originPath, fullPath, tileFiles, midRow, endRow, midCol, endCol,task_num,taskCounter)
            );
        }
    }

   public static ConcurrentHashMap<String, String> getTileFiles3(int[] tileArea, String originPath) {
        String fullPath = originPath + File.separator + tileArea[4];
        ConcurrentHashMap<String, String> tileFiles = new ConcurrentHashMap<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        int num=(tileArea[3]-tileArea[2])*( tileArea[1]-tileArea[0]);
        System.out.println(num);
        int task_num= num/100000;
        long startTime = System.currentTimeMillis();
        AtomicInteger taskCounter = new AtomicInteger(0);
        TileFileTask tileFileTask=new TileFileTask(tileArea, originPath, fullPath, tileFiles, tileArea[2], tileArea[3], tileArea[0], tileArea[1],task_num,taskCounter);
        forkJoinPool.invoke(tileFileTask);
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("执行时间: " + executionTime + " milliseconds");
        return tileFiles;
    }

}
