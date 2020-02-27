/**  
 * All rights Reserved, Designed By Suixingpay.
 * @author: qiujiayu[qiu_jy@suixingpay.com] 
 * @date: 2018年7月13日 下午11:31:41   
 * @Copyright ©2018 Suixingpay. All rights reserved. 
 * 注意：本内容仅限于随行付支付有限公司内部传阅，禁止外泄以及用于其他的商业用途。
 */
package com.jarvis.cache.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jarvis.cache.annotation.Cache;
import com.jarvis.cache.demo.condition.UserCondition;
import com.jarvis.cache.demo.entity.UserDO;
import com.jarvis.cache.demo.mapper.UserMapper;

/**  
 * TODO
 * @author: qiujiayu[qiu_jy@suixingpay.com]
 * @date: 2018年7月13日 下午11:31:41
 * @version: V1.0
 * @review: qiujiayu[qiu_jy@suixingpay.com]/2018年7月13日 下午11:31:41
 */
@Component
//@Transactional(readOnly = true)
public class UserDAO {

    @Autowired
    private UserMapper userMapper;
    
    @Cache(expire = 600, key = "'user-list2-' + #hash(#args[0])")
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
}
