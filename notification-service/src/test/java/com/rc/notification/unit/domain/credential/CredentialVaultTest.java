package com.rc.notification.unit.domain.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc.notification.domain.credential.CredentialVault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CredentialVault 单元测试
 * <p>
 * 验证 AES-256-GCM 加解密的正确性、边界处理和篡改检测
 */
class CredentialVaultTest {

    private CredentialVault vault;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Environment environment = mock(Environment.class);
        when(environment.matchesProfiles("dev", "test")).thenReturn(true);

        vault = new CredentialVault(objectMapper, environment);

        // 注入一个 32 字节的 Base64 编码密钥
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) (i + 1);
        }
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        setField(vault, "masterKeyBase64", base64Key);

        vault.init();
    }

    @Test
    @DisplayName("加密后解密应返回原始凭证")
    void encryptThenDecrypt_roundtrip() {
        Map<String, Object> credentials = Map.of(
                "apiKey", "sk-abc123",
                "secret", "my-secret-value"
        );

        String encrypted = vault.encrypt(credentials);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());

        Map<String, Object> decrypted = vault.decrypt(encrypted);
        assertEquals("sk-abc123", decrypted.get("apiKey"));
        assertEquals("my-secret-value", decrypted.get("secret"));
    }

    @Test
    @DisplayName("相同明文多次加密应生成不同密文(随机IV)")
    void encrypt_sameInput_differentCiphertext() {
        Map<String, Object> credentials = Map.of("key", "value");

        String encrypted1 = vault.encrypt(credentials);
        String encrypted2 = vault.encrypt(credentials);

        assertNotEquals(encrypted1, encrypted2, "每次加密应因随机 IV 生成不同密文");

        // 但解密结果应相同
        assertEquals(vault.decrypt(encrypted1), vault.decrypt(encrypted2));
    }

    @Test
    @DisplayName("UTF-8 密钥格式应正常工作")
    void utf8Key_encryptDecrypt_works() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Environment environment = mock(Environment.class);
        when(environment.matchesProfiles("dev", "test")).thenReturn(true);

        CredentialVault utf8Vault = new CredentialVault(objectMapper, environment);
        // 使用非 Base64 字符串作为密钥（UTF-8模式），32字符以上
        String utf8Key = "ThisIsA32ByteKeyForAES256Testing";
        setField(utf8Vault, "masterKeyBase64", utf8Key);
        utf8Vault.init();

        Map<String, Object> credentials = Map.of("token", "abc123");
        String encrypted = utf8Vault.encrypt(credentials);
        Map<String, Object> decrypted = utf8Vault.decrypt(encrypted);

        assertEquals("abc123", decrypted.get("token"));
    }

    @Test
    @DisplayName("篡改密文应抛出异常")
    void tampered_ciphertext_throwsException() {
        Map<String, Object> credentials = Map.of("key", "value");
        String encrypted = vault.encrypt(credentials);

        // 篡改密文中间的一个字节
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        decoded[decoded.length / 2] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(decoded);

        assertThrows(IllegalArgumentException.class, () -> vault.decrypt(tampered));
    }

    @Test
    @DisplayName("空凭证加密应返回 null")
    void encrypt_emptyCredentials_returnsNull() {
        assertNull(vault.encrypt(null));
        assertNull(vault.encrypt(Map.of()));
    }

    @Test
    @DisplayName("空密文解密应返回空 Map")
    void decrypt_emptyOrNull_returnsEmptyMap() {
        assertEquals(Map.of(), vault.decrypt(null));
        assertEquals(Map.of(), vault.decrypt(""));
        assertEquals(Map.of(), vault.decrypt("   "));
    }

    @Test
    @DisplayName("非法 Base64 密文应抛出异常")
    void decrypt_invalidBase64_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> vault.decrypt("not-valid-base64!!!"));
    }

    @Test
    @DisplayName("复杂嵌套凭证应正确往返")
    void encryptDecrypt_nestedCredentials() {
        Map<String, Object> credentials = Map.of(
                "oauth", Map.of("clientId", "id-123", "clientSecret", "secret-456"),
                "endpoint", "https://api.example.com"
        );

        String encrypted = vault.encrypt(credentials);
        Map<String, Object> decrypted = vault.decrypt(encrypted);

        assertEquals("https://api.example.com", decrypted.get("endpoint"));
        @SuppressWarnings("unchecked")
        Map<String, Object> oauth = (Map<String, Object>) decrypted.get("oauth");
        assertEquals("id-123", oauth.get("clientId"));
        assertEquals("secret-456", oauth.get("clientSecret"));
    }

    /**
     * 通过反射设置私有字段
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
