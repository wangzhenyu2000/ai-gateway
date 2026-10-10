package com.uemind.merlion.ai.gateway.policy;

import com.uemind.merlion.ai.gateway.common.CallContext;
import com.uemind.merlion.ai.gateway.common.PolicyDecision;
import com.uemind.merlion.ai.gateway.tool.ToolMeta;

import java.util.Map;

/**
 * 策略引擎（设计文档 6.3）。
 *
 * <p>判定逻辑统一封装为 {@code decide(CallContext, ToolMeta, params) -> PolicyDecision}。
 * 底层使用 Casbin（jcasbin），model 见文档 5.2（RBAC + keyMatch2 资源匹配）；
 * adapter 直连 MySQL casbin_rule 表，策略变更通过 watcher 热加载，不发版。</p>
 *
 * <p>第一性原则：Agent Permission &lt;= User Permission，本引擎只做减法，永不放大。</p>
 */
public interface PolicyEngine {

    /**
     * 对一次工具调用做策略判定。
     *
     * @param context 调用上下文（用户、角色、客户端等）
     * @param tool    目标工具元数据
     * @param params  调用参数（用于审批快照与敏感参数判定，不做持久化）
     * @return 三态判定结果
     */
    PolicyDecision decide(CallContext context, ToolMeta tool, Map<String, Object> params);
}
