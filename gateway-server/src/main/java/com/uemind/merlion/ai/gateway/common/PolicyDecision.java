package com.uemind.merlion.ai.gateway.common;

/**
 * 策略判定结果三态（设计文档 5.2）。
 */
public enum PolicyDecision {

    /** 放行 */
    ALLOW,

    /** 拒绝：返回 403，审计记录 policy_decision=DENY */
    DENY,

    /** 需审批：risk_level=WRITE/SENSITIVE 的工具命中此态，进入审批流程 */
    REQUIRE_APPROVAL
}
