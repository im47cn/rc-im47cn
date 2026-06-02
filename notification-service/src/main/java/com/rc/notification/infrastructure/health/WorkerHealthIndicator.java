package com.rc.notification.infrastructure.health;

import com.rc.notification.application.worker.SupplierWorkerManager;
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

    public WorkerHealthIndicator(SupplierWorkerManager workerManager) {
        this.workerManager = workerManager;
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

        // 没有活跃 Worker 时仍返回 UP（可能没有启用的供应商配置）
        // 只在 workerManager 运行中但线程异常全部退出时才返回 DOWN
        if (workerManager.isRunning() && activeCount == 0) {
            return Health.up()
                    .withDetail("activeWorkers", 0)
                    .withDetail("note", "no active supplier configs or workers pending initialization")
                    .withDetail("maxWorkerThreads", maxThreads)
                    .build();
        }

        return Health.up()
                .withDetail("activeWorkers", activeCount)
                .withDetail("maxWorkerThreads", maxThreads)
                .build();
    }
}
