# 结构体（`me.hd.wauxv.*` shim）

WeKit 在 `app/src/main/java/me/hd/wauxv/` 下自建 9 个 shim 类，用于承接 WA 插件的对象用法。以下成员**全部来自 WeKit 源码**，是可用上限。

## `me.hd.wauxv.data.bean.MsgInfoBean`（`shim/MsgInfoBean.kt`）

构造：`(origin: Any)`；`@JvmField val origin`、`val msg: MessageInfo`。

字段式属性（同时提供同名 getter）：`msgId:Long`、`msgSvrId:Long`、`type:Int`、`isSendInt:Int`、`createTime:Long`、`talker:String`、`originContent:String`、`imgPath:String?`、`lvBuffer:ByteArray`、`talkerId:Int`、`msgSeq:Long`。

值 getter：`getMsgId` `getMsgSvrId` `getType` `getCreateTime` `getTalker` `getOriginContent` `getImgPath` `getLvBuffer` `getTalkerId` `getMsgSeq` `getOrigin` `isSendInt()`。

类型判定：`isText isImage isEmoji isVoice isVideo isShareCard isPat isSystem isQuote isLocation isApp isLink isTransfer isRedBag isVideoNumberVideo isNote isFile isRecalled isVoip isVoipVideo isVoipVoice isSend isGroupChat isChatroom isImChatroom isOpenIM isOfficialAccount isPrivateChat` + `isEnumMsg(int)`。

发送者与内容：`getSendTalker()` `getContent()` `getMsgSource()` `getAtUserList():List<String>` `isAtMe()` `isAnnounceAll()` `isNotifyAll()`。

子结构（可能返回 null）：`getFileMsg()` `getImageMsg()` `getQuoteMsg()` `getTransferMsg()`（➕）`getPatMsg()`。

### 内嵌类

| 类 | 成员 | 对比 WA 文档 |
|---|---|---|
| `FileMsg` | `getTitle getSize(long) getExt getMd5 getUrl getKey` | ✅ 一致（6） |
| `ImageMsg` | `getAesKey` `getCdnUrl` `getMd5` `getBigImgUrl` `getMidImgUrl` `getThumbUrl` `getKey` | ➕ 超集（WA 文档只列 5） |
| `QuoteMsg` | `getTitle getSendTalker getDisplayName getMsgSource getOriginContent getSvrId getTalker getType getContent` | ➕ 超集（WA 7） |
| `TransferMsg` | `getTitle getDes getTransactionId getTransferId getBeginTransferTime getFeeDesc getInvalidTime getPayerUsername getReceiverUsername` | ➕ WA 文档无此结构 |
| `PatMsg` | `getTemplate getFromUser getCreateTime getPattedUser getReadStatus getRecordNum getShowModifyTip getSvrId getTalker` | ➕ 超集（WA 5） |

## `me.hd.wauxv.data.bean.PayMsgBean`（`onRecvPayMsg` 实参）

`origin:Any`、`username:String`、`displayName:String`、`fee:Double`、`timestamp:Int`、`status:Int`、`statusDesc:String`；`toString()` 输出 JSON；status 有 0/1/2 文案映射。与 WA 文档 6 员一致。

## `me.hd.wauxv.data.bean.ConversationBean`

`username unReadCount msgCount isSendInt conversationTime content msgType flag digest digestUser parentRef` + `isSend():Boolean`。

⚠️ WeKit **没有任何回调会投递它**（`onCreateConversationItemMenu`、`addConversationItemMenuItem` 均不存在）→ 该类目前不可达，勿在新插件里引用。

## `me.hd.wauxv.data.bean.ContactBean`

`username alias conRemark nickname` + 对应 getter + `getOrigin()`。➕ WA 文档与语料均无。

## `me.hd.wauxv.data.bean.ContactLabelBean`

`getLabelName` `getDisplayName` `getName`（14-16）、`getLabelId` `getLabelID` `getId`（17-19）。

⚠️ `getOrigin()` 直接 `error("not implemented")`（20 行）→ 调用必抛。
✅ 其余 6 个名字覆盖了 WA 插件用反射暴力试探的候选集合（`corpus:分组管理/main.java:922-970`），在 WeKit 上可直调。

## `me.hd.wauxv.data.bean.info.FriendInfo`

data class：`wxid` `alias` `remark` `nickname` `type:Int` `sourceExtInfo` `createTime:Long`；次构造 `(contact: WeContact)`。

⚠️ **没有 `getUserName()`**。WA 语料里 `f.getClass().getMethod("getUserName")`（`corpus:僵尸粉检测/main.java:791`）在 WeKit 会抛 `NoSuchMethodException` → 移植时改用 `getWxid()`。

## `me.hd.wauxv.data.bean.info.GroupInfo` / `GroupData`

`GroupInfo`：`roomId` `remark` `name` `groupData:GroupData`；次构造 `(group: WeGroup)`。
`GroupData`：`roomId` `memberIds:List` `memberNames:List` `memberCount:Int` `membersHash:Map` `mineRoomName` `owner` `notice` `noticeEditor` `noticeTime:Long`；次构造 `(roomId:String)` 直接查库。
WA 侧只证实 `GroupInfo.getRoomId()/getName()` → WeKit 为超集。

## `me.hd.wauxv.hook.HookHandle`

`HookHandle(unhook: IHookBridge.MemberUnhookHandle)`（`shim/hook/HookHandle.kt:7`）。由 `hookBefore/hookAfter/hookReplace` 返回，交给 `unhook(handle)` 释放。

⚠️ 没有实例方法 `unhook()`（原生 Xposed 的 `Unhook` 才有）；`param.hookedMethod` 在 WeKit 叫 `param.member`。
