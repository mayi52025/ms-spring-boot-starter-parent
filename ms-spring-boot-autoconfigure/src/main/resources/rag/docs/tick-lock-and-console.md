# 分布式 Tick 锁与控制台

## 多实例要注意什么

tick 锁多实例要注意什么、文档里分布式 tick 锁要注意什么：

1. **多实例互斥**：自治 tick 使用 **Redisson 分布式锁**（key 为 `ms:autonomy:tick:{tenant}`），未获取到锁的实例 **跳过本轮**，避免多实例同时触发 tick、重复 AUTO。
2. **租户隔离**：控制台默认一应用一控制台（tenant=`spring.application.name`）；鉴权可用 `auth-token`。
3. **写路径**：Agent 只读 Tool，写配置需走采纳/PUBLISH，不由 LLM 直写。

## 摘要

多实例部署时，自治 tick 使用 Redisson 分布式锁（ms:autonomy:tick:{tenant}），未获锁的实例跳过本轮，避免重复 AUTO。
