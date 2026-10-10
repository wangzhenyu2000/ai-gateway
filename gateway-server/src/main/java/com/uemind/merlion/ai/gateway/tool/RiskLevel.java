package com.uemind.merlion.ai.gateway.tool;

/**
 * 工具风险等级（设计文档 4.2 tool_registry.risk_level）。
 *
 * <p>策略引擎据此决定放行或进入审批：MVP 只放行 READ（文档 2 最小起步原则）。</p>
 */
public enum RiskLevel {

    /** 只读工具，MVP 直接放行 */
    READ,

    /** 写操作，需审批（二期） */
    WRITE,

    /** 敏感操作，强制审批 + 严格审计 */
    SENSITIVE
}
