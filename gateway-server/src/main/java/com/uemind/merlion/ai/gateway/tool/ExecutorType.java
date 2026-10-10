package com.uemind.merlion.ai.gateway.tool;

/**
 * 工具执行器类型（设计文档 4.2 tool_registry.executor_type，三种执行器见 6.4）。
 */
public enum ExecutorType {

    /** 调用业务系统 HTTP API */
    REST,

    /** 代理外部 MCP 服务 */
    MCP_PROXY,

    /** 网关内部实现 */
    INTERNAL
}
