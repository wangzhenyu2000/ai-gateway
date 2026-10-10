package com.uemind.merlion.ai.gateway.policy.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 4 步：限流（设计文档 5.1）。
 *
 * <p>职责：用户 x 客户端 x 工具三维令牌桶（Bucket4j + Redis，文档 3.3），
 * 外加单会话调用预算与异常 QPS 告警（防恶意 Agent 死循环，文档 7）。</p>
 *
 * <p>顺序约束：在策略判定之前，避免恶意流量消耗策略判定资源。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.RATE_LIMIT)
public class RateLimitFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(policy): Bucket4j 三维限流；超限返回 429
        return chain.filter(exchange);
    }
}
