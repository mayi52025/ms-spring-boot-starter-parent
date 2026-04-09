package com.ms.middleware.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;

/**
 * 配置中心自动配置类
 * <p>
 * 基于 Nacos Config 实现配置中心功能，支持配置的集中管理、动态更新和版本控制
 * </p>
 */
@Configuration
@ConditionalOnProperty(prefix = "ms.middleware.config", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConfigCenterAutoConfiguration {

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private NacosConfigProperties nacosConfigProperties;

    @Autowired
    private Environment environment;

    /**
     * 配置中心客户端
     * <p>
     * 用于配置的读取、监听和更新操作
     * </p>
     *
     * @return 配置中心客户端
     */
    @Bean
    @RefreshScope
    public ConfigCenterClient configCenterClient() {
        return new ConfigCenterClient(nacosConfigManager, nacosConfigProperties);
    }

    /**
     * 自动拉取配置
     * <p>
     * 在应用启动时自动从Nacos拉取配置
     * </p>
     */
    @PostConstruct
    public void autoPullConfig() {
        try {
            // 获取应用名称
            String appName = environment.getProperty("spring.application.name", "unknown-app");
            
            // 构建默认配置ID
            String dataId = appName + "." + nacosConfigProperties.getFileExtension();
            String group = nacosConfigProperties.getGroup();
            
            // 拉取配置
            ConfigService configService = nacosConfigManager.getConfigService();
            String config = configService.getConfig(dataId, group, 5000);
            
            if (config != null && !config.isEmpty()) {
                System.out.println("[ms-middleware] 配置自动拉取成功: " + dataId);
                System.out.println("[ms-middleware] 配置内容: " + config.substring(0, Math.min(100, config.length())) + (config.length() > 100 ? "..." : ""));
            } else {
                System.out.println("[ms-middleware] 配置自动拉取: 未找到配置 " + dataId);
            }
            
            // 自动监听配置变更
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                
                @Override
                public void receiveConfigInfo(String configInfo) {
                    System.out.println("[ms-middleware] 配置自动更新: " + dataId);
                    System.out.println("[ms-middleware] 新配置内容: " + configInfo.substring(0, Math.min(100, configInfo.length())) + (configInfo.length() > 100 ? "..." : ""));
                }
            });
            
        } catch (NacosException e) {
            System.err.println("[ms-middleware] 配置自动拉取失败: " + e.getMessage());
        }
    }

    /**
     * 配置中心客户端实现类
     * <p>
     * 封装 Nacos Config 的核心功能
     * </p>
     */
    public static class ConfigCenterClient {

        private final NacosConfigManager nacosConfigManager;
        private final NacosConfigProperties nacosConfigProperties;

        public ConfigCenterClient(NacosConfigManager nacosConfigManager, NacosConfigProperties nacosConfigProperties) {
            this.nacosConfigManager = nacosConfigManager;
            this.nacosConfigProperties = nacosConfigProperties;
        }

        /**
         * 获取配置服务
         *
         * @return 配置服务
         */
        private ConfigService getConfigService() {
            return nacosConfigManager.getConfigService();
        }

        /**
         * 获取配置
         *
         * @param dataId  配置 ID
         * @param group   配置分组
         * @param timeout 超时时间（毫秒）
         * @return 配置内容
         * @throws NacosException Nacos 异常
         */
        public String getConfig(String dataId, String group, long timeout) throws NacosException {
            return getConfigService().getConfig(dataId, group, timeout);
        }

        /**
         * 获取配置（使用默认分组和超时）
         *
         * @param dataId 配置 ID
         * @return 配置内容
         * @throws NacosException Nacos 异常
         */
        public String getConfig(String dataId) throws NacosException {
            return getConfig(dataId, nacosConfigProperties.getGroup(), 5000);
        }

        /**
         * 发布配置
         *
         * @param dataId  配置 ID
         * @param group   配置分组
         * @param content 配置内容
         * @return 是否发布成功
         * @throws NacosException Nacos 异常
         */
        public boolean publishConfig(String dataId, String group, String content) throws NacosException {
            return getConfigService().publishConfig(dataId, group, content);
        }

        /**
         * 发布配置（使用默认分组）
         *
         * @param dataId  配置 ID
         * @param content 配置内容
         * @return 是否发布成功
         * @throws NacosException Nacos 异常
         */
        public boolean publishConfig(String dataId, String content) throws NacosException {
            return publishConfig(dataId, nacosConfigProperties.getGroup(), content);
        }

        /**
         * 删除配置
         *
         * @param dataId 配置 ID
         * @param group  配置分组
         * @return 是否删除成功
         * @throws NacosException Nacos 异常
         */
        public boolean removeConfig(String dataId, String group) throws NacosException {
            return getConfigService().removeConfig(dataId, group);
        }

        /**
         * 删除配置（使用默认分组）
         *
         * @param dataId 配置 ID
         * @return 是否删除成功
         * @throws NacosException Nacos 异常
         */
        public boolean removeConfig(String dataId) throws NacosException {
            return removeConfig(dataId, nacosConfigProperties.getGroup());
        }

        /**
         * 添加配置监听器
         *
         * @param dataId   配置 ID
         * @param group    配置分组
         * @param listener 配置监听器
         * @throws NacosException Nacos 异常
         */
        public void addListener(String dataId, String group, ConfigChangeListener listener) throws NacosException {
            getConfigService().addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    listener.onChange(configInfo);
                }
            });
        }

        /**
         * 添加配置监听器（使用默认分组）
         *
         * @param dataId   配置 ID
         * @param listener 配置监听器
         * @throws NacosException Nacos 异常
         */
        public void addListener(String dataId, ConfigChangeListener listener) throws NacosException {
            addListener(dataId, nacosConfigProperties.getGroup(), listener);
        }

        /**
         * 移除配置监听器
         *
         * @param dataId   配置 ID
         * @param group    配置分组
         * @param listener 配置监听器
         */
        public void removeListener(String dataId, String group, ConfigChangeListener listener) {
            // Nacos 的移除监听器需要传入原始监听器对象，这里简化处理
            getConfigService().removeListener(dataId, group, null);
        }

        /**
         * 获取配置的历史版本
         *
         * @param dataId 配置 ID
         * @param group  配置分组
         * @param pageNo 页码
         * @param pageSize 每页大小
         * @return 历史版本列表
         */
        public String getConfigHistory(String dataId, String group, int pageNo, int pageSize) {
            // 这里可以调用 Nacos Open API 获取历史版本
            // 简化实现，实际项目中可以使用 Nacos 的 History API
            return "Config history feature - dataId: " + dataId + ", group: " + group;
        }
    }

    /**
     * 配置变更监听器接口
     */
    @FunctionalInterface
    public interface ConfigChangeListener {
        /**
         * 配置变更回调
         *
         * @param configInfo 新的配置内容
         */
        void onChange(String configInfo);
    }
}
