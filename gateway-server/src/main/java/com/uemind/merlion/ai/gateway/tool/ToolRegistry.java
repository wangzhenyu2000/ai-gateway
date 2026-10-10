package com.uemind.merlion.ai.gateway.tool;

import java.util.List;
import java.util.Optional;

/**
 * 工具注册表（设计文档 6.4）。
 *
 * <p>实现要点：DB 持久化（tool_registry 表，MySQL），策略即数据、变更不发版；
 * 策略/工具变更秒级生效（watcher 推送，文档 8 一致性）。</p>
 */
public interface ToolRegistry {

    /**
     * 按工具名查找元数据。
     *
     * @param name 工具唯一标识
     * @return 工具元数据；不存在或未启用时为空
     */
    Optional<ToolMeta> find(String name);

    /**
     * 列出全部已启用工具（供 MCP tools/list 与 GET /v1/tools 使用，
     * 返回前由 access 层按调用者权限过滤）。
     */
    List<ToolMeta> listEnabled();
}
