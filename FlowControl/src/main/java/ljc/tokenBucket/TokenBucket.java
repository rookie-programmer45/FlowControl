package ljc.tokenBucket;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 令牌桶抽象类，可实现为本地令牌桶限流算法，也可实现为分布式令牌桶限流算法
 */
public abstract class TokenBucket {
    /**
     * 惰性放token的方法
     * @param nowTick
     */
    protected void putToken(long nowTick) {
        throw new UnsupportedOperationException();
    }

    /**
     * 限流接口
     * @param need 本次请求所需的资源量，因为不同的业务场景可能会请求多个token，不一定是1
     * @param millsOfReq 客户端的时间
     * @return true为未触发限流，false为触发了限流
     */
    public boolean flowControl(int need, long millsOfReq) {
        throw new UnsupportedOperationException();
    }

    /**
     * 计算本次请求所属的tick，即从令牌桶工作开始，到现在是第几个周期
     * @param millsOfReq
     * @return
     */
    protected final long getNowTick(long millsOfReq) {
        return (millsOfReq - getStartTime()) / getInterval();
    }

    protected long getStartTime() {
        throw new UnsupportedOperationException();
    }

    protected int getInterval() {
        throw new UnsupportedOperationException();
    }
}
