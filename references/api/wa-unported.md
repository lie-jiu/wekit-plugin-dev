# WA 有、WeKit 没有的接口

移植 WA 插件时逐条对照本页。命中即需要**降级、改写或声明不支持**，不要在插件里造替身。

## 入口与回调（整体缺失）

| WA 接口 | WeKit 状况 | 移植处理 |
|---|---|---|
| `void openSettings()` | 无查找点 | 配置入口改为 `config.prop` + `toast` 提示，或走 WeKit 设置页；不要空写该方法 |
| `void onCreateChatItemMenu(Object msgInfoBean)` | 无 | 菜单类需求在 WeKit 上不可实现；若用户坚持，需改 WeKit 本体（在 `JavaEngine.initNameSpace` 增加注册与宿主菜单 hook），属功能开发不是插件工作 |
| `void onCreateHomePopMenu()` | 无 | 同上 |
| `void onCreateConversationItemMenu(Object conversationBean)` | 无 | 同上；`ConversationBean` 在 WeKit 存在但无投递方 |
| `addChatItemMenuItem(String title, String icon, Consumer<MsgInfoBean>)` | 无 | 同上 |
| `addHomePopMenuItem(String title, String icon, Runnable)` | 无 | 同上 |
| `addConversationItemMenuItem(String title, Consumer<ConversationBean>)` | 无 | 同上 |

替代路径（WeKit 已有）：`onClickSendBtn(String)` 拦发送、`onHandleMsg(Object)` 驱动自动化、`sendFile(...)` 直接投递文件、聊天工具栏由 WeKit 内置功能提供。

## 函数缺失

| WA 接口 | 证据 | WeKit 替代 |
|---|---|---|
| `getLogFile()` → File | `corpus:ai总结/main.java:909`、`corpus:微信密友/main.java:89` | 无。`log()` 进模块日志；插件自行写 `<pluginDir>/plugin.log`（`java.io.FileWriter`） |
| `sendMusicCard(talker,name,singer,url,link,appId)` | `corpus:Fetch-Music/main.java:49` | 用 `shareMusic(...)` 7 参版 |
| `downloadImg(ImageMsg, String savePath)` | `doc:PluginMsgMethod.md:216` | 取 `imageMsg.getBigImgUrl()/getMd5()/getKey()` 走 4 参版，或 `get/post` 自行下载 |
| `getBool` / `putBool` 别名 | `corpus:定时发送助手/main.java:79-80`（插件自己 try/catch 探测） | 用 `getBoolean` / `putBoolean` |
| `addChatroomMember(chatroomId, member, reason)` 及 List/invite 的 4 参 `reason` 版 | `doc:PluginContactMethod.md:179,183,201,205` | 去掉 `reason`；必须带理由时改走 `sendNetScene(Object)` |
| `getFriendDisplayName(String)` 单参重载 | ❓ 语料不确定 | 用 `getFriendName(wxid)` |
| `queryHistoryMsg(talker, startTime, isAsc, count)` 4 参 | `doc:PluginMsgMethod.md:196` | WeKit 只有 `(talker, long, int)` 且第 2 参被忽略 → 自行按 `getCreateTime()` 过滤排序 |

## 语义不等价（有同名函数，但别照抄）

见 [../we-wa-diff.md#6](../we-wa-diff.md) 的 18 条，尤其是：`getFriendDisplayName` 参数序相反、`sendText` 回调 Boolean、`sendQuoteMsg` 参数序相反、`sendVoice` 毫秒、`sendImage/sendEmoji` 的 `msgId` 失效、`mp3ToSilk/silkToMp3` 的 `hz` 失效、`getGroupMemberList` 返回对象列表、`FriendInfo` 无 `getUserName()`、`reloadPlugin()` 不重跑正文、`log()` 落点不同、`pluginDir` 是 `File`。

## 反向清单（WeKit 独有，可用于补齐 WA 功能）

`setTargetTalker` `getDisplayName` `getTopActivity` `sendFile` `sendCard` `sendXmlAppMsg` `sendXml` `revokeMsgByMsgId` `revokeMsgByMsgSvrId` `wavToSilk` `addToQueue` `sendNetScene` `jsLogin` `getOfficialList`(已对齐) `FriendInfo.getAlias/getType/getCreateTime` `GroupInfo.getGroupData` `GroupData` `ContactBean` `MsgInfoBean.getTransferMsg`/`TransferMsg` 系列、全局量 `hostLoader` `myWxId` `pluginPath` `engineId` `engineVerCode` `engineVerName`。
