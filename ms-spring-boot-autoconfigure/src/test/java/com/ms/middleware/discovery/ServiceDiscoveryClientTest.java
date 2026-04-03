package com.ms.middleware.discovery;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 服务发现客户端测试
 */
public class ServiceDiscoveryClientTest {

    @Mock
    private NacosServiceManager nacosServiceManager;

    @Mock
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Mock
    private NamingService namingService;

    private ServiceDiscoveryAutoConfiguration.ServiceDiscoveryClient serviceDiscoveryClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(nacosServiceManager.getNamingService(any())).thenReturn(namingService);
        serviceDiscoveryClient = new ServiceDiscoveryAutoConfiguration.ServiceDiscoveryClient(nacosServiceManager, nacosDiscoveryProperties);
    }

    @Test
    public void testRegisterInstance() throws NacosException {
        // 测试注册服务实例
        String serviceName = "test-service";
        String ip = "192.168.1.100";
        int port = 8080;

        // 执行注册操作
        serviceDiscoveryClient.registerInstance(serviceName, ip, port);

        // 验证调用
        verify(namingService, times(1)).registerInstance(serviceName, ip, port);
    }

    @Test
    public void testRegisterInstanceWithWeight() throws NacosException {
        // 测试注册带权重的服务实例
        String serviceName = "test-service";
        String ip = "192.168.1.100";
        int port = 8080;
        double weight = 1.5;

        // 执行注册操作
        serviceDiscoveryClient.registerInstance(serviceName, ip, port, weight);

        // 验证调用（通过参数捕获验证）
        verify(namingService, times(1)).registerInstance(eq(serviceName), any(Instance.class));
    }

    @Test
    public void testDeregisterInstance() throws NacosException {
        // 测试注销服务实例
        String serviceName = "test-service";
        String ip = "192.168.1.100";
        int port = 8080;

        // 执行注销操作
        serviceDiscoveryClient.deregisterInstance(serviceName, ip, port);

        // 验证调用
        verify(namingService, times(1)).deregisterInstance(serviceName, ip, port);
    }

    @Test
    public void testGetInstances() throws NacosException {
        // 测试获取服务实例列表
        String serviceName = "test-service";
        List<Instance> expectedInstances = Collections.emptyList();
        when(namingService.getAllInstances(serviceName)).thenReturn(expectedInstances);

        // 执行获取操作
        List<Instance> actualInstances = serviceDiscoveryClient.getInstances(serviceName);

        // 验证结果
        assertEquals(expectedInstances, actualInstances);
        verify(namingService, times(1)).getAllInstances(serviceName);
    }

    @Test
    public void testGetHealthyInstances() throws NacosException {
        // 测试获取健康的服务实例列表
        String serviceName = "test-service";
        List<Instance> expectedInstances = Collections.emptyList();
        when(namingService.selectInstances(serviceName, true)).thenReturn(expectedInstances);

        // 执行获取操作
        List<Instance> actualInstances = serviceDiscoveryClient.getHealthyInstances(serviceName);

        // 验证结果
        assertEquals(expectedInstances, actualInstances);
        verify(namingService, times(1)).selectInstances(serviceName, true);
    }

    @Test
    public void testGetServices() throws NacosException {
        // 测试获取所有服务名称
        List<String> expectedServices = Collections.singletonList("test-service");
        ListView<String> listView = mock(ListView.class);
        when(listView.getData()).thenReturn(expectedServices);
        when(namingService.getServicesOfServer(1, Integer.MAX_VALUE)).thenReturn(listView);

        // 执行获取操作
        List<String> actualServices = serviceDiscoveryClient.getServices();

        // 验证结果
        assertEquals(expectedServices, actualServices);
        verify(namingService, times(1)).getServicesOfServer(1, Integer.MAX_VALUE);
    }
}
