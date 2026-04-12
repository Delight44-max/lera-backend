package com.lera.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@Configuration
public class SocketIOProxyConfig {

    @Value("${lera.socketio.port}")
    private int socketioPort;

    @Bean
    public FilterRegistrationBean<Filter> socketIOProxyFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                String uri = req.getRequestURI();

                if (!uri.startsWith("/socket.io")) {
                    chain.doFilter(request, response);
                    return;
                }

                // ✅ Handle CORS preflight
                if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                    String origin = req.getHeader("Origin");
                    if (origin != null) {
                        res.setHeader("Access-Control-Allow-Origin", origin);
                        res.setHeader("Access-Control-Allow-Credentials", "true");
                        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                        res.setHeader("Access-Control-Allow-Headers", "*");
                    }
                    res.setStatus(HttpServletResponse.SC_OK);
                    return;
                }

                String query = req.getQueryString();
                String targetUrl = "http://localhost:" + socketioPort + uri + (query != null ? "?" + query : "");

                HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
                conn.setRequestMethod(req.getMethod());
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000);

                // Forward headers (skip CORS headers — we handle them manually)
                req.getHeaderNames().asIterator().forEachRemaining(name -> {
                    if (!name.equalsIgnoreCase("origin") &&
                            !name.equalsIgnoreCase("access-control-request-method") &&
                            !name.equalsIgnoreCase("access-control-request-headers")) {
                        conn.setRequestProperty(name, req.getHeader(name));
                    }
                });

                // Forward body
                if ("POST".equalsIgnoreCase(req.getMethod())) {
                    try (InputStream in = req.getInputStream();
                         OutputStream out = conn.getOutputStream()) {
                        in.transferTo(out);
                    }
                }

                // ✅ Add CORS headers manually
                String origin = req.getHeader("Origin");
                if (origin != null) {
                    res.setHeader("Access-Control-Allow-Origin", origin);
                    res.setHeader("Access-Control-Allow-Credentials", "true");
                    res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                    res.setHeader("Access-Control-Allow-Headers", "*");
                }

                // Write response (skip CORS headers from Socket.IO — we added our own)
                res.setStatus(conn.getResponseCode());
                conn.getHeaderFields().forEach((key, values) -> {
                    if (key != null &&
                            !key.equalsIgnoreCase("access-control-allow-origin") &&
                            !key.equalsIgnoreCase("access-control-allow-credentials") &&
                            !key.equalsIgnoreCase("access-control-allow-methods") &&
                            !key.equalsIgnoreCase("access-control-allow-headers")) {
                        values.forEach(v -> res.addHeader(key, v));
                    }
                });

                InputStream responseStream = conn.getResponseCode() >= 400
                        ? conn.getErrorStream() : conn.getInputStream();

                if (responseStream != null) {
                    try (OutputStream out = res.getOutputStream()) {
                        responseStream.transferTo(out);
                    }
                }
            }
        });

        reg.addUrlPatterns("/socket.io/*");
        reg.setOrder(-200);
        return reg;
    }
}