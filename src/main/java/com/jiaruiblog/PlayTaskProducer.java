package com.jiaruiblog;

import java.util.Queue;

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

    private FrameBuffer frameBuffer;

    public PlayTaskProducer(FrameBuffer frameBuffer, PlayParam playParam, String node, boolean preloadFillBufferMode) {
        this.frameBuffer = frameBuffer;
        this.playParam = playParam;
        this.node = node;
        this.preloadFillBufferMode = preloadFillBufferMode;
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
        int i = 1;
        while (true) {
            System.out.println("生产者准备生产第" + i + "个");
            frameBuffer.put(i);
            i ++;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
