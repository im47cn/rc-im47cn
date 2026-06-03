package com.rc.notification.interfaces.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 通用分页响应封装
 */
@Schema(description = "通用分页响应")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> records;
    @Schema(description = "总记录数", example = "100")
    private long total;
    @Schema(description = "当前页码", example = "1")
    private int page;
    @Schema(description = "每页数量", example = "20")
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
