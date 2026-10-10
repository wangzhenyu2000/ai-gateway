/**
 * 管理 API 层（设计文档 6.8）。
 *
 * <p>职责：工具注册/编辑/上下线/版本回滚、策略配置（角色-工具矩阵，底层改 casbin_rule）、
 * 审计查询（按用户/工具/时间/traceId 检索）、审批处理。</p>
 *
 * <p>形态：MVP 阶段不做页面（API + SQL 直接操作），二期用 Vue 3 / React 实现
 * Admin Console 前端。Admin 本身走同一套飞书 SSO，操作全部进审计表（自我审计）。</p>
 */
package com.uemind.merlion.ai.gateway.admin;
