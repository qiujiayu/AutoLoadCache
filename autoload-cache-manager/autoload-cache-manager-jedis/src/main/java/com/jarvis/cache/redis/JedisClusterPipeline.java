package com.jarvis.cache.redis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.Response;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

/**
 * 在集群模式下提供批量操作的功能。由于集群模式存在节点的动态添加删除，且client不能实时感知，所以需要有重试功能
 * <p>
 * 该类非线程安全
 * <p>
 *
 *
 */
@Getter
@Slf4j
public class JedisClusterPipeline extends PipelineBase implements Closeable {

    private final JedisClusterInfoCache clusterInfoCache;

    /**
     * 根据顺序存储每个命令对应的Client
     */
    private final Queue<Client> clients;
    /**
     * 用于缓存连接
     */
    private final Map<JedisPool, Jedis> jedisMap;

    public JedisClusterPipeline(JedisClusterInfoCache clusterInfoCache) {
        this.clusterInfoCache = clusterInfoCache;
        this.clients = new LinkedList<>();
        this.jedisMap = new HashMap<>(3);
    }

    /**
     * 同步读取所有数据. 与syncAndReturnAll()相比，sync()只是没有对数据做反序列化
     */
    protected void sync() {
        innerSync(null);
    }

    /**
     * 同步读取所有数据 并按命令顺序返回一个列表
     *
     * @return 按照命令的顺序返回所有的数据
     */
    protected List<Object> syncAndReturnAll() {
        List<Object> responseList = new ArrayList<>(clients.size());
        innerSync(responseList);
        return responseList;
    }

    private void innerSync(List<Object> formatted) {
        try {
            Response<?> response;
            Object data;
            for (Client client : clients) {
                response = generateResponse(client.getOne());
                if (null != formatted) {
                    data = response.get();
                    formatted.add(data);
                }
            }
        } catch (JedisRedirectionException jre) {
            throw jre;
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        clean();
        clients.clear();
        for (Jedis jedis : jedisMap.values()) {
            flushCachedData(jedis);
            jedis.close();
        }
        jedisMap.clear();
    }

    private void flushCachedData(Jedis jedis) {
        try {
            //FIXME 这个count怎么取值? 执行命令的个数??
            jedis.getClient().getMany(jedisMap.size());
            //jedis.getClient().getAll();
        } catch (RuntimeException ex) {
            // 其中一个client出问题，后面出问题的几率较大
        }
    }

    @Override
    protected Client getClient(String key) {
        byte[] bKey = SafeEncoder.encode(key);
        return getClient(bKey);
    }

    @Override
    protected Client getClient(byte[] key) {
        Client client = getClient(JedisClusterCRC16.getSlot(key));
        clients.add(client);
        return client;
    }

    private Client getClient(int slot) {
        JedisPool pool = clusterInfoCache.getSlotPool(slot);
        // 根据pool从缓存中获取Jedis
        Jedis jedis = jedisMap.get(pool);
        if (null == jedis) {
            jedis = pool.getResource();
            jedisMap.put(pool, jedis);
        }
        return jedis.getClient();
    }
}
