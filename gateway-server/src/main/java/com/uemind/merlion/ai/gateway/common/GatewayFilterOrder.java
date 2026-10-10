package com.uemind.merlion.ai.gateway.common;

/**
 * 网关过滤器链顺序常量（设计文档 5.1）。
 *
 * <p>主链路过滤器链，<b>顺序固定，不可调整</b>：</p>
 * <pre>
 *  1. AuthNFilter          校验 JWT（RS256，JWKS 自动轮换），拒绝匿名
 *  2. ClientIdentityFilter 识别客户端身份（client_id + 凭证），校验客户端启用状态
 *  3. AuditContextFilter   生成 traceId，构建 CallContext，写入 Reactor Context
 *  4. RateLimitFilter      用户 x 客户端 x 工具三维令牌桶
 *  5. PolicyFilter         Casbin 判定：ALLOW / DENY / REQUIRE_APPROVAL
 *  6. ApprovalFilter       敏感操作：校验 approvalId 有效性（存在、未过期、未被消费）
 *  7. ToolRouterFilter     按 tool_registry.executor_type 路由到执行器
 *  8. ToolExecutorFilter   注入凭证与用户身份，调用下游（超时/重试/熔断）
 *  9. AuditWriterFilter    try/finally 保证成功失败都留痕，异步批量写 Doris
 * </pre>
 *
 * <p>顺序理由（文档 5.1）：</p>
 * <ul>
 *   <li>审计上下文（3）必须在策略（5）之前建立，否则拒绝调用无迹可查；</li>
 *   <li>限流（4）在策略之前，避免恶意流量消耗策略判定资源；</li>
 *   <li>审计写入（9）用 try/finally 兜底——审计漏记比工具报错更严重。</li>
 * </ul>
 *
 * <p>注意：Spring Security 自身的 WebFilter 链 order 为 -100，先于本链执行；
 * 后续 AuthNFilter 落地时将与之整合。</p>
 */
public final class GatewayFilterOrder {

    /** 1. 认证 */
    public static final int AUTHN = 1;

    /** 2. 客户端身份 */
    public static final int CLIENT_IDENTITY = 2;

    /** 3. 审计上下文 */
    public static final int AUDIT_CONTEXT = 3;

    /** 4. 限流 */
    public static final int RATE_LIMIT = 4;

    /** 5. 策略判定 */
    public static final int POLICY = 5;

    /** 6. 审批校验 */
    public static final int APPROVAL = 6;

    /** 7. 工具路由 */
    public static final int TOOL_ROUTER = 7;

    /** 8. 工具执行 */
    public static final int TOOL_EXECUTOR = 8;

    /** 9. 审计写入（链路末端兜底） */
    public static final int AUDIT_WRITER = 9;

    private GatewayFilterOrder() {
    }
}
