package com.test;

import com.jarvis.cache.CacheUpdateHandler;
import com.jarvis.cache.annotation.Cache;

import java.util.ArrayList;

/**
 * 类注释/描述
 *
 * @author hailin0@yeah.net
 * @date 2017-3-24 0:13
 */
public class CacheUpdateHandlerTest {
    public static void main(String[] args) {

        //调用示例
        CacheUpdateHandler
                .mark(CacheUpdateHandler.Operation.CLEAR)
                .batchs(
                        String.valueOf(1),//模拟调用有返回值的缓存方法
                        String.valueOf(2),//模拟调用有返回值的缓存方法
                        new CacheUpdateHandler.CacheBatchObj() {//模拟调用无返回值的缓存方法
                            public void exe() {
                                //模拟无返回值的缓存方法
                                new ArrayList<Object>().clear();
                                /*try {
                                    System.out.println(1);
                                    Thread.currentThread().sleep(5000l);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }*/
                            }
                        });


        //调用示例
        CacheUpdateHandler
                .mark(CacheUpdateHandler.Operation.UPDATE)
                .batchs(
                        String.valueOf(1),//模拟调用有返回值的缓存方法
                        String.valueOf(2),//模拟调用有返回值的缓存方法
                        new CacheUpdateHandler.CacheBatchObj() {//模拟调用无返回值的缓存方法
                            public void exe() {
                                //模拟无返回值的缓存方法
                                new ArrayList<Object>().clear();
                                //System.out.println(2);
                            }
                        });
    }

}

class User{
    int id;
    String name;
    int authorCode;
}

class UserInfoDao{

    @Cache(expire = 100,key = "'user_id_'+#args[0]")
    public User getUserById(int id){
        return new User();
    }

    @Cache(expire = 100,key = "'user_name_'+#args[0]")
    public User getUserByName(String name){
        return new User();
    }

    public void updateUser(User user){
    }
}

class UserAuthorService{

    UserInfoDao userInfoDao;

    public void updateUserAuthor(int id, int authorCode){
        //update
        User user = null; //查询db  db.getUserId(int);
        user.authorCode = authorCode;
        userInfoDao.updateUser(user);

        //clear
        CacheUpdateHandler
                .mark(CacheUpdateHandler.Operation.CLEAR)
                .batchs(userInfoDao.getUserById(user.id),
                        userInfoDao.getUserByName(user.name));
    }
}