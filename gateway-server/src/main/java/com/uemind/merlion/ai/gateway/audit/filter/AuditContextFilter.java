package com.uemind.merlion.ai.gateway.audit.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 3 步：审计上下文（设计文档 5.1）。
 *
 * <p>职责：生成 traceId，汇总前两步的认证/客户端结果构建 CallContext，
 * 写入 Reactor Context，供后续所有环节取用并透传到下游。</p>
 *
 * <p>顺序约束：必须在策略判定（第 5 步）之前建立，否则拒绝调用无迹可查。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.AUDIT_CONTEXT)
public class AuditContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(audit): 生成 traceId；构建 CallContext 并写入 Reactor Context
        return chain.filter(exchange);
    }
}
