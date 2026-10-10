/**
 * 凭证代理层（设计文档 6.5）。
 *
 * <p>职责：凭证存取、调用下游时注入用户身份。</p>
 *
 * <ul>
 *   <li>MVP：服务账号凭证存 K8s Secret，启动加载到内存，按 credential_ref 取用；</li>
 *   <li>二期：接入 Vault/KMS，按"用户 + 工具"动态签发短期凭证（&lt;= 15min），
 *   支持 on-behalf-of token exchange，使下游审计能定位到真实用户。</li>
 * </ul>
 *
 * <p>红线：凭证<b>永不返回给客户端</b>。</p>
 */
package com.uemind.merlion.ai.gateway.credential;
