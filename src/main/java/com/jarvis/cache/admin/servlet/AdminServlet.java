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

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.to.AutoLoadTO;
import com.jarvis.lib.util.BeanUtil;

/**
 * 缓存管理页面
 * @author jiayu.qiu
 */
public class AdminServlet extends HttpServlet {

    private static final long serialVersionUID=252742830396906514L;

    private String cacheManagerNames[]=null;

    private String user="admin";

    private String password="admin";

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String tmpNames=servletConfig.getInitParameter("cacheManagerNames");
        cacheManagerNames=tmpNames.split(",");
        String _user=servletConfig.getInitParameter("user");
        if(null != _user) {
            user=_user;
        }
        String _password=servletConfig.getInitParameter("password");
        if(null != _password) {
            password=_password;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html");
        String cacheManagerName=req.getParameter("cacheManagerName");
        if(null == cacheManagerNames || cacheManagerNames.length == 0) {
            String errMsg="set \"cacheManagerNames\" parameter with bean names of com.jarvis.cache.ICacheManager instance";
            resp.getWriter().println(errMsg);
            return;
        }
        if(null == cacheManagerName || cacheManagerName.trim().length() == 0) {
            cacheManagerName=cacheManagerNames[0];
        }
        printHtmlHead(resp, cacheManagerName);
        try {

            ApplicationContext ctx=
                WebApplicationContextUtils.getRequiredWebApplicationContext(req.getSession().getServletContext());
            ICacheManager<?> cacheManager=(ICacheManager<?>)ctx.getBean(cacheManagerName);
            if(null == cacheManager) {
                String errMsg=cacheManagerName + " is not exists!";
                throw new Exception(errMsg);
            }
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
                    printForm(resp, cacheManagerName);
                    printList(req, resp, cacheManager, cacheManagerName);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            resp.getWriter().println(e.getMessage());
        }

        printCloseHtml(resp);
    }

    private void doServices(HttpServletRequest req, HttpServletResponse resp, ICacheManager<?> cacheManager) throws Exception {
        String act=req.getParameter("act");
        String cacheKey=req.getParameter("cacheKey");
        if("removeCache".equals(act)) {
            cacheManager.delete(cacheKey);
            resp.getWriter().println("处理成功！");
        } else if("removeAutoloadTO".equals(act)) {
            cacheManager.getAutoLoadHandler().removeAutoLoadTO(cacheKey);
            resp.getWriter().println("处理成功！");
        } else if("resetLastLoadTime".equals(act)) {
            cacheManager.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKey);
            resp.getWriter().println("处理成功！");
        } else if("showArgs".equals(act)) {
            AutoLoadTO tmpTO=cacheManager.getAutoLoadHandler().getAutoLoadTO(cacheKey);
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
        html.append("function removeCache(cacheKey){if(!cacheKey){return;}");
        html.append("  if(confirm(\"确定要删除缓存?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeCache\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function removeAutoloadTO(cacheKey){");
        html.append("  if(confirm(\"确定要删除?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeAutoloadTO\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function resetLastLoadTime(cacheKey){");
        html.append("  if(confirm(\"确定要重置?\")){");
        html.append("    document.getElementById(\"act\").value=\"resetLastLoadTime\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
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

    private void printForm(HttpServletResponse resp, String cacheManagerName) throws IOException {
        StringBuilder html=new StringBuilder();
        html.append("<form  action=\"\" method=\"get\">");
        html.append("cache manager bean name:");
        html.append("<select name=\"cacheManagerName\">");
        for(String tmpName: cacheManagerNames) {
            html.append("  <option value=\"" + tmpName + "\" " + (tmpName.equals(cacheManagerName) ? "selected" : "") + " >"
                + tmpName + "</option>");
        }
        html.append("</select>");
        html.append("<input type=\"submit\" value=\"更改缓存\"></input>");
        html.append("</form>");
        html.append("cache key:<input type=\"text\" id=\"deleteCacheKey\"/> <input type=\"button\" onclick=\"removeCache(document.getElementById('deleteCacheKey').value)\" value=\"删除缓存\"/>");
        html.append("<form id=\"updateCacheForm\" action=\"\" method=\"get\" target=\"_blank\">");
        html.append("<input type=\"hidden\" id=\"act\" name=\"act\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheKey\" name=\"cacheKey\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheManagerName\" name=\"cacheManagerName\" value=\"\" />");
        html.append("</form>");
        resp.getWriter().println(html.toString());
    }

    private void printList(HttpServletRequest req, HttpServletResponse resp, ICacheManager<?> cacheManager, String cacheManagerName)
        throws IOException {
        AutoLoadTO queue[]=cacheManager.getAutoLoadHandler().getAutoLoadQueue();
        if(null == queue || queue.length == 0) {
            resp.getWriter().println("自动加载队列中无数据！");
            return;
        }
        StringBuilder html=new StringBuilder();
        html.append("<table cellpadding=\"0\" cellspacing=\"0\">");
        html.append("  <tr>");
        html.append("    <th>CacheKey </th>");
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
            ProceedingJoinPoint pjp=tmpTO.getJoinPoint();
            String className=pjp.getTarget().getClass().getName();
            String methodName=pjp.getSignature().getName();
            html.append("  <tr>");
            html.append("    <td>" + tmpTO.getCacheKey() + "</td>");
            html.append("    <td>" + className + "." + methodName + "</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastRequestTime()) + "</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getFirstRequestTime()) + "</td>");
            html.append("    <td>" + tmpTO.getRequestTimes() + "次</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime() + tmpTO.getExpire() * 1000) + "(" + tmpTO.getExpire()
                + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastRequestTime() + tmpTO.getRequestTimeout() * 1000) + "("
                + tmpTO.getRequestTimeout() + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime()) + "</td>");
            html.append("    <td>" + tmpTO.getLoadCnt() + "次</td>");
            html.append("    <td>" + tmpTO.getAverageUseTime() + "毫秒</td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeCache('" + tmpTO.getCacheKey()
                + "')\">删除缓存</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeAutoloadTO('" + tmpTO.getCacheKey()
                + "')\">移除 AutoloadTO</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"resetLastLoadTime('" + tmpTO.getCacheKey()
                + "')\">重置最后加载时间</a></td>");
            html.append("<td>");
            if(null != tmpTO.getArgs() && tmpTO.getArgs().length > 0) {
                html.append("<a href=\"" + req.getContextPath() + req.getServletPath() + "?act=showArgs&cacheManagerName="
                    + cacheManagerName + "&cacheKey=" + tmpTO.getCacheKey() + "\" target=\"_blank\">show args values</a>");
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
