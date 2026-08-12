# SimpleHook

[Chinese](README.md) | **English**

> **[HookNext](https://github.com/littleWhiteDuck/HookNextHome) is recommended.** It is a complete iterative upgrade of SimpleHook, with ongoing feature development and an improved user experience.

SimpleHook is an Xposed/LSPosed module for Android application debugging and research. It provides configurable Java/Smali hooks, call recording, runtime analysis, and configuration export tools.

Use it only with applications that you own or are explicitly authorized to test. Records may contain accounts, tokens, input data, or key material. Handle them carefully and comply with applicable law, service terms, and privacy requirements.

## Quick Start

1. Install and enable a framework supporting Xposed API 51+ or libxposed API 101, such as LSPosed.
2. Enable SimpleHook in the framework manager and add the target app to its module scope.
3. Open SimpleHook, add a configuration from the home screen, and select the target app.
4. Add custom hook entries on the configuration page or enable the required items on the extension page, then save.
5. Fully stop and relaunch the target app. Inspect results in the record view or floating window.

Saved configurations are synchronized to a location that the target app can read. If a change does not take effect, verify module scope, framework logs, and required storage/Root/Shizuku permissions, then restart the target app.

## Writing Hook Configurations

### Classes, methods, and fields

For manual input, use Java notation:

```text
Class: me.example.LoginService
Method: checkLogin
Parameter types: java.lang.String,int,byte[]
```

- Leave parameter types blank for a no-argument method.
- Separate multiple parameter types with commas.
- Use `*` as parameter types to target every overload; use `*` as the method name to target every method in the class.
- Constructors use `<init>` as the method name. Parameter replacement and record modes are generally safer for constructors; return replacement or interception can break target initialization.
- Use the Smali-to-config tool in Settings to paste a member signature or call such as `Lme/example/LoginService;->checkLogin(Ljava/lang/String;I)Z`. The app converts it to a configuration. Java notation remains recommended for manually entered parameter types.

For field modes, `Hook point` controls the point relative to the trigger method: `before` runs before it; blank or `after` runs after it.

### Value rules

SimpleHook infers primitive values from text, so a separate value type is unnecessary. Suffixes resolve ambiguous values.

| Target value | Input examples |
| --- | --- |
| Boolean | `true`, `false` |
| int | `42`, `-1` |
| long | `42L` |
| float / double | `3.14f`, `3.14d` |
| byte / short | `7b`, `12short` |
| char | `ac`, character `a` followed by `c` |
| null | `null` |
| Empty string | `empty` |
| Normal string | Plain text, for example `premium` |
| Numeric-looking string | `10086s` |
| String `true`, `false`, or `null` | `trues`, `falses`, `nulls` |
| Empty string list | `empty_list_string` |

For parameter replacement, comma-separated values map to argument positions; an empty position leaves that argument untouched. For a method with `(Context, String, int)`:

```text
,,99             # change only the third argument
,hello,99        # change the second and third arguments
```

### Random string return values

Use the following JSON in the Hook return value mode to generate a random string. `key` should be unique within one target app. `updateTime` is measured in seconds; `-1` generates a value for every call.

```json
{
  "random": "abcdefgh123456789",
  "length": 9,
  "key": "session-id",
  "updateTime": 60,
  "defaultValue": ""
}
```

## Hook Mode Guide

### 1. Hook return value

Returns the configured value before the target method executes. This is useful for boolean checks, numeric results, and strings.

```java
public boolean isVip() { return false; }
```

```text
Mode: Hook return value
Class: me.example.Account
Method: isVip
Parameter types:
Value: true
```

### 2. Hook return value+

This mode uses Gson to deserialize JSON into the configured return class. It works best for simple data classes. Fill in the target class and method, parameter types, return class name, and JSON. Arrays and objects needing specialized construction are not guaranteed to work. If conversion fails, SimpleHook falls back to normal return-value rules.

```text
Mode: Hook return value+
Class: me.example.Account
Method: profile
Return class: me.example.Profile
Value: {"vip":true,"level":99}
```

Record the return value first when you need to inspect an object's approximate JSON structure.

### 3. Hook parameter value

Replaces arguments before execution. Parameter types are required. Comma-separated values map to parameters one by one; leave a position empty to preserve it.

### 4. Intercept execution

Skips the target method. Supply the class, method, and enough parameter information to locate it; no value is needed. Start with calls that do not initialize state or release resources.

### 5. Hook static field

Set a static field directly, or set it before or after a trigger method.

```text
Mode: Hook static field
Field class: me.example.Flags
Field name: enabled
Value: true
```

If the field is assigned again at runtime, also provide the trigger class, method, parameter types, and hook point. For example, set the field after `MainActivity.initData()` by using trigger class `me.example.MainActivity`, method `initData`, and hook point `after`.

### 6. Hook instance field

An instance field must be attached to a method or constructor on that instance. Provide the instance class, trigger method, field name, and value. Choose `after` when the field is initialized by the trigger method.

### 7. Record parameters, return value, or both

These modes do not modify calls. They write parameters, return values, or both to the record view after execution. Records can be searched, marked, inspected in detail, or watched in the floating window.

### 8. Record static and instance fields

Field record modes use the same lookup rules as field replacement but only read values.

- A static field can be recorded directly with field class and field name, or around a trigger method.
- An instance field requires its instance class, trigger method, and field name.

## Extensions

The extension page's master switch must be enabled and saved before its features take effect. Enable only what you need; each item may install broad runtime hooks.

| Area | Features |
| --- | --- |
| Algorithm analysis | Base64, message digest, HMAC, and Cipher operation records, with algorithm-level filters for digest, HMAC, and Cipher. |
| UI and interaction | Dialog, PopupWindow, Toast, click-event, and Intent records; Dialogs/PopupWindows can be cancellable or blocked by keyword and View ID. |
| Web and JSON | WebView URL/request-header records, WebView debugging, and `JSONObject`/`JSONArray` creation and write records. |
| Environment and security | Application-entry and signature-read records, signature spoofing, clipboard controls, contact blocking, sensor disabling, and ADB/VPN check handling. |
| Runtime controls | Exit/finish/kill-process records or interception, crash records, file-access monitoring, and hot-fix DEX loading. |

Algorithm, parameter/return, and stack-trace records can create substantial data. In Record settings, disable stack traces, Base64, or Hex output as needed, and limit cache and per-record size. Do not leave sensitive recording enabled in production use.

## Debugging and Export

- **DEX browser**: browse installed apps or APKs, inspect classes/methods/fields, and send targets into a configuration.
- **Smali-to-config**: paste field/method signatures or call statements to avoid manual input mistakes.
- **Collections and templates**: reuse hook groups; configurations can be imported, exported, backed up, and restored.
- **Frida script export**: choose Frida Hook script export in Settings to turn custom configurations into an adjustable Frida Java script.
- **Plugin APK export**: choose plugin APK from the home-screen export action, select configurations, then supply plugin name, package name, and version. Exported plugins use the repository's public default signing material, which is not the SimpleHook application release key.

## Troubleshooting

### A configuration has no effect

Confirm that the module and target scope are enabled, the configuration itself is enabled and saved, and the target app was fully restarted. Check framework logs and error records. For protected storage environments, grant the Root, Shizuku, or directory permission requested by the app.

### The target becomes slow or produces too many records

Disable unused extensions, especially algorithm, parameter/return, and stack-trace recording. Target specific methods and parameter signatures instead of all methods or overloads. Limit cache and per-record size in Record settings.

### Choosing a hook point

Return-value and parameter modes determine their own timing. `before` / `after` applies to field modes: choose `before` when a field must be overwritten before the trigger begins; choose `after` when the trigger assigns the field.

## Build from Source

Use JDK 17 and the Android SDK (`compileSdk = 36`):

```bash
git clone --recurse-submodules https://github.com/littleWhiteDuck/SimpleHook.git
cd SimpleHook
bash ./gradlew :app:assembleRootDebug
```

Without `sign.properties` or signing environment variables, debug builds use Android's default debug keystore. For release signing, create a local `sign.properties` from `sign.properties.example`; it is ignored by Git, so never commit a personal release key.

GitHub Actions runs unit tests and builds a debug APK for pushes to `main` and pull requests. Pushing a tag such as `v1.2.0` automatically builds a signed release APK, creates a GitHub Release with that tag, and uploads the APK. The tag without its leading `v` becomes the Android `versionName`. The release build reads these GitHub Secrets:

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded keystore file |
| `SIGNING_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |
| `SIGNING_STORE_PASSWORD` | Keystore password |

The workflow decodes the keystore into a temporary directory and passes its path through `SIGNING_STORE_FILE`. Gradle also accepts `SIGNING_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, and `VERSION_NAME` directly. Values in local `sign.properties` take precedence.

## License

Licensed under the [Apache License 2.0](LICENSE). Third-party code and dependencies retain their respective licenses.

## Acknowledgements

Thanks to the following open-source projects and their maintainers for frameworks, compatibility layers, or implementation foundations:

- [XposedBridge](https://github.com/rovo89/XposedBridge), [LSPosed](https://github.com/LSPosed/LSPosed), and [libxposed](https://github.com/libxposed)
- [QAuxiliary](https://github.com/cinit/QAuxiliary) for loader compatibility code
- [EzXHelper](https://github.com/KyuubiRan/EzXHelper) and [ARSCLib](https://github.com/REAndroid/ARSCLib)
- [RikkaX](https://github.com/RikkaApps/RikkaX), [Shizuku](https://github.com/RikkaApps/Shizuku), AndroidX, Kotlin, and Material Components

See the [in-app manifest](app/src/main/assets/lib_license.json) for the complete third-party dependency and license list.

<sub>Community: TG group [@simpleHook](https://t.me/simpleHook)</sub>
