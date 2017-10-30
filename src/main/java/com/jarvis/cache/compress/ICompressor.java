package com.jarvis.cache.compress;

import java.io.ByteArrayInputStream;

/**
 * 
 * @author: jiayu.qiu
 */
public interface ICompressor {

    /**
     * 
     * 压缩
     * @param bais
     * @return
     * @throws Exception
     */
    byte[] compress(ByteArrayInputStream bais) throws Exception;

    /**
     * 
     * 解压
     * @param bais
     * @return
     * @throws Exception
     */
    byte[] decompress(ByteArrayInputStream bais) throws Exception;
}
