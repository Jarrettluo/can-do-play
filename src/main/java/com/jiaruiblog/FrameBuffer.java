package com.jiaruiblog;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * <p>帧数据的buffer</p>
 * edit at 2024/10/6 10:32
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class FrameBuffer{

    Queue<Frame> queue = new ArrayDeque<Frame>();

    private int num;

    // 定义缓存的状态
    private boolean state = true;

    public synchronized void put(int num) {
        Frame frame = new Frame();
        frame.setTimestamp(System.currentTimeMillis());
        queue.offer(frame);
    }

    public synchronized Frame get() {
        if (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return queue.remove();
    }
}

