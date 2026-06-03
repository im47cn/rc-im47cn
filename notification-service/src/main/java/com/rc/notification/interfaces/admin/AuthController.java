package com.rc.notification.interfaces.admin;

import com.rc.notification.interfaces.admin.dto.LoginRequest;
import com.rc.notification.interfaces.admin.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台鉴权控制器
 * <p>
 * MVP 阶段支持硬编码账号，可通过 Spring 外部化配置覆盖。
 * 后续可替换为企业 SSO/LDAP。
 */
@Tag(name = "鉴权管理", description = "管理后台鉴权控制器")
@RestController
@RequestMapping("/api/v1/admin")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** Session 中标记已认证用户的属性键 */
    public static final String SESSION_ATTR_USER = "ADMIN_USER";

    @Value("${admin.username:admin}")
    private String configuredUsername;

    @Value("${admin.password:admin}")
    private String configuredPassword;

    /**
     * 管理员登录
     * <p>
     * 校验用户名密码，成功后写入 HttpSession。
     */
    @Operation(summary = "管理员登录", description = "校验用户名密码，成功后写入 HttpSession")
    @ApiResponse(responseCode = "200", description = "登录结果")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpServletRequest) {
        String username = request.getUsername();
        if (configuredUsername.equals(username)
                && configuredPassword.equals(request.getPassword())) {
            // Session Fixation 防护：先销毁旧 Session，再创建新 Session
            HttpSession oldSession = httpServletRequest.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession newSession = httpServletRequest.getSession(true);
            newSession.setAttribute(SESSION_ATTR_USER, username);
            log.info("[AUTH] Login success: user={}, ip={}", username, httpServletRequest.getRemoteAddr());
            return ResponseEntity.ok(LoginResponse.ok());
        }
        log.warn("[AUTH] Login failed: user={}, ip={}", username, httpServletRequest.getRemoteAddr());
        return ResponseEntity.ok(LoginResponse.fail("用户名或密码错误"));
    }

    /**
     * 管理员登出
     * <p>
     * 清除 Session。
     */
    @Operation(summary = "管理员登出", description = "清除 Session")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}
