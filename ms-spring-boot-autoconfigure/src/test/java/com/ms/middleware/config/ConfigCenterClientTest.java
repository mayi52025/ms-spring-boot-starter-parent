package com.ms.middleware.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 配置中心客户端测试
 */
public class ConfigCenterClientTest {

    @Mock
    private NacosConfigManager nacosConfigManager;

    @Mock
    private NacosConfigProperties nacosConfigProperties;

    @Mock
    private ConfigService configService;

    private ConfigCenterAutoConfiguration.ConfigCenterClient configCenterClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(nacosConfigManager.getConfigService()).thenReturn(configService);
        when(nacosConfigProperties.getGroup()).thenReturn("DEFAULT_GROUP");
        configCenterClient = new ConfigCenterAutoConfiguration.ConfigCenterClient(nacosConfigManager, nacosConfigProperties);
    }

    @Test
    public void testGetConfig() throws NacosException {
        // 测试获取配置
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        String expectedConfig = "key: value";
        when(configService.getConfig(dataId, group, 5000)).thenReturn(expectedConfig);

        // 执行获取操作
        String actualConfig = configCenterClient.getConfig(dataId);

        // 验证结果
        assertEquals(expectedConfig, actualConfig);
        verify(configService, times(1)).getConfig(dataId, group, 5000);
    }

    @Test
    public void testGetConfigWithCustomParams() throws NacosException {
        // 测试获取配置（自定义参数）
        String dataId = "test-config";
        String group = "CUSTOM_GROUP";
        long timeout = 10000;
        String expectedConfig = "key: custom_value";
        when(configService.getConfig(dataId, group, timeout)).thenReturn(expectedConfig);

        // 执行获取操作
        String actualConfig = configCenterClient.getConfig(dataId, group, timeout);

        // 验证结果
        assertEquals(expectedConfig, actualConfig);
        verify(configService, times(1)).getConfig(dataId, group, timeout);
    }

    @Test
    public void testPublishConfig() throws NacosException {
        // 测试发布配置
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        String content = "key: new_value";
        when(configService.publishConfig(dataId, group, content)).thenReturn(true);

        // 执行发布操作
        boolean success = configCenterClient.publishConfig(dataId, content);

        // 验证结果
        assertTrue(success);
        verify(configService, times(1)).publishConfig(dataId, group, content);
    }

    @Test
    public void testPublishConfigWithCustomGroup() throws NacosException {
        // 测试发布配置（自定义分组）
        String dataId = "test-config";
        String group = "CUSTOM_GROUP";
        String content = "key: custom_value";
        when(configService.publishConfig(dataId, group, content)).thenReturn(true);

        // 执行发布操作
        boolean success = configCenterClient.publishConfig(dataId, group, content);

        // 验证结果
        assertTrue(success);
        verify(configService, times(1)).publishConfig(dataId, group, content);
    }

    @Test
    public void testRemoveConfig() throws NacosException {
        // 测试删除配置
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        when(configService.removeConfig(dataId, group)).thenReturn(true);

        // 执行删除操作
        boolean success = configCenterClient.removeConfig(dataId);

        // 验证结果
        assertTrue(success);
        verify(configService, times(1)).removeConfig(dataId, group);
    }

    @Test
    public void testRemoveConfigWithCustomGroup() throws NacosException {
        // 测试删除配置（自定义分组）
        String dataId = "test-config";
        String group = "CUSTOM_GROUP";
        when(configService.removeConfig(dataId, group)).thenReturn(true);

        // 执行删除操作
        boolean success = configCenterClient.removeConfig(dataId, group);

        // 验证结果
        assertTrue(success);
        verify(configService, times(1)).removeConfig(dataId, group);
    }

    @Test
    public void testAddListener() throws NacosException {
        // 测试添加配置监听器
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";

        // 创建监听器
        ConfigCenterAutoConfiguration.ConfigChangeListener listener = configInfo -> {
            // 配置变更回调
        };

        // 执行添加监听器操作
        configCenterClient.addListener(dataId, listener);

        // 验证调用
        ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(configService, times(1)).addListener(eq(dataId), eq(group), listenerCaptor.capture());

        // 验证监听器
        Listener capturedListener = listenerCaptor.getValue();
        assertNotNull(capturedListener);
        assertNull(capturedListener.getExecutor());
    }

    @Test
    public void testAddListenerWithCustomGroup() throws NacosException {
        // 测试添加配置监听器（自定义分组）
        String dataId = "test-config";
        String group = "CUSTOM_GROUP";

        // 创建监听器
        ConfigCenterAutoConfiguration.ConfigChangeListener listener = configInfo -> {
            // 配置变更回调
        };

        // 执行添加监听器操作
        configCenterClient.addListener(dataId, group, listener);

        // 验证调用
        ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(configService, times(1)).addListener(eq(dataId), eq(group), listenerCaptor.capture());

        // 验证监听器
        Listener capturedListener = listenerCaptor.getValue();
        assertNotNull(capturedListener);
    }

    @Test
    public void testListenerCallback() throws NacosException {
        // 测试监听器回调
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        String newConfig = "key: updated_value";

        // 创建监听器
        final String[] receivedConfig = new String[1];
        ConfigCenterAutoConfiguration.ConfigChangeListener listener = configInfo -> {
            receivedConfig[0] = configInfo;
        };

        // 添加监听器
        configCenterClient.addListener(dataId, listener);

        // 获取捕获的监听器
        ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(configService).addListener(eq(dataId), eq(group), listenerCaptor.capture());
        Listener capturedListener = listenerCaptor.getValue();

        // 模拟配置变更
        capturedListener.receiveConfigInfo(newConfig);

        // 验证回调
        assertEquals(newConfig, receivedConfig[0]);
    }

    @Test
    public void testGetConfigHistory() {
        // 测试获取配置历史
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        int pageNo = 1;
        int pageSize = 10;

        // 执行获取历史操作
        String history = configCenterClient.getConfigHistory(dataId, group, pageNo, pageSize);

        // 验证结果
        assertNotNull(history);
        assertTrue(history.contains(dataId));
        assertTrue(history.contains(group));
    }
}
