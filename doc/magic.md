# Magic模式

得益于7.0.0版本开始支持批量处理，7.0.1增加了Magic模式。Magic模式要解决的问题是：先从缓存中批量查询，然后部分key缓存命中，然后将取未命中缓存的key，再批量去数据源加载，最后把数据源返回的数据刷入缓存。

下面举个实际例子，在没有使用Magic模式时：

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

    /**
     * 根据动态组合查询条件，获取用户id列表
     * 
     * @param condition
     * @return
     **/
    List<Long> listIdsByCondition(UserCondition condition);

}

```

下面是根据一定的查询用户信息：

```java
    public List<UserDO> listByCondition(UserCondition condition) {
        List<Long> ids = userMapper.listIdsByCondition(condition);
        List<UserDO> list = null;
        if (null != ids && ids.size() > 0) {
            list = new ArrayList<>(ids.size());
            UserDO userDO = null;
            for (Long id : ids) {
                userDO = userMapper.getUserById(id);
                if (null != userDO) {
                    list.add(userDO);
                }
            }
        }
        return list;
    }
```
如果上面ids有10条记录，那么就需要访问10次缓存和10数据源才能获取最终需要的完整数据，如果10条缓存都没有命中，那么还需要写10次缓存。

下面是使用Magic模式进行优化：

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

    /**
     * 根据动态组合查询条件，获取用户id列表
     * 
     * @param condition
     * @return
     **/
    List<Long> listIdsByCondition(UserCondition condition);
    
    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60",
            key = "'user-byid-' + #args[0]",
            magic = @Magic(key = "'user-byid-' + #retVal.id"))
    List<UserDO> listByIds(@Param("ids") List<Long> ids);

}

```

下面是根据一定的查询用户信息：

```java
    public List<UserDO> listByCondition(UserCondition condition) {
        List<Long> ids = userMapper.listIdsByCondition(condition);
        List<UserDO> list = null;
        if (null != ids && !ids.isEmpty()) {
            list = userMapper.listByIds(ids);
        }
        return list;
    }
```

使用Magic模式后，如果上面ids有10条记录，那么就需要访问1次缓存和1数据源就能获取最终需要的完整数据，如果还有数据没有缓存命中，那么还需要1次写缓存操作。但使用Magic模式后，就不允许自动加载(autoload设置为true也不会生效)、不支持“拿来主义”、异步刷新等功能。
