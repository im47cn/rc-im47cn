package com.rc.notification.interfaces.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 管理后台简易鉴权过滤器
 * <p>
 * 拦截 /api/v1/admin/** 路径（排除 /login 和 /logout），
 * 无有效 Session 返回 401 JSON 响应。
 */
@Component
public class AdminAuthFilter implements Filter {

    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";
    private static final String LOGIN_PATH = "/api/v1/admin/login";
    private static final String LOGOUT_PATH = "/api/v1/admin/logout";

    private final ObjectMapper objectMapper;

    public AdminAuthFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();
        // 标准化尾部斜杠
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 仅拦截 /api/v1/admin/** 路径
        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 排除登录和登出端点
        if (path.startsWith(LOGIN_PATH) || path.startsWith(LOGOUT_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        // 校验 Session 中是否存在已认证用户
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(AuthController.SESSION_ATTR_USER) == null) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(httpResponse.getWriter(),
                    Map.of("error", "未认证，请先登录", "code", 401));
            return;
        }

        chain.doFilter(request, response);
    }
}
