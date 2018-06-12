package com.jarvis.cache.map;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jarvis.cache.serializer.HessianSerializer;
import com.jarvis.cache.serializer.ISerializer;
import com.jarvis.cache.to.CacheWrapper;
import com.jarvis.lib.util.OsUtil;

/**
 * @author: jiayu.qiu
 */
public class CacheTask implements Runnable, CacheChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(CacheTask.class);

    /**
     * 缓存被修改的个数
     */
    private AtomicInteger cacheChanged = new AtomicInteger(0);

    private MapCacheManager cacheManager;

    private volatile boolean running = false;

    private File saveFile;

    private ISerializer<Object> persistSerializer;

    public CacheTask(MapCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void start() {
        if (!this.running) {
            loadCache();
            this.running = true;
        }

    }

    public void destroy() {
        persistCache(true);
        this.running = false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.running = false;
    }

    private String getSavePath() {
        String persistFile = cacheManager.getPersistFile();
        if (null != persistFile && persistFile.trim().length() > 0) {
            return persistFile;
        }
        String path = "/tmp/autoload-cache/";
        String nsp = cacheManager.getAutoLoadConfig().getNamespace();
        if (null != nsp && nsp.trim().length() > 0) {
            path += nsp.trim() + "/";
        }
        if (OsUtil.getInstance().isLinux()) {
            return path;
        }
        return "C:" + path;
    }

    private File getSaveFile() {
        if (null != saveFile) {
            return saveFile;
        }
        String path = getSavePath();
        File savePath = new File(path);
        if (!savePath.exists()) {
            savePath.mkdirs();
        }
        saveFile = new File(path + "map.cache");
        return saveFile;
    }

    private ISerializer<Object> getPersistSerializer() {// 因为只有HessianSerializer才支持SoftReference序列化，所以只能使用HessianSerializer
        if (null == persistSerializer) {
            persistSerializer = new HessianSerializer();
        }
        return persistSerializer;
    }

    /**
     * 从磁盘中加载之前保存的缓存数据，避免刚启动时，因为没有缓存，而且造成压力过大
     */
    @SuppressWarnings("unchecked")
    public void loadCache() {
        if (!cacheManager.isNeedPersist()) {
            return;
        }
        File file = getSaveFile();
        if (null == file) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        BufferedInputStream bis = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte buf[] = new byte[1024];
            int len = -1;
            while ((len = bis.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            byte retArr[] = baos.toByteArray();
            Object obj = getPersistSerializer().deserialize(retArr, null);
            if (null != obj && obj instanceof ConcurrentHashMap) {
                cacheManager.getCache().putAll((ConcurrentHashMap<String, Object>) obj);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (null != bis) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void persistCache(boolean force) {
        if (!cacheManager.isNeedPersist()) {
            return;
        }
        int cnt = cacheChanged.intValue();
        if (!force && cnt <= cacheManager.getUnpersistMaxSize()) {
            return;
        }
        cacheChanged.set(0);
        FileOutputStream fos = null;
        try {
            byte[] data = getPersistSerializer().serialize(cacheManager.getCache());
            File file = getSaveFile();
            fos = new FileOutputStream(file);
            fos.write(data);
        } catch (Exception ex) {
            cacheChanged.addAndGet(cnt);
            logger.error(ex.getMessage(), ex);
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void run() {
        while (running) {
            try {
                cleanCache();
                persistCache(false);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(cacheManager.getClearAndPersistPeriod());
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 清除过期缓存
     */
    private void cleanCache() {
        Iterator<Entry<String, Object>> iterator = cacheManager.getCache().entrySet().iterator();
        int cacheChanged = 0;
        int i = 0;
        while (iterator.hasNext()) {
            cacheChanged += removeExpiredItem(iterator);
            i++;
            if (i == 2000) {
                i = 0;
                try {
                    Thread.sleep(0);// 触发操作系统立刻重新进行一次CPU竞争, 让其它线程获得CPU控制权的权力。
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (cacheChanged > 0) {
            cacheChange(cacheChanged);
        }
    }

    @SuppressWarnings("unchecked")
    private int removeExpiredItem(Iterator<Entry<String, Object>> iterator) {
        int cacheChanged = 0;
        Object value = iterator.next().getValue();
        if (value instanceof SoftReference) {
            SoftReference<CacheWrapper<Object>> reference = (SoftReference<CacheWrapper<Object>>) value;
            if (null != reference && null != reference.get()) {
                CacheWrapper<Object> tmp = reference.get();
                if (tmp.isExpired()) {
                    iterator.remove();
                    cacheChanged++;
                }
            } else {
                iterator.remove();
                cacheChanged++;
            }
        } else if (value instanceof ConcurrentHashMap) {
            ConcurrentHashMap<String, Object> hash = (ConcurrentHashMap<String, Object>) value;
            Iterator<Entry<String, Object>> iterator2 = hash.entrySet().iterator();
            while (iterator2.hasNext()) {
                Object tmpObj = iterator2.next().getValue();
                if (tmpObj instanceof SoftReference) {
                    SoftReference<CacheWrapper<Object>> reference = (SoftReference<CacheWrapper<Object>>) tmpObj;
                    if (null != reference && null != reference.get()) {
                        CacheWrapper<Object> tmp = reference.get();
                        if (tmp.isExpired()) {
                            iterator2.remove();
                            cacheChanged++;
                        }
                    } else {
                        iterator2.remove();
                        cacheChanged++;
                    }
                } else if (tmpObj instanceof CacheWrapper) {// 兼容老版本
                    CacheWrapper<Object> tmp = (CacheWrapper<Object>) tmpObj;
                    if (tmp.isExpired()) {
                        iterator2.remove();
                        cacheChanged++;
                    }
                }
            }
            if (hash.isEmpty()) {
                iterator.remove();
            }
        } else {
            CacheWrapper<Object> tmp = (CacheWrapper<Object>) value;
            if (tmp.isExpired()) {
                iterator.remove();
                cacheChanged++;
            }
        }
        return cacheChanged;
    }

    @Override
    public void cacheChange() {
        cacheChanged.incrementAndGet();
    }

    @Override
    public void cacheChange(int cnt) {
        cacheChanged.addAndGet(cnt);
    }

}
