package com.jarvis.cache.admin.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.jarvis.cache.ICacheManager;
import com.jarvis.cache.to.AutoLoadTO;

/**
 * 缓存管理页面
 * @author jiayu.qiu
 */
public class AdminServlet extends HttpServlet {

    private static final long serialVersionUID=252742830396906514L;

    private String cacheManagerNames[]=null;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String tmpNames=servletConfig.getInitParameter("cacheManagerNames");
        cacheManagerNames=tmpNames.split(",");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html");
        String cacheManagerName=req.getParameter("cacheManagerName");
        printHtmlHead(resp, cacheManagerName);
        try {
            if(null == cacheManagerNames || cacheManagerNames.length == 0) {
                String errMsg="set \"cacheManagerNames\" parameter with bean names of com.jarvis.cache.ICacheManager instance";
                throw new Exception(errMsg);
            }
            ApplicationContext ctx=
                WebApplicationContextUtils.getRequiredWebApplicationContext(req.getSession().getServletContext());
            ICacheManager<?> cacheManager=(ICacheManager<?>)ctx.getBean(cacheManagerName);
            if(null == cacheManager) {
                String errMsg=cacheManagerName + " is not exists!";
                throw new Exception(errMsg);
            }
            doServices(req, resp, cacheManager);
            printForm(resp, cacheManagerName);
            printList(resp, cacheManager);
        } catch(Exception e) {
            resp.getOutputStream().println(e.getMessage());
        }

        printCloseHtml(resp);
    }

    private void doServices(HttpServletRequest req, HttpServletResponse resp, ICacheManager<?> cacheManager) throws Exception {
        String act=req.getParameter("act");
        String cacheKey=req.getParameter("cacheKey");
        if("removeCache".equals(act)) {
            cacheManager.delete(cacheKey);
        } else if("removeAutoloadTO".equals(act)) {
            cacheManager.getAutoLoadHandler().removeAutoLoadTO(cacheKey);
        } else if("resetLastLoadTime".equals(act)) {
            cacheManager.getAutoLoadHandler().resetAutoLoadLastLoadTime(cacheKey);
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
        html.append("function removeCache(cacheKey){");
        html.append("  if(confirm(\"Are you sure?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeCache\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function removeAutoloadTO(cacheKey){");
        html.append("  if(confirm(\"Are you sure?\")){");
        html.append("    document.getElementById(\"act\").value=\"removeAutoloadTO\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");

        html.append("function resetLastLoadTime(cacheKey){");
        html.append("  if(confirm(\"Are you sure?\")){");
        html.append("    document.getElementById(\"act\").value=\"resetLastLoadTime\";");
        html.append("    document.getElementById(\"cacheKey\").value=cacheKey;");
        html.append("    document.getElementById(\"cacheManagerName\").value=cacheManagerName;");
        html.append("    document.getElementById(\"updateCacheForm\").submit();");
        html.append("}}");
        html.append("</script></head><body>");
        resp.getOutputStream().println(html.toString());
    }

    private void printForm(HttpServletResponse resp, String cacheManagerName) throws IOException {
        StringBuilder html=new StringBuilder();
        html.append("<form  action=\"\">");
        html.append("cache manager bean name:");
        html.append("<select name=\"cacheManagerName\">");
        for(String tmpName: cacheManagerNames) {
            html.append("  <option value=\"" + tmpName + "\" " + (tmpName.equals(cacheManagerName) ? "selected" : "") + " >"
                + tmpName + "</option>");
        }
        html.append("</select>");
        html.append("<input type=\"submit\" value=\"submit\"></input>");
        html.append("</form>");
        html.append("<form id=\"updateCacheForm\" action=\"\">");
        html.append("<input type=\"hidden\" id=\"act\" name=\"act\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheKey\" name=\"cacheKey\" value=\"\" />");
        html.append("<input type=\"hidden\" id=\"cacheManagerName\" name=\"cacheManagerName\" value=\"\" />");
        html.append("</form>");
        resp.getOutputStream().println(html.toString());
    }

    private void printList(HttpServletResponse resp, ICacheManager<?> cacheManager) throws IOException {
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
        html.append("  </tr>");
        AutoLoadTO queue[]=cacheManager.getAutoLoadHandler().getAutoLoadQueue();
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
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime() + tmpTO.getExpire()) + "(" + tmpTO.getExpire()
                + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastRequestTime() + tmpTO.getRequestTimeout()) + "("
                + tmpTO.getRequestTimeout() + "秒)</td>");
            html.append("    <td>" + getDateFormat(tmpTO.getLastLoadTime()) + "</td>");
            html.append("    <td>" + tmpTO.getLoadCnt() + "次</td>");
            html.append("    <td>" + tmpTO.getAverageUseTime() + "毫秒</td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeCache('" + tmpTO.getCacheKey()
                + "')\">remove cache</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"removeAutoloadTO('" + tmpTO.getCacheKey()
                + "')\">remove AutoloadTO</a></td>");
            html.append("    <td><a href=\"javascript:void()\" onclick=\"resetLastLoadTime('" + tmpTO.getCacheKey()
                + "')\">reset last load time</a></td>");
            html.append("  </tr>");
        }
        html.append("</table>");

        resp.getOutputStream().println(html.toString());
    }

    private void printCloseHtml(HttpServletResponse resp) throws IOException {
        resp.getOutputStream().println("</body></html>");
    }

    private String getDateFormat(long time) {
        if(time < 100000) {
            return "";
        }
        Date date=new Date(time);
        SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS");
        return df.format(date);
    }
}
