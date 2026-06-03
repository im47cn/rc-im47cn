package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单表达式仿真结果
 */
@Schema(description = "单表达式仿真结果")
public class SimulationResultDto {

    @Schema(description = "是否成功")
    private boolean success;
    @Schema(description = "转换结果")
    private String transformedResult;
    @Schema(description = "错误信息")
    private String error;
    @Schema(description = "错误偏移量，-1 表示无错误")
    private int errorOffset = -1;

    public static SimulationResultDto ok(String result) {
        SimulationResultDto dto = new SimulationResultDto();
        dto.setSuccess(true);
        dto.setTransformedResult(result);
        return dto;
    }

    public static SimulationResultDto fail(String error, int offset) {
        SimulationResultDto dto = new SimulationResultDto();
        dto.setSuccess(false);
        dto.setError(error);
        dto.setErrorOffset(offset);
        return dto;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getTransformedResult() { return transformedResult; }
    public void setTransformedResult(String transformedResult) { this.transformedResult = transformedResult; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getErrorOffset() { return errorOffset; }
    public void setErrorOffset(int errorOffset) { this.errorOffset = errorOffset; }
}
