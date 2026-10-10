package com.uemind.merlion.ai.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 骨架验收测试：Spring 上下文能完整装配启动。
 *
 * <p>说明：仅验证包结构与依赖装配正确，不依赖外部中间件
 * （Hikari 连接池首次使用时才建连，Redis 客户端懒连接）。</p>
 */
@SpringBootTest
class AiGatewayApplicationTests {

    @Test
    void contextLoads() {
        // 上下文加载成功即通过
    }
}
