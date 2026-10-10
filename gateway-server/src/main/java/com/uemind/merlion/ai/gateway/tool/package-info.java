/**
 * 工具层（设计文档 6.4）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>Registry：工具元数据存 tool_registry 表（MySQL），策略即数据，变更不发版；</li>
 *   <li>三种执行器：REST（调业务系统 HTTP API）、MCP_PROXY（代理外部 MCP 服务）、
 *   INTERNAL（网关内部实现）；</li>
 *   <li>路由：按 tool_registry.executor_type 路由到对应执行器；</li>
 *   <li>执行纪律：注入凭证与用户身份后调用下游，带超时/重试/熔断；
 *   写类工具 schema 强制包含 idempotencyKey。</li>
 * </ul>
 *
 * <p>工具粒度纪律：按业务动作设计（query_payment_detail(orderId)），
 * 禁止向普通角色开放万能工具（如裸 query_mysql(sql)）。
 * 判断标准：工具名应该能写进审计报告，让非工程师看懂发生了什么。</p>
 */
package com.uemind.merlion.ai.gateway.tool;
