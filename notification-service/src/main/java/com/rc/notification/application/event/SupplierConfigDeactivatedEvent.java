package com.rc.notification.application.event;

import org.springframework.context.ApplicationEvent;

/**
 * 供应商配置停用领域事件
 * <p>
 * 当供应商被禁用时触发，
 * SupplierWorkerManager 监听此事件停止对应的 Worker 线程
 */
public class SupplierConfigDeactivatedEvent extends ApplicationEvent {

    private final String supplierCode;

    public SupplierConfigDeactivatedEvent(Object source, String supplierCode) {
        super(source);
        this.supplierCode = supplierCode;
    }

    public String getSupplierCode() {
        return supplierCode;
    }
}
