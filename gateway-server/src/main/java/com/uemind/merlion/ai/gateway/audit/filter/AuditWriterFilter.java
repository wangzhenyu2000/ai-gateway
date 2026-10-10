package com.uemind.merlion.ai.gateway.audit.filter;

import com.uemind.merlion.ai.gateway.common.GatewayFilterOrder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 过滤器链第 9 步：审计写入（设计文档 5.1 / 6.6）。
 *
 * <p>职责：链路末端兜底，组装 AuditEvent 投递给 AuditSink（异步批量，主链路零阻塞）。</p>
 *
 * <p>纪律：<b>审计漏记比工具报错更严重</b>——用 try/finally（响应式中等价于
 * doFinally）保证成功失败都留痕；params_summary 必须脱敏，全量参数永不落审计库。</p>
 */
@Component
@Order(GatewayFilterOrder.AUDIT_WRITER)
public class AuditWriterFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // TODO(audit): 从 Reactor Context 取 CallContext，组装 AuditEvent
        return chain.filter(exchange)
                // try/finally 兜底结构：无论成功、业务错误还是异常终止都留痕
                .doFinally(signal -> {
                    // TODO(audit): auditSink.emit(event)；骨架阶段暂无 sink 实现
                });
    }
}
