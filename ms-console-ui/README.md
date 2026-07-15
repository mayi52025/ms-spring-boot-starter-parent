# MS 控制台（React）

Phase 5.0：替换 `static/ms-console/index.html` 单页静态控制台。

## 开发

```bash
cd ms-console-ui
npm install
npm run dev
```

Vite 开发服务器 `http://localhost:5173`，API 代理到 `http://localhost:8080/ms-console/api`。

先启动 order-system（或任意启用 `ms.middleware.console.enabled=true` 的应用）。

## 构建进 JAR

```bash
npm run build
```

产物输出到：

`ms-spring-boot-autoconfigure/src/main/resources/static/ms-console/`

访问路径不变：`http://<host>:<port>/ms-console`（Demo token 默认 `demo-secret`）。

## Maven（可选）

在 `ms-spring-boot-autoconfigure` 模块使用 profile 自动构建前端：

```bash
mvn generate-resources -Pconsole-ui -pl ms-spring-boot-autoconfigure
```

日常 `mvn install` 使用已提交的构建产物，无需 Node。

## 功能对齐

- Token 鉴权（`X-MS-Console-Token`）
- 活跃 issues / 历史 / run 详情
- SSE 实时时间线
- 战时失败 Trace、recoveryEvidence、采纳 / Nacos 草稿发布
- 底部规则聊天（Phase 5.1 接 LLM）

旧版静态页备份：`legacy/index.html`
