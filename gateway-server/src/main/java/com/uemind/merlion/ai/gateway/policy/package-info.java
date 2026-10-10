/**
 * 策略层（设计文档 6.3）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>权限判定：Casbin（jcasbin）RBAC，工具/动作两维，判定结果三态
 *   ALLOW / DENY / REQUIRE_APPROVAL；adapter 直连 MySQL casbin_rule 表，
 *   watcher 热加载（策略即数据，变更不发版）；</li>
 *   <li>限流：Bucket4j + Redis，用户 x 客户端 x 工具三维令牌桶；</li>
 *   <li>审批状态机：PENDING -> APPROVED / REJECTED / EXPIRED；
 *   审批通过生成一次性 approvalId，被消费即失效（防重放）。</li>
 * </ul>
 *
 * <p>第一性原则：Agent Permission &lt;= User Permission，网关只做减法，永不放大；
 * Session Scope（客户端声明的工具白名单子集）同样只缩不放。</p>
 */
package com.uemind.merlion.ai.gateway.policy;
