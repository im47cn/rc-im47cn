package com.rc.notification.interfaces.admin.dto;

/**
 * 批量操作结果
 */
public class BatchResult {

    private int successCount;
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
