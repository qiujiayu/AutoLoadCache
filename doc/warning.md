## 注意事项

### 1. 使用@Cache后，对应方法的参数及返回值必须都是可Serializable的。

为了避免参数被修改，AutoLoadHandler中需要**深度复制**后的参数进行异步刷新缓存。

### 2. 参数中只设置必要的属性值，在DAO中用不到的属性值尽量不要设置，这样能避免生成不同的缓存Key，降低缓存的使用率。
例如：

        public CollectionTO<AccountTO> getAccountByCriteria(AccountCriteriaTO criteria)  {
            List<AccountTO> list=null;
            PaginationTO paging=criteria.getPaging();
            if(null != paging && paging.getPageNo() > 0 && paging.getPageSize() > 0) {// 如果需要分页查询，先查询总数
                criteria.setPaging(null);// 减少缓存KEY的变化，在查询记录总数据时，不用设置分页相关的属性值
                Integer recordCnt=accountDAO.getAccountCntByCriteria(criteria);
                if(recordCnt > 0) {
                    criteria.setPaging(paging);
                    paging.setRecordCnt(recordCnt);
                    list=accountDAO.getAccountByCriteria(criteria);
                }
                return new CollectionTO<AccountTO>(list, recordCnt, criteria.getPaging().getPageSize());
            } else {
                list=accountDAO.getAccountByCriteria(criteria);
                return new CollectionTO<AccountTO>(list, null != list ? list.size() : 0, 0);
            }
        }

### 3. 注意AOP失效的情况;
例如：

        TempDAO {

            public Object a() {
                return b().get(0);
            }

            @Cache(expire=600)
            public List<Object> b(){
                return ... ...;
            }
        }

通过 new TempDAO().a() 调用b方法时，AOP失效，也无法进行缓存相关操作。

### 4. 自动加载缓存时，不能在缓存方法内叠加查询参数值;
例如：

        @Cache(expire=600, autoload=true, key="'myKey'+#hash(#args[0])")
        public List<AccountTO> getDistinctAccountByPlayerGet(AccountCriteriaTO criteria) {
            List<AccountTO> list;
            int count=criteria.getPaging().getThreshold() ;
            // 查预设查询数量的10倍
            criteria.getPaging().setThreshold(count * 10);
            … …
        }

因为自动加载时，AutoLoadHandler 缓存了查询参数，执行自动加载时，每次执行时 threshold 都会乘以10，这样threshold的值就会越来越大。


### 5. 对于一些比较耗时的方法尽量使用自动加载。

### 6. 对于查询条件变化比较剧烈的，不要使用自动加载机制。
比如，根据用户输入的关键字进行搜索数据的方法，不建议使用自动加载。

### 7. DAO方法中不能从ThreadLocal 获取数据，自动加载及异步刷新缓存数据是在别的线程池中进行的，会取不到ThreadLocal中的数据。

### 8. 使用 @Cache(opType=CacheOpType.WRITE)的坑
因为AutoloadCache是不支持事务回滚的，所以在如下情况时，会出现缓存中的数据不正确的情况：

    public class UserDAO {
        // 更新数据后，同时把数据缓存
        @Cache(expire=600, key="'user'+#retVal.id", opType=CacheOpType.WRITE)
        public UserTO updateUser(UserTO user) {
            getSqlMapClient().update("USER.updateUser", user);
            return user;
        }
    }

如果事务提交失败时，此时缓存中的数据无法回滚，所以使用时要注意。

## 在事务环境中，如何减少“脏读”

1. 不要从缓存中取数据，然后应用到修改数据的SQL语句中

2. 在事务完成后，再删除相关的缓存, 使用@CacheDeleteTransactional 完成


大部分情况，只要做到第1点就可以了，因为保证数据库中的数据准确才是最重要的。因为这种“脏读”的情况只能减少出现的概率，不能完成解决。一般只有在非常高并发的情况才有可能发生。就像12306，在查询时告诉你还有车票，但最后支付时不一定会有。

