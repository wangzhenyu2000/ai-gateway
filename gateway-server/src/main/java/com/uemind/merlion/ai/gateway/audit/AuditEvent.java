package com.uemind.merlion.ai.gateway.audit;

import com.uemind.merlion.ai.gateway.common.PolicyDecision;
import com.uemind.merlion.ai.gateway.tool.RiskLevel;

import java.time.Instant;

/**
 * 审计事件（设计文档 4.3 audit_log 的内存模型）。
 *
 * <p>字段纪律：{@code paramsSummary} 必须脱敏（身份证、银行卡、手机号打码），
 * 全量参数永不落审计库；需要取证时凭 {@code paramsDigest} + 下游系统日志还原。</p>
 *
 * @param traceId        全链路追踪 ID
 * @param ts             调用时间
 * @param userId         员工唯一标识（飞书 union_id）
 * @param clientId       调用客户端
 * @param sessionId      客户端会话标识（审计聚合用）
 * @param toolName       被调用工具名
 * @param riskLevel      工具风险等级
 * @param paramsDigest   参数规范化后的 SHA-256
 * @param paramsSummary  脱敏后的参数摘要，供人阅读
 * @param policyDecision 策略判定结果
 * @param approvalId     关联审批单 ID（可空）
 * @param resultStatus   调用结果状态
 * @param httpStatus     下游返回的 HTTP 状态码
 * @param latencyMs      调用耗时（毫秒）
 * @param downstream     实际调用的目标系统
 * @param errorMsg       错误信息（可空）
 */
public record AuditEvent(
        String traceId,
        Instant ts,
        String userId,
        String clientId,
        String sessionId,
        String toolName,
        RiskLevel riskLevel,
        String paramsDigest,
        String paramsSummary,
        PolicyDecision policyDecision,
        Long approvalId,
        ResultStatus resultStatus,
        Integer httpStatus,
        Long latencyMs,
        String downstream,
        String errorMsg
) {
}
