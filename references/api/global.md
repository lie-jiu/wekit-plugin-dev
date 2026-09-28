# 全局变量

由 `JavaEngine.kt:231-264`（`initNameSpace`）注入，共 18 个。全部 `setVariable`，没有把任何对象作为变量注入（Hook API 只以函数形式暴露）。

| 变量 | 类型 | 值来源 | 行 | 备注 |
|---|---|---|---|---|
| `hostContext` | `android.content.Context` | `HostInfo.application` | 235 | |
| `hostVerName` | `String` | `HostInfo.versionName` | 236 | |
| `hostVerCode` | `int` | `HostInfo.versionCode.toInt()` | 237 | |
| `hostVerClient` | `int` | stub `com.tencent.mm.boot.BuildConfig.CLIENT_VERSION_ARM64` | 238 | 取自 compileOnly stub，随编译期 stub 变化 |
| `hostLoader` | `ClassLoader` | `ClassLoaders.HOST` | 239 | ➕ WeKit 独有；用于宿主类反射 |
| `myWxId` | `String` | `WeApi.selfWxId` | 240 | ➕ |
| `moduleVer` | `int` | 常量 `WA_MODULE_VER` = **1418** | 244（常量 78） | ⚠️ 伪装值，恒等于 1418；不要用 `moduleVer>=1439` 做 WeKit 分支 |
| `cacheDir` | `String` | `KnownPaths.moduleCache.absolutePathString()` = `<moduleRoot>/cache` | 248 | 全插件共享目录，非插件私有 |
| `pluginPath` | `String` | `plugin.dir` 绝对路径 | 252 | ➕ |
| `pluginDir` | **`java.io.File`** | `plugin.dir.toFile()` | 253 | ⚠️ WA 文档记为 `String`。拼接可用，赋给 `String` 会失败 |
| `pluginId` | `String` | 插件目录名 | 254 | |
| `pluginName` | `String` | `info.prop` 的 `name` | 255 | |
| `pluginAuthor` | `String`（可 null） | `info.prop` 的 `author` | 256 | |
| `pluginVersion` | `String`（可 null） | `info.prop` 的 `version` | 257 | |
| `pluginUpdateTime` | `String`（可 null） | `info.prop` 的 `updateTime` | 258 | |
| `engineId` | `String` | `BuildConfig.TAG` | 262 | ➕ 引擎（WeKit）标识 |
| `engineVerCode` | `Int` | `BuildConfig.VERSION_CODE` | 263 | ➕ |
| `engineVerName` | `String` | `BuildConfig.VERSION_NAME` | 264 | ➕ |

WA 文档侧的 12 个（`hostContext` `hostVerName` `hostVerCode` `hostVerClient` `cacheDir` `moduleVer` `pluginDir` `pluginId` `pluginName` `pluginAuthor` `pluginVersion` `pluginUpdateTime`）在 WeKit 全部存在，仅 `pluginDir` 类型不同。

`global.foo()` 不是注入变量：它是 BeanShell 内建的根命名空间访问器（`bsh/Name.java:538`），两侧行为一致，别当成模块 API。
