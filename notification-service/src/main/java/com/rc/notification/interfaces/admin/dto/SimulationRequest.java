package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 单表达式仿真请求
 */
@Schema(description = "单表达式仿真请求")
public class SimulationRequest {

    @Schema(description = "JSONata 表达式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "JSONata 表达式不能为空")
    private String jsonataExpression;

    @Schema(description = "模拟输入上下文", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模拟输入上下文不能为空")
    private Map<String, Object> mockInputContext;

    public String getJsonataExpression() { return jsonataExpression; }
    public void setJsonataExpression(String jsonataExpression) { this.jsonataExpression = jsonataExpression; }

    public Map<String, Object> getMockInputContext() { return mockInputContext; }
    public void setMockInputContext(Map<String, Object> mockInputContext) { this.mockInputContext = mockInputContext; }
}
