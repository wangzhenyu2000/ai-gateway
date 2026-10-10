package com.uemind.merlion.ai.gateway.policy.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 6 步：审批校验（设计文档 5.1 / 6.3）。
 *
 * <p>职责：敏感操作校验 approvalId 有效性（存在、未过期、未被消费）；
 * 审批通过生成一次性 approvalId，<b>被消费即失效</b>，防重放（文档 7）。</p>
 *
 * <p>MVP 刻意不做：只放行只读工具，从根上不需要审批流（文档 9）。
 * 本过滤器在二期接入。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.APPROVAL)
public class ApprovalFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(policy 二期): 校验并消费一次性 approvalId
        return chain.filter(exchange);
    }
}
