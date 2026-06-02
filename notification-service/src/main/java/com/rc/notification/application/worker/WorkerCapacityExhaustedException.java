package com.rc.notification.application.worker;

/**
 * Worker 线程池容量耗尽异常
 * <p>
 * 当活跃线程总数逼近 max-worker-threads 硬上限时抛出
 */
public class WorkerCapacityExhaustedException extends RuntimeException {

    public WorkerCapacityExhaustedException(String message) {
        super(message);
    }
}
