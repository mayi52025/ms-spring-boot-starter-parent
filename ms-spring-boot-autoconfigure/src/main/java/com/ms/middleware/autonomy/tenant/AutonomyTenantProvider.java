package com.ms.middleware.autonomy.tenant;

/**
 * 自治模块的「应用级 tenant」标识。
 *
 * <p>默认实现 {@link SpringEnvironmentTenantProvider} 使用 {@code spring.application.name}，
 * 使 order-system 与 payment-service 在同一 Redis / 同一 starter 下账本、tick 锁、指标 label 相互隔离。</p>
 */
public interface AutonomyTenantProvider {

    /** 当前 Spring 应用对应的 tenant 字符串 */
    String getTenant();
}
