package com.test;

import com.jarvis.cache.CacheUpdateHandler;

import java.util.ArrayList;

/**
 * 类注释/描述
 *
 * @author hailin0@yeah.net
 * @date 2017-3-24 0:13
 */
public class CacheUpdateHandlerTest {
    public static void main(String[] args) {

        //调用示例
        CacheUpdateHandler
                .mark(CacheUpdateHandler.Operation.CLEAR)
                .batchs(
                        String.valueOf(1),//模拟调用有返回值的缓存方法
                        String.valueOf(2),//模拟调用有返回值的缓存方法
                        new CacheUpdateHandler.CacheBatchObj() {//模拟调用无返回值的缓存方法
                            public void exe() {
                                //模拟无返回值的缓存方法
                                new ArrayList<Object>().clear();
                                /*try {
                                    System.out.println(1);
                                    Thread.currentThread().sleep(5000l);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }*/
                            }
                        });


        //调用示例
        CacheUpdateHandler
                .mark(CacheUpdateHandler.Operation.UPDATE)
                .batchs(
                        String.valueOf(1),//模拟调用有返回值的缓存方法
                        String.valueOf(2),//模拟调用有返回值的缓存方法
                        new CacheUpdateHandler.CacheBatchObj() {//模拟调用无返回值的缓存方法
                            public void exe() {
                                //模拟无返回值的缓存方法
                                new ArrayList<Object>().clear();
                                //System.out.println(2);
                            }
                        });
    }
}
