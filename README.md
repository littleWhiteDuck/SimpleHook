# SimpleHook

**中文** | [English](README_EN.md)

> **推荐使用 [HookNext](https://github.com/littleWhiteDuck/HookNextHome)**：它是 SimpleHook 的全新迭代升级版本，提供持续的功能演进与更好的使用体验。

SimpleHook 是用于 Android 应用调试与研究的 Xposed/LSPosed 模块。它提供可配置的 Java/Smali Hook、调用记录、运行时分析与配置导出工具。

仅可在你拥有或明确获授权测试的应用上使用。记录内容可能包含账号、令牌、输入数据或密钥材料，请妥善保管并遵守适用法律、服务条款及隐私要求。

## 开始使用

1. 安装并启用支持 Xposed API 51+ 或 libxposed API 101 的框架，例如 LSPosed。
2. 在框架管理器中启用 SimpleHook，并将目标应用加入模块作用域。
3. 打开 SimpleHook，在首页点击添加配置，选择目标应用后进入配置页。
4. 在配置页添加 Hook 项或在扩展页启用所需功能，保存配置。
5. 完全结束并重新启动目标应用，在记录页或悬浮窗查看结果。

配置保存后会同步到目标应用可读取的位置。若修改未生效，先确认模块作用域、框架日志和所需的存储/Root/Shizuku 授权，再重启目标应用。

## 填写 Hook 配置

### 定位类、方法和字段

手动填写时使用 Java 格式：

```text
类名: me.example.LoginService
方法名: checkLogin
参数类型: java.lang.String,int,byte[]
```

- 无参数方法的“参数类型”留空。
- 多个参数使用英文逗号分隔，不要加入空格以外的额外字符。
- `*` 作为参数类型表示该方法的全部重载；方法名为 `*` 表示该类的全部方法。
- 构造方法的名称是 `<init>`。构造方法通常优先使用“Hook 参数值”或记录模式，替换返回值、拦截执行可能导致目标应用异常。
- 可使用设置中的“Smali 转配置”，粘贴诸如 `Lme/example/LoginService;->checkLogin(Ljava/lang/String;I)Z` 的成员签名或调用语句，再由应用转换为配置。手动填写参数类型时仍建议使用 Java 格式。

字段模式中，`Hook 点` 指定字段读写操作相对触发方法的位置：`before` 是方法执行前，留空或 `after` 是执行后。

### 修改值规则

SimpleHook 根据文本自动转换基本类型；不需要另填类型。下表中的后缀用于消除歧义。

| 目标值 | 填写示例 |
| --- | --- |
| 布尔值 | `true`、`false` |
| int | `42`、`-1` |
| long | `42L` |
| float / double | `3.14f`、`3.14d` |
| byte / short | `7b`、`12short` |
| char | `ac`，即字符 `a` 后加 `c` |
| null | `null` |
| 空字符串 | `empty` |
| 普通字符串 | 直接填写，例如 `premium` |
| 看起来像数字的字符串 | `10086s` |
| `true`、`false`、`null` 字符串 | `trues`、`falses`、`nulls` |
| 空字符串列表 | `empty_list_string` |

参数替换值以英文逗号对应参数位置，空位表示不修改该参数。例如方法参数为 `(Context, String, int)`：

```text
,,99             # 只将第三个参数改为 99
,hello,99        # 修改第二、第三个参数
```

### 随机字符串返回值

“Hook 返回值”可填写以下 JSON 来生成随机字符串。`key` 在同一目标应用内应唯一；`updateTime` 的单位为秒，`-1` 表示每次调用都重新生成。

```json
{
  "random": "abcdefgh123456789",
  "length": 9,
  "key": "session-id",
  "updateTime": 60,
  "defaultValue": ""
}
```

## Hook 模式教程

### 1. Hook 返回值

在目标方法执行前直接返回指定值。适合修改布尔判断、数值结果或字符串。

```java
public boolean isVip() { return false; }
```

```text
模式: Hook 返回值
类名: me.example.Account
方法名: isVip
参数类型:
修改值: true
```

### 2. Hook 返回值+

该模式使用 Gson 将 JSON 转换为指定类的对象并作为结果返回，适合简单数据类。填写目标方法所在类和方法、参数类型、返回值类名及 JSON；数组或需要特殊构造过程的对象不保证适用。转换失败时会按普通返回值规则处理。

```text
模式: Hook 返回值+
类名: me.example.Account
方法名: profile
返回值类名: me.example.Profile
修改值: {"vip":true,"level":99}
```

可先使用“记录返回值”获取对象的大致 JSON 结构。

### 3. Hook 参数值

在方法执行前替换参数。必须填写参数类型，用逗号分隔的修改值与参数逐一对应。要保留某个参数，在对应位置留空。

### 4. 拦截执行

跳过目标方法执行。填写类名、方法名和必要的参数定位即可，不需要修改值。优先从不会影响初始化或资源释放的调用点开始测试。

### 5. Hook 静态变量

可直接设置静态字段，也可在某个方法前后设置。

```text
模式: Hook 静态变量
变量所在类名: me.example.Flags
变量名: enabled
修改值: true
```

若字段会在运行时被重新赋值，再补充触发类、触发方法、参数类型和 Hook 点。例如在 `MainActivity.initData()` 后设置字段，触发类填 `me.example.MainActivity`，方法名填 `initData`，Hook 点填 `after`。

### 6. Hook 实例变量

实例字段必须依附于该实例的某个方法或构造方法。填写实例所属类、触发方法、字段名和修改值；字段会在指定 Hook 点被修改。通常选择字段已初始化之后的 `after`。

### 7. 记录参数值、返回值和参返

这三种模式不会修改调用结果，分别记录参数、返回值或两者。记录在方法执行后写入记录页；可搜索、标记、查看详情或打开悬浮窗观察实时输出。

### 8. 记录静态变量和实例变量

字段记录模式与字段修改模式的定位方式相同，但只读取字段值。

- 静态字段可以直接填“变量所在类名”和“变量名”，也可以绑定到触发方法前后。
- 实例字段需要填写实例所属类、触发方法和字段名。

## 扩展功能

扩展页的“总开关”必须开启，保存后才会对目标应用生效。按需打开功能，避免同时启用不必要的全局 Hook。

| 分类 | 功能 |
| --- | --- |
| 算法分析 | Base64、消息摘要、HMAC、Cipher 加解密记录；摘要、HMAC 与 Cipher 可按算法筛选。 |
| 界面与交互 | Dialog、PopupWindow、Toast、点击事件、Intent 记录；Dialog/PopupWindow 可取消或按关键词、View ID 拦截。 |
| Web 与 JSON | WebView URL 与请求头记录、WebView 调试开关、`JSONObject`/`JSONArray` 创建和写入记录。 |
| 环境与安全 | 应用入口与签名读取记录、签名伪装、剪贴板读取/写入过滤、联系人拦截、传感器禁用、ADB/VPN 检测处理。 |
| 运行控制 | 退出/结束/杀进程调用记录或拦截、崩溃记录、文件访问监控、热修复 DEX 加载。 |

算法、参数、返回值和调用栈记录可能产生大量数据。可在“记录设置”中关闭调用栈、Base64 或 Hex 表示，并按需要降低缓存和单条记录大小。不要在生产环境长期记录敏感内容。

## 调试与导出

- **DEX 浏览器**：选择已安装应用或 APK，浏览类、方法、字段，并将目标信息带入配置。
- **Smali 转配置**：粘贴字段/方法签名或调用语句，减少手工填写错误。
- **配置收藏与模板**：复用常用 Hook 组合；支持导入、导出、备份和恢复。
- **Frida 脚本导出**：在设置中选择导出 Frida Hook 脚本，将当前自定义配置转换为可调整的 Frida Java 脚本。
- **插件 APK 导出**：从首页导出时选择插件 APK，勾选要包含的应用配置，填写插件名称、包名与版本。导出的插件使用项目内置的公开默认签名材料签名；该签名不是 SimpleHook 应用的发布签名。

## 常见问题

### 配置没有生效

确认模块已启用、目标应用在作用域中、配置本身已启用且已保存。完全结束目标应用后重新启动，并查看框架日志或记录页中的错误记录。对于系统目录或受限存储环境，按应用提示授予 Root、Shizuku 或目录权限。

### 应用变慢、记录过多或内存占用增加

关闭不需要的扩展，尤其是算法、参数/返回值和调用栈记录；收窄到具体方法和参数签名，而不是使用全部方法或全部重载；在记录设置中限制缓存和单条记录体积。

### 如何选择 Hook 点

普通方法的返回值和参数模式由模式本身决定执行时机。字段模式才使用 `before` / `after`：字段应在方法开始前覆盖时选 `before`，字段会在方法中被赋值时通常选 `after`。

## 从源码构建

使用 JDK 17 与 Android SDK（`compileSdk = 36`）：

```bash
git clone --recurse-submodules https://github.com/littleWhiteDuck/SimpleHook.git
cd SimpleHook
bash ./gradlew :app:assembleRootDebug
```

不提供 `sign.properties` 或签名环境变量时，debug 构建会使用 Android 默认 debug keystore。发布签名可参考 `sign.properties.example` 创建本地 `sign.properties`；它已被 Git 忽略，切勿提交个人发布密钥。

GitHub Actions 会在推送到 `main` 和提交 PR 时执行单元测试、构建 debug APK。推送形如 `v1.2.0` 的标签时，会自动构建签名 release APK、创建同名 GitHub Release 并上传 APK；该标签去掉 `v` 后即为 Android `versionName`。release 构建读取以下 GitHub Secrets：

| Secret | 用途 |
| --- | --- |
| `KEYSTORE_BASE64` | 经 Base64 编码的 keystore 文件 |
| `SIGNING_ALIAS` | 密钥别名 |
| `SIGNING_KEY_PASSWORD` | 密钥密码 |
| `SIGNING_STORE_PASSWORD` | keystore 密码 |

工作流会将 keystore 解码到临时目录，并通过 `SIGNING_STORE_FILE` 传给 Gradle。Gradle 也可直接读取 `SIGNING_ALIAS`、`SIGNING_KEY_PASSWORD`、`SIGNING_STORE_FILE`、`SIGNING_STORE_PASSWORD` 和 `VERSION_NAME` 环境变量；同名本地 `sign.properties` 值优先。

## 许可证

本项目采用 [Apache License 2.0](LICENSE)，第三方代码与依赖保留各自许可证。

## 致谢

感谢下列开源项目及其维护者提供框架、兼容层或实现基础：

- [XposedBridge](https://github.com/rovo89/XposedBridge)、[LSPosed](https://github.com/LSPosed/LSPosed) 与 [libxposed](https://github.com/libxposed)
- [QAuxiliary](https://github.com/cinit/QAuxiliary) 的加载器兼容实现
- [EzXHelper](https://github.com/KyuubiRan/EzXHelper)、[ARSCLib](https://github.com/REAndroid/ARSCLib)
- [RikkaX](https://github.com/RikkaApps/RikkaX)、[Shizuku](https://github.com/RikkaApps/Shizuku)、AndroidX、Kotlin 与 Material Components

完整的第三方依赖与许可证清单见 [应用内清单](app/src/main/assets/lib_license.json)。

<sub>交流：TG 群 [@simpleHook](https://t.me/simpleHook)</sub>
