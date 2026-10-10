/**
 * 身份与认证层（设计文档 6.2）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>登录认证：飞书 OAuth2/OIDC 全流程由网关完成，签发网关内部 JWT
 *   （claim：sub=union_id、name、dept、roles、client_id、exp）；</li>
 *   <li>资源服务器：Spring Security 校验内部 JWT（RS256，JWKS 自动轮换），
 *   {@code GET /auth/jwks.json} 暴露公钥供客户端独立验签；</li>
 *   <li>客户端身份：识别 client_id + 凭证，校验客户端启用状态（client_registry 表）；</li>
 *   <li>会话存储：Redis，sessionId -> {union_id, 姓名, 部门, 角色, larkRefreshToken(加密)}，
 *   TTL 跟随内部 JWT（8h）；</li>
 *   <li>委托 token：供 Web Agent 离线任务使用，scope 收窄、短有效期、审计标记 delegated=true。</li>
 * </ul>
 *
 * <p>身份映射纪律：以飞书 union_id 作为 CallContext.userId 主键
 * （open_id 仅单应用内唯一，不可作主键）。将来更换 IdP 只动本模块。</p>
 */
package com.uemind.merlion.ai.gateway.identity;
