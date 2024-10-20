package com.jiaruiblog;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * <p>生产数据</p>
 * edit at 2024/10/6 10:30
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class PlayTaskProducer implements Runnable {

    // 缓冲队列
    private Queue<?> queue;

    // 播放参数
    private PlayParam playParam;

    // todo 节点信息
    private String node;

    private int earliestBeginTime;

    private int latestEndTime;

    private int totalMsgNum;

    // 预加载填充缓冲模式
    private boolean preloadFillBufferMode;

//    private FrameBuffer frameBuffer;

    LinkedBlockingDeque<Integer> blockingQueue;


    int i = 0;

    private String name;

    public PlayTaskProducer(LinkedBlockingDeque<Integer> frameBuffer, PlayParam playParam, String node,
                            boolean preloadFillBufferMode,
                            String name) {
        this.blockingQueue = frameBuffer;
        this.playParam = playParam;
        this.node = node;
        this.preloadFillBufferMode = preloadFillBufferMode;

        this.name = name;
    }

    private void start() {

    }

    private void stop() {

    }

    private boolean readRecordInfo() {
        return false;
    }

    private boolean updatePlayParam() {
        return false;
    }



    @Override
    public void run() {
        while(true){
            try {
                blockingQueue.put(i);
                System.out.println("[" + name + "] Producing value : " + i);
                i++;

                //暂停最多1秒
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
