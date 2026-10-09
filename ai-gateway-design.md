# 企业 AI 网关详细设计文档

| 项目 | 内容 |
|---|---|
| 版本 | v0.1（评审稿） |
| 日期 | 2026-10-09 |
| 状态 | 待评审 |
| 读者 | 架构组、安全组、后端团队、SRE |

---

## 1. 背景与目标

### 1.1 背景

公司内部 AI 客户端形态增多（自研 Web Agent、Codex CLI、ChatGPT、第三方 IDE 插件、自动化工作流），每个客户端若直接对接企业系统（数据库、GitHub、AWS、Jira、财务/业务系统），将导致：

- 权限模型碎片化，无法统一管控；
- 凭证分散在各客户端，泄露面大；
- 无统一审计，出事故无法追溯；
- 每接入一个新客户端，需要重复对接 N 个业务系统。

### 1.2 目标

建设统一的企业 AI 网关（Enterprise AI Gateway），作为所有 AI 客户端访问企业能力的**唯一入口**：

1. **统一入口**：所有客户端通过同一网关访问企业工具与系统；
2. **安全可控**：统一身份认证、权限判定、敏感操作审批；
3. **可审计**：全链路调用留痕，支撑合规与事故追溯；
4. **可扩展**：新客户端、新工具低成本接入，面向未来 Agent 形态演进。

### 1.3 非目标（明确不做的）

| 事项 | 归属 |
|---|---|
| 模型路由、对话编排、会话与记忆 | Web Agent / LiteLLM |
| Workflow 引擎 | Web Agent（二期以后） |
| 业务逻辑本身 | 各业务系统 |
| 用户体系、组织架构 | 复用飞书（Lark）开放平台 |

网关的世界观只有一件事：**一次经过认证与授权的工具调用**。

---

## 2. 设计原则

1. **Agent Permission <= User Permission**：Agent 的权限继承自发起用户，网关只做减法，永不放大。这是第一性原则。
2. **客户端与网关解耦**：客户端专注交互体验，网关负责安全、策略与路由，两者独立演进。
3. **零信任直连**：任何客户端（包括自研 Web Agent）不得绕过网关直连企业系统；业务系统也不得因为请求来自网关就放弃自身的鉴权（纵深防御）。
4. **审计先行**：审计不是事后补的功能，第一版上线即具备完整 audit trail。
5. **策略即数据**：权限规则、工具注册、限流配置全部存库，变更不发版。
6. **最小起步**：MVP 只放行只读工具，从根上推迟审批流与凭证代理的复杂度。

---

## 3. 总体架构

### 3.1 上下文视图

```
+----------------+  +-----------+  +----------------+  +--------------+
| 自研 Web Agent |  | Codex CLI |  | ChatGPT/Mobile |  | 自动化工作流 |
+-------+--------+  +-----+-----+  +-------+--------+  +------+-------+
        |                |                 |                  |
        +----------------+-------+---------+------------------+
                                 |  MCP / REST (HTTPS)
                    +------------v-------------+
                    |      企业 AI 网关        |
                    |  认证 / 策略 / 工具路由  |
                    |  审批 / 凭证 / 审计      |
                    +------------+-------------+
                                 |
        +----------------+-------+---------+------------------+
        |                |                 |                  |
+-------v-------+ +------v------+ +--------v-------+ +--------v-------+
| 数据与分析     | | 工程与运维   | | 业务系统        | | 协作与外部服务 |
| Doris/MySQL   | | GitHub/AWS  | | 支付/CRM/财务  | | Jira/Slack    |
+---------------+ +-------------+ +----------------+ +---------------+
```

### 3.2 内部模块结构

单 Spring Boot 服务，内部按包组织（物理单体，逻辑分层）：

```
ai-gateway
├── access/       协议入口：MCP Server + REST Controller
├── identity/     身份：OIDC 资源服务器、用户身份透传、客户端身份
├── policy/       策略：Casbin 判定、限流、审批状态机
├── tool/         工具：Registry（DB）、执行器（REST/MCP/内部）、路由
├── credential/   凭证：凭证存取、用户身份注入下游请求
├── audit/        审计：异步批量写 Doris
├── observe/      可观测：OpenTelemetry、指标、告警
└── admin/        管理 API：工具注册、策略配置、审计查询、审批处理
```

**为什么 MVP 是单体**：网关是无状态请求处理器，拆分微服务的收益（独立伸缩）在早期不存在，成本（分布式审计、链路透传、运维复杂度）却立刻兑现。流量上来后优先拆分的是 tool 执行器（IO 密集）和 audit 写入（吞吐敏感）。

### 3.3 技术选型

| 维度 | 选型 | 说明 |
|---|---|---|
| 语言/框架 | Java 21 + Spring Boot 3 + WebFlux | IO 密集的代理与流式转发，响应式模型 |
| MCP 接入 | MCP Java SDK（Spring AI 维护） | 官方 SDK，仅作协议适配层 |
| 登录认证 | 飞书 OAuth2/OIDC + 网关自签发内部 JWT | 飞书企业自建应用；Spring Security 校验内部 JWT，见 6.2 |
| Admin 前端 | Vue 3 / React（二期建设） | 管理控制台前端，见 6.8 |
| 策略引擎 | Casbin（jcasbin） | RBAC + 工具/动作两维；规则复杂化后再评估 OPA |
| 限流 | Bucket4j + Redis | 用户 x 客户端 x 工具三维度 |
| 元数据库 | MySQL 8（InnoDB） | 工具注册表、策略、审批单；复用团队现有运维体系 |
| 审计存储 | 生产：Doris（Stream Load 批量写入）；本地/测试：MySQL 同名表 | sink 可配置切换，见 6.6 |
| 链路追踪 | OpenTelemetry + Prometheus | traceId 贯穿客户端到业务系统 |
| 凭证（二期） | HashiCorp Vault / 云 KMS | MVP 阶段用 K8s Secret + 环境变量 |
| 部署 | K8s，无状态多副本 | 会话状态全部外置（Redis/DB） |

---

## 4. 核心概念与数据模型

### 4.1 CallContext（调用上下文）

每次请求的权威上下文，由认证过滤器创建，贯穿整条处理链，并透传到下游：

```java
public record CallContext(
    String userId,        // 员工唯一标识（飞书 union_id）
    String userName,
    Set<String> roles,    // 角色集合（来自 IdP 或网关侧映射）
    String deptId,
    String clientId,      // web-agent / codex-cli / chatgpt / ...
    String sessionId,     // 客户端会话标识（审计聚合用）
    String traceId,       // 全链路追踪 ID
    String approvalId,    // 敏感操作：已批准的审批单号，可空
    Instant issuedAt
) {}
```

### 4.2 元数据表（MySQL 8）

**tool_registry —— 工具注册表**

```sql
CREATE TABLE tool_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL UNIQUE,  -- query_order_status
    display_name    VARCHAR(128) NOT NULL,         -- 查询订单状态
    description     TEXT NOT NULL,                 -- 给模型看的语义描述
    input_schema    JSON NOT NULL,                 -- JSON Schema
    output_schema   JSON,                          -- 可选，便于结果校验
    executor_type   VARCHAR(32) NOT NULL,          -- REST / MCP_PROXY / INTERNAL
    executor_config JSON NOT NULL,                 -- endpoint、超时、重试等
    credential_ref  VARCHAR(256),                  -- 凭证引用（非凭证本身）
    risk_level      VARCHAR(16) NOT NULL,          -- READ / WRITE / SENSITIVE
    required_scope  VARCHAR(128),                  -- 策略引擎使用的资源标识
    owner_team      VARCHAR(64) NOT NULL,          -- 工具归属团队
    enabled         TINYINT(1) NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 1,
    created_by      VARCHAR(64) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**casbin_rule —— 策略表**（Casbin 默认结构，策略即数据）

```sql
-- ptype=p: sub(角色), obj(工具/资源), act(read/write)
-- ptype=g: 用户 -> 角色 映射（如不走 IdP 组声明）
CREATE TABLE casbin_rule (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    ptype VARCHAR(16) NOT NULL,
    v0    VARCHAR(128), v1 VARCHAR(128), v2 VARCHAR(128),
    v3    VARCHAR(128), v4 VARCHAR(128), v5 VARCHAR(128)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**approval_request —— 审批单**

```sql
CREATE TABLE approval_request (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id    VARCHAR(64) NOT NULL,
    client_id       VARCHAR(32) NOT NULL,
    tool_name       VARCHAR(128) NOT NULL,
    params_snapshot JSON NOT NULL,          -- 调用快照，审批通过后据此重放
    reason          TEXT,                   -- Agent/用户给出的操作理由
    status          VARCHAR(16) NOT NULL,   -- PENDING/APPROVED/REJECTED/EXPIRED
    approver_id     VARCHAR(64),
    decided_at      DATETIME,
    expires_at      DATETIME NOT NULL,
    trace_id        VARCHAR(64) NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**client_registry —— 客户端注册表**

```sql
CREATE TABLE client_registry (
    client_id  VARCHAR(64) PRIMARY KEY,     -- web-agent / codex-cli / ...
    secret_ref VARCHAR(256) NOT NULL,       -- 客户端凭证引用
    owner_team VARCHAR(64) NOT NULL,
    enabled    TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.3 审计表（Doris）

```sql
CREATE TABLE audit_log (
    trace_id        VARCHAR(64),
    ts              DATETIME,
    user_id         VARCHAR(64),
    client_id       VARCHAR(32),
    session_id      VARCHAR(64),
    tool_name       VARCHAR(128),
    risk_level      VARCHAR(16),
    params_digest   VARCHAR(64),           -- 参数规范化后的 SHA-256
    params_summary  VARCHAR(1024),         -- 脱敏摘要，供人阅读
    policy_decision VARCHAR(16),           -- ALLOW / DENY / REQUIRE_APPROVAL
    approval_id     BIGINT,
    result_status   VARCHAR(16),           -- SUCCESS / BIZ_ERROR / SYS_ERROR / TIMEOUT
    http_status     INT,
    latency_ms      INT,
    downstream      VARCHAR(256),          -- 实际调用的目标系统
    error_msg       VARCHAR(512)
)
DUPLICATE KEY(trace_id, ts)
PARTITION BY RANGE(ts) ()
DISTRIBUTED BY HASH(trace_id) BUCKETS 8
PROPERTIES ("dynamic_partition.enable" = "true",
            "dynamic_partition.time_unit" = "DAY",
            "dynamic_partition.end" = "7",
            "dynamic_partition.prefix" = "p");
```

**本地/测试环境（无 Doris）**：审计落 MySQL 同名表，字段与 Doris 版一一对应，普通 InnoDB 表加常用索引即可：

```sql
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 其余字段与 Doris 版 audit_log 一致
    INDEX idx_user_ts (user_id, ts),
    INDEX idx_tool_ts (tool_name, ts),
    INDEX idx_trace   (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**审计字段纪律**：params_summary 必须脱敏（身份证、银行卡、手机号打码），全量参数永不落审计库；需要取证时凭 params_digest + 下游系统日志还原。

---

## 5. 请求处理主链路

### 5.1 过滤器链（顺序固定，不可调整）

```
请求
 1. AuthNFilter          校验 JWT（RS256，JWKS 自动轮换），拒绝匿名
 2. ClientIdentityFilter 识别客户端身份（client_id + 凭证），校验客户端启用状态
 3. AuditContextFilter   生成 traceId，构建 CallContext，写入 Reactor Context
 4. RateLimitFilter      用户 x 客户端 x 工具三维令牌桶
 5. PolicyFilter         Casbin 判定：ALLOW / DENY / REQUIRE_APPROVAL
 6. ApprovalFilter       敏感操作：校验 approvalId 有效性（存在、未过期、未被消费）
 7. ToolRouter           按 tool_registry.executor_type 路由到执行器
 8. ToolExecutor         注入凭证与用户身份，调用下游（超时/重试/熔断）
 9. AuditWriter          try/finally 保证成功失败都留痕，异步批量写 Doris
响应
```

**顺序理由**：审计上下文（3）必须在策略（5）之前建立，否则拒绝调用无迹可查；限流（4）在策略之前，避免恶意流量消耗策略判定资源；审计写入（9）用 try/finally 兜底——**审计漏记比工具报错更严重**。

### 5.2 策略判定模型

Casbin model（第一版，工具级 + 动作级）：

```ini
[request_definition]
r = sub, obj, act

[policy_definition]
p = sub, obj, act

[role_definition]
g = _, _

[policy_effect]
e = some(where (p.eft == allow))

[matchers]
m = g(r.sub, p.sub) && keyMatch2(r.obj, p.obj) && (r.act == p.act || p.act == "*")
```

判定结果三态：

| 结果 | 行为 |
|---|---|
| ALLOW | 放行 |
| DENY | 返回 403，审计记录 policy_decision=DENY |
| REQUIRE_APPROVAL | risk_level=WRITE/SENSITIVE 的工具命中此态，进入审批流程 |

权限收窄（Session Scope）：客户端可在建立会话时声明工具白名单子集，网关在策略判定后再次取交集——只缩不放。

### 5.3 典型时序

**（a）只读调用（MVP 主路径）**

```
Client            Gateway                    下游系统
  |  tools/call     |                          |
  |---------------->| 1-6 过滤器链 (ALLOW)     |
  |                 | 7 路由 REST 执行器        |
  |                 | 8 GET /internal/orders   |
  |                 |   (X-User-Id + 签名)     |
  |                 |------------------------->|
  |                 |<-------------------------|
  |  SSE 流式返回   | 9 审计落库               |
  |<----------------|                          |
```

**（b）敏感写操作（二期）**

```
Client            Gateway                 Admin/IM            下游
  |  tools/call     |                       |                  |
  |---------------->| 5 判定 REQUIRE_APPROVAL                 |
  |                 |-- 创建 approval_request，挂起 ---------->|
  |  202 + approvalId                       |                  |
  |<----------------|                       | 审批人点击通过    |
  |                 |<----------------------|                  |
  |  tools/call(approvalId)                 |                  |
  |---------------->| 6 校验 approvalId     |                  |
  |                 | 7-8 执行（幂等键去重）|----------------->|
  |  结果           | 9 审计（含 approvalId）                  |
  |<----------------|                       |                  |
```

**（c）动态工具注册（Admin）**

```
Admin             Gateway                    Client
  | POST /admin/tools (schema + executor)     |
  |------------------------------------------>|
  |                 | 校验 schema / 灰度 enabled=false
  |                 | 启用 -> 刷新 ToolCallbackProvider
  |                 | 推送 tools/listChanged（MCP 通知）
  |                 |------------------------->| 客户端下次 list 即见新工具
```

---

## 6. 模块详细设计

### 6.1 access —— 协议接入层

- **MCP Server**：基于 MCP Java SDK（Streamable HTTP + SSE）。实现 ToolCallbackProvider，从 Tool Registry 动态生成工具列表，**不写任何 @Tool 硬编码方法**。
- **REST 入口**：与 MCP 同能力的 HTTP API，供自动化脚本与定时任务使用。
  - `GET  /v1/tools` 工具列表（按调用者权限过滤）
  - `POST /v1/tools/{name}:invoke` 调用工具（同步/SSE）
  - `POST /v1/approvals/{id}:approve`、`:reject` 审批操作（也可走 IM 回调）
- **流式转发**：下游 SSE 原样透传，禁止攒包；背压由 WebFlux 天然支持。
- **协议防腐**：Registry 是自有模型，MCP 只是出口适配器。未来出现新协议（A2A 等）只新增 access 层实现，不动内核。

### 6.2 identity —— 身份与认证（飞书 Lark）

**登录接入**：网关在飞书开放平台注册企业自建应用（获取 app_id / app_secret，配置回调地址），作为 OAuth relying party。用户登录完成后，网关签发自己的内部 JWT 作为会话凭证，飞书 token 不透传到任何下游：

```
Client -> 网关 -> 401 + 登录引导 -> 飞书授权页（authen/v1/authorize）
  -> 回调 code -> 网关调 authen/v1/oidc/access_token
       换 id_token + user_access_token + refresh_token
  -> authen/v1/user_info 拉用户详情
  -> 网关签发内部 JWT（有效期 8h）建立会话
```

要点：

- **token 策略**：飞书 user_access_token 约 2h 过期、refresh_token 约 30 天；网关内部 JWT 与飞书 token 解耦，飞书 token 仅存服务端用于刷新和调飞书 API。将来更换 IdP（企业微信 / Keycloak）只动本模块。
- **MCP 客户端**：MCP 协议支持 OAuth 2.1 流程，ChatGPT / Codex 连接网关 MCP 地址时自动跳转飞书授权页，CLI 场景体验为弹浏览器扫码。
- **身份信息缓存**：user_info 为服务端到服务端调用且飞书有接口限流，身份信息按会话缓存（Redis，跟随会话 TTL），禁止每次工具调用都查飞书。

**Web Agent 登录集成（客户端样板流程）**：飞书 OAuth 全流程由网关完成，Web Agent 只负责跳转和接回 token，不接触飞书 token：

```
浏览器 -> Web Agent（无会话）-> 302 网关 GET /auth/lark/login?redirect=...
  -> 网关校验 redirect 白名单、生成 state（防 CSRF，Redis 存 5min）-> 302 飞书授权页
  -> 用户扫码/确认 -> 飞书回调 GET /auth/lark/callback?code=xxx&state=xxx
  -> 网关验 state、code 换 token、拉 user_info、建会话、签发内部 JWT
  -> 302 回 Web Agent（token 走 URL fragment，不进浏览器历史与服务端日志）
  -> 后续请求统一携带 Authorization: Bearer <网关内部 JWT>
```

网关侧配套端点与存储：

- `GET /auth/lark/login`、`GET /auth/lark/callback`：登录入口与回调；
- `GET /auth/jwks.json`：暴露网关签名公钥，供 Web Agent 等客户端独立验签；
- `POST /auth/refresh`：内部 JWT 剩余有效期 < 1h 时续签（用 Redis 中的飞书 refresh_token 校验在职状态后发新 JWT）；
- 会话存储：Redis，`sessionId -> {union_id, 姓名, 部门, 角色, larkRefreshToken(加密)}`，TTL 跟随内部 JWT（8h）。

内部 JWT claim：sub=union_id、name、dept、roles、client_id、exp。Web Agent 后端用网关 jwks.json 验签后直接以 union_id 作为本服务用户主键——**一次登录，网关与 Web Agent 后端共同认可，Web Agent 无需自建用户体系**。

**委托 token（离线任务）**：Web Agent 后台任务（定时报表等）在用户离线后执行时，由 Web Agent 后端持客户端凭证 + 任务创建时的 userId 向网关换取委托 token（scope 收窄、短有效期、审计标记 delegated=true）。审计中委托调用与本人调用一眼可辨。

**身份映射**：以飞书 union_id 作为 CallContext.userId 主键（同一开发商多应用下唯一；open_id 仅单应用内唯一，不可作主键）。角色映射分两档：

- MVP：按飞书部门路径映射角色（如"技术部/平台组" -> dev），规则存配置；
- 精细版：调飞书通讯录 API 查用户所在用户组作为角色来源。

**客户端认证**：每个注册客户端持有独立凭证（mTLS 或 client_secret），与用户身份**双因子并存**：一次合法调用 = 有效用户 token + 有效客户端身份。

**身份透传**：调用下游时注入 X-User-Id / X-User-Roles / X-Trace-Id + 网关内部签名（HMAC，密钥存 Secret）。下游业务系统验签后仍需执行自身数据权限过滤。

**会话收窄**：X-Session-Scope 头声明本次会话允许的工具子集。

### 6.3 policy —— 策略与审批

- 判定逻辑封装为 `PolicyEngine.decide(CallContext, ToolMeta, params) -> Decision`。
- Casbin adapter 直连 MySQL，策略变更通过 watcher 热加载。
- 审批状态机：PENDING -> APPROVED / REJECTED / EXPIRED，由定时任务处理超时；审批通过生成一次性 approvalId，**被消费即失效**，防重放。
- **审批通知（飞书联动）**：敏感操作触发审批后，网关调飞书消息 API 向审批人推送交互卡片（操作摘要：谁 / 工具 / 参数摘要 / 理由 + 同意 / 拒绝按钮）；审批人点击后飞书回调 `POST /callbacks/lark/approval`（需验签）更新审批单状态，并通过飞书消息通知发起人。审批人默认取工具的 owner_team 负责人。

### 6.4 tool —— 工具注册与执行

**三种执行器**：

| 类型 | 用途 | 实现要点 |
|---|---|---|
| REST | 业务系统已有内部 API | HTTP 调用，超时/重试（指数退避，仅幂等工具）/熔断（Resilience4j） |
| MCP_PROXY | 挂载第三方 MCP Server（GitHub、Jira） | 网关作为 MCP Client 连接，拦截权限与审计后转发，对客户端透明 |
| INTERNAL | 网关自带能力（如 query_audit_log） | 进程内 Bean 调用 |

**工具治理**：

- schema 变更必须升 version，Admin 保留历史版本供回滚；
- 新工具默认 enabled=false，灰度启用；
- 健康检查：定时探测下游 endpoint，不健康时调用直接返回结构化错误（UPSTREAM_UNAVAILABLE），不打爆下游；
- 写类工具 schema 强制包含 idempotencyKey，执行器透传给业务侧去重（Agent 超时重试是常态）。

**工具粒度纪律**：按业务动作设计（query_payment_detail(orderId)），禁止向普通角色开放万能工具（如裸 query_mysql(sql)）；确需存在的万能工具限制到极少数角色 + 强制审批。判断标准：**工具名应该能写进审计报告，让非工程师看懂发生了什么。**

### 6.5 credential —— 凭证代理

- MVP：服务账号凭证存 K8s Secret，启动加载到内存，调用时按 credential_ref 取用；凭证**永不返回给客户端**。
- 二期：接入 Vault/KMS，按"用户 + 工具"动态签发短期凭证（<= 15min），支持代持（on-behalf-of）token exchange，使下游审计能定位到真实用户。

### 6.6 audit —— 审计

- **sink 抽象**：审计写入统一走 `AuditSink` 接口，三个实现按环境配置切换，架构不因环境分叉：

| 实现 | 环境 | 说明 |
|---|---|---|
| DorisStreamLoadSink | 生产 | Stream Load 批量写 Doris（500 条 / 2s 一批） |
| JdbcAuditSink | 本地 / 测试 | 写 MySQL audit_log 表，字段与 Doris 版一致 |
| LogSink | 本地开发（可选） | 审计事件直接打日志文件，连 MySQL 都不用建 |

- 降级设计：生产 Doris 不可用时临时切 JdbcAuditSink 或本地文件，事后补导——**审计组件不构成网关的单点依赖**；
- 内存队列缓冲 + 批量 Stream Load 写 Doris（默认 500 条 / 2s 一批），主链路零阻塞；
- 队列积压告警；网关宕机时容忍丢失缓冲批次，但日志文件同步落本地盘兜底（Filebeat 补采）；
- 查询入口：Admin Console（近 7 天）+ Doris BI（全量分析）。

### 6.7 observe —— 可观测性

- OpenTelemetry：traceId 从客户端请求头继承（无则生成），跨下游传播（traceparent）；
- 核心指标：gateway_invoke_total{tool,client,decision,status}、gateway_latency_seconds、gateway_ratelimit_rejected_total、gateway_approval_pending；
- 告警规则：单工具 5xx 率 > 5%（5min）、单用户 QPS 突增 10 倍、审批积压 > 30min、审计写入失败。

### 6.8 admin —— 管理控制台

- 工具管理：注册 / 编辑 / 上下线 / 版本回滚 / 健康状态查看；
- 策略管理：角色-工具矩阵可视化编辑（底层改 casbin_rule）；
- 审批处理：待办列表、详情（调用快照 + 理由）、通过/驳回；
- 审计查询：按用户/工具/时间/traceId 检索；
- Admin 本身走同一套 SSO，操作全部进审计表（自我审计）。

**前端形态**：网关唯一的前端就是 Admin Console。MVP 阶段不做页面（API + SQL 直接操作），二期用 Vue 3 / React + 团队现有中后台脚手架实现，包含工具管理、角色-工具矩阵、审批处理台、审计检索四个模块。除 Admin 外网关没有其他前端：登录页由飞书授权页承担，业务交互全部发生在各 AI 客户端侧。Admin 登录同样走飞书 OAuth（复用同一自建应用）。

---

## 7. 安全设计

| 威胁 | 对策 |
|---|---|
| 凭证泄露 | 凭证集中托管、内存驻留、短期化（二期）；客户端零凭证 |
| 越权调用 | 双层权限：网关管"能否调"，业务系统管"数据范围"，互不信任 |
| 权限放大 | 网关只做减法；Session Scope 收窄；写操作强制审批 |
| Prompt injection 经工具结果回流 | 敏感操作判定在网关侧按 risk_level 执行，不信任 Agent 自述；工具结果中检测到的指令性内容不改变策略判定 |
| 重放 | approvalId 一次性；内部签名带时间戳；审计 digest 防抵赖 |
| 审计数据本身敏感 | 参数脱敏后入库，全量参数永不落审计；审计表独立权限 |
| 恶意 Agent 死循环 | 三维限流 + 单会话调用预算 + 异常 QPS 告警 |
| 内部签名密钥泄露 | 密钥存 K8s Secret / KMS，定期轮换，签名算法可灰度切换 |

---

## 8. 非功能设计

- **性能**：单副本目标 500 QPS（工具调用多为下游延迟主导），P99 网关自身开销 < 20ms（不含下游）；
- **可用性**：无状态多副本 + HPA；元数据库主备；Redis 哨兵；下游故障熔断隔离，不拖垮网关；
- **一致性**：策略/工具变更秒级生效（watcher 推送），无需强一致；
- **容量**：审计按 100 万次调用/天估算，约 2GB/月（脱敏后），Doris 动态分区保留 2 年（热 90 天）；
- **兼容**：MCP 协议跟随官方 SDK 升级；REST API 遵循 /v1 版本化，破坏性变更发 /v2。

---

## 9. 里程碑

### MVP（第 1-6 周）：只读能力闭环

| 周 | 交付 |
|---|---|
| 1-2 | 工程骨架、OIDC 认证、1 个硬编码只读工具（query_doris）、MCP 调通（MCP Inspector 验证） |
| 3-4 | Tool Registry + 动态注册、Casbin 策略、审计落 Doris |
| 5-6 | 限流、Admin 最小 API、3-5 个只读工具、接 ChatGPT/Codex 真实场景验证 |

**MVP 刻意不做**：审批流（只放行只读工具，从根上不需要）、Vault、微服务拆分、Admin 前端页面（先用 API + SQL）。

### 二期（第 7-12 周）：写能力与审批

审批状态机、IM 审批通知、凭证代理（Vault）、写类工具开放（幂等键强制）、Admin Console 前端、Session Scope 收窄。

### 三期（第 13 周起）：精细化运营

成本配额（若代理模型调用）、异常行为检测、工具市场/自助注册流程、执行器独立伸缩拆分。

---

## 10. 开放问题

1. ~~SSO 选型~~ 已确定为飞书。待确认：角色映射走部门路径还是飞书用户组；部门异动时角色缓存的刷新策略。
2. 审批人体系：按工具 owner 团队审批，还是按数据敏感级别集中到安全组？（建议前者起步）
3. 是否需要代理模型调用（LLM 流量也过网关）？若需要，引入 LiteLLM Proxy 内嵌还是独立部署？（建议独立部署，二期再议）
4. 业务系统验签改造的配合排期：哪些系统能接受 X-User-Id + 签名改造，哪些只能先用服务账号顶？（影响第一批工具清单）
5. 审计保留周期的合规要求（金融相关数据可能有强制年限）。

---

## 附录 A：第一批候选工具清单（示例，待与业务方确认）

| 工具 | 系统 | 类型 | 风险 | 角色 |
|---|---|---|---|---|
| query_doris_metrics | Doris | REST | READ | ops, analyst |
| query_logs | 日志平台 | REST | READ | dev, sre |
| github_search | GitHub | MCP_PROXY | READ | dev |
| query_jira_issue | Jira | MCP_PROXY | READ | all |
| query_order_status | 订单中心 | REST | READ | cs, ops |
| create_pr | GitHub | MCP_PROXY | WRITE（二期，审批） | dev |
