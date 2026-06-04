package com.rc.notification.domain.detection;

import java.util.List;

/**
 * 字段指纹 Repository 接口
 */
public interface FieldFingerprintRepository {

    FieldFingerprint findByEventTypeCodeAndFieldPath(String eventTypeCode, String fieldPath);

    List<FieldFingerprint> findByEventTypeCode(String eventTypeCode);

    FieldFingerprint save(FieldFingerprint fingerprint);

    FieldFingerprint update(FieldFingerprint fingerprint);
}
