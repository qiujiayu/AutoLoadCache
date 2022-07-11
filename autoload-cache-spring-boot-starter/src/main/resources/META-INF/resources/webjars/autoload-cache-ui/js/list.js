//Vue对象上下文，当中的属性可以任意获取，例如 app.$data.items
var app = new Vue({
    el: '#app',
    data: {
        items: [],
        args: "",
        fields: [
            {
                key: 'namespace',
                label: '命名空间'
            },
            {
                key: 'key',
                label: 'key'
            },
            {
                key: 'hfield',
                label: 'hash field'
            },
            {
                key: 'method',
                label: '方法'
            },
            {
                key: 'firstRequestTime',
                label: '首次请求时间'
            },
            {
                key: 'lastRequestTime',
                label: '上次请求时间'
            },
            {
                key: 'requestTimes',
                label: '请求次数'
            },
            {
                key: 'expire',
                label: '缓存时长(秒)'
            },
            {
                key: 'expireTime',
                label: '缓存过期时间'
            },
            {
                key: 'requestTimeout',
                label: '自动加载期限' // 如果超过此项设置的时长内没有人请求此数据，则认为此数据已经没有缓存的意义，不需要再自动加载
            },
            {
                key: 'requestTimeoutTime',
                label: '结束自动加载时间'
            },
            {
                key: 'lastLoadTime',
                label: '上次加载时间'
            },
            {
                key: 'loadCount',
                label: '加载次数'
            },
            {
                key: 'averageUseTime',
                label: '平均耗时'
            },
            {
                key: 'opt',
                label: '操作'
            }
        ],
        showDismissibleAlert: false
    }

});

function handleError(error){
    if (error.response) {
          // The request was made and the server responded with a status code
          // that falls out of the range of 2xx
          console.log(error.response.data);
          console.log(error.response.status);
          console.log(error.response.headers);
    } else if (error.request) {
          // The request was made but no response was received
          // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
          // http.ClientRequest in node.js
          console.log(error.request);
    } else {
          // Something happened in setting up the request that triggered an Error
          console.log('Error', error.message);
    }
    console.log(error.config);
}

/**
 * 加载数据
 */
function loadData() {
    axios.get("autoload-cache")
      .then(function (response) {
        console.log("loadData response:", response);
        app.$data.items = response.data;
      })
      .catch(function (error) {
        handleError(error);
      });
}

/**
 * 模态窗口和按钮中间数据传递变量
 */
var currentItem;
var currentAction;
/**
 * 处理模态窗口 ok 绑定事件 @ok="handleOk"
 * @param oper
 */
function handleOk() {
    var params = new URLSearchParams();// 查询参数（注意不是json格式）
    params.append("key", currentItem.key);
    if(currentItem.hfield){
       params.append("hfield", currentItem.hfield);
    }
    axios.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';
    var action = currentAction;
    console.log("handleOk(", action, ") params:", params);
    axios.post("autoload-cache/" + action, params)
      .then(function (response) {
          console.log("handleOk(", action, ") response:", response);
          if (response.data) {
              loadData();
          } else {
              app.$data.showDismissibleAlert = true;
          }
      })
      .catch(function (error) {
          handleError(error);
      });
}

function showModal(item, action, event) {
    currentItem = item;
    currentAction = action;
    app.$root.$emit('bv::show::modal', 'confirmModal')
}

function showArgs(item, event){
    var params = {"key": item.key, "hfield": item.hfield};
    console.log("showArgs params", params);
    axios.get("autoload-cache/args", {params:params})
      .then(function (response) {
          console.log("showArgs response", response);
          var content = "[]";
          if(response.data){
              content= JSON.stringify(response.data, null, 2);
          }
          app.$data.args  = content
          app.$root.$emit('bv::show::modal', 'argsModal')
      })
      .catch(function (error) {
          handleError(error);
      });
}

loadData();