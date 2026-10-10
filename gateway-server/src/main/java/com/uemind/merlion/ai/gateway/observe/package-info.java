/**
 * 可观测层（设计文档 3.3 / 8）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>链路追踪：OpenTelemetry，traceId 贯穿客户端到业务系统；</li>
 *   <li>指标：Prometheus（QPS、延迟、限流命中率、审计队列积压等）；</li>
 *   <li>告警：队列积压、异常 QPS、下游故障等。</li>
 * </ul>
 *
 * <p>性能目标：单副本 500 QPS，P99 网关自身开销 &lt; 20ms（不含下游）。</p>
 */
package com.uemind.merlion.ai.gateway.observe;
