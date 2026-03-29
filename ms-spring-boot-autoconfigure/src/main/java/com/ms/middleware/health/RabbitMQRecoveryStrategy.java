package com.ms.middleware.health;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RabbitMQ恢复策略
 */
public class RabbitMQRecoveryStrategy implements RecoveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQRecoveryStrategy.class);
    private final CachingConnectionFactory connectionFactory;

    public RabbitMQRecoveryStrategy(CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public boolean recover() {
        try {
            logger.info("Attempting to recover RabbitMQ connection...");
            // 重置连接工厂
            connectionFactory.resetConnection();
            // 验证连接
            boolean isOpen = connectionFactory.createConnection().isOpen();
            if (isOpen) {
                logger.info("RabbitMQ connection recovered successfully");
            } else {
                logger.warn("RabbitMQ connection recovery failed");
            }
            return isOpen;
        } catch (Exception e) {
            logger.error("Failed to recover RabbitMQ connection: {}", e.getMessage());
            return false;
        }
    }
}
