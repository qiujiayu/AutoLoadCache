##AutoLoadConfig 配置

    <bean id="autoLoadConfig" class="com.jarvis.cache.to.AutoLoadConfig">
      <property name="threadCnt" value="10" />
      <property name="maxElement" value="20000" />
      <property name="printSlowLog" value="true" />
      <property name="slowLoadTime" value="500" />
      <property name="sortType" value="1" />
      <property name="checkFromCacheBeforeLoad" value="true" />
      <property name="autoLoadPeriod" value="50" />
    </bean>


* threadCnt :处理自动加载队列的线程数量,默认值为10；

* maxElement ： 自动加载队列中允许存放的最大容量，默认值为20000；

* printSlowLog ： 是否打印比较耗时的请求，默认值：true；

* slowLoadTime ：当请求耗时超过此值时，记录目录（printSlowLog=true 时才有效），单位：毫秒；默认值500；
* sortType： 自动加载队列排序算法, **0**：按在Map中存储的顺序（即无序）；**1** ：越接近过期时间，越耗时的排在最前；**2**：根据请求次数，倒序排序，请求次数越多，说明使用频率越高，造成并发的可能越大。更详细的说明，请查看代码com.jarvis.cache.type.AutoLoadQueueSortType

* checkFromCacheBeforeLoad： 加载数据之前去缓存服务器中检查，数据是否快过期，如果应用程序部署的服务器数量比较少，设置为false, 如果部署的服务器比较多，可以考虑设置为true；

* autoLoadPeriod： 单个线程中执行自动加载的时间间隔, 此值越小，遍历自动加载队列频率起高，对CPU会越消耗CPU；

* functions： 注册表达式中用到的自定义函数；