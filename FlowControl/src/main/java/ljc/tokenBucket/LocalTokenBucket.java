package ljc.tokenBucket;

import java.util.concurrent.atomic.AtomicInteger;

public class LocalTokenBucket extends TokenBucket {

    /**
     * 速率怎么定，还有放token的策略是什么，比如确定的速率是5w个token/s，那是每隔1s一次性放5w，还是每隔0.1s放5000？
     * 为了能够灵活选择放token的策略，就定义2个量，一个是放token的时间周期，另一个是一次放多少
     */
    // 放token的时间周期，单位为ms
    protected int interval;

    // 每个周期放的token的数量
    protected int quantity;

    /**
     * 自然还需要定义令牌桶的最大容量
     */
    protected int capacity;

    // 当前桶中存在的token数
    protected final AtomicInteger availableTokens = new AtomicInteger();

    // 上一次放入token时是第几个周期，用long类型可以保证不会溢出
    protected long lastTick;

    // 令牌桶开始工作时的时间(毫秒)
    protected final long startTime = System.currentTimeMillis();

    public LocalTokenBucket(int interval, int quantity, int capacity) {
        this.interval = interval;
        this.quantity = quantity;
        this.capacity = capacity;
        availableTokens.set(capacity);
    }

    /**
     * 关于放token，可以有两种方案：
     * 1、如大家一般理解的直观的方案，即真的有一个主动放token的动作。可以新开个线程，周期性地轮询(周期就是上面定义的tick)，若桶未满则放入
     * min{quantity + availableTokens, capacity}个token。
     * 2、惰性放入方案，只要在请求到来需要获取token时往里放就行，即不需要专门往桶里放token，只需要在请求到来时，计算当前已经过了多少个tick，
     * 然后更新token的数量为min{((long) (nowTick - lastTick)) * quantity, capacity}
     *
     * 这里选择用第二种方案
     *
     * 这个方法要做原子性控制，因为请求是并发打过来的。这里用的是无锁方案
     * @param nowTick 当前是第几个周期
     */
    @Override
    public void putToken(long nowTick) {
        // 若当前可用token已达上限；或者若nowTick不大于lastTick，则表明已经有别的请求放过token了,直接退出循环就好
        while (availableTokens.get() < capacity && nowTick > lastTick) {
            int addTokens = (int) ((nowTick - lastTick) * quantity);    // 本次tick应该添加的token数
            int oldAvailable = availableTokens.get();
            int newAvailable = addTokens + oldAvailable > capacity ? capacity : addTokens + oldAvailable;
            if (availableTokens.compareAndSet(oldAvailable, newAvailable)) {
                lastTick = nowTick; // 只有修改成功的才能改lastTick
                System.out.println("putToken");
                break;
            }
        }
    }

    /**
     * 限流接口
     * @param need 本次请求所需的资源量，因为不同的业务场景可能会请求多个token，不一定是1
     * @param millsOfReq 客户端的时间
     * @return true为未触发限流，false为触发了限流
     */
    @Override
    public boolean flowControl(int need, long millsOfReq) {
        if (need <= 0) {
            return true;
        }

        // 先放token，这段逻辑是配合惰性放token的方案，若是主动周期性地放token，则不需要这段逻辑
        putToken(getNowTick(millsOfReq));

        // 退出该循环有2个情况，1是已经分配成功，这种情况返回true；2是当前可用的token数小于need，这种情况返回false
        int available;
        while ((available = availableTokens.get()) >= need) {
            if (availableTokens.compareAndSet(available, available - need)) {
                System.out.println("success=" + availableTokens.get());
                return true;
            }
        }
        System.out.println("fail=" + available);
        return false;
    }

    protected long getStartTime() {
        return startTime;
    }

    protected int getInterval() {
        return interval;
    }
}
