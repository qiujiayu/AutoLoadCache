package com.jarvis.cache.compress;

import java.io.ByteArrayInputStream;

/**
 * @author: jiayu.qiu
 */
public interface ICompressor {

    /**
     * 压缩
     * 
     * @param bais ByteArrayInputStream
     * @return 压缩后数据
     * @throws Exception 异常
     */
    byte[] compress(ByteArrayInputStream bais) throws Exception;

    /**
     * 解压
     * 
     * @param bais ByteArrayInputStream
     * @return 解压后数据
     * @throws Exception 异常
     */
    byte[] decompress(ByteArrayInputStream bais) throws Exception;
}
