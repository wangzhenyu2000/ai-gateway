package com.uemind.merlion.ai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.WebFilter;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 过滤器链骨架验收测试（设计文档 5.1）。
 *
 * <p>断言网关注册的 9 个 WebFilter 按文档固定顺序排列（顺序固定，不可调整）：
 * AuthN -> ClientIdentity -> AuditContext -> RateLimit -> Policy ->
 * Approval -> ToolRouter -> ToolExecutor -> AuditWriter。</p>
 */
@SpringBootTest
class FilterChainSkeletonTest {

    /** 文档 5.1 规定的过滤器链顺序（类简单名） */
    private static final List<String> EXPECTED_ORDER = List.of(
            "AuthNFilter",
            "ClientIdentityFilter",
            "AuditContextFilter",
            "RateLimitFilter",
            "PolicyFilter",
            "ApprovalFilter",
            "ToolRouterFilter",
            "ToolExecutorFilter",
            "AuditWriterFilter"
    );

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void filtersRegisteredInDocumentedOrder() {
        List<String> actualOrder = applicationContext.getBeansOfType(WebFilter.class).values().stream()
                // 只关心网关自己的过滤器，排除 Spring Security 等框架内置 WebFilter
                .filter(f -> f.getClass().getName().startsWith("com.uemind.merlion.ai.gateway"))
                .sorted(Comparator.comparingInt(f -> {
                    Order order = AnnotationUtils.findAnnotation(f.getClass(), Order.class);
                    return order != null ? order.value() : Integer.MAX_VALUE;
                }))
                .map(f -> f.getClass().getSimpleName())
                .toList();

        assertEquals(EXPECTED_ORDER, actualOrder, "过滤器链顺序必须与设计文档 5.1 一致");
    }
}
