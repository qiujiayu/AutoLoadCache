package com.jarvis.cache.compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

/**
 * @author: jiayu.qiu
 */
public class CommonsCompressor implements ICompressor {

    private static final int BUFFER = 1024;

    private static final CompressorStreamFactory FACTORY = new CompressorStreamFactory();

    private String name;

    public CommonsCompressor(String name) {
        this.name = name;
    }

    @Override
    public byte[] compress(ByteArrayInputStream bais) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CompressorOutputStream cos = FACTORY.createCompressorOutputStream(name, baos);
        int len;
        byte buf[] = new byte[BUFFER];
        while ((len = bais.read(buf, 0, BUFFER)) != -1) {
            cos.write(buf, 0, len);
        }
        cos.flush();
        cos.close();
        byte[] output = baos.toByteArray();
        baos.flush();
        baos.close();
        bais.close();
        return output;
    }

    @Override
    public byte[] decompress(ByteArrayInputStream bais) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CompressorInputStream cis = FACTORY.createCompressorInputStream(name, bais);
        int len;
        byte buf[] = new byte[BUFFER];
        while ((len = cis.read(buf, 0, BUFFER)) != -1) {
            baos.write(buf, 0, len);
        }
        cis.close();

        byte[] output = baos.toByteArray();
        baos.flush();
        baos.close();
        bais.close();
        return output;
    }

}
