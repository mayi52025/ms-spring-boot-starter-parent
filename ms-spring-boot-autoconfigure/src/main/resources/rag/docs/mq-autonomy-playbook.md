# MQ 自治演示要点

中间件自治在 MQ 消费失败偏高（MQ_DEGRADED）时：DETECT → PLAN → AUTO 限流（THROTTLE_CONSUMER）→ 恢复后 STABLE，并记录 MTTR 与 recoveryEvidence。

控制台可查看活跃故障、时间线、采纳建议；运维助手通过 Insight Tool 查询 run / Trace / 指标，不直接改 Nacos。

历史类似问题可检索本摘要：限流止血、窗口清空、STABLE 结案证据。
