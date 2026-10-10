/**
 * 共享内核：跨模块公用的核心类型。
 *
 * <p>设计文档 3.2 只列了 8 个功能包，未单列本包；但 {@code CallContext}（文档 4.1）
 * 由 identity 创建、贯穿 policy/tool/audit 全链路，放入任何单一功能包都会造成
 * 包间反向依赖，故设本包承载。只放纯粹的值对象与枚举，<b>不放业务逻辑</b>。</p>
 */
package com.uemind.merlion.ai.gateway.common;
