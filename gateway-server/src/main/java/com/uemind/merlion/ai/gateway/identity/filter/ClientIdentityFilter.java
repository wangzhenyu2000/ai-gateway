package com.uemind.merlion.ai.gateway.identity.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 2 步：客户端身份（设计文档 5.1）。
 *
 * <p>职责：识别客户端身份（client_id + 凭证），校验 client_registry 表中的
 * 启用状态；零信任直连原则——任何客户端（包括自研 Web Agent）不得绕过网关。</p>
 *
 * <p>骨架阶段：直接放行。</p>
 */
@Component
@Order(GatewayFilterOrder.CLIENT_IDENTITY)
public class ClientIdentityFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(identity): 校验 client_id 凭证与启用状态（client_registry 表）
        return chain.filter(exchange);
    }
}
