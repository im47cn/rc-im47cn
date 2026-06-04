package com.rc.notification.interfaces.api;

import com.rc.notification.domain.publisher.Publisher;
import com.rc.notification.domain.publisher.PublisherRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * v2 API 发布方鉴权过滤器
 * 校验 X-Publisher-Key header，将 publisherCode 注入 request attribute
 */
public class PublisherAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PublisherAuthFilter.class);
    public static final String PUBLISHER_CODE_ATTR = "publisherCode";
    private static final String API_KEY_HEADER = "X-Publisher-Key";

    private final PublisherRepository publisherRepository;

    public PublisherAuthFilter(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        // Only filter v2 API paths
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v2/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"REJECTED\",\"message\":\"Missing X-Publisher-Key header\"}");
            return;
        }

        Publisher publisher = publisherRepository.findByApiKey(apiKey);
        if (publisher == null || publisher.getStatus() == null || publisher.getStatus() != 1) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":\"REJECTED\",\"message\":\"Invalid or disabled publisher key\"}");
            return;
        }

        request.setAttribute(PUBLISHER_CODE_ATTR, publisher.getPublisherCode());
        filterChain.doFilter(request, response);
    }
}
