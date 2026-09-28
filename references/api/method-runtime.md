# Hook、DexKit、反射

## Hook（`JavaEngine.kt:1414-1456` → `JavaHookApi.kt`）

| 函数 | WeKit 签名 | 行 | 备注 |
|---|---|---|---|
| `hookBefore` | `(Member member, Consumer<HookParam> cb)` → `HookHandle` | 1416 | ⚠️ 实现里 `result = consumer.accept(this)`（JHA:30）会把回调返回值（`Unit`）写进 `param.result`，而三套桥接都把「给 result 赋值」视为跳过原方法 → **可能吞掉宿主原方法**。未经真机验证 |
| `hookAfter` | `(Member, Consumer<HookParam>)` → `HookHandle` | 1428 | 纯观察，安全 |
| `hookReplace` | `(Member, Function<HookParam,Object>)` → `HookHandle` | 1440 | `Function` 返回值即方法结果 |
| `unhook` | `(HookHandle handle)` | 1452 | 只接受 `HookHandle`；无实例 `handle.unhook()` |

`JavaHookApi.unhookEverything()`（JHA:66）由引擎 `onDisable` 调用（JSH:262）作为兜底全清；插件仍应在 `onUnload()` 里自己 unhook。

### 回调参数对象

WeKit 的 `HookParam` = `IHookBridge.IMemberHookParam`（`utils/HookUtils.kt:14`）：

| 成员 | 说明 | WA 对应 |
|---|---|---|
| `member` | 被 hook 的 `Member` | Xposed 的 `hookedMethod` ⚠️ 名字不同 |
| `thisObject` | 接收者，可空 | ✅ |
| `args` | `Object[]` | ✅ |
| `result`（读写） | 等价 `getResult()/setResult()`；**赋值即请求跳过原方法**（Zygisk `earlyReturn`、libxposed `skipOriginal`、xp51 `setResult`） | ⚠️ |
| `throwable`（读写） | 置位即抛出/早返回 | 近似 `setThrowable` |
| `extra` | 每次注册独立槽位 | WeKit 独有 |

写法建议：

```beanshell
// 观察：用 hookAfter
h = hookAfter(m, param -> { log("args=" + java.util.Arrays.toString(param.args)); });

// 改结果/拦截：用 hookReplace，显式决定返回值
h = hookReplace(m, param -> { param.args[0] = "改写后的值"; return param.getResult(); });
```

⚠️ 与 WA 相同的前提：Hook 依赖宿主结构，WeKit 只适配微信 **8.0.65–8.0.78**。跨版本前先用 DexKit 定位并判空。

## DexKit（`JavaEngine.kt:1459-1491`）

| 函数 | 签名 | 返回 |
|---|---|---|
| `findClassList` | `(List<String> usingStrings)` | `List<Class<?>>`，`dexKit.findClass { usingStrings(...) }` |
| `findMemberList` | `(List<String> usingStrings)` | `List<Member>`；`<init>`/`<clinit>` 归一为构造器，其余为方法 |

只有 `usingStrings` 一种匹配维度；没有 `findClass/findMethod/findConstructor`、matcher DSL、缓存 API（与 WA 文档一致）。BeanShell 里可直接写 `findClassList({"关键字"})`。

## 反射（`JavaEngine.kt:1649-1746`）

12 个注册与 WA 文档 12 个重载同名同参；WeKit 底层是 `reflekt`，WA 是 KavaRef。

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `firstMethod` | `(Object clazzOrInstance, String name)` | 1649 | 只看 `methods`（public，含继承） |
| `firstMethod` | `(Object, String, int paramCount)` | 1653 | |
| `firstConstructor` | `(Object, int paramCount)` | 1657 | |
| `firstField` | `(Object, String name)` | 1661 | ⚠️ 用 `getDeclaredField`，**不查父类** |
| `getField` | `(Object, String name)` | 1667 | 走 reflekt，`searchSuperclass = true` |
| `setField` | `(Object, String name, Object value)` | 1676 | 同上，查父类 |
| `invokeMethod` | `(Object, String)` / `(Object,String,Object[])` / `(Object,String,int)` / `(Object,String,int,Object[])` | 1689-1718 | 重载多时**必须**用带 `paramCount` 的形式 |
| `createInstance` | `(Object, int)` / `(Object,int,Object[])` | 1730 / 1738 | |

坑：
- `firstField` 与 `getField` 的父类查找行为不同，跨类层次取字段一律用 `getField/setField`。
- 宿主类在插件里可通过 `hostLoader.loadClass("...")` 获取（WeKit 独有便利）。
- 反射取到 `Member` 后才能交给 `hook*`；DexKit → 反射 → Hook 的顺序不可颠倒（`findMemberList` 返回的 `Member` 可直接 hook）。
