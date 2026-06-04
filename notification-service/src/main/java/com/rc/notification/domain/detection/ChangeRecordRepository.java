package com.rc.notification.domain.detection;

import java.util.List;

/**
 * 变更记录 Repository 接口
 */
public interface ChangeRecordRepository {

    ChangeRecord save(ChangeRecord record);

    List<ChangeRecord> findByEventTypeCode(String eventTypeCode);

    List<ChangeRecord> findByStatus(String status);

    ChangeRecord findById(Long id);

    ChangeRecord update(ChangeRecord record);
}
