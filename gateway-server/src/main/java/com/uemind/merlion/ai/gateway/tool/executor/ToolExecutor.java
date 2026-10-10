package com.uemind.merlion.ai.gateway.tool.executor;

import com.uemind.merlion.ai.gateway.common.CallContext;
import com.uemind.merlion.ai.gateway.tool.ExecutorType;
import com.uemind.merlion.ai.gateway.tool.ToolMeta;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 工具执行器（设计文档 6.4）。
 *
 * <p>职责：注入凭证与用户身份，调用下游（超时/重试/熔断）。
 * 由 ToolRouter 按 {@link ToolMeta#executorType()} 路由到具体实现。</p>
 *
 * <p>实现约束：</p>
 * <ul>
 *   <li>写类工具必须把 params 中的 idempotencyKey 透传给业务侧去重
 *   （Agent 超时重试是常态）；</li>
 *   <li>下游 SSE 原样透传，禁止攒包（背压由 WebFlux 支持）；</li>
 *   <li>凭证经 credential 包解析注入，永不返回给客户端。</li>
 * </ul>
 */
public interface ToolExecutor {

    /**
     * 本执行器处理的类型（REST / MCP_PROXY / INTERNAL）。
     */
    ExecutorType supports();

    /**
     * 执行一次工具调用。
     *
     * @param context 调用上下文（用户身份、traceId 等，需透传到下游）
     * @param tool    工具元数据（含执行器配置）
     * @param params  调用参数（已通过 inputSchema 校验）
     * @return 调用结果（响应式；SSE 场景由具体实现返回流式封装）
     */
    Mono<ToolResult> execute(CallContext context, ToolMeta tool, Map<String, Object> params);
}
