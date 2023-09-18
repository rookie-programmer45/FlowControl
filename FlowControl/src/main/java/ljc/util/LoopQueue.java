package ljc.util;

import java.util.StringJoiner;

/**
 * 循环队列
 */
public class LoopQueue<E> {
    private int capacity;

    private int size;

    private Object[] elements;

    // inclusive
    private int startIdx;

    // exclusive
    private int endIdx;

    public LoopQueue(int capacity) {
        this.capacity = capacity;
        elements = new Object[capacity];
    }

    // 是否是空的
    public boolean empty() {
        return size == 0;
    }

    // 是否满了
    public boolean full() {
        return size == capacity;
    }

    public int size() {
        return size;
    }

    // 获取当前index的下一个index
    private int next(int idx) {
        idx += 1;
        return idx == capacity ? 0 : idx;
    }

    // 获取当前index的前一个index
    private int prev(int idx) {
        idx -= 1;
        return idx == -1 ? capacity - 1 : idx;
    }

    // 添加到队尾
    public boolean add(E e) {
        if (full()) {
            return false;
        }

        elements[endIdx] = e;
        endIdx = next(endIdx);
        size++;
        return true;
    }

    // 从队头移除
    public boolean removeFirst() {
        if (empty()) {
            return false;
        }

        startIdx = next(startIdx);
        size--;
        return true;
    }

    public E getFirst() {
        return (E) elements[startIdx];
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        if (empty()) {
            return sj.toString();
        }
        for (int i = 0; i < size; i++) {
            int idx = startIdx + i == capacity ? 0 : startIdx + i;
            sj.add(String.valueOf(elements[idx]));
        }
        return sj.toString();
    }
}
