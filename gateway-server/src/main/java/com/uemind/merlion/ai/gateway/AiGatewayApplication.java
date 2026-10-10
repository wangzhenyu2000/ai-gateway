package com.uemind.merlion.ai.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业 AI 网关启动类。
 *
 * <p>定位（设计文档 1.2）：所有 AI 客户端访问企业能力的<b>唯一入口</b>。
 * 网关的世界观只有一件事：一次经过认证与授权的工具调用。</p>
 *
 * <p>内部模块结构（文档 3.2，物理单体、逻辑分层，按包组织）：</p>
 * <ul>
 *   <li>{@code access} —— 协议入口：MCP Server + REST Controller</li>
 *   <li>{@code identity} —— 身份：OIDC 资源服务器、用户身份透传、客户端身份</li>
 *   <li>{@code policy} —— 策略：Casbin 判定、限流、审批状态机</li>
 *   <li>{@code tool} —— 工具：Registry（DB）、执行器（REST/MCP/内部）、路由</li>
 *   <li>{@code credential} —— 凭证：凭证存取、用户身份注入下游请求</li>
 *   <li>{@code audit} —— 审计：异步批量写 Doris</li>
 *   <li>{@code observe} —— 可观测：OpenTelemetry、指标、告警</li>
 *   <li>{@code admin} —— 管理 API：工具注册、策略配置、审计查询、审批处理</li>
 * </ul>
 */
@SpringBootApplication
public class AiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
    }
}
