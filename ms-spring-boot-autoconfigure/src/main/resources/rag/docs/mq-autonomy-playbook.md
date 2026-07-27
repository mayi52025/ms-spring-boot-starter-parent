# MQ 自治演示要点

## 手册里 MQ 自治怎么止血

手册里 MQ 自治怎么止血、限流止血怎么做：

| 阶段 | 操作 |
|------|------|
| DETECT | MQ 消费失败偏高（MQ_DEGRADED）判定故障 |
| PLAN | 生成限流计划 |
| AUTO | **THROTTLE_CONSUMER** 限流止血（背压） |
| SAFETY_UNWIND | 限流超时保护，强制关限流（防误杀常态化） |
| ESCALATE | 限流后连续 tick 无改善，升级人工，勿继续自动加压 |
| STABLE | 恢复后结案，记录 MTTR 与 recoveryEvidence |

**注意：** STABLE ≠ 业务无损；误杀/无效限流靠超时与无改善规则回撤，不是模型反思。

窗口清空、STABLE 结案证据可在控制台时间线查看。

## 摘要

中间件自治在 MQ 消费失败偏高（MQ_DEGRADED）时：DETECT → PLAN → AUTO 限流（THROTTLE_CONSUMER）→ 恢复后 STABLE，并记录 MTTR 与 recoveryEvidence。

控制台可查看活跃故障、时间线、采纳建议；运维助手通过 Insight Tool 查询 run / Trace / 指标，不直接改 Nacos。

历史类似问题可检索本摘要：限流止血、窗口清空、STABLE 结案证据。
