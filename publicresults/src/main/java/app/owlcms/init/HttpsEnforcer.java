/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

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
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (request.getHeader(X_FORWARDED_PROTO) != null) {
                if (request.getHeader(X_FORWARDED_PROTO).indexOf("https") != 0) {
                    String url = request.getRequestURL().toString();
                    // request was not sent with https; redirect to https unless running locally
                    if (request instanceof HttpServletRequest) {
                        String serverName = request.getServerName();
                        if (serverName.endsWith(".localhost") || serverName.endsWith("localhost")) {
                            logger.debug("{} received, local path, not redirecting to https", url);
                        } else {
                            logger.info("{} received, forcing redirect to https", url);
                            String pathInfo = (request.getPathInfo() != null) ? request.getPathInfo() : "";
                            response.sendRedirect("https://" + serverName + pathInfo);
                            return;
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            // ignored
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }
}