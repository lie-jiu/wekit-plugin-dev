# 联系人、群组、标签、验证与支付

均注册于 `JavaEngine.kt`。⚠️ = 与 WA 文档不等价，见 [../porting-diff.md#6](../porting-diff.md)。

## 身份与会话

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `getLoginWxid` | `()` → String | 当前账号 wxid | 660 |
| `getLoginAlias` | `()` → String | 微信号（可空） | 668 |
| `getTargetTalker` | `()` → String | 当前打开的会话 id | 644 |
| `setTargetTalker` | `(String)` | ➕ WeKit 独有 | 652 |
| `getDisplayName` | `(String convId)` → String | 会话显示名（备注 > 昵称 > id） | 809 |
| `getTopActivity` | `()` → Activity | 顶层 Activity 或 null | 1931 |

## 列表

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `getFriendList` | `()` → `List<FriendInfo>` | | 678 |
| `getGroupList` | `()` → `List<GroupInfo>` | | 688 |
| `getOfficialList` | `()` → `List<FriendInfo>` | 公众号；只填 `wxid`/`nickname`，其余字段为空/0 | 698 |
| `getGroupMemberList` | `(String groupId)` | ⚠️ 返回 chatroomStorage 的**成员对象**列表；WA 文档承诺 `List<String>` 成员 wxid。失败时返回空列表 | 710 |
| `getGroupMemberCount` | `(String groupId)` → int | 查库计数；失败时返回**空列表**而不是 -1 | 723 |

## 详情

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `getFriendNickName` | `(String wxId)` → String | 失败返回 `""` | 736 |
| `getFriendRemarkName` | `(String wxId)` → String | | 747 |
| `getFriendName` | `(String wxId)` → String | 显示名 | 758 |
| `getFriendName` | `(String wxId, String groupId)` → String | ⚠️ WA 文档序为 `(friendWxid, roomId)`，一致 | 767 |
| `getFriendDisplayName` | `(String groupId, String memberId)` → String | ⚠️ **与 WA 参数顺序相反**（WA: `(friendWxid, roomId)`）。失败兜底返回 `memberId` | 779 |
| `getAvatarUrl` | `(String username)` → String | CDN URL | 791 |
| `getAvatarUrl` | `(String username, boolean big)` | ⚠️ `big` 忽略，同 URL | 800 |

`FriendInfo` 只有 `getWxid/getAlias/getRemark/getNickname/getType/getSourceExtInfo/getCreateTime`——**没有 `getUserName()`**（见 [struct.md](struct.md)）。

## 标签

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `getContactLabelList` | `()` → `List<ContactLabelBean>` | | 1277 |
| `getContactByLabelId` | `(int labelId)` | ➕ WeKit 额外重载 | 1287 |
| `getContactByLabelId` | `(String labelId)` | `toIntOrNull` 后转调 | 1298 |
| `getContactByLabelName` | `(String name)` | | 1310 |
| `modifyContactLabelList` | `(String username, String labelName)` | 设单个标签 | 1883 |
| `modifyContactLabelList` | `(String username, List labelNames)` | | 1886 |

## 群管理

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `addChatroomMember` | `(String chatroomId, String member)` | | 1348 |
| `addChatroomMember` | `(String, List members)` | | 1358 |
| `delChatroomMember` | `(String, String)` / `(String, List)` | | 1370 / 1380 |
| `inviteChatroomMember` | `(String, String)` / `(String, List)` | | 1392 / 1402 |

⚠️ WA 文档另有 4 参带 `reason`（入群申请理由）的 `addChatroomMember`/`inviteChatroomMember` 重载，**WeKit 没有**；移植含 `reason` 的调用要去掉该参数或改走 `sendNetScene`。

## 好友验证

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `verifyUser` | `(String userId, String ticket, int scene)` | 打开验证界面 | 1323 |
| `verifyUser` | `(String, String, int, int privacy)` | | 1334 |

## 支付与设备

| 函数 | 签名 | 说明 | 行 |
|---|---|---|---|
| `confirmTransfer` | `(String transactionId, String transferId, String payerUsername, int)` | 收款；第 4 参 WeKit 是原始 `int`，WA 语料传 `Object invalidTime` | 1749 |
| `refuseTransfer` | `(String, String, String)` | 第 4 参内部固定 0 | 1752 |
| `uploadDeviceStep` | `(long step)` | 反射宿主 `DeviceStepManager` 上传步数 | 1907 |

`onRecvPayMsg(Object payMsgBean)` 的 `PayMsgBean` 成员见 [struct.md](struct.md)。
