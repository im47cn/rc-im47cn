package com.rc.notification.domain.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * 供应商凭证安全保险柜
 * <p>
 * 统一管理供应商敏感凭证的加解密生命周期。
 * 使用 AES-256-GCM 算法，每次加密生成随机 IV，确保密文不可预测。
 * <p>
 * 加密主密钥（Master Key）通过 Spring 外部化配置注入，
 * 严禁硬编码或提交至版本控制。
 */
@Component
public class CredentialVault {

    private static final Logger log = LoggerFactory.getLogger(CredentialVault.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String DEFAULT_UNSAFE_KEY = "CHANGE_ME_IN_PRODUCTION";

    @Value("${credential.master-key}")
    private String masterKeyBase64;

    private final ObjectMapper objectMapper;
    private SecretKey secretKey;

    public CredentialVault(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()
                || DEFAULT_UNSAFE_KEY.equals(masterKeyBase64)) {
            log.warn("凭证主密钥未配置或使用默认值，生产环境必须通过 CREDENTIAL_MASTER_KEY 环境变量注入安全密钥！");
        }
        // 将 Master Key 转为 AES-256 密钥（取前32字节或补齐）
        byte[] keyBytes = normalizeKeyBytes(masterKeyBase64);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 解密供应商凭证密文，返回明文 KV 结构
     *
     * @param encryptedCredentials Base64 编码的密文（格式: IV + CipherText + GCM Tag）
     * @return 明文 KV 结构
     * @throws IllegalArgumentException 密文格式错误或 GCM 认证失败
     */
    public Map<String, Object> decrypt(String encryptedCredentials) {
        if (encryptedCredentials == null || encryptedCredentials.isBlank()) {
            return Map.of();
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedCredentials);

            // 前 12 字节为 IV，其余为密文 + GCM Tag
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherText);
            String plainJson = new String(plainBytes);

            return objectMapper.readValue(plainJson, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("凭证解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加密供应商凭证明文，供管理后台写入时调用
     *
     * @param plainCredentials 明文 KV 结构
     * @return Base64 编码的密文
     */
    public String encrypt(Map<String, Object> plainCredentials) {
        if (plainCredentials == null || plainCredentials.isEmpty()) {
            return null;
        }

        try {
            String plainJson = objectMapper.writeValueAsString(plainCredentials);
            byte[] plainBytes = plainJson.getBytes();

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] cipherBytes = cipher.doFinal(plainBytes);

            // 拼接 IV + CipherText + GCM Tag
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("凭证序列化失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("凭证加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 Master Key 字符串标准化为 32 字节 AES-256 密钥
     * <p>
     * 优先尝试 Base64 解码，解码失败则使用 UTF-8 字节。
     * 截取前 32 字节或用 0 补齐至 32 字节。
     */
    private byte[] normalizeKeyBytes(String keyStr) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(keyStr);
        } catch (IllegalArgumentException e) {
            // 非 Base64 格式，直接使用 UTF-8 字节
            raw = keyStr.getBytes();
        }
        byte[] keyBytes = new byte[32];
        System.arraycopy(raw, 0, keyBytes, 0, Math.min(raw.length, 32));
        return keyBytes;
    }
}
