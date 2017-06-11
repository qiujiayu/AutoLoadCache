## Annotation

### [@Cache](../src/main/java/com/jarvis/cache/annotation/Cache.java "@Cache")

    

### [@ExCache](../src/main/java/com/jarvis/cache/annotation/ExCache.java "@ExCache")

  使用场景举例：如果系统中用getUserById和getUserByName,两种方法来获取用户信息，我们可以在getUserById 时把 getUserByName 的缓存也生成。反过来getUserByName 时，也可以把getUserById 的缓存生成：

    @Cache(expire=600, key="'USER.getUserById'+#args[0]", exCache={@ExCache(expire=600, key="'USER.getUserByName'+#retVal.name")})
    public User getUserById(Long id){... ...}
    
    @Cache(expire=600, key="'USER.getUserByName'+#args[0]", exCache={@ExCache(expire=600, key="'USER.getUserById'+#retVal.id")})
    public User getUserByName(Long id){... ...}
    
    
  

### [@CacheDelete](../src/main/java/com/jarvis/cache/annotation/CacheDelete.java "@CacheDelete") 删除缓存注解

### [@CacheDeleteKey](../src/main/java/com/jarvis/cache/annotation/CacheDeleteKey.java "@CacheDeleteKey") 生成删除缓存Key注解

### [@CacheDeleteTransactional](../src/main/java/com/jarvis/cache/annotation/CacheDeleteTransactional.java "@CacheDeleteTransactional") 事务环境中批量删除缓存注解
    
### [@LocalCache](../src/main/java/com/jarvis/cache/annotation/LocalCache.java "@LocalCache") 本地缓存注解