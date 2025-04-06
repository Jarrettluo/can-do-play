package com.jiaruiblog.player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class PrecisionTimestampPlayer<T> implements AutoCloseable {
    // 时间单位统一使用纳秒（1毫秒=1,000,000纳秒）
    private static final long NANOS_PER_MICRO = 1_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    // 事件数据结构
    public static class TimestampedData<T> {
        private final long timestamp; // 微秒时间戳
        private final T data;

        public TimestampedData(long timestamp, T data) {
            this.timestamp = timestamp;
            this.data = data;
        }
        // 构造函数/getters


        public long getTimestamp() {
            return timestamp;
        }

        public T getData() {
            return data;
        }
    }

    // 核心组件
    private final PriorityBlockingQueue<TimestampedData<T>> eventQueue;
    private final ScheduledExecutorService scheduler;
    private final Consumer<T> dataHandler;
    private final Thread preloadThread;
    private volatile boolean isRunning = false;
    private volatile long startNanos; // 纳秒基准时间
    private volatile double speedFactor = 1.0;
    private final AtomicLong scheduleOffset = new AtomicLong();

    // 构造函数
    public PrecisionTimestampPlayer(List<TimestampedData<T>> initialData,
                                    Consumer<T> dataHandler,
                                    int bufferSize) {
        this.eventQueue = new PriorityBlockingQueue<>(bufferSize, 
            Comparator.comparingLong(TimestampedData::getTimestamp));
        this.dataHandler = dataHandler;
        
        // 初始化调度器（使用单线程高精度调度）
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Scheduler-Thread");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });

        // 数据预加载线程
        this.preloadThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    // 此处可扩展为从外部源持续加载数据
                    if (eventQueue.size() < bufferSize && !initialData.isEmpty()) {
                        TimestampedData<T> data = initialData.remove(0);
                        eventQueue.put(data);
                    }
                    Thread.sleep(1); // 避免CPU空转
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        preloadThread.setDaemon(true);
    }

    // 核心调度方法（递归触发）
    private void scheduleNext() {
        if (!isRunning || eventQueue.isEmpty()) return;

        final long nowNanos = System.nanoTime();
        final TimestampedData<T> nextEvent = eventQueue.peek();

        // 计算理论触发时间
        final long eventMicros = nextEvent.getTimestamp();
        final long baseMicros = startNanos / NANOS_PER_MICRO;
        final long relativeMicros = (long)((eventMicros - baseMicros) / speedFactor);
        final long targetNanos = startNanos + relativeMicros * NANOS_PER_MICRO;

        // 计算需要等待的时间差
        long delayNanos = targetNanos - nowNanos - scheduleOffset.get();

        // 动态时间补偿（当偏差超过1ms时进行补偿）
        if (Math.abs(delayNanos) > NANOS_PER_MILLI) {
            long compensation = delayNanos / 10; // 渐进式补偿
            scheduleOffset.addAndGet(compensation);
            delayNanos -= compensation;
        }

        if (delayNanos <= 0) {
            // 立即触发
            triggerEvent();
            scheduleNext();
        } else {
            // 精确调度
            scheduler.schedule(() -> {
                triggerEvent();
                scheduleNext();
            }, delayNanos, TimeUnit.NANOSECONDS);
        }
    }

    private void triggerEvent() {
        TimestampedData<T> event = eventQueue.poll();
        if (event != null) {
            long actualNanos = System.nanoTime();
            long expectedNanos = startNanos + (long)((event.getTimestamp() - startNanos/NANOS_PER_MICRO) / speedFactor * NANOS_PER_MICRO);
            long drift = actualNanos - expectedNanos;
            scheduleOffset.addAndGet(drift); // 记录时间漂移

            dataHandler.accept(event.getData());
        }
    }

    // 播放控制
    public synchronized void play() {
        if (isRunning) return;
        isRunning = true;
        startNanos = System.nanoTime();
        preloadThread.start();
        scheduler.execute(this::scheduleNext);
    }

    public synchronized void pause() {
        isRunning = false;
        scheduler.shutdownNow();
        preloadThread.interrupt();
    }

    public void setSpeed(double speed) {
        this.speedFactor = Math.max(0.1, Math.min(10.0, speed));
    }

    @Override
    public void close() {
        pause();
        try {
            if (!scheduler.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
            preloadThread.join(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 测试用例
    public static void main(String[] args) throws Exception {
        List<TimestampedData<String>> data = new ArrayList<>();
        long baseMicros = System.nanoTime() / NANOS_PER_MICRO;
        
        // 创建测试数据（间隔1秒）
        for (int i = 0; i < 50; i++) {
            data.add(new TimestampedData<>(baseMicros + i * 1_000_000, "Event-" + (i+1)));
        }

        PrecisionTimestampPlayer<String> player = new PrecisionTimestampPlayer<>(
            new ArrayList<>(data), 
            event -> {
                long currentMicros = System.nanoTime() / NANOS_PER_MICRO;
                System.out.printf("计划时间：%d | 实际时间：%d | 事件：%s%n",
                    data.get(0).getTimestamp(), currentMicros, event);
                data.remove(0);
            },
            10
        );

        player.play();
        TimeUnit.SECONDS.sleep(6);
        player.close();
    }
}