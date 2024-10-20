package com.jiaruiblog;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * <p></p>
 * edit at 2024/10/20 22:22
 *
 * @author Jarrett Luo
 * @version 1.0
 */
public class MyProducer extends PlayTaskProducer {

    public MyProducer(LinkedBlockingDeque<Integer> frameBuffer, PlayParam playParam, String node, boolean preloadFillBufferMode, String name) {
        super(frameBuffer, playParam, node, preloadFillBufferMode, name);
    }

    @Override
    public void run() {
        super.run();

    }
}
