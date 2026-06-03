package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 批量操作结果
 */
@Schema(description = "批量操作结果")
public class BatchResult {

    @Schema(description = "成功数量")
    private int successCount;
    @Schema(description = "失败数量")
    private int failureCount;

    public BatchResult() {
    }

    public BatchResult(int successCount, int failureCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
}
