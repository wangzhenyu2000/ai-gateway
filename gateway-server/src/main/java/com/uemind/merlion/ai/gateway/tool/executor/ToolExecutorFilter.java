package com.uemind.merlion.ai.gateway.tool.executor;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 8 步：工具执行（设计文档 5.1 / 6.4）。
 *
 * <p>职责：调用第 7 步选定的 ToolExecutor——注入凭证与用户身份，
 * 调用下游（超时/重试/熔断）；写类工具透传 idempotencyKey；
 * 下游 SSE 原样透传，禁止攒包。</p>
 *
 * <p>本步是主链路的实际业务执行点；骨架阶段无执行器实现，直接放行
 * （请求最终落入 DispatcherHandler，无路由时返回 404，属预期）。</p>
 */
@Component
@Order(GatewayFilterOrder.TOOL_EXECUTOR)
public class ToolExecutorFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(tool): 调 ToolExecutor.execute()，异常交给第 9 步统一留痕
        return chain.filter(exchange);
    }
}
