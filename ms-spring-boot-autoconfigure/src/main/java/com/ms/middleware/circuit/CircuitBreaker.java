package com.ms.middleware.circuit;

import java.util.function.Supplier;

/**
 * 熔断接口
 */
public interface CircuitBreaker {

    /**
     * 执行受熔断保护的操作
     *
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     * @throws Exception 执行异常
     */
    <T> T execute(Supplier<T> supplier) throws Exception;

    /**
     * 执行受熔断保护的操作，带 fallback
     *
     * @param supplier   要执行的操作
     * @param fallback   熔断时的 fallback 操作
     * @param <T>        返回值类型
     * @return 操作结果或 fallback 结果
     */
    <T> T execute(Supplier<T> supplier, Supplier<T> fallback);

    /**
     * 执行受熔断保护的操作，带异常处理
     *
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     */
    <T> T executeWithExceptionHandling(Supplier<T> supplier);

    /**
     * 获取熔断状态
     *
     * @return 熔断状态
     */
    CircuitState getState();

    /**
     * 重置熔断状态
     */
    void reset();

    /**
     * 熔断状态枚举
     */
    enum CircuitState {
        /**
         * 关闭状态，允许所有请求通过
         */
        CLOSED,

        /**
         * 打开状态，拒绝所有请求
         */
        OPEN,

        /**
         * 半开状态，允许部分请求通过以测试服务是否恢复
         */
        HALF_OPEN
    }

}