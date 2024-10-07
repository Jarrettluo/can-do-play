package com.jiaruiblog;

import java.util.Queue;
import java.util.concurrent.DelayQueue;

/**
 * <p></p>
 * edit at 2024/10/6 10:33
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class CanPlayer {

    // 是否初始化标志位
    private boolean isInitialized = false;

    // 是否停止标志位
    private final boolean isStopped = true;

    // 用户存储播放任务的共享缓冲区
    private Queue<?> taskBuffer;

    // 构造方法，初始化相关成员，创建对应的消费者和生产者
    public CanPlayer(boolean isInitialized) {
        this.isInitialized = isInitialized;

        PlayTaskConsumer playTaskConsumer = new PlayTaskConsumer(new DelayQueue<>(), 20);
        new PlayTaskProducer(new DelayQueue<>(), new PlayParam(), "", false);

    }

    // 是否初始化的判断；如果没有则调用生产者的初始化方法
    public boolean init() {
        return isInitialized;
    }

    // 从标准输入读取一个字符，用于捕获用户输入（例如，暂停、继续播放）
    public void Getch() {
        // todo 获取char的方法，功能未知
    }

    // 播放器的主要播放逻辑，检查播放器状态并启动生产者和消费者。
    // 循环检查播放进度、控制暂停和继续，并处理播放完成的情况。
    public boolean threadFuncPlayNohup() {
        return false;
        // todo功能未知
    }

    // 处理终端输入，允许用户通过按键控制播放状态，比如按s进行单步播放，按空格键暂停或继续播放。
    public void threadFuncTerm() {
        // 功能未知
    }

    public void nohupPlayRecord(){
        //
    }

    // 预加载
    //方法用于预加载播放记录。它接受当前播放进度和暂停状态作为参数。具体功能包括：
    // 如果已预加载，重置播放进度并更新暂停状态。
    // 用 producer_ 的 FillPlayTaskBuffer() 方法，将任务加载到缓冲区中。
    // 如果进度为0，重置为初始状态；否则，保持之前的状态。
    public boolean preloadPlayRecord() {
        return false;
    }

    // 开始; 启动播放过程，处理预加载和延迟，控制播放的持续时间，并不断输出当前播放进度。
    public boolean start() {
        return false;
    }

    // 停止和重置播放器，确保所有相关的生产者和消费者线程安全地停止。
    public boolean stop() {
        return false;
    }

    public boolean reset() {
        return false;
    }

    public static void main(String[] args) {

    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }
}
