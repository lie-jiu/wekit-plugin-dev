# WeKit ↔ WAuxiliary 插件接口差异

WeKit `脚本引擎 (Java)` 是 WA 插件体系的兼容重实现。本表给出**不等价**之处，逐条带依据。

标记：✅ 一致｜⚠️ 同名但签名/语义不同｜❌ WeKit 缺失｜➕ WeKit 独有｜❓ 不可验证。

依据缩写：`JE`=`app/src/main/java/dev/ujhhgtg/wekit/features/items/scripting_java/JavaEngine.kt`，`JSH`=同目录 `JavaScriptingHook.kt`，`JHA`=同目录 `JavaHookApi.kt`，`JP`=`JavaPlugin.kt`，`shim/…`=`app/src/main/java/me/hd/wauxv/…`，`BSD`=`utils/BshSnapshotDecompiler.kt`，`doc:`=`HdShare/WAuxiliary_Plugin@1f51603` 的 `docs/api/**`（已验证与其 skill 快照逐字节相同），`corpus:`=同仓库 75 个真实 WA 插件的调用点。行号会腐烂，回源方式见 [repository-map.md](repository-map.md)。

WA 侧可信度限制：WA 运行时源码 `HdShare/WAuxiliary` 为私有/已删（HTTP 404），`moduleVer` 真值、发现扫描机制、`onUnload` 触发时机等均不可验证；已发布的最新公开 WA 版是 `1.2.7.r1418`，而社区插件已按 `r1439`/`r1454` 写门控。

## 1 载体与目录

| 项 | WA | WeKit | 状态 |
|---|---|---|---|
| 插件根 | `/storage/emulated/0/Android/media/com.tencent.mm/WAuxiliary/Plugin/`（分身 `/storage/emulated/999/…`） | `/data/user/0/com.tencent.mm/files/wekit/scripts_java/`（`moduleRoot`=filesDir/`wekit`，JSH:70） | ⚠️ 私有目录，写入需 root；不能像 WA 拖进 /sdcard |
| 单插件目录 | 作者自定文件夹 | 一级子目录，目录名即 `pluginId`（JSH:119-121） | ✅ |
| 必备文件 | `info.prop`+`main.java` | 同名；缺任一 → 跳过并 warn（JSH:127-132） | ✅ |
| 启用/禁用 | WA 设置页开关，目录内无标记文件（存储位置 ❓） | 目录内 `disabled.flag` 存在即禁用；UI 开关写/删该文件（JSH:68,240-253） | ⚠️ |
| 插件日志 | `<pluginDir>/plugin.log` | 无该文件，走 `WeLogger`（JE:477） | ⚠️ 见 §6#12 |
| 脚本依赖库 | WA 内置 okhttp/fastjson | 需安装扩展包 `script-deps`（fastjson2+okhttp+kotlin-stdlib 的 dex，SDP:13-24），未装则开关时弹窗建议（JSH:158-162） | ⚠️ |
| 热重载 | `boolean reloadPlugin()` 真重载（`doc:PluginOtherMethod.md:94`） | `reloadPlugin()` 仅重注入命名空间，不重跑 `main.java`（JE:1920-1924） | ⚠️ 见 §6#13 |
| BeanShell | fork `HdShare/beanshell`（上游无 snapshot 类） | fork `libs/common/bsh`，含 `bsh/snapshot/*` | ⚠️ |

## 2 `info.prop`

| key | WA | WeKit | 状态 |
|---|---|---|---|
| `name` `author` `version` `updateTime` | 文档承认的 4 键 | 只解析这 4 键，`name` 缺省 `"unnamed"`（JP:21-37） | ✅ |
| `contact` `group` `source` | 有插件在用，是否解析 ❓ | 一律忽略 | ⚠️ |
| 版本/权限声明键 | 无机制（75 份 info.prop 全量核对） | 无 | ✅ |
| `updateTime` 格式 | `YYYYMMDD` 只在 skill/CONTRIBUTING 强制，`docs/api` 未写 | 不校验 | ⚠️ |

## 3 生命周期回调（WA 11 → WeKit 7）

| WA 回调（原文签名） | WeKit | WeKit 触发点 |
|---|---|---|
| `void onLoad()` | ✅ JE:86-90 | 引擎 onEnable 加载完成 JSH:154 |
| `void onUnload()` | ✅ JE:100-103 | 引擎 onDisable JSH:264 |
| `void onHandleMsg(Object msgInfoBean)` | ✅ JE:117-124（形参实为 `shim/MsgInfoBean`） | 消息插入 hookAfter JSH:96-99 |
| `boolean onClickSendBtn(String text)` | ✅ JE:138-147；返回 true → `param.result=null` 拦发 | 发送按钮 hookBefore JSH:102-107 |
| `void onMemberChange(String type,String groupWxid,String userWxid,String userName)` | ✅ JE:164-171；`type∈{join,left}` | `chatroom` 表成员数差分 JSH:319,325 |
| `void onNewFriend(String wxid,String ticket,int scene)` | ✅ JE:186-193 | `fmessage_msginfo` 插入且 `isSend==0` JSH:283（首参 fromusername/encryptusername，语义存疑） |
| `void onRecvPayMsg(Object payMsgBean)` | ✅ JE:206-213（WA 语料 0 使用） | `methodPayMsg.hookBefore` JSH:110-113 |
| `void openSettings()` | ❌ 全仓无 BSH 查找 | — |
| `void onCreateChatItemMenu(Object msgInfoBean)` | ❌ 0 命中 | — |
| `void onCreateHomePopMenu()` | ❌ 0 命中 | — |
| `void onCreateConversationItemMenu(Object conversationBean)` | ❌ 0 命中 | — |

WeKit 引擎**只查找这 7 个** BSH 方法名（`getMethod(` 全量核对）。任何第 8 种回调名都是无效代码。

## 4 全局变量（WA 文档 12 → WeKit 18）

| 变量 | WeKit 类型/来源 | 状态 |
|---|---|---|
| `hostContext` `hostVerName` `hostVerCode` `hostVerClient` `cacheDir` `moduleVer` `pluginId` `pluginName` `pluginAuthor` `pluginVersion` `pluginUpdateTime` | 全部存在，JE:235-258 | ✅（WA 侧类型全部 ❓） |
| `pluginDir` | **`java.io.File`**（JE:253），WA 文档记为 `String` | ⚠️ 唯一类型冲突：`pluginDir + "/x"` 仍可用，`String p = pluginDir` 失败 |
| `hostLoader` `myWxId` `pluginPath` `engineId` `engineVerCode` `engineVerName` | `ClassLoader`/`String`/`String`/`String`/`Int`/`String`，JE:239,240,252,262-264 | ➕ |

`global.foo()` 在两侧都不是注入变量，而是 BeanShell 内建根命名空间（`bsh/Name.java:538`）。

## 5 函数总账

| 维度 | WA | WeKit |
|---|---|---|
| 文档化名 / 重载 | 94 / 130 | 106 名 / 141 注册（138 唯一签名） |
| WeKit 未实现 | 3 个 `add*MenuItem`（+ 对应回调） | — |
| WA 未写进文档但真实存在 | `getLogFile` `sendXmlMsg` `sendMusicCard` `confirmTransfer` `refuseTransfer` | WeKit 命中 `sendXmlMsg`(JE:1821)、`confirmTransfer`(JE:1749)、`refuseTransfer`(JE:1752)；缺 `getLogFile`、`sendMusicCard` |
| WeKit 独有名（12） | — | `wavToSilk` `setTargetTalker` `getDisplayName` `sendFile` `sendXmlAppMsg` `sendXml` `sendCard` `revokeMsgByMsgId` `revokeMsgByMsgSvrId` `addToQueue` `sendNetScene` `jsLogin` |
| 完全对齐的域 | config 12/12、reflect 12/12、dexkit 2/2、sns 8/8、http 6/6、audio 名 2/3 | 语义差见 §6 |
| 同名重复注册（BSH 后写覆盖前写） | — | `sendText(,,Consumer)` JE:834→1826、`sendImage(,,String)` JE:906→1831、`sendMediaMsg(,,String)` JE:1172→1845 |

## 6 同名但**不字节等价**（迁移即出错）

| # | 函数 | WA 原文 | WeKit 实况 | 后果 |
|---|---|---|---|---|
| 1 | `getFriendDisplayName` | `(String friendWxid, String roomId)` `doc:PluginContactMethod.md:96` | `(groupId, memberId)` JE:776-786 | **参数顺序相反**，静默错数据 |
| 2 | `sendText(,,Consumer)` | `Consumer<Long>`=服务端 svrId，可 null `doc:PluginMsgMethod.md:20,25` | 生效实现 JE:1826 传 **Boolean**；`Consumer<Long>` 版 JE:834 被同名同签名后注册覆盖 | ClassCastException / 逻辑反转 |
| 3 | `queryHistoryMsg` | `(String,long startTime,boolean isAsc,int count)`；旧 3 参 r1439 起抛异常 `corpus:ai总结/readme.md:77` | `(String talker, long ignored, int limit)`，第 2 参被忽略，等价 `getMessages(talker,1,limit)` JE:1070-1075 | WeKit 停留在 WA 已废弃形态；startTime/排序不可用 |
| 4 | `sendVoice(,,int)` | `duration` = **秒** `doc:PluginMsgMethod.md:45,49` | `durationMs` = **毫秒** JE:952-963 | 时长差 1000 倍 |
| 5 | `sendImage(,,long msgId)` | 引用回复该 msgId `doc:62,68` | msgId 忽略 JE:928-938 | 引用静默丢失 |
| 6 | `sendEmoji(,,long)` | 引用 `doc:90` | msgId 忽略 JE:1096 | 同上 |
| 7 | `sendImage(,,String appId)` | 带 appId | appId 忽略 JE:903-913 | — |
| 8 | `mp3ToSilk` / `silkToMp3` `(,,int hz)` | `hz` 生效、默认 24000，返回 int 状态码 `doc:PluginAudioMethod.md:18-42` | 第 3 参忽略，返回 `AudioUtils` 结果（状态码语义未确认）JE:270-303 | 采样率不可控 |
| 9 | `getGroupMemberList` | `List<String>`（成员 wxid）`doc:152` | 直接返回 chatroomStorage 反射结果=成员**对象**列表 JE:707-719 | 取 wxid 的脚本崩 |
| 10 | `downloadImg` | 重载 `(MsgInfoBean.ImageMsg, String)` `doc:216` | 只有 4×String，且实现会解析 `[AtWx=…]` JE:1890 | 结构体重载缺失 |
| 11 | `getContactByLabelId` | 仅 `String` `doc:266` | `int`+`String` 两重载 JE:1287,1298 | ➕超集 |
| 12 | `log(Object)` | 写 `<pluginDir>/plugin.log` `doc:86,89` | 写 `WeLogger.i(插件目录名, …)` JE:477，且无 `getLogFile()` | 定位日志方式不同 |
| 13 | `reloadPlugin()` | `boolean`、真重载 | `void`、不重跑正文 JE:1920-1924 | 语义不等价 |
| 14 | `post(url,params,headers,cb)` | `Content-Type` 含 `application/json` 时发 JSON body `doc:39,46` | 源码注释为 form POST JE:1549 | 需回源确认是否实现 JSON 分支 |
| 15 | `get/post/download` 默认超时 | 30 s `doc:25` | 未见文档化默认值 | ❓ |
| 16 | `getBool` / `putBool` | corpus 探测的疑似别名（不可靠） | 只有 `getBoolean/putBoolean` | ➖ |
| 17 | `sendCipherMsg` `sendNoteMsg` `sendAppBrandMsg` `sendLocation` `share*` | 文档有、75 插件 0 调用 | WeKit 全部实现 JE:1120-1270,1854-1882 | WeKit 覆盖更全 |
| 18 | `unhook` | 只有 `void unhook(HookHandle)`；`handle.unhook()` 属原生 Xposed `Unhook` | `shim/HookHandle` 无 `unhook()` 方法；另有引擎级 `unhookEverything()`（JHA:66，onDisable 全清 JSH:262） | ✅大体一致 |

## 7 Hook 参数对象（最危险）

| 项 | WA | WeKit | 状态 |
|---|---|---|---|
| 回调参数类型 | `XC_MethodHook.MethodHookParam` | `dev.ujhhgtg.wekit.utils.HookParam` = `IHookBridge.IMemberHookParam`（`utils/HookUtils.kt:14`） | ⚠️ |
| 成员 | `thisObject` `args` `getResult()/setResult()` `throwable` `hookedMethod` | `member`（对应 `hookedMethod`，**名字不同**）`thisObject` `args` `result`(var) `throwable` `extra`（`loader/abc/IHookBridge.kt:18-30`） | ⚠️ 用 `param.hookedMethod` 的脚本在 WeKit 要改 `param.member` |
| `hookBefore` 语义 | 纯观察，Consumer 返回值被丢弃 | `JHA:30` 写成 `result = consumer.accept(this)` → `Consumer` 返回 `Unit`；三套桥接都把「给 result 赋值」当作跳过原方法（`ArtHookBridgeRuntime.kt:135-144` `earlyReturn=true`、`LxpHookWrapper.kt:149-158` `skipOriginal=true`、`Xp51HookWrapper.java:63-66` 转 `setResult`） | ⚠️ **疑似缺陷**：每次脚本 `hookBefore` 都可能吞掉宿主原方法。源码层面可确认，**未经真机验证** |
| 直接 `XposedBridge.hookMethod` | 可用，但需关闭 WA「Xposed API 调用保护」（仅见于 `corpus:HookDemo/main.java:15` 注释） | ❓ 未验证 | ❓ |

插件写法建议：只观察用 `hookAfter`；要改参数或结果优先 `hookReplace(Member, Function)`（WeKit 中 `Function` 返回值即方法结果，JHA:49-58）；确需 `hookBefore` 时先真机验证原方法是否仍执行。

## 8 结构体成员

| bean | WA 证据 | WeKit 实际 | 状态 |
|---|---|---|---|
| `MsgInfoBean` | 文档 43 员（`doc:PluginStruct.md:19-69`） | 全含，另加 `getMsgSvrId` `getOriginContent` `getImgPath` `getLvBuffer` `getTalkerId` `getMsgSeq` `isSendInt()` `getTransferMsg()` `isEnumMsg(int)` `getOrigin()`（`shim/MsgInfoBean.kt`） | ➕超集 |
| ↳ `ImageMsg` | 5 员 | 7（+`getAesKey` `getCdnUrl`） | ➕ |
| ↳ `QuoteMsg` | 7 员 | 9（+`getSvrId` `getOriginContent`） | ➕ |
| ↳ `PatMsg` | 5 员 | 9 | ➕ |
| ↳ `FileMsg` | 6 员 | 6 员 | ✅ |
| ↳ `TransferMsg` | 文档无 | 9 员 | ➕ |
| `ConversationBean` | 10 员，由 `onCreateConversationItemMenu` 投递 | 类存在（`shim/ConversationBean.kt`）但**无任何生产者** | ❌不可达 |
| `PayMsgBean` | 6 员 | 6 员一致（`shim/PayMsgBean.kt:9-15`） | ✅ |
| `FriendInfo` | corpus 证实 `getWxid` `getNickname` `getRemark` **`getUserName`**（末者靠反射） | data class `wxid,alias,remark,nickname,type,sourceExtInfo,createTime`（`shim/info/FriendInfo.kt:8-14`）→ **无 `getUserName`** | ❌照抄即 `NoSuchMethodException` |
| `GroupInfo` | `getRoomId` `getName` | 4 员（+`getRemark` `getGroupData`） | ➕超集 |
| `ContactLabelBean` | 成员 ❓（插件靠暴力反射试探 `corpus:分组管理:922-970`） | `getLabelName` `getDisplayName` `getName` `getLabelId` `getLabelID` `getId`；`getOrigin()` 抛 `error("not implemented")`（`shim/ContactLabelBean.kt:14-20`） | ⚠️超集+一个必抛方法 |
| `ContactBean` `GroupData` | 文档与语料均无 | WeKit 有 | ➕ |

## 9 版本伪装与宿主范围

| 项 | WA | WeKit | 状态 |
|---|---|---|---|
| `moduleVer` | 变量存在，类型与取值 ❓（文档只写「模块版本」，75 插件 0 引用）；公开最高版 `1.2.7.r1418`，社区已按 r1439/r1454 门控 | 硬编码 **1418**（`WA_MODULE_VER` JE:78，注入 JE:244） | ⚠️ 伪装值=最新公开版号；`moduleVer>=1439` 分支在 WeKit 上走旧路径，而旧 `queryHistoryMsg` 三参在 WeKit 可用、在新 WA 上抛异常 |
| 宿主微信范围 | 公开 README 称 8.0.44–8.0.65（陈旧） | 8.0.65–8.0.78 | ⚠️ 仅重叠 8.0.65 |
| `isAtLeast` 类版本判定 | 插件面无 | 插件面无 | ✅ |

## 10 `.bshs` 加密快照

| 项 | WA | WeKit | 状态 |
|---|---|---|---|
| 脚本 API | `compileSnapshot(String)`→输出 `path+".bshs"`、`evalSnapshot(String)`、`evalSnapshot(InputStream)` `doc:PluginOtherMethod.md:40-51` | 三个同名同参（JE:599,618,1902） | ✅ |
| 容器与密钥 | 内部表示 ❓ | `MAGIC='BSHS'` + 1B 版本 + 12B IV + AES/GCM/128tag，体为序列化的 `BshSnapshot{formatVersion, Node[]}`（`libs/common/bsh/…/BshSnapshotHelper.java:22-26,43-75`）；密钥字面量 `"0123456789abcdef"`（BSD:64-65，WeKit 侧标注为从 WA 反编译源码恢复） | ⚠️ 能否执行 WA 产出的快照**未经证实** |
| 迁移工具 | — | `BSD` 可把 `.bshs` 反编译回 Java 伪代码（功能「反编译 BeanShell 快照」） | ➕ |

## 11 WeKit 独有、与 WA skill 无关的通道

| 通道 | 形态 | 依据 |
|---|---|---|
| Python 插件 | `scripts_python/<dir>/plugin.json`（`schema,id,name,version,author,description,entry="main",minWeKitVersionCode,processes`）+ `main.py` | `features/items/scripting_python/plugin/PythonPluginManager.kt:41,192-193`、`PythonPluginManifest.kt:5-15` |
| 内置 Kotlin Feature | 编译进 APK、KSP 注册，**不是**插件 | `AGENTS.md` |
| `ExtensionPack` | 官方二进制资源包下载框架（含 `script-deps`），非第三方代码通道 | `extensions/ExtensionPack.kt:12-73` |
| `@AgentTool` | 编译期生成 WeAgent 内置工具表，不可外部加载 | `features/AgentToolScanner.kt:29-48` |

## 12 迁移风险排序

| 序 | 风险 | 表现 |
|---|---|---|
| 1 | §6#1 参数序反 | 静默错误数据，无异常 |
| 2 | §7 `hookBefore` 吞原方法 | 被 hook 的宿主逻辑不再执行（需真机验证） |
| 3 | §6#2 `sendText` 回调类型 | ClassCastException / 逻辑反转 |
| 4 | §3 四回调 + §6 三 `add*MenuItem` 全无 | 菜单/设置类插件整体失效 |
| 5 | §6#4 `sendVoice` 单位 | 语音时长错 3 个数量级 |
| 6 | §8 `FriendInfo.getUserName` 缺 | 反射调用抛异常 |
| 7 | §6#3 `queryHistoryMsg` 形态 | 新 WA 的 4 参调用直接失败 |
| 8 | §9 `moduleVer=1418` | 版本门控走错分支 |
| 9 | §1 `disabled.flag`/私有目录/`script-deps` | 装不上、找不到插件、`import` 失败 |
