# 事实来源与回源方式

本 skill 的所有结论都可被下列命令重新导出。行号会随 WeKit 提交漂移，签名冲突时**以命令输出为准**。

## WeKit 侧（最高优先级）

仓库：本地 `WeKit`（`master`/`dev`），或 https://github.com/Ujhhgtg/WeKit

| 事实 | 位置 | 回源命令 |
|---|---|---|
| 脚本可见函数全集与参数类型 | `app/src/main/java/dev/ujhhgtg/wekit/features/items/scripting_java/JavaEngine.kt` | `grep -n 'BshMethod(' JavaEngine.kt`（数量应与 `grep -c 'setMethod('` 相等） |
| 注入全局变量 | 同上 `initNameSpace` | `grep -n 'setVariable(' JavaEngine.kt` |
| 引擎查找的回调 | 同上 | `grep -n 'getMethod(' JavaEngine.kt` |
| 回调触发点与宿主条件 | 同目录 `JavaScriptingHook.kt` | `grep -n 'hookBefore\|hookAfter\|executeAllOn' JavaScriptingHook.kt` |
| 目录/必需文件/禁用标记 | 同上 | `grep -n 'main.java\|info.prop\|disabled.flag\|SCRIPTS_DIR' JavaScriptingHook.kt` |
| Hook 桥接与 `unhookEverything` | 同目录 `JavaHookApi.kt` | `grep -n 'result = \|unhook' JavaHookApi.kt` |
| Hook 参数对象成员 | `loader/abc/IHookBridge.kt` + `utils/HookUtils.kt` | `grep -n -A14 'interface IMemberHookParam' IHookBridge.kt` |
| 「赋值 result 即跳过原方法」证据 | `loader/entry/zygisk/ArtHookBridgeRuntime.kt`、`app/src/standard/java/dev/ujhhgtg/wekit/loader/entry/lxp/LxpHookWrapper.kt`、`loader/entry/xp51/Xp51HookWrapper.java` | `grep -n 'earlyReturn\|skipOriginal\|setResult' <上述三文件>` |
| shim bean 成员 | `app/src/main/java/me/hd/wauxv/**` | `ls -R app/src/main/java/me/hd/wauxv` 后逐个读 |
| 快照容器与密钥 | `libs/common/bsh/src/main/java/bsh/snapshot/BshSnapshotHelper.java`、`app/src/main/java/dev/ujhhgtg/wekit/utils/BshSnapshotDecompiler.kt` | `grep -n 'MAGIC\|SECRET_KEY\|GCM' <两文件>` |
| 脚本依赖扩展包 | `app/src/main/java/dev/ujhhgtg/wekit/extensions/ScriptDepsPack.kt` | `grep -n 'classes.dex\|classLoader' ScriptDepsPack.kt` |
| 参数类型别名 | `app/src/main/java/dev/ujhhgtg/wekit/utils/reflection/Classes.kt` | `grep -n 'BString\|any =' Classes.kt` |
| 宿主范围 / 工程约束 | 仓库根 `AGENTS.md`；`utils/HostInfo.kt` | — |
| 用户侧文档 | `docs/features/scripting_java/java-scripting-hook.md`、`java-hook-api.md`、`decompile-bean-shell-snapshot.md` | — |

注意：WeKit 官方 gitbook（`ujhhgtgteams.gitbook.io/wekit-docs`）**没有**插件开发章节，其「开发指南」讲的是参与本仓库功能开发，不要拿它当插件 API 依据。

## WAuxiliary 侧（次级，仅用于判定差异）

| 资料 | 可达性 | 用法 |
|---|---|---|
| `HdShare/WAuxiliary_Plugin` → `docs/api/**`（12 个 method 页 + Callback/Global/Struct/QuickStart/INDEX） | 公开 | `gh api "repos/HdShare/WAuxiliary_Plugin/contents/docs/api" --jq '.[].name'`；取正文 `gh api <path> --jq '.content' \| base64 -d` |
| 同仓库 `wauxiliary-plugin-dev/references/**` | 公开 | 已验证与 `docs/api` 逐字节相同（@`1f51603`，2026-09-22） |
| 同仓库 `plugins/v126/**`、`plugins/v127/**`（75 个真实插件） | 公开 | 判断「某 API 是否真的存在于已发布 WA 版」的最强代理证据 |
| `HdShare/WAuxiliary_Public` | 公开 | WA 自身开源演示 Hook + releases（最新 `1.2.7.r1418`）；**不含**插件引擎 |
| `HdShare/WAuxiliary`（插件引擎运行时源码） | **404，私有/已删** | skill 自带的 `repository-map.md` 指向其 `me/hd/wauxv/plugin/PluginRuntime.kt`，公开渠道读不到 |
| 已发布 APK | 公开 | ⚠️ 其内置 `readme.txt` 明示「勿逆向抄袭借鉴闭源代码」→ **不做反编译**；WA 侧不可验证项一律标 ❓ |

## 结论优先级

1. 本地 WeKit 源码（命令输出）。
2. 本 skill `references/`。
3. WA `docs/api`（只描述 WA 最新版，且可能晚于其公开发布版）。
4. WA 插件语料（证明「在某已发布版真实可用」）。
5. 用户提供的示例插件。

冲突时按此顺序取值，并在回答里说明采用了哪一份。任何来源都查不到的签名 → 不实现、不猜测，向用户说明缺口。

## 许可与出处

- 本 skill 的 WeKit 侧接口清单导自 **Ujhhgtg/WeKit（GPL-3.0）** 源码，故仓库按 GPL-3.0 发布（见 `LICENSE`）。
- WAuxiliary 侧引用 **HdShare/WAuxiliary_Plugin（Apache-2.0）** 的公开 `docs/api/**`；引用其结论时保留该出处署名。
- WAuxiliary 运行时为闭源，其 APK 自带「勿逆向抄袭借鉴闭源代码」声明：本 skill 不含任何反编译所得内容，WA 侧不可验证项一律标 ❓。
- 与 WeKit/WAuxiliary 官方均无隶属关系；接口可用性以对应仓库当前源码为准。

## 刷新本 skill

改动 `JavaEngine.kt` 或 shim 类后，重跑上表命令，比对 `references/api/*` 与 `references/we-wa-diff.md` 的行号与条目数（当前基线：18 全局量 / 106 函数名 / 141 注册 / 7 回调 / 9 shim 类）。
