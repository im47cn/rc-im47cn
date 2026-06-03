package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 管理后台登录请求
 */
@Schema(description = "管理后台登录请求")
public class LoginRequest {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin")
    @NotBlank(message = "密码不能为空")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
