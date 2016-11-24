package com.jarvis.cache.admin.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.jarvis.cache.AbstractCacheManager;
import com.jarvis.cache.aop.CacheAopProxyChain;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.cache.to.CacheKeyTO;
import com.jarvis.lib.util.BeanUtil;

/**
 * 缓存管理页面
 * @author jiayu.qiu
 */
public class AdminServlet extends HttpServlet {

    private static final long serialVersionUID=252742830396906514L;

    private String user="admin";

    private String password="admin";

    private String _cacheManagerConfig;

    private CacheManagerConfig cacheManagerConfig;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String _user=servletConfig.getInitParameter("user");
        if(null != _user && _user.length() > 0) {
            user=_user;
        }
        String _password=servletConfig.getInitParameter("password");
        if(null != _password && _password.length() > 0) {
            password=_password;
        }
        _cacheManagerConfig=servletConfig.getInitParameter("cacheManagerConfig");
        if(null != _cacheManagerConfig && _cacheManagerConfig.length() > 0) {
            try {
                cacheManagerConfig=(CacheManagerConfig)Class.forName(_cacheManagerConfig).newInstance();
            } catch(Exception e) {
                throw new ServletException(e);
            }
        } else {
            throw new ServletException("请设置com.jarvis.cache.admin.servlet.AdminServlet 中的 cacheManagerConfig 参数！");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html");
        try {
            if(null == cacheManagerConfig) {
                String errMsg="the \"cacheManagerConfig\" is null!";
                resp.getWriter().println(errMsg);
                return;
            }
            String cacheManagerName=req.getParameter("cacheManagerName");

            String cacheManagerNames[]=cacheManagerConfig.getCacheManagerNames(req);
            if(null == cacheManagerNames || cacheManagerNames.length == 0) {
                String errMsg="get \"cacheManagerNames\" is empty!";
                resp.getWriter().println(errMsg);
                return;
            }
            if(null == cacheManagerName || cacheManagerName.trim().length() == 0) {
                cacheManagerName=cacheManagerNames[0];
            }
            AbstractCacheManager cacheManager=cacheManagerConfig.getCacheManagerByName(req, cacheManagerName);
            if(null == cacheManager) {
                String errMsg="get cacheManager by '" + cacheManagerName + "' is null!";
                resp.getWriter().println(errMsg);
                return;
            }
            printHtmlHead(resp, cacheManagerName);

            HttpSession session=req.getSession();
            String logined=(String)session.getAttribute("LOGINED");
            String act=req.getParameter("act");
            if(null == logined) {
                String message=null;
                boolean printLoginForm=true;
                if("login".equals(act)) {
                    String _user=req.getParameter("user");
                    String _password=req.getParameter("password");
                    if(user.equals(_user) && password.equals(_password)) {
                        session.setAttribute("LOGINED", "LOGINED");
                        printLoginForm=false;
                        act=null;
                    } else {
                        message="用户名或密码错误！";
                    }
                }
                if(printLoginForm) {
                    printLoginForm(resp, message);
                }
            }
            logined=(String)session.getAttribute("LOGINED");
            if(null != logined) {
                if(null != act) {
                    doServices(req, resp, cacheManager);
                } else {
                    printForm(resp, cacheManagerName, cacheManagerNames);
                    printList(req, resp, cacheManager, cacheManagerName);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            resp.getWriter().println(e.getMessage());
        }

        printCloseHtml(resp);
    }

    private void doServices(HttpServletRequest req, HttpServletResponse resp, AbstractCacheManager cacheManager) throws Exception {
        String act=req.getParameter("act");
        String cacheKey=req.getParameter("cacheKey");
        String hfield=req.getParameter("hfield");
        CacheKeyTO to=new CacheKeyTO(cacheManager.getNamespace(), cacheKey, hfield);

        if("removeCache".equals(act)) {
            cacheManager.delete(to);
            resp.getWriter().println("处理成功！");
        } else if("removeAutoloadTO".equals(act)) {
            cacheManager.getAutoLoadHandler().removeAutoLoadTO(to);
            resp.getWriter().println("处理成功！");
        } else if("resetLastLoadTime".equals(act)) {
            cacheManager.getAutoLoadHandler().resetAutoLoadLastLoadTime(to);
            resp.getWriter().println("处理成功！");
        } else if("showArgs".equals(act)) {
            AutoLoadTO tmpTO=cacheManager.getAutoLoadHandler().getAutoLoadTO(to);
            if(null != tmpTO && null != tmpTO.getArgs() && tmpTO.getArgs().length > 0) {
                Object[] args=tmpTO.getArgs();
                int len=args.length;
                StringBuilder html=new StringBuilder();
                for(int i=0; i < len; i++) {
                    html.append("#args[" + i + "] = ").append(BeanUtil.toString(args[i])).append("<hr/>");
                }
                resp.getWriter().println(html.toString());
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void printHtmlHead(HttpServletResponse resp, String cacheManagerName) throws IOException {
        StringBuilder html=new StringBuilder();
        html.append("<html>").append("<head>").append("<title>Cache Admin</title>");
        html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
        html.append("<style type=\"text/css\">");
        html.append("th {text-align: center; line-height: 24px; border-top: 1px solid #555555; border-bottom: 1px solid #555555; border-right: 1px solid #555555; word-wrap: break-word; }");
        html.append("table { border-left: 1px solid #555555; }");
        html.append("td { border-right: 1px solid #555555; border-bottom: 1px solid #555555; text-align: center; line-height: 24px; word-wrap: break-word; }");
        html.append("</style>");
        html.append("<script type=\"text/javascript\">");
        html.append("var cacheManagerName=\"" + cacheManagerName + "\";");
        html.append("function removeCache(cacheKey, hfield){if(!cacheKey){return;}");
        html.append("  if(confirm(\"确定要删除缓存?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeCache\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"hfield\").value=hfield;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function removeAutoloadTO(cacheKey, hfield){");
        html.append("  if(confirm(\"确定要删除?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeAutoloadTO\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"hfield\").value=hfield;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function resetLastLoadTime(cacheKey, hfield){");
        html.append("  if(confirm(\"确定要重置?\")){");
        html.append("    document.getElementById(\"act\").value=\"resetLastLoadTime\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"hfield\").value=hfield;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");
        html.append("</script></head><body>");
        resp.getWriter().println(html.toString());
    }

    private void printLoginForm(HttpServletResponse resp, String message) throws IOException {
        StringBuilder html=new StringBuilder();
        if(null != message) {
            html.append("ERROR:" + message);
        }
        html.append("<form  action=\"\" method=\"post\">");
        html.append("user:<input type=\"text\" name=\"user\" />");
        html.append("password:<input type=\"password\" name=\"password\" />");
        html.append("<input type=\"hidden\" id=\"act\" name=\"act\" value=\"login\" />");
        html.append("<input type=\"submit\" value=\"登录\"></input>");
        html.append("</form>");
        resp.getWriter().println(html.toString());
    }

    private void printForm(HttpServletResponse resp, String cacheManagerName, String cacheManagerNames[]) throws IOException {
        StringBuilder html=new StringBuilder();
        html.append("<form  action=\"\" method=\"get\">");
        html.append("cache manager bean name:");
        html.append("<select name=\"cacheManagerName\">");
        for(String tmpName: cacheManagerNames) {
            html.append("  <option value=\"" + tmpName + "\" " + (tmpName.equals(cacheManagerName) ? "selected" : "") + " >" + tmpName + "</option>");
        }
        html.append("</select>");
        html.append("<input type=\"submit\" value=\"更改缓存\"></input>");
        html.append("</form>");
        html.append("cache key:<input type=\"text\" id=\"deleteCacheKey\"/> <input type=\"button\" onclick=\"removeCache(document.getElementById('deleteCacheKey').value)\" value=\"删除缓存\"/>");
        html.append("<form id=\"updateCacheForm\" action=\"\" method=\"get\" target=\"_blank\">");
        html.append("<input type=\"hidden\" id=\"act\" name=\"act\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheKey\" name=\"cacheKey\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"hfield\" name=\"hfield\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheManagerName\" name=\"cacheManagerName\" value=\"\" />");
        html.append("</form>");
        resp.getWriter().println(html.toString());
    }

    private void printList(HttpServletRequest req, HttpServletResponse resp, AbstractCacheManager cacheManager, String cacheManagerName) throws IOException {
        AutoLoadTO queue[]=cacheManager.getAutoLoadHandler().getAutoLoadQueue();
        if(null == queue || queue.length == 0) {
            resp.getWriter().println("自动加载队列中无数据！");
            return;
        }
        StringBuilder html=new StringBuilder();
        html.append("<table cellpadding=\"0\" cellspacing=\"0\">");
        html.append("  <tr>");
        html.append("    <th>namespace </th>");
        html.append("    <th>key </th>");
        html.append("    <th>hash field </th>");
        html.append("    <th>className.method </th>");
        html.append("    <th>last request time </th>");
        html.append("    <th>first request time </th>");
        html.append("    <th>request times </th>");
        html.append("    <th>expire-time(expire) </th>");
        html.append("    <th>request timeout </th>");
        html.append("    <th>last load time </th>");
        html.append("    <th>load count </th>");
        html.append("    <th>average use time </th>");
        html.append("    <th>remove cache </th>");
        html.append("    <th>remove AutoloadTO </th>");
        html.append("    <th>reset last load time </th>");
        html.append("    <th>show arguments </th>");
        html.append("  </tr>");

        for(AutoLoadTO tmpTO: queue) {
            CacheAopProxyChain pjp=tmpTO.getJoinPoint();
            String className=pjp.getTargetClass().getName();
            String methodName=pjp.getMethod().getName();
            CacheKeyTO cacheKeyTO=tmpTO.getCacheKey();
            String _key=cacheKeyTO.getKey();
            String _hfield=cacheKeyTO.getHfield();
            if(null == _hfield) {
                _hfield="";
            }
            html.append("  <tr>");
            html.append("    <td>" + cacheKeyTO.getNamespace() + "</td>");
            html.append("    <td>" + _key + "</td>");
            html.append("    <td>" + _hfield + "</td>");
            html.append("    <td>" + className + "." + methodName + "</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastRequestTime()) + "</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getFirstRequestTime()) + "</td>");
            html.append("    <td>" + tmpTO.getRequestTimes() + "次</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime() + tmpTO.getCache().expire() * 1000) + "(" + tmpTO.getCache().expire() + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastRequestTime() + tmpTO.getCache().requestTimeout() * 1000) + "(" + tmpTO.getCache().requestTimeout() + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime()) + "</td>");
            html.append("    <td>" + tmpTO.getLoadCnt() + "次</td>");
            html.append("    <td>" + tmpTO.getAverageUseTime() + "毫秒</td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeCache('" + _key + "','" + _hfield + "')\">删除缓存</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeAutoloadTO('" + _key + "','" + _hfield + "')\">移除 AutoloadTO</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"resetLastLoadTime('" + _key + "','" + _hfield + "')\">重置最后加载时间</a></td>");
            html.append("<td>");
            if(null != tmpTO.getArgs() && tmpTO.getArgs().length > 0) {
                html.append("<a href=\"" + req.getContextPath() + req.getServletPath() + "?act=showArgs&cacheManagerName=" + cacheManagerName + "&cacheKey=" + _key + "&hfield=" + _hfield
                    + "\" target=\"_blank\">show args values</a>");
            }
            html.append("</td>");
            html.append("  </tr>");
        }
        html.append("</table>");

        resp.getWriter().println(html.toString());
    }

    private void printCloseHtml(HttpServletResponse resp) throws IOException {
        resp.getWriter().println("</body></html>");
    }

    private String getDateFormat(long time) {
        if(time < 100000) {
            return "";
        }
        Date date=new Date(time);
        SimpleDateFormat df=new SimpleDateFormat("MM/dd/HH:mm:ss");
        return df.format(date);
    }
}
