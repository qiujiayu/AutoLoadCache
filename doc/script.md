##表达式的应用

以下是以Spring EL表达式为例子，如果使用其它表达式，需要注意语法的不同。

###缓存Key的生成

 在@Cache中设置key，可以是字符串或Spring EL表达式:

    例如： 

        @Cache(expire=600, key="'goods.getGoodsById'+#args[0]")
        public GoodsTO getGoodsById(Long id){...}

    为了使用方便，调用hash 函数可以将任何Object转为字符串，使用方法如下：
     
        @Cache(expire=720, key="'GOODS.getGoods:'+#hash(#args)")
        public List<GoodsTO> getGoods(GoodsCriteriaTO goodsCriteria){...}
    生成的缓存Key为"GOODS.getGoods:xxx",xxx为args，的转在的字符串。

    在拼缓存Key时，各项数据最好都用特殊字符进行分隔，否则缓存的Key有可能会乱的。比如：a,b 两个变量a=1,b=11,如果a=11,b=1,两个变量中间不加特殊字符，拼在一块，值是一样的。
  Spring EL表达式支持调整类的static 变量和方法，比如："T(java.lang.Math).PI"。

###提供的SpEL上下文数据

| 名字 | 描述 | 示例 |
| ------------- | ------------- | ------------- |
| args | 当前被调用的方法的参数列表 | #args[0] |
| retVal | 方法执行后的返回值（仅当方法执行之后才有效，如@Cache(opType=CacheOpType.WRITE),expireExpression,autoloadCondition,@ExCache() | #retVal |

###提供的SpEL函数

| 名字 | 描述 | 示例 |
| ------------- | ------------- | ------------- |
| hash | 将Object 对象转换为唯一的Hash字符串 | #hash(#args) |
| empty | 判断Object对象是否为空 | #empty(#args[0]) |

###自定义SpEL函数
通过AutoLoadConfig 的functions 注册自定义函数，例如：

    <bean id="autoLoadConfig" class="com.jarvis.cache.to.AutoLoadConfig">
      ... ...
      <property name="functions">
        <map>
          <entry key="isEmpty" value="com.jarvis.cache.CacheUtil" />
          <!--#isEmpty(#args[0]) 表示调com.jarvis.cache.CacheUtil中的isEmpty方法-->
        </map>
      </property>
    </bean>

网上大概搜了一下，有以下几种表达式计算引擎:

1. Ognl http://commons.apache.org/proper/commons-ognl/
2. fast-el https://code.google.com/archive/p/fast-el/
3. JSEL https://code.google.com/archive/p/lite/wikis/JSEL.wiki
4. Commons EL http://commons.apache.org/proper/commons-el/index.html
5. commons-jexl http://commons.apache.org/proper/commons-jexl/
6. Aviator https://code.google.com/archive/p/aviator/
7. IKExpression https://code.google.com/archive/p/ik-expression/
8. JDK自带脚本引擎：javax.script.ScriptEngineManager
9. JUEL  
10. beanshell http://www.beanshell.org/
11. Groovy
12. JRuby


脚本性能测试代码：com.test.script.ScriptTest，通过这个测试发现OGNL的性能最优。

已经实现了SpringEL、OGNL、JavaScript三种表达式的支持。