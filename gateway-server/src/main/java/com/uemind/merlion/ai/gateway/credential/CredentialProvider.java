package com.uemind.merlion.ai.gateway.credential;

/**
 * 凭证提供方（设计文档 6.5）。
 *
 * <ul>
 *   <li>MVP：服务账号凭证存 K8s Secret，启动加载到内存，调用时按 credentialRef 取用；</li>
 *   <li>二期：接入 Vault/KMS，按"用户 + 工具"动态签发短期凭证（&lt;= 15min），
 *   支持 on-behalf-of token exchange。</li>
 * </ul>
 *
 * <p>红线：解析出的凭证只用于本服务内向下游注入，<b>永不返回给客户端</b>。</p>
 */
public interface CredentialProvider {

    /**
     * 按凭证引用解析出真实凭证。
     *
     * @param credentialRef 凭证引用（tool_registry.credential_ref，非凭证本身）
     * @return 真实凭证（仅存在于服务内存，禁止外泄）
     * @throws IllegalStateException 引用不存在或凭证已失效时抛出
     */
    String resolve(String credentialRef);
}
