package com.jarvis.cache.to;

import java.io.Serializable;

/**
 * 缓存Key
 * @author jiayu.qiu
 */
public class CacheKeyTO implements Serializable {

    private static final long serialVersionUID=7229320497442357252L;

    private String namespace;

    private String key;// 缓存Key

    private String hfield;// 设置哈希表中的字段，如果设置此项，则用哈希表进行存储

    private String fullKey;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace=namespace;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key=key;
    }

    public String getHfield() {
        return hfield;
    }

    public void setHfield(String hfield) {
        this.hfield=hfield;
    }

    public String getCacheKey() {
        if(null != this.namespace && this.namespace.length() > 0) {
            return this.namespace + ":" + this.key;
        }
        return this.key;
    }

    public String getFullKey() {
        if(null == fullKey) {
            StringBuilder b=new StringBuilder();
            if(null != this.namespace && this.namespace.length() > 0) {
                b.append(this.namespace).append(":");
            }
            b.append(this.key);
            if(null != this.hfield && this.hfield.length() > 0) {
                b.append(":").append(this.hfield);
            }
            fullKey=b.toString();
        }

        return fullKey;
    }

    @Override
    public int hashCode() {
        final int prime=31;
        int result=1;
        String _fullKey=getFullKey();
        result=prime * result + ((_fullKey == null) ? 0 : _fullKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        }
        if(obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CacheKeyTO other=(CacheKeyTO)obj;
        if(!isEquals(this.getFullKey(), other.getFullKey())) {
            return false;
        }
        return true;
    }

    private boolean isEquals(String a, String b) {
        if(a == null) {
            return b == null ? true : false;
        } else {
            return a.equals(b);
        }
    }

}
