package com.ms.middleware.health;

/**
 * 恢复策略接口
 * 用于定义组件故障时的恢复方法
 */
@FunctionalInterface
public interface RecoveryStrategy {
    /**
     * 执行恢复操作
     * @return 是否恢复成功
     */
    boolean recover();
}
