package com.jarvis.cache.demo.mapper;

import java.util.List;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.annotation.CacheDelete;
import com.jarvis.cache.annotation.CacheDeleteKey;
import com.jarvis.cache.annotation.CacheDeleteMagicKey;
import com.jarvis.cache.annotation.Magic;
import com.jarvis.cache.demo.condition.UserCondition;
import com.jarvis.cache.demo.entity.UserDO;
import com.jarvis.cache.demo.mapper.temp.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 在接口中使用注解的例子 业务背景：用户表中有id, name, password,
 * status字段，name字段是登录名。并且注册成功后，用户名不允许被修改。
 * 
 *
 */
public interface UserMapper {// extends BaseMapper<UserDO, Long>
    String CACHE_NAME = "user2";

//    default String getCacheName() {
//        return CACHE_NAME;
//    }
    
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
    
    /**
     * 
     * 测试 autoload = true
     * @return
     */
    @Cache(expire = 60, key = "user-all", autoload = true)
    List<UserDO> allUsers();
    
    /**
     * 
     * 测试 autoload = true
     * @return
     */
    @Cache(expire = 60, key = "'user-list-' + #hash(#args[0])", autoload = true)
    List<UserDO> listByCondition(UserCondition condition);

    /**
     * 根据用户名获取用户id
     * 
     * @param name
     * @return
     */
    @Cache(expire = 60, expireExpression = "null == #retVal ? 60: 61", key = "'userid-byname-' + #args[0]")
    Long getUserIdByName(String name);

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
    @CacheDelete({
            @CacheDeleteKey(value = "'user-byid-' + #args[0].id"),
            @CacheDeleteKey(value = "'userid-byname-' + #args[0].name") })
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

    /**
     * 根据用户ids删除用户记录
     **/
    @CacheDelete(magic = { @CacheDeleteMagicKey(value = "'user-byid-' + #args[0]", condition = "#retVal > 0", iterableArgIndex = 0, iterableReturnValue = false) })
    int deleteUserByIds(@Param("ids") Long... ids);
}
