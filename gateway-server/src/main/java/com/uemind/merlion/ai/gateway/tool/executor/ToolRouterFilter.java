package com.uemind.merlion.ai.gateway.tool.executor;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 7 步：工具路由（设计文档 5.1 / 6.4）。
 *
 * <p>职责：解析请求中的工具名，从 ToolRegistry 取 ToolMeta，
 * 持有全部 {@link ToolExecutor} Bean 并按 executor_type（REST / MCP_PROXY / INTERNAL）
 * 匹配 {@link ToolExecutor#supports()} 选定执行器，存入上下文供第 8 步使用；
 * 工具不存在或未启用时返回 404。</p>
 *
 * <p>包归属说明：路由是执行的前置动作，与执行器 SPI 同包（tool.executor）。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.TOOL_ROUTER)
public class ToolRouterFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(tool): 查 ToolRegistry 选定 ToolExecutor，放入上下文
        return chain.filter(exchange);
    }
}
