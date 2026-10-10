package com.uemind.merlion.ai.gateway.identity.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 1 步：认证（设计文档 5.1）。
 *
 * <p>职责：校验网关内部 JWT（RS256，公钥来自 {@code GET /auth/jwks.json}，自动轮换），
 * 拒绝匿名请求；从 JWT claim 中解析 union_id / name / dept / roles / client_id，
 * 供第 3 步构建 CallContext 使用。登录流程（飞书 OAuth）见文档 6.2。</p>
 *
 * <p>骨架阶段：直接放行。落地时与 Spring Security 的资源服务器配置整合，
 * 本过滤器负责将认证结果转存为网关自有模型。</p>
 */
@Component
@Order(GatewayFilterOrder.AUTHN)
public class AuthNFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(identity): 校验内部 JWT，拒绝匿名；解析 claim 暂存 exchange attributes
        return chain.filter(exchange);
    }
}
