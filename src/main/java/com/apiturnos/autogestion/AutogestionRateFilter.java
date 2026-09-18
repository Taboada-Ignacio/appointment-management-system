package com.apiturnos.autogestion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class AutogestionRateFilter extends OncePerRequestFilter {
    private final LimitarAutogestion limiter;
    public AutogestionRateFilter(LimitarAutogestion limiter) { this.limiter = limiter; }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api/autogestion/") || "OPTIONS".equals(request.getMethod());
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        // Never trust client-supplied forwarding headers.
        boolean allowed = limiter.permitido("ip:" + request.getRemoteAddr(), 60);
        String credential = request.getHeader("Authorization");
        if (allowed && credential != null && credential.length() <= 100)
            allowed = limiter.permitido("session:" + credential, 120) && allowed;
        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"message\":\"Demasiados intentos\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
