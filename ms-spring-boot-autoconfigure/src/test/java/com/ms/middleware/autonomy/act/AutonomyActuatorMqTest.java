package com.ms.middleware.autonomy.act;

import com.ms.middleware.MsMiddlewareProperties;
import com.ms.middleware.autonomy.AutonomyActionType;
import com.ms.middleware.autonomy.plan.PlannedAction;
import com.ms.middleware.health.FaultSelfHealing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link AutonomyActuator} 对 MQ 类动作的执行桥接。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutonomyActuatorMqTest {

    @Mock
    private FaultSelfHealing faultSelfHealing;
    @Mock
    private MqConsumerThrottle consumerThrottle;
    @Mock
    private MqDelayedRetryExecutor delayedRetryExecutor;

    private AutonomyActuator actuator;
    private MsMiddlewareProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MsMiddlewareProperties();
        properties.getAutonomy().getMq().setThrottleLimit(25);
        properties.getAutonomy().getMq().setThrottleWindowSeconds(45);

        @SuppressWarnings("unchecked")
        ObjectProvider<MqConsumerThrottle> throttleProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MqDelayedRetryExecutor> retryProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<com.ms.middleware.ai.HotKeyManager> hotKeyProvider = mock(ObjectProvider.class);

        when(throttleProvider.getIfAvailable()).thenReturn(consumerThrottle);
        when(retryProvider.getIfAvailable()).thenReturn(delayedRetryExecutor);

        actuator = new AutonomyActuator(faultSelfHealing, hotKeyProvider, throttleProvider, retryProvider, properties);
    }

    /** THROTTLE_CONSUMER 应启用 MqConsumerThrottle */
    @Test
    void throttleConsumerEnablesThrottle() {
        when(consumerThrottle.enable(25, 45)).thenReturn(true);
        when(consumerThrottle.getLimit()).thenReturn(25);
        when(consumerThrottle.getWindowSeconds()).thenReturn(45L);

        PlannedAction action = planned(AutonomyActionType.THROTTLE_CONSUMER);
        actuator.execute(action);

        assertEquals("SUCCESS", action.getExecutionStatus());
        verify(consumerThrottle).enable(25, 45);
    }

    /** DELAYED_RETRY_BATCH 应调用批量延迟重试 */
    @Test
    void delayedRetryBatchExecutesRetry() {
        when(delayedRetryExecutor.retryFailedBatch()).thenReturn(3);

        PlannedAction action = planned(AutonomyActionType.DELAYED_RETRY_BATCH);
        actuator.execute(action);

        assertEquals("SUCCESS", action.getExecutionStatus());
        verify(delayedRetryExecutor).retryFailedBatch();
    }

    /** STABLE 清理应关闭限流 */
    @Test
    void clearMqThrottleDisablesThrottle() {
        actuator.clearMqThrottle();
        verify(consumerThrottle).disable();
    }

    private PlannedAction planned(AutonomyActionType type) {
        PlannedAction action = new PlannedAction();
        action.setActionType(type);
        action.setRisk(type.getRisk());
        return action;
    }
}
