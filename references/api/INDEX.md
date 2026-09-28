# WeKit Java 脚本接口索引

快照来源：WeKit `master`（`JavaEngine.kt` 1965 行、`JavaScriptingHook.kt` 330 行）+ WA 官方 `docs/api`（`HdShare/WAuxiliary_Plugin@1f51603`）。行号仅用于回源定位，签名以 `JavaEngine.kt` 为准；有疑义按 [../repository-map.md](../repository-map.md) 的 grep 模式重跑。

## 页面

- [callback.md](callback.md) — WeKit 实际查找的 7 个回调、触发点、模板。
- [global.md](global.md) — 注入命名空间的 18 个全局变量及类型。
- [struct.md](struct.md) — `me.hd.wauxv.*` shim bean 的成员清单。
- [method-msg.md](method-msg.md) — 收发、撤回、历史、分享/媒体消息。
- [method-contact.md](method-contact.md) — 好友、群、成员、标签、验证、转账。
- [method-service.md](method-service.md) — 配置、HTTP、音频、朋友圈、日志/Toast、加载与快照。
- [method-runtime.md](method-runtime.md) — Hook、DexKit、反射。
- [unported.md](unported.md) — WA 有、WeKit 无的接口（移植时必须降级或删除）。
- [../porting-diff.md](../porting-diff.md) — 差异表与迁移风险排序。

## 规模

| 指标 | 值 |
|---|---|
| 脚本可见函数名 | 106 |
| `BshMethod` 注册数 | 141（唯一签名 138；3 处同名同签名后写覆盖，见 method-msg.md） |
| 注入全局变量 | 18 |
| 引擎查找的回调 | 7 |
| shim bean 类 | 9 |

## 参数类型标记

`JavaEngine.kt` 用别名声明 BSH 形参类型（`utils/reflection/Classes.kt`）：

| 标记 | 实际类型 |
|---|---|
| `BString` | `java.lang.String` |
| `any` | `java.lang.Object` |
| `int` | `int.class` 原始类型 |
| `long` / `Integer.TYPE` | `long.class` / `int.class`（源码两种写法都存在，按标注） |
| `float` `bool` | `float.class` `boolean.class` |
| `List` `Map` `Set` `Array` | `java.util.*` / `Object[]` |
| `Consumer` `Function` | `java.util.function.Consumer` / `Function` |
| `Member` | `java.lang.reflect.Member`（`Method` 或 `Constructor`） |
| `JSONObject` | `org.json.JSONObject`（宿主提供） |

BeanShell 侧调用按 Java 重载解析；数组字面量 `{a, b}` 可直接传给 `List`/`Object[]` 形参。

## 读取规则

1. 只读本次任务涉及的页面，不要整目录灌进上下文。
2. 页面里标 ⚠️ 的行是 WeKit 与 WA 文档**不等价**处，写代码前必须先看；标 ➕ 的是 WeKit 独有，标 ❌ 的见 unported.md。
3. 忽略参数（源码注明 "arg ignored"）意味着该位仅为兼容占位，不要指望它生效。
4. 结构体成员只用 struct.md 列出的名字；`FriendInfo` 没有 `getUserName()`。
