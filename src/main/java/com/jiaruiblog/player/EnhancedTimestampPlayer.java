package com.jiaruiblog.player;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

public class EnhancedTimestampPlayer<T> implements AutoCloseable {
    // 核心数据结构
    public static class TimestampedData<T> {
        private final long timestamp; // 微秒时间戳
        private final T data;

        public TimestampedData(long timestamp, T data) {
            this.timestamp = timestamp;
            this.data = data;
        }
        // Getters
        public long getTimestamp() { return timestamp; }
        public T getData() { return data; }
    }

    // 核心组件
    private final PriorityBlockingQueue<TimestampedData<T>> playQueue = new PriorityBlockingQueue<>(
            1024, Comparator.comparingLong(TimestampedData::getTimestamp)
    );
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                private final AtomicInteger count = new AtomicInteger();
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "PlayerThread-" + count.getAndIncrement());
                    t.setPriority(Thread.MAX_PRIORITY);
                    return t;
                }
            }
    );
    private final Consumer<T> dataHandler;
    private final DataPreloader preloader;
    private volatile boolean isPlaying = false;
    private volatile long baseTimestamp; // 微秒基准时间
    private volatile double speedFactor = 1.0;
    private final AtomicLong lastScheduledTime = new AtomicLong();
    private final AtomicLong timeDrift = new AtomicLong();

    // 时间补偿参数
    private static final long MAX_COMPENSATION = 1_000_000; // 1秒微秒数
    private static final long COMPENSATION_STEP = 10_000; // 10毫秒

    // 高精度时钟源
    private static long nanoTime() {
        return System.nanoTime(); // 1纳秒分辨率
    }

    // 数据预加载组件
    private class DataPreloader extends Thread {
        private final Iterator<TimestampedData<T>> source;
        private volatile boolean running = true;
        private final int bufferSize;

        DataPreloader(Iterator<TimestampedData<T>> source, int bufferSize) {
            this.source = source;
            this.bufferSize = bufferSize;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (running && !Thread.interrupted()) {
                synchronized (playQueue) {
                    while (playQueue.size() < bufferSize && source.hasNext()) {
                        TimestampedData<T> data = source.next();
                        playQueue.put(data);
                    }
                }
                try {
                    Thread.sleep(1); // 降低CPU占用
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        void shutdown() {
            running = false;
            interrupt();
        }
    }

    // 构造函数
    public EnhancedTimestampPlayer(Iterable<TimestampedData<T>> dataSource,
                                   Consumer<T> dataHandler,
                                   int bufferSize) {
        this.dataHandler = dataHandler;
        this.preloader = new DataPreloader(dataSource.iterator(), bufferSize);
        preloader.start();
    }

    // 播放控制
    public synchronized void play() {
        if (isPlaying) return;

        isPlaying = true;
        baseTimestamp = nanoTime() / 1000; // 转换为微秒
        scheduler.execute(this::scheduleNext);
    }

    public synchronized void pause() {
        isPlaying = false;
        scheduler.shutdownNow();
    }

    public void setSpeed(double speed) {
        this.speedFactor = Math.max(0.1, Math.min(10.0, speed));
    }

    // 核心调度逻辑
    private void scheduleNext() {
        if (!isPlaying) return;

        long currentMicros = nanoTime() / 1000;
        TimestampedData<T> next = playQueue.peek();

        if (next != null) {
            long expectedTime = baseTimestamp + (long)((next.getTimestamp() - baseTimestamp) / speedFactor);
            long delayMicros = expectedTime - currentMicros - timeDrift.get();

            // 动态时间补偿
            if (Math.abs(delayMicros) > COMPENSATION_STEP) {
                long compensation = Math.min(Math.max(-COMPENSATION_STEP, delayMicros), COMPENSATION_STEP);
                timeDrift.addAndGet(compensation);
                delayMicros -= compensation;
            }

            if (delayMicros <= 0) {
                // 立即触发
                handleData(playQueue.poll());
                scheduleNext();
            } else {
                // 高精度调度
                long delayNanos = delayMicros * 1000;
                lastScheduledTime.set(nanoTime());
                scheduler.schedule(this::handleAndSchedule, delayNanos, TimeUnit.NANOSECONDS);
            }
        }
    }

    private void handleAndSchedule() {
        handleData(playQueue.poll());
        scheduleNext();
    }

    private void handleData(TimestampedData<T> data) {
        if (data != null) {
            long actualTime = nanoTime() / 1000;
            long expectedTime = baseTimestamp + (long)((data.getTimestamp() - baseTimestamp) / speedFactor);
            timeDrift.addAndGet(actualTime - expectedTime);

            dataHandler.accept(data.getData());
        }
    }

    // 高级控制
    public void seekTo(long timestampMicros) {
        pause();
        playQueue.clear();
        baseTimestamp = nanoTime() / 1000 - (long)(timestampMicros / speedFactor);
        preloader.interrupt(); // 触发重新预加载
        play();
    }

    // 性能优化：内存映射文件支持
    public static <T> Iterable<TimestampedData<T>> createMappedFileSource(Path filePath,
                                                                          Function<ByteBuffer, T> parser) {
        return () -> {
            try {
                return new MappedFileIterator<>(filePath, parser);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static class MappedFileIterator<T> implements Iterator<TimestampedData<T>> {
        private final FileChannel channel;
        private final ByteBuffer buffer;
        private final Function<ByteBuffer, T> parser;
        private long position;

        MappedFileIterator(Path path, Function<ByteBuffer, T> parser) throws Exception {
            this.channel = FileChannel.open(path, StandardOpenOption.READ);
            this.buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            this.parser = parser;
        }

        public boolean hasNext() {
            return buffer.remaining() >= 16; // 假设时间戳 8字节+数据长度4字节+数据
        }

        public TimestampedData<T> next() {
            long timestamp = buffer.getLong();
            int length = buffer.getInt();
            byte[] dataBytes = new byte[length];
            buffer.get(dataBytes);
            T data = parser.apply(ByteBuffer.wrap(dataBytes));
            return new TimestampedData<>(timestamp, data);
        }
    }

    @Override
    public void close() {
        pause();
        preloader.shutdown();
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 使用示例
    public static void main(String[] args) throws Exception {
        // 创建测试数据源（内存映射文件）
        long baseMicros = System.nanoTime() / 1000;

        List<TimestampedData<String>> data = Arrays.asList(
                new TimestampedData<>(1_000_000L + baseMicros, "Event1"),
                new TimestampedData<>(3_000_000L + baseMicros, "Event2"),
                new TimestampedData<>(6_000_000L + baseMicros, "Event3"),
                new TimestampedData<>(9_000_000L + baseMicros, "Event4"),
                new TimestampedData<>(11_000_000L + baseMicros, "Event5"),
                new TimestampedData<>(20_000_000L + baseMicros, "Event36")
        );

        // 创建播放器
        EnhancedTimestampPlayer<String> player = new EnhancedTimestampPlayer<>(
                data,
                event -> {
                    System.out.printf("[%dµs] %s%n", System.nanoTime()/1000, event);
                    System.out.println("当前时间：" + System.currentTimeMillis());
                },
                1024
        );

        player.setSpeed(2.0);
        player.play();

        // 运行演示
        TimeUnit.SECONDS.sleep(25);
        player.close();
    }
}