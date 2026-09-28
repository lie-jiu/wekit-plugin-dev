# 配置、网络、音频、朋友圈、日志与加载

均注册于 `JavaEngine.kt`。⚠️ = 与 WA 文档不等价，见 [../porting-diff.md#6](../porting-diff.md)。

## 配置（`config.prop`）

存储位置 `pluginDir/config.prop`，`java.util.Properties` 实现（JE:1940-1958）：读时若文件不存在返回默认值；**首次 `put*` 才创建文件**。12 个函数与 WA 全部同名同参：

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `getString` | `(String key, String def)` | 314 | |
| `putString` | `(String key, String value)` | 324 | |
| `getStringSet` | `(String key, Set def)` | 336 | ⚠️ 以 JSONArray 字符串存进**单个键**，与 WA 存量 `config.prop` 不保证互通 |
| `putStringSet` | `(String key, Set value)` | 356 | ⚠️ 同上 |
| `getBoolean` | `(String key, boolean def)` | 368 | 解析 `"true"/"false"` |
| `putBoolean` | `(String key, boolean value)` | 385 | |
| `getInt` | `(String key, int def)` | 397 | |
| `putInt` | `(String key, int value)` | 411 | |
| `getFloat` | `(String key, float def)` | 423 | |
| `putFloat` | `(String key, float value)` | 437 | |
| `getLong` | `(String key, long def)` | 449 | |
| `putLong` | `(String key, long value)` | 463 | |

没有 `getBool/putBool` 别名，没有显式 save/flush/close，没有删除键的 API（`remove` 需自己 `putString(key, "")` 或改文件）。

## HTTP（全部异步，OkHttp）

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `get` | `(String url, Map header, Consumer<String> cb)` | 1508 | 回调可能收到 null |
| `get` | `(String url, Map header, long timeout, Consumer<String> cb)` | 1526 | |
| `post` | `(String url, Map param, Map header, Consumer<String> cb)` | 1549 | ⚠️ 源码为表单 POST；WA 文档承诺 `Content-Type` 含 `application/json` 时发 JSON body → 需回源确认，JSON 接口建议自己拼 body |
| `post` | `(String, Map, Map, long timeout, Consumer)` | 1572 | |
| `download` | `(String url, String path, Map header, Consumer<File> cb)` | 1600 | |
| `download` | `(String, String, Map, long timeout, Consumer<File>)` | 1621 | |

## 音频

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `mp3ToSilk` | `(String src, String dst)` | 276 | |
| `mp3ToSilk` | `(String, String, int hz)` | 270 | ⚠️ `hz` 忽略（WA 中控制采样率，默认 24000） |
| `wavToSilk` | `(String, String)` | 282 | ➕ WeKit 独有 |
| `silkToMp3` | `(String, String)` / `(String,String,int hz)` | 297 / 288 | ⚠️ `hz` 忽略；中转 `.tmp` 后删除 |
| `getDuration` | `(String path)` → long **毫秒** | 306 | ✅ 与 WA 单位一致 |

返回值未承诺 WA 的 `0 = 成功` 状态码语义。

## 朋友圈

| 函数 | 签名 | 行 |
|---|---|---|
| `uploadText` | `(String content)` | 1765 |
| `uploadText` | `(String content, String sdkId, String sdkAppName)` | 1768 |
| `uploadText` | `(JSONObject{content,sdkId,sdkAppName})` | 1771 |
| `uploadTextAndPicList` | `(String content, String picPath)` | 1780 |
| `uploadTextAndPicList` | `(String, String, String sdkId, String sdkAppName)` | 1783 |
| `uploadTextAndPicList` | `(String content, List picPathList)` | 1786 |
| `uploadTextAndPicList` | `(String, List, String sdkId, String sdkAppName)` | 1790 |
| `uploadTextAndPicList` | `(JSONObject{content,picPathList,sdkId,sdkAppName})` | 1794 |

✅ 与 WA 文档 8 个重载完全对齐。朋友圈读取、评论、点赞、删除、可见范围 API 两侧都没有。

## 日志 / Toast / 通知 / 延时

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `log` | `(Object msg)` | 477 | ⚠️ 写 WeKit 模块日志（tag=插件目录名），**不落 `pluginDir/plugin.log`**；WeKit 无 `getLogFile()` |
| `toast` | `(String text)` | 488 | 主线程 Toast，前缀插件名 |
| `notify` | `(String title, String text)` | 501 | 通知渠道 `script_<插件目录名>` |
| `delay` | `(long millis, Runnable action)` | 1496 | 后台线程 `sleep` 后执行；一次性，取消只能自己标志位 |

## 加载与快照

| 函数 | 签名 | 行 | 备注 |
|---|---|---|---|
| `eval` | `(String code)` | 536 | 在当前解释器执行 |
| `loadJava` | `(String path)` | 545 | 相对 `pluginDir`，自动补 `.java`；同一解释器内 `source` |
| `loadJar` | `(String path)` | 565 | `URLClassLoader(parent=ClassLoaders.MODULE)` 挂进解释器 |
| `loadDex` | `(String path)` | 581 | `InMemoryDexClassLoader(parent=MODULE)` |
| `compileSnapshot` | `(String path)` | 599 | 输出 `path + ".bshs"` |
| `evalSnapshot` | `(String path)` | 618 | 读 `path + ".bshs"`，缺失时仅 warn 返回 null |
| `evalSnapshot` | `(InputStream)` | 1902 | |
| `reloadPlugin` | `()` | 1920 | ⚠️ 只重注入命名空间，**不**重跑 `main.java`；WA 侧为 `boolean` 真重载 |

快照容器与密钥见 [../runtime.md](../runtime.md)。**`okhttp3` / `com.alibaba.fastjson2` 需先安装「脚本依赖扩展包」(`script-deps`)**，`org.json` 由宿主提供。
