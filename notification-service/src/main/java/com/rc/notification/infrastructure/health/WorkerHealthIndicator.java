package com.rc.notification.infrastructure.health;

import com.rc.notification.application.worker.SupplierWorkerManager;
import com.rc.notification.domain.config.SupplierConfigDomainService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Worker 线程健康检查
 * <p>
 * 用于 Readiness Probe 组合检查，至少一个 Worker 线程活跃时返回 UP
 */
@Component("workerHealthCheck")
public class WorkerHealthIndicator implements HealthIndicator {

    private final SupplierWorkerManager workerManager;
    private final SupplierConfigDomainService configDomainService;

    public WorkerHealthIndicator(SupplierWorkerManager workerManager,
                                 SupplierConfigDomainService configDomainService) {
        this.workerManager = workerManager;
        this.configDomainService = configDomainService;
    }

    @Override
    public Health health() {
        int activeCount = workerManager.getActiveWorkerCount();
        int maxThreads = workerManager.getMaxWorkerThreads();

        if (activeCount > 0) {
            return Health.up()
                    .withDetail("activeWorkers", activeCount)
                    .withDetail("maxWorkerThreads", maxThreads)
                    .build();
        }

        // workerManager 运行中但无活跃 Worker，检查是否有启用的供应商
        if (workerManager.isRunning() && activeCount == 0) {
            boolean hasEnabledSuppliers = !configDomainService.getAllActive().isEmpty();
            if (hasEnabledSuppliers) {
                return Health.down()
                        .withDetail("reason", "No active workers despite enabled suppliers")
                        .withDetail("activeWorkers", 0)
                        .withDetail("maxWorkerThreads", maxThreads)
                        .build();
            }
            return Health.up()
                    .withDetail("activeWorkers", 0)
                    .withDetail("note", "no active supplier configs")
                    .withDetail("maxWorkerThreads", maxThreads)
                    .build();
        }

        return Health.up()
                .withDetail("activeWorkers", activeCount)
                .withDetail("maxWorkerThreads", maxThreads)
                .build();
    }
}
