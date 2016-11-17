package com.test;

import com.jarvis.cache.serializer.CompressorSerializer;
import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.to.CacheWrapper;

public class CompressorSerializerTest {

    public static void main(String args[]) throws Exception {
        HessianSerializer hs=new HessianSerializer();
        CompressorSerializer cs=new CompressorSerializer(hs, 100);
        CacheWrapper<String> wrapper=new CacheWrapper<String>();
        wrapper.setCacheObject("1111asdfadsfasdfasdfasdffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff中茜轼阿斯达克苛阿卡斯斯柯达卡罗拉中中国国际中叫苦连天或或，巴彦县呀，四海无闲田工烟圈卡里考虑中喵来看，轼功用fffffffffasdfsadfasdf");
        wrapper.setExpire(2100);
        wrapper.setLastLoadTime(System.currentTimeMillis());
        byte[] data=cs.serialize(wrapper);

        CacheWrapper<String> wrapper2=(CacheWrapper<String>)cs.deserialize(data, null);
        System.out.println(wrapper2.getCacheObject().equals(wrapper.getCacheObject()));
    }
}
