package com.jarvis.cache.demo.test;

import com.jarvis.cache.demo.CacheDemoApplication;
import com.jarvis.cache.redis.JedisClusterPipeline;
import com.jarvis.cache.redis.RetryableJedisClusterPipeline;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.JedisCluster;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CacheDemoApplication.class)
public class JedisClusterTest {

    @Autowired
    private RedisConnectionFactory connectionFactory;


    private JedisCluster getJedisCluster() {
        if (null == connectionFactory) {
            return null;
        }
        if (!(connectionFactory instanceof JedisConnectionFactory)) {
            log.debug("connectionFactory is not JedisConnectionFactory");
            return null;
        }

        RedisConnection redisConnection = null;
        try {
            redisConnection = connectionFactory.getConnection();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        if (redisConnection instanceof JedisClusterConnection) {
            JedisClusterConnection redisClusterConnection = (JedisClusterConnection) redisConnection;
            // 优先使用JedisCluster; 因为JedisClusterConnection 不支持eval、evalSha等方法需要使用JedisCluster
            JedisCluster jedisCluster = redisClusterConnection.getNativeConnection();
            return jedisCluster;
        }
        return null;
    }

    @Test
    public void testJedisCluster() {
        JedisCluster jedisCluster = getJedisCluster();
        if (null == jedisCluster) {
            return;
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            jedisCluster.hset("htest", "tttt", "aaaa");
            jedisCluster.expire("htest", 100);
        }
        System.out.println("testJedisCluster use time :" + (System.currentTimeMillis() - start));
        System.out.println("testJedisCluster htest ttl :" + jedisCluster.ttl("htest"));
    }

    @Test
    public void testJedisCluster2() throws Exception {
        JedisCluster jedisCluster = getJedisCluster();
        if (null == jedisCluster) {
            return;
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            RetryableJedisClusterPipeline retryableJedisClusterPipeline = new RetryableJedisClusterPipeline(jedisCluster) {
                @Override
                public void execute(JedisClusterPipeline pipeline) {
                    pipeline.hset("htest2", "tttt", "aaaa");
                    pipeline.expire("htest2", 100);
                }
            };
            retryableJedisClusterPipeline.sync();
        }
        System.out.println("testJedisCluster2 use time :" + (System.currentTimeMillis() - start));
        System.out.println("testJedisCluster2 htest2 ttl :" + jedisCluster.ttl("htest2"));
    }
}
