package com.uemind.merlion.ai.gateway.audit;

/**
 * 调用结果状态（设计文档 4.3 audit_log.result_status）。
 */
public enum ResultStatus {

    /** 成功 */
    SUCCESS,

    /** 业务错误（下游正常响应但业务校验失败） */
    BIZ_ERROR,

    /** 系统错误（下游 5xx 或调用异常） */
    SYS_ERROR,

    /** 超时 */
    TIMEOUT
}
