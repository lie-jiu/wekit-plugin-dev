# 回调

WeKit 引擎**只查找这 7 个** BSH 方法名（`JavaEngine.kt` 内 `getMethod(` 全量核对：86,100,117,138,164,186,206）。其余名字（含 WA 的 `openSettings`、三个 `onCreate*Menu`）永远不会被调用。

查找均为「有则调用」，未定义该方法的插件被跳过；每次调用包 `try/catch`，异常只进模块日志。

| 回调 | WeKit 查找签名（JE） | 触发点（JSH） | 说明 |
|---|---|---|---|
| `void onLoad()` | `getMethod("onLoad", emptyArray())` JE:86 | 开启引擎、脚本枚举完成后 JSH:154 | 初始化变量、注册 Hook、预读配置 |
| `void onUnload()` | JE:100 | 关闭引擎 JSH:264 | 逐个 `unhook(handle)`、停线程/定时器 |
| `void onHandleMsg(Object msgInfoBean)` | 形参 `me.hd.wauxv.data.bean.MsgInfoBean` JE:117-120 | 消息插入 hookAfter JSH:96-99 | 入站处理先 `if (msgInfoBean.isSend()) return;` |
| `boolean onClickSendBtn(String text)` | `BString` JE:138-141 | 发送按钮 hookBefore JSH:102-107 | 返回 `true` → 引擎置 `param.result=null`，本次发送被抑制 JE:145-147 |
| `void onMemberChange(String type, String groupWxid, String userWxid, String userName)` | 4×`BString` JE:164-167 | `chatroom` 表成员数差分 JSH:319(`join`)/325(`left`) | 由 DB 变化推导，非实时事件；冷启动首次同步可能误报 |
| `void onNewFriend(String wxid, String ticket, int scene)` | `BString,BString,int` JE:186-189 | `fmessage_msginfo` 插入且 `isSend==0` JSH:283 | 首参取 fromusername/encryptusername，跨版本语义存疑 |
| `void onRecvPayMsg(Object payMsgBean)` | 形参 `Object`，实传 `PayMsgBean` JE:206-209 | `methodPayMsg.hookBefore` JSH:110-113 | 转账/收款消息 |

## 自动回复模板

```beanshell
void onHandleMsg(Object msgInfoBean) {
    if (msgInfoBean.isSend()) return;
    if (!msgInfoBean.isText()) return;
    String talker = msgInfoBean.getTalker();
    String content = msgInfoBean.getContent();
    if (content.equals("在吗")) {
        sendText(talker, "在");
    }
}
```

## 群欢迎模板

```beanshell
void onMemberChange(String type, String groupWxid, String userWxid, String userName) {
    if (type.equals("join")) {
        sendText(groupWxid, "[AtWx=" + userWxid + "] 欢迎加入");
    }
}
```

## Hook 与卸载模板

```beanshell
var handle = null;

void onLoad() {
    List classes = findClassList({"稳定的日志字符串"});
    if (classes.size() > 0) {
        Member m = firstMethod(classes.get(0), "目标方法名", 1);
        handle = hookAfter(m, param -> { log("hit " + param.getResult()); });
    }
}

void onUnload() {
    if (handle != null) { unhook(handle); handle = null; }
}
```

改参数或结果优先 `hookReplace`；在 WeKit 里 `hookBefore` 存在吞掉原方法的风险，见 [../porting-diff.md#7](../porting-diff.md)。

## 宿主事件之外

WeKit 不提供定时器 API：只有 `delay(long, Runnable)` 一次性延时。周期任务需自建 `Thread`/`Timer`，并在 `onUnload()` 里中断——这是 WA 与 WeKit 的共同约束。
