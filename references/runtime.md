# 运行时事实

## 目录与发现

```
/data/user/0/com.tencent.mm/files/wekit/
├── scripts_java/<pluginId>/     # 本 skill 关注的 Java 插件根（JSH:70）
│   ├── main.java                # 必需
│   ├── info.prop                # 必需
│   ├── config.prop              # 首次 put* 才生成（JE:1940-1958）
│   ├── disabled.flag            # 存在 = 禁用（JSH:68,240-253）
│   └── *.java / *.dex / *.jar   # loadJava / loadDex / loadJar 目标
├── cache/                       # 全局量 cacheDir 指向这里（KnownPaths:42-44）
└── extensions/script-deps/<version>/classes.dex   # 脚本依赖扩展包
```

- `moduleRoot = HostInfo.application.filesDir / "wekit"`（`utils/fs/KnownPaths.kt:17-19`）→ 微信**私有目录**，读写需 root 或模块管理器；不像 WA 那样放在 `/sdcard/Android/media/`。
- 插件 id = 目录名（JSH:121,140），与 `info.prop` 的 `name` 无关。
- 枚举只取一级子目录，且要求同时存在 `main.java` 与 `info.prop`，否则 warn 跳过（JSH:119-132）。
- 加载发生在**开启引擎**时（`onEnable` 内的协程 JSH:116-156）；运行期放入/修改文件不会自动生效，需关闭再开启引擎或重启微信。禁用某插件 = 写 `disabled.flag`。

## 解释器与类加载

- 每个插件一个 `Interpreter(null, "")`（JSH:144），互相隔离；同名 BSH 函数不跨插件可见。
- `initPlugin`（JE:220-229）把解释器 classManager 设为 `ClassLoaders.HYBRID`，再追加 `ScriptDepsPack.classLoader()`。可用类：
  - 宿主类：微信自身类 + `libs/common/stubs` 编译期可见的宿主 API，通过 `hostLoader` 或反射获取；
  - 模块类：`ClassLoaders.MODULE`（WeKit 的类，含 `me.hd.wauxv.*` shim）；
  - `script-deps` dex：`fastjson2`、`okhttp`、`kotlin-stdlib`。
- `loadDex` 用 `InMemoryDexClassLoader`、`loadJar` 用 `URLClassLoader`，父加载器都是 `ClassLoaders.MODULE`（JE:565-594）。相对路径基于插件目录。
- 关掉引擎时 `JavaHookApi.unhookEverything()` 清理脚本注册的全部 Hook（JSH:262）。

## 快照（`.bshs`）

`compileSnapshot(path)` → `path + ".bshs"`；`evalSnapshot(path|InputStream)` 用固定密钥解密执行。

- 容器（`libs/common/bsh/src/main/java/bsh/snapshot/BshSnapshotHelper.java:22-26,43-75`）：`MAGIC 'BSHS'` + 1 字节 header 版本(1) + 1 字节 IV 长度 + 12 字节 IV + `AES/GCM/NoPadding`（128 位 tag），体为 Java 序列化的 `BshSnapshot{int formatVersion; bsh.Node[] nodes}`。
- 密钥：`SecretKeySpec("0123456789abcdef".toByteArray(UTF_8), "AES")`（`utils/BshSnapshotDecompiler.kt:64-65`）。WeKit 侧记录该密钥是从 WAuxiliary 反编译源码恢复的（见 WeKit `AGENTS.md`）。
- 反编译：WeKit 内置功能「反编译 BeanShell 快照」把 `.bshs` 还原为类 Java 源码，是移植加密 WA 插件的入口工具。
- ⚠️ WA 官方 fork（`HdShare/beanshell`）上游**不含** snapshot 类，因此「WeKit 能执行 WA 产出的快照」属推断而非实证；跨模块使用前先真机验证。

## 宿主伪装与适配范围

- `moduleVer` 恒为 `1418`（JE:78,244），对应 WA `1.2.7.r1418`（当前最新公开发布版）。真实 WA 已到 r1439/r1454 特性面，WeKit **不**具备那些接口。
- 微信适配范围 **8.0.65–8.0.78**（WeKit `AGENTS.md`）。DexKit 字符串关键字、Hook 目标在范围外不保证可解析。
- `hostVerClient` 取自 compileOnly stub 的 `BuildConfig.CLIENT_VERSION_ARM64`（JE:238），随 WeKit 编译期 stub 而定，不要当宿主真实内部版本用。

## 与 WeKit 其他扩展通道的边界

| 通道 | 是否本 skill 范围 |
|---|---|
| Java 脚本引擎 `scripts_java/` | ✅ |
| Python 插件 `scripts_python/<dir>/plugin.json` + `main.py`（`PythonPluginManager.kt:41,192-193`） | ❌ 独立体系，字段与生命周期完全不同 |
| 内置 Kotlin `BaseFeature`/`ExtensionPack`/`@AgentTool` | ❌ 编译进 APK，不是插件 |
| Frida 注入入口 | ❌ 模块自身启动方式 |
