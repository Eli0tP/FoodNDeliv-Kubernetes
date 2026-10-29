package com.example.foodndeliv.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class ReadOnlyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!"GET".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This is a read-only instance. Only GET requests are allowed.");
            return;
        }
        
        chain.doFilter(request, response);
    }
}
