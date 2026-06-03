package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理后台登录响应
 */
@Schema(description = "管理后台登录响应")
public class LoginResponse {

    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "响应消息")
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static LoginResponse ok() {
        return new LoginResponse(true, "登录成功");
    }

    public static LoginResponse fail(String message) {
        return new LoginResponse(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
