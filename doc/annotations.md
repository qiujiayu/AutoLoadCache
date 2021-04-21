## Annotation

### [@Cache](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/Cache.java "@Cache")


```java

public interface UserMapper {
    
    /**
     * 根据用户id获取用户信息
     * 
     * @param id
     * @return
     */
    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60", key = "'user-byid-' + #args[0]")
    UserDO getUserById(Long id);

    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60",
            // 因为Magic 模式下会对参数进行分隔，所以取参数固定使用 #args[0]，
            // 如果参数据是复杂类型，比如List<UserDO>, 那么取参使用 #args[0].id
            // 为了降低缓存不致问题，些处生的有key值要与getUserById 方法的一样
            key = "'user-byid-' + #args[0]",
            magic = @Magic(
                    // 因为Magic 模式下会对数组及集合类型的数据进行分隔，所以取返回值固定使用 #retVal，
                    // 此key表达生成的值也必须要与getUserById 方法的一样
                    key = "'user-byid-' + #retVal.id"))
    List<UserDO> listByIds(@Param("ids") List<Long> ids);

    /**
     * 根据动态组合查询条件，获取用户id列表
     * 
     * @param condition
     * @return
     **/
    List<Long> listIdsByCondition(UserCondition condition);

    /**
     * 添加用户信息
     * 
     * @param user
     */
    @CacheDelete({ @CacheDeleteKey(value = "'user-byid-' + #args[0].id") })
    int addUser(UserDO user);

    /**
     * 更新用户信息
     * 
     * @param user
     * @return
     */
    @CacheDelete({ @CacheDeleteKey(value = "'user-byid-' + #args[0].id", condition = "#retVal > 0") })
    int updateUser(UserDO user);

    /**
     * 根据用户id删除用户记录
     **/
    @CacheDelete({ @CacheDeleteKey(value = "'user-byid-' + #args[0]", condition = "#retVal > 0") })
    int deleteUserById(Long id);

}

```


### [@ExCache](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/ExCache.java "@ExCache")


  使用场景举例：如果系统中用getUserById和getUserByName,两种方法来获取用户信息，我们可以在getUserById 时把 getUserByName 的缓存也生成。反过来getUserByName 时，也可以把getUserById 的缓存生成：

```java
@Cache(expire=600, key="'USER.getUserById'+#args[0]", exCache={@ExCache(expire=600, key="'USER.getUserByName'+#retVal.name")})
public User getUserById(Long id){... ...}

@Cache(expire=600, key="'USER.getUserByName'+#args[0]", exCache={@ExCache(expire=600, key="'USER.getUserById'+#retVal.id")})
public User getUserByName(Long id){... ...}
``` 


### [@CacheDelete](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/CacheDelete.java "@CacheDelete") 删除缓存注解

### [@CacheDeleteKey](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/CacheDeleteKey.java "@CacheDeleteKey") 生成删除缓存Key注解

### [@CacheDeleteTransactional](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/CacheDeleteTransactional.java "@CacheDeleteTransactional") 事务环境中批量删除缓存注解
    
### [@LocalCache](../autoload-cache-common/src/main/java/com/jarvis/cache/annotation/LocalCache.java "@LocalCache") 本地缓存注解
