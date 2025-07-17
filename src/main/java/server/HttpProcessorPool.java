package server;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 业务处理器池
 * 负责初始化processor池，创建processor，获取processor，归还processor
 */
public class HttpProcessorPool {
    private int minSize;
    private int maxSize;
    private int currentSize;

    /**
     * 使用接口而非具体实现，方便后续替换
     * 示例代码中用的ArrayDeque + synchronized ，直接用并发工具类是不是也行
     */
    private final Deque<HttpProcessor> processors = new ArrayDeque<>();

    public HttpProcessorPool(int minSize, int maxSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
    }

    /**
     * 主线程启动时执行初始化动作，线程安全
     */
    public void init() {
        for (int i = 0; i < minSize; i++) {
            // 示例代码中用的
            HttpProcessor processor = new HttpProcessor(this);
            processor.start();
            processors.add(processor);
        }
        currentSize = minSize;
    }

    /**
     * 从池中获取processor
     * synchronized 拿processor，高并发下性能堪忧呀
     */
    synchronized public HttpProcessor getProcessor() {
        HttpProcessor processor;
        if (processors.size() > 0) {
            processor = processors.poll();
        } else if (currentSize < maxSize) {
            // 临时创建，不丢到池中，仅做计数
            processor = new HttpProcessor(this);
            processor.start();
            currentSize ++;
        } else {
            processor = null;
        }
        return processor;
    }

    /**
     * 归还线程池
     * 高并发下性能也一般，但是先实现功能，保证线程安全
     */
    synchronized public void recycle(HttpProcessor httpProcessor) {
        processors.add(httpProcessor);
    }
}
