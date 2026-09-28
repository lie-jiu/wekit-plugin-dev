# 工作流

## 任务类型判定

| 类型 | 信号 | 首要动作 |
|---|---|---|
| 新建 | 「写个插件」「做一个…」 | 填下方需求表 → 用 `assets/plugin-template/` 起目录 |
| 修改 | 指向已有 `scripts_java/<id>/` | 读全部已 `loadJava` 的模块，再定位改动点 |
| 调试 | 日志/现象/「不生效」 | 先确认加载链（目录/必需文件/`disabled.flag`/引擎是否重开），再看根因 |
| 从 WA 移植 | 给出 WA 插件目录或 `.bshs` | 先跑 [api/unported.md](api/unported.md) + [porting-diff.md](porting-diff.md) 全量比对，输出「可直迁 / 需改写 / WeKit 不支持」三分类，再动手 |
| 审核 | 「检查这个插件」 | 按 [review-checklist.md](review-checklist.md) 逐项 |

## 需求表（开工前必须填满）

```
插件目录名(pluginId)：
触发条件：            # 哪个回调 / 哪类消息 / 哪个开关
适用会话：            # 私聊 / 群聊 / 公众号 / 全部；是否排除 isSend()
动作：                # 发送内容、写配置、调用宿主方法
配置项：              # config.prop 键名 + 类型 + 默认值
失败反馈：            # log / toast / notify 哪一项，文案是什么
卸载行为：            # onUnload 要取消的 Hook、线程、定时器清单
宿主依赖：            # DexKit 关键字 / 反射类名 / 需要的扩展包
待真机验证项：        # 无法静态确认的全部列在这里
```

任一项无法从需求或代码得到答案 → 一次性列出问题问用户，不要逐轮追问。

## 完成标准写法

写成可检查的断言，例如：

- 「群里出现关键词 X 且发送者不是自己时，向该群发送 Y，并在 `config.prop` 记 `last_hit_time`。」
- 「关闭插件后，`param.method` 目标恢复原行为（不再有日志）。」

避免「功能正常」「能跑」这类无法验证的标准。

## 移植专用检查序列

1. 加密 `.bshs` → 先用 WeKit 的「反编译 BeanShell 快照」还原源码。
2. 扫出全部 `me.hd.wauxv.*` 引用与 WA 全局量，对照 [api/struct.md](api/struct.md)、[api/global.md](api/global.md) 判定可用成员。
3. 扫出所有回调名：命中 `onCreate*Menu` / `openSettings` → 标记为 WeKit 不支持，向用户报告并给出替代交互。
4. 扫出 WA-only 函数（[api/unported.md](api/unported.md)）与 18 条语义不等价项（[porting-diff.md#6](porting-diff.md)），逐条改写。
5. 依赖 `okhttp3`/`fastjson2` → 在交付说明里要求安装 `script-deps` 扩展包。
6. 保留原 `config.prop` 键名与格式；若必须改变（如 `getStringSet` 编码不同），先征得用户同意并在 readme 里写迁移步骤。

## 验证分层

| 层 | 手段 | 能证明 |
|---|---|---|
| 静态 | 逐条比对接口页；检查必需文件、相对路径、回调签名、`onUnload` 清理 | 接口存在、签名匹配 |
| 加载 | 真机开启引擎后看模块日志（`log()` 输出、`JavaEngine`/`JavaScriptingHook` tag）中该插件的 `onLoad executed` 行 | 插件被枚举并执行 onLoad |
| 行为 | 真实触发宿主事件 | 唯一可信的功能验证 |

只有静态层通过时，交付文案必须写「已静态核对，待真机验证」，禁止写「已测试通过」。
