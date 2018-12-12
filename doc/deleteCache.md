## 缓存删除

### 通过删除缓存，实现数据实时性

下面商品评论的例子中，如果用户发表了评论，要所有涉及到的前端页面立即显示该如何来处理？

```java
package com.jarvis.example.dao;
import ... ...
public class GoodsCommentDAO{
    @Cache(expire=600, key="'goods_comment_list_'+#args[0]", hfield = "#args[1]+'_'+#args[2]", autoload=true, requestTimeout=18000)
    // goodsId=1, pageNo=2, pageSize=3 时相当于Redis命令：HSET goods_comment_list_1 2_3  List
    public List<CommentTO> getCommentListByGoodsId(Long goodsId, int pageNo, int pageSize) {
        ... ...
    }

    @CacheDelete({@CacheDeleteKey(value="'goods_comment_list_'+#args[0].goodsId")}) // 删除当前所属商品的所有评论，不删除其它商品评论
    // #args[0].goodsId = 1时，相当于Redis命令: DEL goods_comment_list_1
    public void addComment(Comment comment) {
        ... ...// 省略添加评论代码
    }

    @CacheDelete({@CacheDeleteKey(value="'goods_comment_list_'+#args[0]", hfield = "#args[1]+'_'+#args[2]")}) 
    // goodsId=1, pageNo=2, pageSize=3 时相当于Redis命令：DEL goods_comment_list_1 2_3 
    public void removeCache(Long goodsId, int pageNo, int pageSize) {
        ... ...// 使用空方法来删除缓存
    }
}
```    

### 批量删除缓存

  在一些用户交互比较多的系统中，使用程序删除缓存是非常常见的事情，当我们遇到一些查询条件比较复杂的查询时，我们没有办法通过程序，反向生成缓存key,也就无法精确定位需要删除的缓存，但我们又不希望把不相关的缓存给误删除时。这时就可以使用下面介绍的批量删除缓存功能。

  注意：批量删除缓存功能，现在只有Reids 和 ConcurrentHashMap两种缓存方式支持。Memcache无法支持。

  批量删除缓存，主要和存缓存的方式有关，我们把需要批量删除的缓存，放到对应的hash表中，需要批量删除时，把这个hash表删除就可以了，实现非常简单（因为Memcache不支持hash表，所以无法实现这种方式的批量删除缓存功能）。所以批量删除缓存，是因缓存缓存数据的方式改变了，才得以实现的。

  使用方法，参照上面删除商品评论的代码。在@Cache中加入hfield。

  另外Reids还支持使用通配符进行批最删除缓存，但不建议使用。