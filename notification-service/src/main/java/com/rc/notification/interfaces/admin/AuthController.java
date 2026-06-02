package com.rc.notification.interfaces.admin;

import com.rc.notification.interfaces.admin.dto.LoginRequest;
import com.rc.notification.interfaces.admin.dto.LoginResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
@RestController
@RequestMapping("/api/v1/admin")
public class AuthController {

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
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpSession session) {
        if (configuredUsername.equals(request.getUsername())
                && configuredPassword.equals(request.getPassword())) {
            session.setAttribute(SESSION_ATTR_USER, request.getUsername());
            return ResponseEntity.ok(LoginResponse.ok());
        }
        return ResponseEntity.ok(LoginResponse.fail("用户名或密码错误"));
    }

    /**
     * 管理员登出
     * <p>
     * 清除 Session。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }
}
