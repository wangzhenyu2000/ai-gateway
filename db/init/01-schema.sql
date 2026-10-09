-- ai-gateway 元数据库初始化脚本（MySQL 8 / InnoDB）
-- 对应《企业 AI 网关详细设计文档》4.2 / 4.3 节
-- 容器初始化时由 docker-entrypoint-initdb.d 自动执行（作用于 MYSQL_DATABASE=ai_gateway）

-- 本文件为 UTF-8 编码；显式声明会话字符集，避免客户端默认 latin1 导致中文注释双重编码乱码
SET NAMES utf8mb4;

-- 工具注册表
CREATE TABLE IF NOT EXISTS tool_registry (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    name            VARCHAR(128) NOT NULL UNIQUE COMMENT '工具唯一标识，如 query_order_status',
    display_name    VARCHAR(128) NOT NULL COMMENT '展示名称，如"查询订单状态"',
    description     TEXT NOT NULL COMMENT '给模型看的语义描述',
    input_schema    JSON NOT NULL COMMENT '入参 JSON Schema',
    output_schema   JSON COMMENT '出参 JSON Schema（可选，便于结果校验）',
    executor_type   VARCHAR(32) NOT NULL COMMENT '执行器类型：REST / MCP_PROXY / INTERNAL',
    executor_config JSON NOT NULL COMMENT '执行器配置：endpoint、超时、重试等',
    credential_ref  VARCHAR(256) COMMENT '凭证引用（非凭证本身）',
    risk_level      VARCHAR(16) NOT NULL COMMENT '风险等级：READ / WRITE / SENSITIVE',
    required_scope  VARCHAR(128) COMMENT '策略引擎使用的资源标识',
    owner_team      VARCHAR(64) NOT NULL COMMENT '工具归属团队',
    enabled         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用：0=停用，1=启用',
    version         INT NOT NULL DEFAULT 1 COMMENT '版本号，用于版本回滚',
    created_by      VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具注册表：网关注册的全部工具元数据';

-- 策略表（Casbin 默认结构，策略即数据）
-- ptype=p: sub(角色), obj(工具/资源), act(read/write)
-- ptype=g: 用户 -> 角色 映射（如不走 IdP 组声明）
CREATE TABLE IF NOT EXISTS casbin_rule (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    ptype VARCHAR(16) NOT NULL COMMENT '策略类型：p=权限策略，g=用户-角色映射',
    v0    VARCHAR(128) COMMENT 'p 时为 sub（角色）；g 时为用户（union_id）',
    v1    VARCHAR(128) COMMENT 'p 时为 obj（工具/资源，支持 keyMatch2 通配）；g 时为角色',
    v2    VARCHAR(128) COMMENT 'p 时为 act（read/write/*）；g 时预留',
    v3    VARCHAR(128) COMMENT '预留字段',
    v4    VARCHAR(128) COMMENT '预留字段',
    v5    VARCHAR(128) COMMENT '预留字段'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略表：Casbin 规则存储（p=权限策略，g=用户-角色映射）';

-- 审批单
CREATE TABLE IF NOT EXISTS approval_request (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键，即审批单号',
    requester_id    VARCHAR(64) NOT NULL COMMENT '发起人（飞书 union_id）',
    client_id       VARCHAR(32) NOT NULL COMMENT '发起客户端：web-agent / codex-cli / ...',
    tool_name       VARCHAR(128) NOT NULL COMMENT '目标工具名',
    params_snapshot JSON NOT NULL COMMENT '调用快照，审批通过后据此重放',
    reason          TEXT COMMENT 'Agent/用户给出的操作理由',
    status          VARCHAR(16) NOT NULL COMMENT '状态：PENDING / APPROVED / REJECTED / EXPIRED',
    approver_id     VARCHAR(64) COMMENT '审批人（飞书 union_id）',
    decided_at      DATETIME COMMENT '审批决定时间',
    expires_at      DATETIME NOT NULL COMMENT '审批过期时间',
    trace_id        VARCHAR(64) NOT NULL COMMENT '全链路追踪 ID',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批单：敏感操作的一次性审批授权';

-- 客户端注册表
CREATE TABLE IF NOT EXISTS client_registry (
    client_id  VARCHAR(64) PRIMARY KEY COMMENT '客户端标识：web-agent / codex-cli / chatgpt / ...',
    secret_ref VARCHAR(256) NOT NULL COMMENT '客户端凭证引用（非凭证本身）',
    owner_team VARCHAR(64) NOT NULL COMMENT '归属团队',
    enabled    TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0=停用，1=启用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端注册表：允许接入网关的 AI 客户端';

-- 审计表（本地/测试环境 MySQL 版，字段与 Doris 版一一对应）
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    trace_id        VARCHAR(64) COMMENT '全链路追踪 ID',
    ts              DATETIME COMMENT '调用时间',
    user_id         VARCHAR(64) COMMENT '员工唯一标识（飞书 union_id）',
    client_id       VARCHAR(32) COMMENT '调用客户端',
    session_id      VARCHAR(64) COMMENT '客户端会话标识（审计聚合用）',
    tool_name       VARCHAR(128) COMMENT '被调用工具名',
    risk_level      VARCHAR(16) COMMENT '工具风险等级：READ / WRITE / SENSITIVE',
    params_digest   VARCHAR(64) COMMENT '参数规范化后的 SHA-256',
    params_summary  VARCHAR(1024) COMMENT '脱敏后的参数摘要，供人阅读',
    policy_decision VARCHAR(16) COMMENT '策略判定结果：ALLOW / DENY / REQUIRE_APPROVAL',
    approval_id     BIGINT COMMENT '关联审批单 ID（如有）',
    result_status   VARCHAR(16) COMMENT '调用结果：SUCCESS / BIZ_ERROR / SYS_ERROR / TIMEOUT',
    http_status     INT COMMENT '下游返回的 HTTP 状态码',
    latency_ms      INT COMMENT '调用耗时（毫秒）',
    downstream      VARCHAR(256) COMMENT '实际调用的目标系统',
    error_msg       VARCHAR(512) COMMENT '错误信息（如有）',
    INDEX idx_user_ts (user_id, ts),
    INDEX idx_tool_ts (tool_name, ts),
    INDEX idx_trace   (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（本地/测试环境 MySQL 版，字段与 Doris 版一致）';
