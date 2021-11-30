/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.servlet;

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

import app.owlcms.utils.LoggerUtils;
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

//        String url = request.getRequestURL().toString();
        String forwarding = request.getHeader(X_FORWARDED_PROTO);
        if (forwarding != null) {
            if (!forwarding.contentEquals("https")) {
                // request was not sent with https; redirect to https unless running locally
                if (request instanceof HttpServletRequest) {
                    String serverName = request.getServerName();
                    // local server behind proxy, don't redirect.
                    if (serverName.endsWith(".localhost") || serverName.endsWith("localhost")) {
                        //logger.debug("{} received on '{}', not redirecting to https", serverName, url);
                    } else {
                        //logger.debug("{} received on '{}', forcing redirect to https", serverName, url);
                        String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
                        response.sendRedirect("https://" + serverName + pathInfo);
                        return;
                    }
                } else {
                    //logger.debug("{} received, do nothing instance type {}", url,request.getClass().getName());
                }
            } else {
                //logger.debug("{} received, do nothing because already https '{}'", url,forwarding);
            }
        } else {
            //logger.debug("{} received, do nothing not through proxy", url);
        }

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            LoggerUtils.logError(logger,e);
            if (logger.isDebugEnabled()) {
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    logger.debug("    {} {}", headerName, request.getHeader(headerName));
                }
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}