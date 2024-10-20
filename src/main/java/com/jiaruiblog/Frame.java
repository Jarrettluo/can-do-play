package com.jiaruiblog;

/**
 * <p>每一帧数据信息，必须包含us的时间戳</p>
 * edit at 2024/10/6 10:32
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class Frame {

    // us时间戳
    private Long timestamp;

    private Integer num;

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "timestamp=" + timestamp +
                ", num=" + num +
                '}';
    }
}
