package com.jiaruiblog;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * <p>从buffer中获取数据</p>
 * edit at 2024/10/6 10:31
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class PlayTaskConsumer implements Runnable{


    // 播放缓冲队列
    private Queue<?> queue;

    // 播放速率
    private int playRate;

    private boolean isStopped = false;

    private boolean isPaused;

    private boolean isPlayOnce;

    int beginTimeNs;

    private int baseMsgPlayTimeNs;

    private int baseMegRealTimeNs;

    private int lastPlayedMsgRealTimeNs;

//    private final FrameBuffer frameBuffer;

    LinkedBlockingDeque<Integer> blockingQueue;

    int i = 0;

    private String name;

    // 初始化构造器
    public PlayTaskConsumer(LinkedBlockingDeque<Integer>  frameBuffer, int playRate, String name) {
        this.blockingQueue = frameBuffer;
        this.playRate = playRate;

        this.name = name;
    }

    public void start(int beginTimeNs) {
        //
    }

    public void stop() {

    }

    @Override
    public void run() {
        while(true){
            try {
                int x = blockingQueue.take();
                System.out.println("[" + name + "] Consuming : " + x);

                //暂停最多1秒
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }




//        System.out.println("这里是消费者");
//        int i = 1;
//        System.out.println(isStopped);
//        while (!isStopped) {
//            Frame frame = frameBuffer.get();
//            System.out.println("frame: " + frame);
//            System.out.println("当前正在消耗" + i + "个");
//            i ++;
//        }
    }
}
