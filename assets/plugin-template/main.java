// WeKit 脚本引擎 (Java) 插件骨架
// 目录：<微信 filesDir>/wekit/scripts_java/<本目录名>/
// 必需：info.prop + main.java；disabled.flag 存在即禁用；改正文需重新开关引擎。
//
// loadJava 拆模块时：先加载被引用的模块，main.java 只保留加载语句、共享的 Hook 引用、
// 回调和模块调用。示例：
// loadJava("lib/config.java");
// loadJava("lib/reply.java");

List hookHandles = new java.util.ArrayList();
String KW_KEY = "keyword";
String DEF_KEYWORD = "在吗";

void onLoad() {
    log("loaded: " + pluginName + " v" + pluginVersion);
    // 需要 Hook 宿主方法时：DexKit 定位 -> 反射取 Member -> 注册并保存 handle。
    // 注意 WeKit 中 hookBefore 可能吞掉原方法，
    // 观察用 hookAfter，改参数/结果用 hookReplace。
    //
    // List cls = findClassList({"宿主里稳定的日志字符串"});
    // if (cls.size() == 1) {
    //     Member m = firstMethod(cls.get(0), "targetMethod", 1);
    //     if (m != null) {
    //         hookHandles.add(hookAfter(m, param -> {
    //             log("hit " + java.util.Arrays.toString(param.args));
    //         }));
    //     }
    // }
}

void onUnload() {
    Iterator it = hookHandles.iterator();
    while (it.hasNext()) {
        unhook(it.next());
    }
    hookHandles.clear();
    log("unloaded, hooks released");
}

void onHandleMsg(Object msgInfoBean) {
    if (msgInfoBean.isSend()) return;          // 排除自己发出的，避免自触发循环
    if (!msgInfoBean.isText()) return;

    String keyword = getString(KW_KEY, DEF_KEYWORD);
    String content = msgInfoBean.getContent();
    if (content == null || !content.contains(keyword)) return;

    // 事件回复一律用 getTalker()；只有用户主动操作的场景才用 getTargetTalker()
    String talker = msgInfoBean.getTalker();
    sendText(talker, "自动回复：" + keyword);
    putString("last_hit_talker", talker);
}

boolean onClickSendBtn(String text) {
    // 返回 true 会抑制本次发送
    return false;
}
