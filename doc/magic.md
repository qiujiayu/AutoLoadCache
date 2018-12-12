# Magic模式

得益于7.0.0版本开始支持批量处理，7.0.1增加了Magic模式。

为了降低缓存数据与数据源数据不一致，也是为了更加方便更新、删除缓存，通常做法是，数据以主键为key进行缓存，然后根据id获取数据，如下面两段代码所示：

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
如果上面ids有10条记录，最差的情况需要访问10次缓存和10数据源，以及写10次缓存才能完成以上操作，最好的情况也需要访问10次缓存才能完成操作。

使用Magic模式，则会将参数分隔，先批量去缓存中查询，获取命中的缓存，然后将缓存未命中的，再批量从数据源加载，然后把从数据源加载的数据刷入缓存，最后把缓存命中的与数据源加载的数据一并返回。

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

使用Magic模式后，如果上面ids有10条记录，最差情况需要访问1次缓存、1数据源以及1次写缓存操作；最好的情况只需要访问1次缓存。但使用Magic模式后，就不允许使用自动加载(autoload设置为true也不会生效)、不支持“拿来主义”、异步刷新等功能。
