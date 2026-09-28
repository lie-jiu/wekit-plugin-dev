---
name: wekit-plugin-dev
description: 当用户要求创建、修改、调试、迁移或审核 WeKit 脚本引擎 (Java) 插件——BeanShell 风格的 main.java、info.prop、scripts_java 目录、脚本内 Hook/消息/群管/朋友圈逻辑，或把 WAuxiliary（WA）插件移植到 WeKit 时使用。仅覆盖 WeKit 的 Java 脚本引擎，不覆盖 WeKit 内置 Kotlin 功能与 Python 插件通道。
---

# WeKit 脚本插件开发

以 WeKit 仓库源码为最终事实。接口索引在 `references/api/`，其中 `JavaEngine.kt:NNN` 形式的行号会腐烂——签名存疑时按 [repository-map.md](references/repository-map.md) 的 grep 模式回源核对，不要凭外部文档或记忆推断。

## 接口边界

WeKit 的 Java 脚本引擎**不是**任何同类模块插件接口的完整子集或超集：部分接口在 WeKit 上不存在，另有若干同名函数的参数顺序、单位或回调类型不同，照抄外部插件代码会静默出错。

日常 WeKit 插件开发只需 `references/api/` 各页——那里列的是 WeKit 实际注册与查找的接口，按它写就是正确的。

## 外部插件移植

接到「把其他模块的插件搬到 WeKit」这类任务时，才读 [we-wa-diff.md](references/we-wa-diff.md)（缺失入口 / 签名不等价 / 结构体成员差异 / 迁移风险排序）与 [api/wa-unported.md](references/api/wa-unported.md)（WeKit 无对应实现的外部接口清单）。逐条核对后再动手，不要凭印象移植。

## 执行流程

| 阶段 | 操作 | 检查标准 |
|---|---|---|
| 1. 明确需求 | 判定任务类型（新建 / 修改 / 调试 / 外部插件移植 / 审核）与插件根目录；按 [workflow.md](references/workflow.md) 写清触发条件、适用会话、动作、配置项、失败反馈、卸载行为 | 得到可直接检查的完成标准 |
| 2. 核对接口 | 从 [api/INDEX.md](references/api/INDEX.md) 进入，只读本次涉及的回调、全局量、结构体与方法页 | 每个脚本可见符号都有 WeKit 源码依据 |
| 3. 设计 | 涉及生命周期、目录、类加载、快照时读 [runtime.md](references/runtime.md)；移植外部插件时按上节两份差异表逐条核对 | 目录、加载顺序、失败处理、卸载责任明确；外部专有依赖已降级或去除 |
| 4. 执行 | 按完成标准落地；审核时同时按 [review-checklist.md](references/review-checklist.md) 逐项检查 | 每处改动可追溯到需求或源码 |
| 5. 验证 | 静态检查 `info.prop`、`main.java`、`loadJava` 相对路径、回调签名、`onUnload` 清理；条件允许时真机加载并读模块日志 | 交付「改动文件 / 已静态验证 / 待真机验证」三段 |

## 遇到不确定情况时

- 先查需求、插件目录、WeKit 源码。多份资料冲突时，顺序为：**WeKit 源码 > 本 skill 的 references > 外部插件官方文档 > 同版本外部插件 > 用户示例**，并说明采用了哪一份。
- 需要 Hook、反射或宿主内部类型才能满足需求时，先说明宿主版本风险（WeKit 只适配微信 8.0.65–8.0.78）并向用户确认；用户不接受就退回公开接口或缩小功能。
- 移植会删减功能、改变配置键或 `config.prop` 存储格式时，先说明改动前后差别；用户确认前保留原有行为与格式。
- 查不到的签名不要猜。WeKit 侧不存在的能力（如菜单注入）应明确告知「需改 WeKit 本体功能」，不要在插件里绕过。
- 没有真机环境时，完成静态检查并列明待验项，禁止写成「已运行通过」。

## WeKit 插件规则

- 插件根：`/data/user/0/com.tencent.mm/files/wekit/scripts_java/<插件目录名>/`（`pluginId` = 目录名，需 root 写入）。必备 `info.prop` + `main.java`，缺任一被跳过并 warn。禁用状态由目录内 `disabled.flag` 表示。
- `main.java` 是 BeanShell 顶层脚本。逻辑多时再拆模块用 `loadJava("相对路径.java")`（相对 `pluginDir`）；先加载被引用的模块，`main.java` 只保留加载语句、需共享的 Hook/任务引用、回调与模块调用。
- 引擎只查找 7 个回调：`onLoad` `onUnload` `onHandleMsg(Object)` `onClickSendBtn(String)` `onMemberChange(String,String,String,String)` `onNewFriend(String,String,int)` `onRecvPayMsg(Object)`。其他生命周期名字写了也不会被调用。
- `config.prop` 只在首次 `put*` 时生成；`getStringSet/putStringSet` 以 JSONArray 编码进单个键，手写或其他工具的存量文件不保证互通。
- `log(obj)` 写入 WeKit 模块日志（tag=插件目录名），**不产生** `pluginDir/plugin.log`，也没有 `getLogFile()`。
- 入站自动处理先排除 `msgInfoBean.isSend()`；事件回复用 `msgInfoBean.getTalker()`，当前会话主动操作才用 `getTargetTalker()`。
- `get` / `post` / `download` 均为异步，回调结果可能为 `null`；请求前先保存事件会话 ID，只在回调里使用结果。
- 依赖 `okhttp3` 或 `fastjson2` 的脚本需要安装「脚本依赖扩展包」(`script-deps`)，否则解释器找不到类；`org.json` 由宿主提供，可直接用。
- 通过 `hookBefore/hookAfter/hookReplace` 注册的 Hook 必须保存返回的 `HookHandle`，并在 `onUnload()` 中逐个 `unhook(handle)`。关闭引擎会调用 `unhookEverything()` 兜底，但脚本不应依赖它。
- `reloadPlugin()` 只重注入命名空间，**不会**重新加载 `main.java`；改脚本正文需重新开关引擎或重启微信。

## 结果要求

- 创建 / 修改 / 移植：用户要求落地时直接编辑目标插件目录，逐项满足完成标准，只交付必要文件和确有职责的模块。
- 调试：按「现象或日志 → 根因与证据（文件:行） → 最小修复 → 验证」报告，并检查所有已 `loadJava` 模块中的同类问题。
- 审核：问题优先，逐项给严重度、文件与行号、实际影响和依据；无问题时明确说明剩余验证风险。
- 涉及 WeKit 本体（而非插件）的功能缺口，输出「需要改哪个 Feature / JavaEngine 注册点」的结论，不要在插件里造替代实现。

## Resources

- [references/api/INDEX.md](references/api/INDEX.md) — 接口索引：回调、全局量、结构体、方法页（**日常开发只需这一支**）。
- [references/we-wa-diff.md](references/we-wa-diff.md) — 外部插件接口与 WeKit 的逐条差异、迁移风险排序（仅移植任务时读）。
- [references/runtime.md](references/runtime.md) — 生命周期、目录发现、类加载、快照格式、宿主伪装。
- [references/workflow.md](references/workflow.md) — 需求拆解与完成标准模板。
- [references/review-checklist.md](references/review-checklist.md) — 审核清单。
- [references/repository-map.md](references/repository-map.md) — 事实来源与回源 grep 模式。
- [assets/plugin-template/](assets/plugin-template/info.prop) — 新插件目录骨架（`info.prop` + `main.java`）。
