package com.jiaruiblog;

import java.util.Queue;

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

    private boolean isStopped;

    private boolean isPaused;

    private boolean isPlayOnce;

    int beginTimeNs;

    private int baseMsgPlayTimeNs;

    private int baseMegRealTimeNs;

    private int lastPlayedMsgRealTimeNs;



    // 初始化构造器
    public PlayTaskConsumer(Queue<?> queue, int playRate) {
        this.queue = queue;
        this.playRate = playRate;
    }

    public void start(int beginTimeNs) {
        //
    }

    public void stop() {

    }

    @Override
    public void run() {

    }
}
