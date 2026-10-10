package com.uemind.merlion.ai.gateway.tool.executor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具调用结果。
 *
 * <p>成功/业务失败/系统失败都通过本对象返回，异常由过滤器链末端
 * AuditWriter 统一兜底记录（文档 5.1 第 9 步）。</p>
 *
 * @param success      是否成功（对应审计 result_status：SUCCESS / BIZ_ERROR / SYS_ERROR / TIMEOUT）
 * @param data         业务返回数据（成功时非空）
 * @param httpStatus   下游 HTTP 状态码（INTERNAL 执行器可为 200）
 * @param errorMessage 错误信息（失败时非空，会脱敏后写入审计）
 */
public record ToolResult(
        boolean success,
        JsonNode data,
        int httpStatus,
        String errorMessage
) {
}
