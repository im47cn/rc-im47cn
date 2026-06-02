package com.rc.notification.interfaces.admin.dto;

/**
 * 管理后台登录响应
 */
public class LoginResponse {

    private boolean success;
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
