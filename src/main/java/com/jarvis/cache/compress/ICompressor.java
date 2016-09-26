package com.jarvis.cache.compress;

import java.io.ByteArrayInputStream;

public interface ICompressor {

    byte[] compress(ByteArrayInputStream bais) throws Exception;

    byte[] decompress(ByteArrayInputStream bais) throws Exception;
}
