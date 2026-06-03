package com.rc.notification.unit.application;

import com.rc.notification.application.admin.SupplierConfigAdminService;
import com.rc.notification.domain.config.SupplierConfig;
import com.rc.notification.domain.config.SupplierConfigRepository;
import com.rc.notification.domain.credential.CredentialVault;
import com.rc.notification.infrastructure.cache.ConfigEvictionListener;
import com.rc.notification.interfaces.admin.dto.SupplierConfigCreateRequest;
import com.rc.notification.interfaces.admin.dto.SupplierConfigDto;
import com.rc.notification.interfaces.admin.dto.SupplierConfigUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SupplierConfigAdminService 单元测试
 * <p>
 * 验证新增/修改供应商的业务逻辑：凭证加密、缓存驱逐、唯一性校验
 */
@ExtendWith(MockitoExtension.class)
class SupplierConfigAdminServiceTest {

    @Mock
    private SupplierConfigRepository supplierConfigRepository;

    @Mock
    private CredentialVault credentialVault;

    @Mock
    private ConfigEvictionListener configEvictionListener;

    private SupplierConfigAdminService service;

    @BeforeEach
    void setUp() {
        service = new SupplierConfigAdminService(
                supplierConfigRepository, credentialVault, configEvictionListener);
    }

    @Test
    @DisplayName("新增供应商应加密凭证并广播 CREATE 事件")
    void createSupplier_encryptsCredentials_andPublishesEviction() {
        SupplierConfigCreateRequest request = new SupplierConfigCreateRequest();
        request.setSupplierCode("WECHAT");
        request.setSupplierName("微信");
        request.setBaseUrl("https://api.weixin.qq.com");
        request.setBodyTemplate("{}");
        request.setStatus(1);
        request.setCredentials(Map.of("appId", "wx123", "appSecret", "secret456"));

        when(supplierConfigRepository.existsBySupplierCode("WECHAT")).thenReturn(false);
        when(credentialVault.encrypt(request.getCredentials())).thenReturn("encrypted-data");
        when(supplierConfigRepository.save(any(SupplierConfig.class))).thenAnswer(inv -> {
            SupplierConfig config = inv.getArgument(0);
            config.setId(1L);
            return config;
        });

        SupplierConfigDto result = service.createSupplier(request);

        assertNotNull(result);
        assertEquals("WECHAT", result.getSupplierCode());

        // 验证凭证被加密
        verify(credentialVault).encrypt(request.getCredentials());

        // 验证 save 时 credentialsEncrypted 已设置
        ArgumentCaptor<SupplierConfig> captor = ArgumentCaptor.forClass(SupplierConfig.class);
        verify(supplierConfigRepository).save(captor.capture());
        assertEquals("encrypted-data", captor.getValue().getCredentialsEncrypted());

        // 验证广播 CREATE 事件
        verify(configEvictionListener).publishEviction("WECHAT", "CREATE");
    }

    @Test
    @DisplayName("新增供应商无凭证时不调用加密")
    void createSupplier_noCredentials_skipsEncrypt() {
        SupplierConfigCreateRequest request = new SupplierConfigCreateRequest();
        request.setSupplierCode("SMS");
        request.setSupplierName("短信服务");
        request.setBaseUrl("https://sms.example.com");
        request.setBodyTemplate("{}");
        request.setStatus(1);
        request.setCredentials(null);

        when(supplierConfigRepository.existsBySupplierCode("SMS")).thenReturn(false);
        when(supplierConfigRepository.save(any(SupplierConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createSupplier(request);

        verify(credentialVault, never()).encrypt(any());
    }

    @Test
    @DisplayName("重复 supplier_code 应抛出异常")
    void createSupplier_duplicateCode_throwsException() {
        SupplierConfigCreateRequest request = new SupplierConfigCreateRequest();
        request.setSupplierCode("WECHAT");
        request.setSupplierName("微信");
        request.setBaseUrl("https://api.weixin.qq.com");
        request.setBodyTemplate("{}");
        request.setStatus(1);

        when(supplierConfigRepository.existsBySupplierCode("WECHAT")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createSupplier(request)
        );
        assertTrue(ex.getMessage().contains("WECHAT"));

        verify(supplierConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("修改供应商配置应触发缓存驱逐")
    void updateSupplier_triggersEviction() {
        SupplierConfig existing = new SupplierConfig();
        existing.setId(1L);
        existing.setSupplierCode("WECHAT");
        existing.setSupplierName("微信旧名");
        existing.setBaseUrl("https://old.api.com");
        existing.setBodyTemplate("{}");

        when(supplierConfigRepository.findById(1L)).thenReturn(existing);
        when(supplierConfigRepository.update(any(SupplierConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierConfigUpdateRequest request = new SupplierConfigUpdateRequest();
        request.setSupplierName("微信新名");
        request.setBaseUrl("https://new.api.com");
        request.setBodyTemplate("{}");

        service.updateSupplier(1L, request);

        verify(configEvictionListener).publishEviction("WECHAT", "UPDATE");
    }

    @Test
    @DisplayName("修改供应商凭证应重新加密")
    void updateSupplier_withCredentials_reEncrypts() {
        SupplierConfig existing = new SupplierConfig();
        existing.setId(1L);
        existing.setSupplierCode("WECHAT");
        existing.setSupplierName("微信");
        existing.setBaseUrl("https://api.weixin.qq.com");
        existing.setCredentialsEncrypted("old-encrypted");

        when(supplierConfigRepository.findById(1L)).thenReturn(existing);
        when(credentialVault.encrypt(any())).thenReturn("new-encrypted");
        when(supplierConfigRepository.update(any(SupplierConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierConfigUpdateRequest request = new SupplierConfigUpdateRequest();
        request.setSupplierName("微信");
        request.setBaseUrl("https://api.weixin.qq.com");
        request.setBodyTemplate("{}");
        request.setCredentials(Map.of("newKey", "newValue"));

        service.updateSupplier(1L, request);

        verify(credentialVault).encrypt(request.getCredentials());

        ArgumentCaptor<SupplierConfig> captor = ArgumentCaptor.forClass(SupplierConfig.class);
        verify(supplierConfigRepository).update(captor.capture());
        assertEquals("new-encrypted", captor.getValue().getCredentialsEncrypted());
    }

    @Test
    @DisplayName("修改不存在的供应商应抛出异常")
    void updateSupplier_notFound_throwsException() {
        when(supplierConfigRepository.findById(999L)).thenReturn(null);

        SupplierConfigUpdateRequest request = new SupplierConfigUpdateRequest();
        request.setSupplierName("test");
        request.setBaseUrl("https://test.com");
        request.setBodyTemplate("{}");

        assertThrows(IllegalArgumentException.class,
                () -> service.updateSupplier(999L, request));
    }

    @Test
    @DisplayName("无实际变更时应跳过更新")
    void updateSupplier_noChange_skipsUpdate() {
        SupplierConfig existing = new SupplierConfig();
        existing.setId(1L);
        existing.setSupplierCode("WECHAT");
        existing.setSupplierName("微信");
        existing.setBaseUrl("https://api.weixin.qq.com");
        existing.setBodyTemplate("{}");

        when(supplierConfigRepository.findById(1L)).thenReturn(existing);

        SupplierConfigUpdateRequest request = new SupplierConfigUpdateRequest();
        request.setSupplierName("微信");
        request.setBaseUrl("https://api.weixin.qq.com");
        request.setBodyTemplate("{}");
        // credentials = null 表示保留原值

        service.updateSupplier(1L, request);

        // 无变更，不应调用 update 和 publishEviction
        verify(supplierConfigRepository, never()).update(any());
        verify(configEvictionListener, never()).publishEviction(anyString(), anyString());
    }
}
