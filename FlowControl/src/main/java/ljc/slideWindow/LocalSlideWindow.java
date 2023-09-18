package ljc.slideWindow;

import ljc.util.LoopQueue;

/**
 * 单机滑动时间窗口限流算法
 */
public class LocalSlideWindow extends SlideWindow {
    // 时间窗口的当前开始时间(毫秒)
    protected long startTime;

    // 时间窗口的时间长度(毫秒)
    long timeSize;

    // 窗口总容量
    int windowCapacity;

    // 用循环数组做时间窗口
    LoopQueue<Long> window = new LoopQueue<>(windowCapacity);

    public LocalSlideWindow(long startTime, long timeSize, int windowCapacity) {
        this.startTime = startTime;
        this.timeSize = timeSize;
        this.windowCapacity = windowCapacity;
    }

    /**
     * 分3种情况：
     * 1、若当前请求的时间点在时间窗口内，则判断时间窗口是否已满，若满了则返回失败，否则将当前时间点加入时间窗口内，并返回成功；
     * 2、若当前请求的时间点早于时间窗口的最早时间点，则直接丢弃；
     * 3、若当前请求的时间点晚于时间窗口最晚时间点(startTime + timeSize)，则将时间窗口往前滑动一个单位，然后再从头开始判断是哪种情况；
     * @param millsOfReq
     * @return
     */
    public boolean flowControl(long millsOfReq) {
        // 若在时间窗口内
        if (millsOfReq >= startTime && millsOfReq <= startTime + timeSize) {
            if (window.full()) {    // 检查当前窗口是否已满，满了直接返回失败
                return false;
            }

            window.add(millsOfReq);
            return true;
        }

        // 在时间窗口之前直接丢弃
        if (millsOfReq < startTime) {
            return false;
        }

        // 在时间窗口之后，则把时间窗口滑动一步，再进行检查
        window.removeFirst();
        startTime = window.getFirst();
        return flowControl(millsOfReq);
    }
}
