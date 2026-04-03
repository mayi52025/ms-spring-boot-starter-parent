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
