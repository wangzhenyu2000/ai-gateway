/**
 * 审计层（设计文档 6.6）。
 *
 * <p>职责：全链路调用留痕，第一版上线即具备完整 audit trail（审计先行原则）。</p>
 *
 * <ul>
 *   <li>写入纪律：过滤器链末端 try/finally 兜底，成功失败都留痕——
 *   <b>审计漏记比工具报错更严重</b>（文档 5.1）；</li>
 *   <li>性能：内存队列缓冲 + 批量写入（默认 500 条 / 2s 一批），主链路零阻塞；</li>
 *   <li>三种 sink：DorisStreamLoadSink（生产，Stream Load 批量写 Doris）、
 *   JdbcAuditSink（本地/测试，写 MySQL audit_log 表）、LogSink（本地开发可选）；</li>
 *   <li>降级：生产 Doris 不可用时临时切 Jdbc/文件，事后补导——审计组件不构成单点依赖。</li>
 * </ul>
 *
 * <p>字段纪律：params_summary 必须脱敏（身份证、银行卡、手机号打码），
 * 全量参数永不落审计库；取证凭 params_digest + 下游系统日志还原。</p>
 */
package com.uemind.merlion.ai.gateway.audit;
