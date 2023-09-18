package ljc.tokenBucket;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 *  用redis做集群令牌桶限流
 */
public class RedisTokenBucket extends TokenBucket {

    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private static final String KEYNAME_TOKENBUCKET = "tokenBucket";

    private static final String VNAME_AVAILABLETOKENS = "availableTokens";

    private static final String VNAME_CAPACITY = "capacity";

    private static final String VNAME_LASTTICK = "lastTick";

    private static final String VNAME_QUANTITY = "quantity";

    private static final String VNAME_STARTTIME = "startTime";

    private static final String VNAME_INTERVAL = "interval";

    @Override
    public void putToken(long nowTick) {

        while (true) {
            List<Object> values = redisTemplate.opsForHash()
                    .multiGet(KEYNAME_TOKENBUCKET, List.of(VNAME_AVAILABLETOKENS, VNAME_CAPACITY, VNAME_LASTTICK, VNAME_QUANTITY));
            
            int availableTokens = Integer.parseInt(((String) values.get(0)));

            int capacity = Integer.parseInt(((String) values.get(1)));

            int lastTick = Integer.parseInt(((String) values.get(2)));

            int quantity = Integer.parseInt(((String) values.get(3)));

            if (availableTokens >= capacity || nowTick <= lastTick) {
                return;
            }

            int addTokens = (int) ((nowTick - lastTick) * quantity);
            int newAvailable = addTokens + availableTokens > capacity ? capacity : addTokens + availableTokens;

            /*
                更新availableTokens前要判断availableTokens是否发生变更，若发生变更则更新失败，否则则进行更新，这里用lua来控制这
                两步操作的原子性，顺带把lastTick更新了。
                lua script:
                    -- ARGV[1]是传过来的availableTokens, ARGV[1]是传过来的newAvailable, ARGV[2]是nowTicks
                    if redis.call('hget', 'tokenBucket', 'availableTokens') != ARGV[1]
                    then
                        return -1
                    end
                    redis.call('hset', 'tokenBucket', 'availableTokens', ARGV[2])
                    redis.call('hset', 'tokenBucket', 'lastTick', ARGV[3])
                    return 0
            * */
            String lua = "if redis.call('hget', 'tokenBucket', 'availableTokens') != ARGV[1] then return -1 end redis.call('hset', 'tokenBucket', 'availableTokens', ARGV[2]) redis.call('hset', 'tokenBucket', 'lastTick', ARGV[3]) return 0";
            Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                    List.of(KEYNAME_TOKENBUCKET), availableTokens, newAvailable, nowTick);
            if (Long.valueOf(0).equals(result)) {
                return;
            }
        }
    }

    @Override
    public boolean flowControl(int need, long millsOfReq) {
        if (need <= 0) {
            return true;
        }

        // 先放token
        putToken(getNowTick(millsOfReq));

        /*
            “判断当前的令牌是否能满足当前请求”后，才能去扣减令牌数，这两部应该是原子的，所以用lua
            lua script:
                -- ARGV[1]是传过来的need
                if redis.call('hget', 'tokenBucket', 'availableTokens') >= ARGV[1]
                then
                    redis.call('hincrby', 'tokenBucket', 'availableTokens', -ARGV[1])
                    return 0
                end
                return -1
        * */
        String lua = "if redis.call('hget', 'tokenBucket', 'availableTokens') >= ARGV[1] then redis.call('hincrby', 'tokenBucket', 'availableTokens', -ARGV[1]) return 0 end return -1";
        Long result = redisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                List.of(KEYNAME_TOKENBUCKET), need);
        return Long.valueOf(0).equals(result);
    }

    protected long getStartTime() {
        String startTimeStr = (String) redisTemplate.opsForHash().get(KEYNAME_TOKENBUCKET, VNAME_STARTTIME);
        return Long.parseLong(startTimeStr);
    }

    protected int getInterval() {
        String intervalStr = (String) redisTemplate.opsForHash().get(KEYNAME_TOKENBUCKET, VNAME_INTERVAL);
        return Integer.parseInt(intervalStr);
    }
}
