package com.uemind.merlion.ai.gateway.common;

import java.time.Instant;
import java.util.Set;

/**
 * 调用上下文（设计文档 4.1）。
 *
 * <p>每次请求的权威上下文，由认证过滤器创建，贯穿整条处理链，并透传到下游。
 * 随 Reactor Context 传递（见文档 5.1 AuditContextFilter）。</p>
 *
 * @param userId     员工唯一标识（飞书 union_id，不用 open_id——仅单应用内唯一）
 * @param userName   员工姓名
 * @param roles      角色集合（来自 IdP 或网关侧映射）
 * @param deptId     部门 ID
 * @param clientId   客户端标识：web-agent / codex-cli / chatgpt / ...
 * @param sessionId  客户端会话标识（审计聚合用）
 * @param traceId    全链路追踪 ID
 * @param approvalId 敏感操作：已批准的审批单号，可空
 * @param issuedAt   签发时间
 */
public record CallContext(
        String userId,
        String userName,
        Set<String> roles,
        String deptId,
        String clientId,
        String sessionId,
        String traceId,
        String approvalId,
        Instant issuedAt
) {
}
