package com.apiturnos.autogestion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.Principal;

/** Acceso sin autenticación exclusivamente para el panel de enlaces en desarrollo. */
@Component
@Profile("dev & !prod & !production")
public class AutenticacionProfesionalDesarrolloFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.matches("/api/profesionales/[1-9][0-9]*/enlace-autogestion")
                || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String id = path.split("/")[3];
        if (request.getUserPrincipal() != null) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public Principal getUserPrincipal() { return () -> id; }
            @Override public String getRemoteUser() { return id; }
            @Override public boolean isUserInRole(String role) { return "PROFESIONAL".equals(role); }
        }, response);
    }
}
