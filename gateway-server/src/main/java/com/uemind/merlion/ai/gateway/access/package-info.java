/**
 * 协议入口层（设计文档 6.1）。
 *
 * <p>职责：对外协议适配，包含 MCP Server 与 REST Controller 两种入口，能力对等：</p>
 * <ul>
 *   <li>MCP Server：基于 MCP Java SDK（Streamable HTTP + SSE），实现 ToolCallbackProvider，
 *   从 Tool Registry 动态生成工具列表，<b>不写任何 @Tool 硬编码方法</b>；</li>
 *   <li>REST 入口：{@code GET /v1/tools}（按调用者权限过滤）、
 *   {@code POST /v1/tools/{name}:invoke}（同步/SSE）、
 *   {@code POST /v1/approvals/{id}:approve | :reject}；</li>
 *   <li>流式转发：下游 SSE 原样透传，禁止攒包，背压由 WebFlux 天然支持。</li>
 * </ul>
 *
 * <p>协议防腐：Registry 是自有模型，MCP 只是出口适配器。未来新协议（A2A 等）
 * 只新增本层实现，不动内核。</p>
 */
package com.uemind.merlion.ai.gateway.access;
