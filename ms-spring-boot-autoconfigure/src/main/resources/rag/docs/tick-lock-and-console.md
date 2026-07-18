# 分布式 Tick 锁与控制台

多实例部署时，自治 tick 使用 Redisson 分布式锁（ms:autonomy:tick:{tenant}），未获锁的实例跳过本轮，避免重复 AUTO。

控制台默认一应用一控制台（tenant=spring.application.name）；鉴权可用 auth-token。Agent 只读 Tool，写配置需走采纳/PUBLISH，不由 LLM 直写。
