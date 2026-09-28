# 审核清单

按顺序检查；每条给出严重度（阻断 / 高 / 中 / 低）、位置（文件:行）、影响与依据。无问题也要说明剩余验证风险。

## 阻断级

1. 出现 WeKit 不存在的回调名：`openSettings`、`onCreateChatItemMenu`、`onCreateHomePopMenu`、`onCreateConversationItemMenu`。写了永远不会执行 → 功能静默缺失。
2. 出现 WeKit 不存在的函数：`addChatItemMenuItem`、`addHomePopMenuItem`、`addConversationItemMenuItem`、`getLogFile`、`sendMusicCard`（对照 [api/wa-unported.md](api/wa-unported.md)）。
3. `getFriendDisplayName(a,b)` 按 WA 的 `(friendWxid, roomId)` 顺序传参（WeKit 相反）。
4. `sendQuoteMsg(talker, msgId, text)` 按 WA 的 `(talker, content, msgId)` 传参。
5. `sendText(talker, text, cb)` 里把 `cb` 的入参当 `Long` svrId 使用。
6. `sendVoice(...,int)` 按秒传时长（WeKit 是毫秒）。
7. 缺 `main.java` 或 `info.prop`（任一缺失 → 插件根本不被加载）。
8. `onUnload()` 缺失或不完整：注册的 Hook 未逐个 `unhook`、自建 `Thread`/`Timer` 未中断。

## 高

9. 依赖 `okhttp3` / `fastjson2` 却未要求安装 `script-deps` 扩展包。
10. 使用 `FriendInfo.getUserName()`（WeKit 无此 getter）。
11. 用 `queryHistoryMsg` 的返回值假设按时间排序或已按 startTime 过滤。
12. 用 `getGroupMemberList()` 结果当 `List<String>` wxid 遍历。
13. `mp3ToSilk/silkToMp3` 传 `hz` 并期望生效；或依赖其返回 `0=成功`。
14. `downloadImg` 走 `ImageMsg` 重载（WeKit 只有 4 参版，且会解析 `[AtWx=…]`）。
15. `String p = pluginDir;`（WeKit 中 `pluginDir` 是 `java.io.File`）。
16. `log()` 之后去读 `<pluginDir>/plugin.log`（WeKit 不落该文件）。
17. 用 `reloadPlugin()` 期望重新加载 `main.java` 正文。
18. `firstField` 取父类字段（WeKit 中它 `getDeclaredField`，要改用 `getField`）。
19. `post(...)` 假设自动 JSON 编码 body（回源确认后再依赖）。
20. 依赖 `moduleVer` 做 WeKit 版本分支（恒为 1418）。

## 中

21. 入站处理未先 `if (msgInfoBean.isSend()) return;` → 自触发循环。
22. 事件回调里用 `getTargetTalker()` 代替 `getTalker()` → 回复到错误会话。
23. 异步 `get/post/download` 回调未做 null 判定，或在回调外使用已过期的会话上下文。
24. `getStringSet/putStringSet` 与 WA 存量文件混用（JSONArray 单键编码）。
25. 混来源选择项只存 ID 字符串，之后靠格式猜类型 → 应存对象或带类型标记。
26. 写死 `/sdcard/Android/media/com.tencent.mm/WAuxiliary/...` 路径（WeKit 插件根是私有 `files/wekit/scripts_java/`，且 `cacheDir` 是全局共享目录）。
27. 跨插件假设可见其他插件的 BSH 变量（每插件独立 `Interpreter`）。
28. 未处理的 `ContactLabelBean.getOrigin()` 调用（WeKit 中必抛）。

## 低

29. `info.prop` 多余键（`contact`/`group`/`source`）——WeKit 忽略，可保留但不必依赖。
30. 创建空 `config.prop`/`readme.md` 占位（`config.prop` 由首次写入生成）。
31. `main.java` 内联过多逻辑，未按 `loadJava` 拆分职责。
32. 日志噪声：循环内无条件 `log()`。

## 交付格式

```
问题（按严重度）
- [阻断] main.java:42  onCreateChatItemMenu 在 WeKit 不会被调用
  影响：聊天菜单入口整块失效
  依据：references/api/wa-unported.md；JavaEngine.kt getMethod 全量核对
  建议：改用 onHandleMsg + 关键词触发，或作为 WeKit 本体功能开发

已静态验证：<清单>
待真机验证：<清单>
```
