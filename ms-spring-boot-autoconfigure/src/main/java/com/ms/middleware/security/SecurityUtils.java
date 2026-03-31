package com.ms.middleware.security;

import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 安全工具类，提供加密、解密、权限验证等功能
 */
public class SecurityUtils {

    /**
     * AES 加密
     * @param data 待加密数据
     * @param key 密钥
     * @return 加密后的数据
     */
    public static String encryptAES(String data, String key) {
        try {
            // 使用密钥的前16位作为AES密钥
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] key16 = new byte[16];
            System.arraycopy(keyBytes, 0, key16, 0, Math.min(keyBytes.length, 16));
            
            SecretKeySpec secretKey = new SecretKeySpec(key16, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * AES 解密
     * @param encryptedData 加密后的数据
     * @param key 密钥
     * @return 解密后的数据
     */
    public static String decryptAES(String encryptedData, String key) {
        try {
            // 使用密钥的前16位作为AES密钥
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] key16 = new byte[16];
            System.arraycopy(keyBytes, 0, key16, 0, Math.min(keyBytes.length, 16));
            
            SecretKeySpec secretKey = new SecretKeySpec(key16, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    /**
     * MD5 哈希
     * @param data 待哈希数据
     * @return MD5 哈希值
     */
    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 hashing failed", e);
        }
    }

    /**
     * 生成安全的缓存键
     * @param key 原始键
     * @param prefix 前缀
     * @return 安全的缓存键
     */
    public static String generateSecureCacheKey(String key, String prefix) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        return prefix + md5(key);
    }

    /**
     * 生成安全的锁键
     * @param key 原始键
     * @param prefix 前缀
     * @return 安全的锁键
     */
    public static String generateSecureLockKey(String key, String prefix) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        return prefix + md5(key);
    }

    /**
     * 验证权限
     * @param requiredPermission 所需权限
     * @param userPermissions 用户权限
     * @return 是否有权限
     */
    public static boolean checkPermission(String requiredPermission, String userPermissions) {
        if (!StringUtils.hasText(requiredPermission)) {
            return true; // 不需要权限
        }
        if (!StringUtils.hasText(userPermissions)) {
            return false; // 用户没有权限
        }
        return userPermissions.contains(requiredPermission);
    }
}
