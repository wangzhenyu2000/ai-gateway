package com.uemind.merlion.ai.gateway.audit;

/**
 * 审计写入出口（设计文档 6.6）。
 *
 * <p>三种实现，sink 可配置切换：</p>
 * <ul>
 *   <li>{@code DorisStreamLoadSink} —— 生产：Stream Load 批量写 Doris（默认 500 条 / 2s 一批）；</li>
 *   <li>{@code JdbcAuditSink} —— 本地/测试：写 MySQL audit_log 表，字段与 Doris 版一致；</li>
 *   <li>{@code LogSink} —— 本地开发（可选）：审计事件直接打日志文件。</li>
 * </ul>
 *
 * <p>纪律：内存队列缓冲 + 批量写入，主链路零阻塞；Doris 不可用时降级 Jdbc/文件，
 * 审计组件不构成网关的单点依赖。</p>
 */
public interface AuditSink {

    /**
     * 投递一条审计事件（异步缓冲，不阻塞调用方）。
     *
     * <p>调用方纪律：过滤器链末端 try/finally 中调用，
     * 成功失败都必须留痕——审计漏记比工具报错更严重（文档 5.1）。</p>
     */
    void emit(AuditEvent event);
}
