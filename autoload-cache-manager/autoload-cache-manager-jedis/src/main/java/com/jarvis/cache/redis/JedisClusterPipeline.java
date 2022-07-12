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
import redis.clients.jedis.*;
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
public class JedisClusterPipeline extends Pipeline implements Closeable {

    private final JedisClusterInfoCache clusterInfoCache;

    /**
     * 根据顺序存储每个命令对应的Client
     */
    private final Queue<Connection> clients;
    /**
     * 用于缓存连接
     */
    private final Map<ConnectionPool, Connection> jedisMap;

    public JedisClusterPipeline(JedisClusterInfoCache clusterInfoCache) {
        super((Connection) null);
        this.clusterInfoCache = clusterInfoCache;
        this.clients = new LinkedList<>();
        this.jedisMap = new HashMap<>(3);
    }

    /**
     * 同步读取所有数据. 与syncAndReturnAll()相比，sync()只是没有对数据做反序列化
     */
    @Override
    public void sync() {
        innerSync(null);
    }

    /**
     * 同步读取所有数据 并按命令顺序返回一个列表
     *
     * @return 按照命令的顺序返回所有的数据
     */
    @Override
    public List<Object> syncAndReturnAll() {
        List<Object> responseList = new ArrayList<>(clients.size());
        innerSync(responseList);
        return responseList;
    }

    private void innerSync(List<Object> formatted) {
        try {
            Response<?> response;
            Object data;
            for (Connection connection : clients) {
                response = generateResponse(connection.getOne());
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
        for (Connection connection : jedisMap.values()) {
            flushCachedData(connection);
            connection.close();
        }
        jedisMap.clear();
    }

    private void flushCachedData(Connection connection) {
        try {
            connection.getMany(jedisMap.size());
            //jedis.getClient().getAll();
        } catch (RuntimeException ex) {
            // 其中一个client出问题，后面出问题的几率较大
        }
    }

    protected Connection getClient(String key) {
        byte[] bKey = SafeEncoder.encode(key);
        return getClient(bKey);
    }

    protected Connection getClient(byte[] key) {
        Connection connection = getClient(JedisClusterCRC16.getSlot(key));
        clients.add(connection);
        return connection;
    }

    private Connection getClient(int slot) {
        ConnectionPool pool = clusterInfoCache.getSlotPool(slot);
        // 根据pool从缓存中获取Jedis
        Connection connection = jedisMap.get(pool);
        if (null == connection) {
            jedisMap.put(pool, pool.getResource());
        }
        return connection;
    }
}
