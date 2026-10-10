package com.uemind.merlion.ai.gateway.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具元数据（设计文档 4.2 tool_registry 表的内存模型）。
 *
 * <p>Registry 是自有模型，MCP 只是出口适配器（协议防腐，文档 6.1）。</p>
 *
 * @param name           工具唯一标识，如 query_order_status
 * @param displayName    展示名称，如"查询订单状态"
 * @param description    给模型看的语义描述
 * @param inputSchema    入参 JSON Schema；写类工具强制包含 idempotencyKey
 * @param outputSchema   出参 JSON Schema（可选，便于结果校验）
 * @param executorType   执行器类型
 * @param executorConfig 执行器配置：endpoint、超时、重试等
 * @param credentialRef  凭证引用（非凭证本身，凭证由 credential 包解析）
 * @param riskLevel      风险等级
 * @param requiredScope  策略引擎使用的资源标识
 * @param ownerTeam      工具归属团队（审批人默认取该团队负责人）
 * @param enabled        是否启用
 * @param version        版本号（用于版本回滚）
 */
public record ToolMeta(
        String name,
        String displayName,
        String description,
        JsonNode inputSchema,
        JsonNode outputSchema,
        ExecutorType executorType,
        JsonNode executorConfig,
        String credentialRef,
        RiskLevel riskLevel,
        String requiredScope,
        String ownerTeam,
        boolean enabled,
        int version
) {
}
