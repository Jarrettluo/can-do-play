
<h2><center>CAN-do-player</center></h2>

## 背景
### 1 功能概述
用于对散点的一系列数据进行播放，支持按照确定的时间间隔进行播放数据。

### 2 功能用途
使用采集工具采集一系列时间序列数据，数据需要按照真实的时间间隔进行回放。例如ROS系统采集的ROSbag数据包，例如CANoe采集的blf等数据。
配合可视化展示，数据流处理等工具，可以对数据进行展示或者处理。

## 使用方法
```java
// 使用示例
public static void main(String[] args) throws Exception {
    // 创建测试数据源（内存映射文件）
    // 注意时间戳都是us
    List<TimestampedData<String>> data = Arrays.asList(
            new TimestampedData<>(1_000_000L, "Event1"),
            new TimestampedData<>(3_000_000L, "Event2"),
            new TimestampedData<>(6_000_000L, "Event3"),
            new TimestampedData<>(9_000_000L, "Event4"),
            new TimestampedData<>(11_000_000L, "Event5"),
            new TimestampedData<>(20_000_000L, "Event6")
    );
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    // 创建播放器
    EnhancedTimestampPlayer<String> player = new EnhancedTimestampPlayer<>(
            data,
            event -> {
                System.out.printf("[%dµs] %s%n", System.nanoTime() / 1000, event);
                System.out.println("当前时间：" + sdf.format(new Date(System.currentTimeMillis())));
            },
            1024
    );

    player.setSpeed(0.5);
    player.play();

    // 运行演示
    TimeUnit.SECONDS.sleep(25);
    player.close();
}
```

输出结果
```shell
[77359625162µs] Event1
当前时间：2025-04-06 11:27:20.000223
[77363622187µs] Event2
当前时间：2025-04-06 11:27:24.000218
[77369624022µs] Event3
当前时间：2025-04-06 11:27:30.000220
[77375623595µs] Event4
当前时间：2025-04-06 11:27:36.000219
[77379623361µs] Event5
当前时间：2025-04-06 11:27:40.000219
```


## 方案设计

在系统中最关键的部分是对时间进行判断。

```java
    // 核心调度逻辑
    private void scheduleNext() {
        if (!isPlaying) return;

        long currentMicros = nanoTime() / 1000;
        TimestampedData<T> next = playQueue.peek();

        if (next != null) {
            long expectedTime = baseTimestamp + (long) ((next.getTimestamp()) / speedFactor);
            long delayMicros = expectedTime - currentMicros - timeDrift.get();
            // 动态时间补偿
            // 统一处理正负偏差，确保补偿步长不超过预设的 COMPENSATION_STEP 绝对值
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
```
对每一个散点时间戳进行判断，判断到与程序的时间差异，进行时间偏移。

通过递归的方式将所有散点按照时间间隔输出出来。

![image](https://github.com/Jarrettluo/can-do-play/blob/master/images/示意图.jpg)


参考自apollo对record数据的播放
> https://github.com/moyituo/apollo/blob/master/cyber/tools/cyber_recorder/player/play_task_producer.cc
