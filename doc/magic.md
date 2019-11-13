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
            magic = @Magic(key = "'user-byid-' + #retVal.id", iterableArgIndex = 0))
    List<UserDO> listByIds(@Param("ids") List<Long> ids);
    
    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60",
            // 因为Magic 模式下会对参数进行分隔，所以取参数固定使用 #args[0]，
            // 如果参数据是复杂类型，比如List<UserDO>, 那么取参使用 #args[0].id
            // 为了降低缓存不致问题，些处生的有key值要与getUserById 方法的一样
            key = "'user-byid-' + #args[0]",
            magic = @Magic(
                    // 因为Magic 模式下会对数组及集合类型的数据进行分隔，所以取返回值固定使用 #retVal，
                    // 此key表达生成的值也必须要与getUserById 方法的一样
                    key = "'user-byid-' + #retVal.id", iterableArgIndex = 0))
    List<UserDO> listByIds(@Param("ids") List<Long> ids);

    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60",
            // 因为Magic 模式下会对参数进行分隔，所以取参数固定使用 #args[0]，
            // 如果参数据是复杂类型，比如List<UserDO>, 那么取参使用 #args[0].id
            // 为了降低缓存不致问题，些处生的有key值要与getUserById 方法的一样
            key = "'user-byid-' + #args[0]",
            magic = @Magic(
                    // 因为Magic 模式下会对数组及集合类型的数据进行分隔，所以取返回值固定使用 #retVal，
                    // 此key表达生成的值也必须要与getUserById 方法的一样
                    key = "'user-byid-' + #retVal.id", iterableArgIndex = 0))
    List<UserDO> listByIds2(@Param("ids") Long... ids);

}

```

下面是在没有使用Magic模式下根据一定条件查询用户信息，先获取用户id列表，然后再遍历获取用户详细信息：

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

从7.0.4版本开始Magic模式也可用于无参函数，但此时只能从数据源加载数据，并批量写入缓存。

```java
    @Cache(expire = 60, expireExpression = "null == #retVal ? 30: 60",
            key = "", magic = @Magic(key = "'user-magic-'+ #retVal.id"))
    public List<UserDO> loadUsers() {
        List<UserDO> list = new ArrayList<>(5);
        for (Long id = 100L; id < 105; id++) {
            list.add(new UserDO(id, "name" + id, "ppp"));
        }
        return list;
    }
```

有时我们也需要动态精确批量删除缓存，比如更新一批商品信息后，也要批量删除缓存，在7.0.1版本之前需要在业务代码中使用循环操作来实现。

7.0.4版本中增加@CacheDeleteMagicKey 用于开启Magic模式，并支持对参数或返回值进行分割，然后生成多个Cache key进行批量删除缓存。如下例子所示：

```java

/**
 * 根据用户ids删除用户记录
 **/
@CacheDelete(magic = { @CacheDeleteMagicKey(value = "'user-byid-' + #args[0]", condition = "#retVal > 0", iterableArgIndex = 0, iterableReturnValue = false) })
int deleteUserByIds(@Param("ids") Long... ids);

// 遍历ids，删除所有缓存， iterableReturnValue 必须设置false
@CacheDelete(magic = {
        @CacheDeleteMagicKey(value = "'user-testMagic-' + #args[0] + '-' + #args[1] + '-' + #args[2]", iterableArgIndex = 2, iterableReturnValue = false)
})
public void testDeleteMagicForArg(String name, String password, Long... ids) {

}

// 遍历返回值，删除所有缓存，iterableArgIndex必须设置为-1, iterableReturnValue 必须设置为true
@CacheDelete(magic = {
        @CacheDeleteMagicKey(value = "'user-testMagic-' + #args[0] + '-' + #args[1] + '-' + #retVal.id", iterableArgIndex = -1, iterableReturnValue = true)
})
public List<UserDO> testDeleteMagicForRetVal(String name, String password, Long... ids) {
    List<UserDO> list = new ArrayList<>(ids.length);
    for (Long id : ids) {
        list.add(new UserDO(id, name, password));
    }
    return list;
}
```

使用Magic模式不仅能简少手动遍历的代码，同时也会使用redis的Pipeline批量处理，减少与Redis的交互次数，提升性能。