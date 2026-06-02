package com.rc.notification.application.event;

import com.rc.notification.infrastructure.persistence.entity.SupplierConfigEntity;
import org.springframework.context.ApplicationEvent;

/**
 * 供应商配置就绪领域事件
 * <p>
 * 当新增或变更的供应商配置被缓存加载后触发，
 * SupplierWorkerManager 监听此事件动态拉起/重载 Worker 线程
 */
public class SupplierConfigActivatedEvent extends ApplicationEvent {

    private final SupplierConfigEntity config;

    public SupplierConfigActivatedEvent(Object source, SupplierConfigEntity config) {
        super(source);
        this.config = config;
    }

    public SupplierConfigEntity getConfig() {
        return config;
    }
}
