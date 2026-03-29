package com.ms.middleware.health;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RabbitMQ健康检查器
 */
public class RabbitMQHealthChecker implements HealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQHealthChecker.class);
    private final CachingConnectionFactory connectionFactory;

    public RabbitMQHealthChecker(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean checkHealth() {
        try {
            // 检查连接是否正常
            return connectionFactory.createConnection().isOpen();
        } catch (Exception e) {
            logger.warn("RabbitMQ health check failed: {}", e.getMessage());
            return false;
        }
    }
}
