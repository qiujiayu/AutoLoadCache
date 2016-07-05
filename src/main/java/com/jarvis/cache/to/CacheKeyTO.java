package com.jarvis.cache.to;

import java.io.Serializable;

/**
 * 缓存Key
 * @author jiayu.qiu
 */
public final class CacheKeyTO implements Serializable {

    private static final long serialVersionUID=7229320497442357252L;

    private final String namespace;

    private final String key;// 缓存Key

    private final String hfield;// 设置哈希表中的字段，如果设置此项，则用哈希表进行存储

    public CacheKeyTO(String namespace, String key, String hfield) {
        this.namespace=namespace;
        this.key=key;
        this.hfield=hfield;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public String getHfield() {
        return hfield;
    }

    public String getCacheKey() {
        if(null != this.namespace && this.namespace.length() > 0) {
            return this.namespace + ":" + this.key;
        }
        return this.key;
    }

    @Override
    public int hashCode() {
        final int prime=31;
        int result=1;
        result=prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result=prime * result + ((key == null) ? 0 : key.hashCode());
        result=prime * result + ((hfield == null) ? 0 : hfield.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        CacheKeyTO other=(CacheKeyTO)obj;
        if(hfield == null) {
            if(other.hfield != null)
                return false;
        } else if(!hfield.equals(other.hfield))
            return false;
        if(key == null) {
            if(other.key != null)
                return false;
        } else if(!key.equals(other.key))
            return false;
        if(namespace == null) {
            if(other.namespace != null)
                return false;
        } else if(!namespace.equals(other.namespace))
            return false;
        return true;
    }

}
