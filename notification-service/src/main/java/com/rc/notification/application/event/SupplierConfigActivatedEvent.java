package com.rc.notification.application.event;

import com.rc.notification.domain.config.SupplierConfig;
import org.springframework.context.ApplicationEvent;

/**
 * 供应商配置启用领域事件
 * <p>
 * 当供应商被启用或配置变更（且仍为启用状态）时触发，
 * SupplierWorkerManager 监听此事件动态拉起/重载 Worker 线程
 */
public class SupplierConfigActivatedEvent extends ApplicationEvent {

    private final SupplierConfig config;

    public SupplierConfigActivatedEvent(Object source, SupplierConfig config) {
        super(source);
        this.config = config;
    }

    public SupplierConfig getConfig() {
        return config;
    }
}
