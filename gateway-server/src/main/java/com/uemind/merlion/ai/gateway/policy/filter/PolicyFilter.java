package com.uemind.merlion.ai.gateway.policy.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 5 步：策略判定（设计文档 5.1 / 5.2）。
 *
 * <p>职责：调用 {@code PolicyEngine.decide()} 得三态结果：</p>
 * <ul>
 *   <li>ALLOW —— 放行；</li>
 *   <li>DENY —— 返回 403，审计记录 policy_decision=DENY；</li>
 *   <li>REQUIRE_APPROVAL —— 进入第 6 步审批校验。</li>
 * </ul>
 *
 * <p>Session Scope：判定后再与客户端声明的工具白名单取交集，只缩不放。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.POLICY)
public class PolicyFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(policy): PolicyEngine.decide() 三态判定 + Session Scope 收窄
        return chain.filter(exchange);
    }
}
