package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量重试请求
 */
@Schema(description = "批量重试请求")
public class BatchRetryRequest {

    /** 按 ID 列表批量重试 */
    @Schema(description = "按 ID 列表批量重试（与 supplierCode 二选一）")
    private List<Long> ids;

    /** 按供应商编码批量重试（与 ids 二选一） */
    @Schema(description = "按供应商编码批量重试（与 ids 二选一）")
    private String supplierCode;

    /** 操作人 */
    @Schema(description = "操作人")
    private String operator;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
