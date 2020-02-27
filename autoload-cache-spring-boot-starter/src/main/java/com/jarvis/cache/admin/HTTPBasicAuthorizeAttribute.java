package com.jarvis.cache.admin;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import com.jarvis.cache.autoconfigure.AutoloadCacheProperties;

/**
 * @author: jiayu.qiu
 */
public class HTTPBasicAuthorizeAttribute implements Filter {

    private static final String SESSION_AUTH_ATTRIBUTE = "autoload-cache-auth";

    private final AutoloadCacheProperties properties;

    public HTTPBasicAuthorizeAttribute(AutoloadCacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String sessionAuth = (String) (request).getSession().getAttribute(SESSION_AUTH_ATTRIBUTE);

        if (sessionAuth == null) {
            if (!checkHeaderAuth(request, response)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader("Cache-Control", "no-store");
                response.setDateHeader("Expires", 0);
                response.setHeader("WWW-Authenticate", "Basic realm=\"input username and password\"");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean checkHeaderAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String userName = properties.getAdminUserName();
        if (null == userName || userName.isEmpty()) {
            return true;
        }
        String password = properties.getAdminPassword();
        String auth = request.getHeader("Authorization");
        if ((auth != null) && (auth.length() > 6)) {
            auth = auth.substring(6, auth.length());
            String decodedAuth = getFromBASE64(auth);
            if (decodedAuth != null) {
                String[] userArray = decodedAuth.split(":");
                if (userArray != null && userArray.length == 2 && userName.equals(userArray[0])) {
                    if ((null == password || password.isEmpty())
                            || (null != password && password.equals(userArray[1]))) {
                        request.getSession().setAttribute(SESSION_AUTH_ATTRIBUTE, decodedAuth);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getFromBASE64(String s) {
        if (s == null) {
            return null;
        }
        try {
            byte[] b = Base64.getDecoder().decode(s);
            return new String(b);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void destroy() {

    }

}
