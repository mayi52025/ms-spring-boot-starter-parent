package com.ms.middleware.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全工具类测试
 */
class SecurityUtilsTest {

    @Test
    void testEncryptAES() {
        String original = "Hello, World!";
        String key = "test-secret-key";
        
        String encrypted = SecurityUtils.encryptAES(original, key);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);
    }

    @Test
    void testDecryptAES() {
        String original = "Hello, World!";
        String key = "test-secret-key";
        
        String encrypted = SecurityUtils.encryptAES(original, key);
        String decrypted = SecurityUtils.decryptAES(encrypted, key);
        assertEquals(original, decrypted);
    }

    @Test
    void testMd5() {
        String data = "test-data";
        String md5 = SecurityUtils.md5(data);
        assertNotNull(md5);
        assertEquals(32, md5.length()); // MD5 长度为 32 位
    }

    @Test
    void testGenerateSecureCacheKey() {
        String key = "test-cache-key";
        String prefix = "ms:cache:";
        
        String secureKey = SecurityUtils.generateSecureCacheKey(key, prefix);
        assertNotNull(secureKey);
        assertTrue(secureKey.startsWith(prefix));
        assertNotEquals(key, secureKey);
    }

    @Test
    void testGenerateSecureLockKey() {
        String key = "test-lock-key";
        String prefix = "ms:lock:";
        
        String secureKey = SecurityUtils.generateSecureLockKey(key, prefix);
        assertNotNull(secureKey);
        assertTrue(secureKey.startsWith(prefix));
        assertNotEquals(key, secureKey);
    }

    @Test
    void testCheckPermission() {
        // 测试有权限的情况
        assertTrue(SecurityUtils.checkPermission("READ", "READ,WRITE"));
        
        // 测试无权限的情况
        assertFalse(SecurityUtils.checkPermission("ADMIN", "READ,WRITE"));
        
        // 测试不需要权限的情况
        assertTrue(SecurityUtils.checkPermission("", "READ,WRITE"));
        
        // 测试用户无权限的情况
        assertFalse(SecurityUtils.checkPermission("READ", ""));
    }
}
