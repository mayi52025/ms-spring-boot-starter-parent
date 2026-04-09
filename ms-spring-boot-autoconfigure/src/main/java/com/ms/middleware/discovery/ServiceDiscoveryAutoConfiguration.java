package com.ms.middleware.discovery;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 服务发现与注册自动配置类
 * <p>
 * 集成Nacos实现服务发现与注册功能，支持微服务架构
 * </p>
 */
@Configuration
@EnableDiscoveryClient
@ConditionalOnProperty(prefix = "ms.middleware.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ServiceDiscoveryAutoConfiguration {

    @Autowired
    private NacosServiceManager nacosServiceManager;

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Autowired
    private Environment environment;

    /**
     * 服务发现客户端
     * <p>
     * 用于服务发现和注册操作
     * </p>
     *
     * @return 服务发现客户端
     */
    @Bean
    public ServiceDiscoveryClient serviceDiscoveryClient() {
        return new ServiceDiscoveryClient(nacosServiceManager, nacosDiscoveryProperties);
    }

    /**
     * 自动注册服务实例
     * <p>
     * 在服务启动时自动注册到Nacos
     * </p>
     */
    @PostConstruct
    public void autoRegisterService() {
        try {
            // 获取服务名称
            String serviceName = environment.getProperty("spring.application.name", "unknown-service");
            
            // 获取服务IP
            String ip = InetAddress.getLocalHost().getHostAddress();
            
            // 获取服务端口
            String portStr = environment.getProperty("server.port", "8080");
            int port = Integer.parseInt(portStr);
            
            // 获取权重
            double weight = Double.parseDouble(environment.getProperty("ms.middleware.discovery.weight", "1.0"));
            
            // 注册服务
            NamingService namingService = nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
            Instance instance = new Instance();
            instance.setServiceName(serviceName);
            instance.setIp(ip);
            instance.setPort(port);
            instance.setWeight(weight);
            instance.setHealthy(true);
            
            namingService.registerInstance(serviceName, instance);
            
            System.out.println("[ms-middleware] 服务自动注册成功: " + serviceName + " - " + ip + ":" + port);
        } catch (UnknownHostException | NacosException e) {
            System.err.println("[ms-middleware] 服务自动注册失败: " + e.getMessage());
        }
    }

    /**
     * 服务发现客户端实现类
     * <p>
     * 封装Nacos服务发现与注册的核心功能
     * </p>
     */
    public static class ServiceDiscoveryClient {

        private final NacosServiceManager nacosServiceManager;
        private final NacosDiscoveryProperties nacosDiscoveryProperties;

        public ServiceDiscoveryClient(NacosServiceManager nacosServiceManager, NacosDiscoveryProperties nacosDiscoveryProperties) {
            this.nacosServiceManager = nacosServiceManager;
            this.nacosDiscoveryProperties = nacosDiscoveryProperties;
        }

        /**
         * 获取Nacos命名服务
         *
         * @return Nacos命名服务
         */
        private NamingService getNamingService() {
            return nacosServiceManager.getNamingService(nacosDiscoveryProperties.getNacosProperties());
        }

        /**
         * 注册服务实例
         *
         * @param serviceName 服务名称
         * @param ip          服务IP
         * @param port        服务端口
         * @throws NacosException Nacos异常
         */
        public void registerInstance(String serviceName, String ip, int port) throws NacosException {
            getNamingService().registerInstance(serviceName, ip, port);
        }

        /**
         * 注册服务实例（带权重）
         *
         * @param serviceName 服务名称
         * @param ip          服务IP
         * @param port        服务端口
         * @param weight      服务权重
         * @throws NacosException Nacos异常
         */
        public void registerInstance(String serviceName, String ip, int port, double weight) throws NacosException {
            Instance instance = new Instance();
            instance.setServiceName(serviceName);
            instance.setIp(ip);
            instance.setPort(port);
            instance.setWeight(weight);
            getNamingService().registerInstance(serviceName, instance);
        }

        /**
         * 注销服务实例
         *
         * @param serviceName 服务名称
         * @param ip          服务IP
         * @param port        服务端口
         * @throws NacosException Nacos异常
         */
        public void deregisterInstance(String serviceName, String ip, int port) throws NacosException {
            getNamingService().deregisterInstance(serviceName, ip, port);
        }

        /**
         * 获取服务实例列表
         *
         * @param serviceName 服务名称
         * @return 服务实例列表
         * @throws NacosException Nacos异常
         */
        public List<Instance> getInstances(String serviceName) throws NacosException {
            return getNamingService().getAllInstances(serviceName);
        }

        /**
         * 获取健康的服务实例列表
         *
         * @param serviceName 服务名称
         * @return 健康的服务实例列表
         * @throws NacosException Nacos异常
         */
        public List<Instance> getHealthyInstances(String serviceName) throws NacosException {
            return getNamingService().selectInstances(serviceName, true);
        }

        /**
         * 获取所有服务名称
         *
         * @return 服务名称列表
         * @throws NacosException Nacos异常
         */
        public List<String> getServices() throws NacosException {
            return getNamingService().getServicesOfServer(1, Integer.MAX_VALUE).getData();
        }
    }
}
