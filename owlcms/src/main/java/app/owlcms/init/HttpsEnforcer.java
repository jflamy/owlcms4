/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.init;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

public class HttpsEnforcer implements Filter {
    public static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";
    Logger logger = (Logger) LoggerFactory.getLogger(HttpsEnforcer.class);

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (request.getHeader(X_FORWARDED_PROTO) != null) {
            if (request.getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                if (request instanceof HttpServletRequest) {
                    String url = request.getRequestURL().toString();
                    logger.info("{} received, forcing redirect to https", url);
                }
                String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
                response.sendRedirect("https://" + request.getServerName() + pathInfo);
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            if (StartupUtils.isDebugSetting()) {
                Enumeration<String> headerNames = request.getHeaderNames();
                while(headerNames.hasMoreElements()) {
                  String headerName = (String)headerNames.nextElement();
                  logger.warn("    {} {}",headerName, request.getHeader(headerName));
                }
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}