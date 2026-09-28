# 消息收发与分享

全部注册于 `JavaEngine.kt`。列出的类型是 BSH 形参实际类型（`String`=java.lang.String，`long`/`bool`=原始类型）。⚠️ = 与 WA 文档不等价，先看 [../we-wa-diff.md#6](../we-wa-diff.md)。

## 同名重复注册（BSH 按签名后写覆盖）

| 函数 | 前一次 | **生效** | 差异 |
|---|---|---|---|
| `sendText(String,String,Consumer)` | JE:834 异步 doScene，回调 `lastMsgSvrId` | JE:1826 同步发送后回调 **Boolean** | ⚠️ 拿不到 svrId |
| `sendImage(String,String,String)` | JE:906 | JE:1831 | 两版都忽略 `appId` |
| `sendMediaMsg(String,Object,String)` | JE:1172 | JE:1845 | 语义基本相同 |

## 发送

| 函数 | WeKit 签名 | 说明 | 行 |
|---|---|---|---|
| `sendText` | `(String toUser, String text)` → Boolean | 支持 `[AtWx=…]` 内联 @ | 822 |
| `sendText` | `(String, String, Consumer)` → null | ⚠️ 回调收到 Boolean（成功标志），不是 svrId | 1826（覆盖 834） |
| `sendImage` | `(String, String)` → Boolean | 本地图片路径 | 894 |
| `sendImage` | `(String, String, String appId)` | ⚠️ `appId` 忽略 | 1831（覆盖 906） |
| `sendImage` | `(String, String, bool isRaw)` | ⚠️ `isRaw` 忽略 | 918 |
| `sendImage` | `(String, String, long msgId)` | ⚠️ WA 中为「引用该消息」，WeKit 忽略 msgId | 931 |
| `sendVoice` | `(String, String)` | 时长固定 0 | 943 |
| `sendVoice` | `(String, String, int durationMs)` | ⚠️ 单位是**毫秒**（WA 文档为秒） | 955 |
| `sendVideo` | `(String, String)` | | 1149 |
| `sendFile` | `(String talker, String path, String title)` | ➕ WA 文档无此名（WA 用 `shareFile`） | 968 |
| `sendEmoji` | `(String, String)` | | 1084 |
| `sendEmoji` | `(String, String, long msgId)` | ⚠️ `msgId`（WA=引用）忽略 | 1096 |
| `sendPat` | `(String talker, String pattedUser)` | 拍一拍 | 1108 |
| `sendShareCard` | `(String, String)` | 联系人名片 | 1137 |
| `sendCard` | `(String, String)` | ➕ 等价 `sendXmlAppMsg`，WA 无此名 | 1001 |
| `sendLocation` | `(String talker, String poiName, String label, String x, String y, String scale)` | x=经度 y=纬度 | 1120 |
| `sendLocation` | `(String talker, JSONObject{poiName,label,x,y,scale})` | | 1834 |
| `sendQuoteMsg` | `(String talker, long msgId, String text)` | ⚠️ WA 参数序为 `(talker, content, msgId)`，**顺序相反** | 1055 |
| `insertSystemMsg` | `(String talker, String content, long createTime)` | 插入本地系统消息 | 1013 |
| `queryHistoryMsg` | `(String talker, long ignored, int limit)` | ⚠️ 第 2 参被忽略；等价 `getMessages(talker,1,limit)`。WA 现行文档为 `(String,long startTime,boolean isAsc,int count)`，其 3 参旧式在 r1439 起抛异常 | 1070 |
| `revokeMsg` | `(long msgId)` | 转 `revokeMsgByMsgId` | 1025 |
| `revokeMsgByMsgId` | `(long)` | ➕ | 1035 |
| `revokeMsgByMsgSvrId` | `(long)` | ➕ | 1045 |
| `downloadImg` | `(String talker, String content, String ?, String savePath)` | ⚠️ 会从内容里解析 `[AtWx=…]` 取 URL，再 OkHttp GET 落盘；WA 的 `(md5,cdnUrl,aesKey,savePath)` 与 `(ImageMsg,savePath)` 语义均不严格对应，WA 第二重载 WeKit **无** | 1890 |

## XML / 应用消息

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `sendXmlAppMsg` | `(String toUser, String xml)` | ➕ 名字 WeKit 独有 | 981 |
| `sendXml` | `(String toUser, String xml, int type)` | ⚠️ `type` 忽略，一律走 appmsg | 991 |
| `sendXmlMsg` | `(String toUser, String xml)` | WA 未写进文档但真实存在，WeKit 已实现 | 1821 |
| `sendCipherMsg` | `(String talker, String title, String content)` | 构造 type=1 appmsg，带 `|WA|` 标记 | 1854 |
| `sendNoteMsg` | `(String talker, String content)` | type=53（笔记/接龙） | 1864 |
| `sendAppBrandMsg` | `(String talker, String title, String pagePath, String ghName)` | type=33 小程序 | 1872 |

## 分享族

| 函数 | 签名 | 行 |
|---|---|---|
| `shareFile` | `(String talker, String title, String path, String appId)` | 1161 |
| `sendMediaMsg` | `(String talker, Object media, String appId)` | 1845（覆盖 1172） |
| `shareWebpage` | `(String, String title, String desc, String url, byte[] thumb, String appId)` | 1182 |
| `shareVideo` | `(String, String title, String desc, String url, byte[] thumb, String appId)` | 1195 |
| `shareText` | `(String talker, String text, String appId)` | 1208 |
| `shareMusic` | `(String, String title, String desc, String musicUrl, String dataUrl, byte[] thumb, String appId)` | 1218 |
| `shareMusicVideo` | `(String, String, String, String, String, String singer, int duration, String lyric, byte[] thumb, String appId)` | 1234 |
| `shareMiniProgram` | `(String, String title, String desc, String userName, String path, byte[] thumb, String appId)` | 1256 |

全部与 WA 文档同名同参；`thumb` 为 `byte[]`，WeKit 侧 `as?` 允许 null。WeKit **没有** WA 语料里那个 6 参 `sendMusicCard`。

## 网络原语

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `addToQueue` | `(Object netScene)` | ➕ WeKit 独有：直接投递宿主 NetScene | 1813 |
| `sendNetScene` | `(Object netScene)` | ➕ 同上 | 1817 |
| `jsLogin` | `(String url, Consumer callback)` | ➕ 公众号网页静默登录 | 1757 |

## 写法约束

- 除 `sendText/sendImage/...` 返回 Boolean 外，发送类调用是同步入队的；需要「发送成功后再做某事」时不要依赖返回值时序，用 `delay` 或事件回调。
- `queryHistoryMsg` 无排序与时间过滤能力；需要按时间段取历史要在脚本内自行过滤 `msgInfoBean.getCreateTime()`。
- 引用类需求（WA 的 `sendImage(...,msgId)` / `sendEmoji(...,msgId)`）在 WeKit 上只能改用 `sendQuoteMsg`（注意其参数序与 WA 相反）。
