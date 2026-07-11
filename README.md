# 仓库 PDA 安卓客户端

这是仓库管理系统的原生 Android PDA 客户端，面向扫码枪和安卓手机，重点支持现场扫码、即时校验与可靠提交。

## 技术与版本

- Kotlin + Jetpack Compose
- Retrofit + OkHttp
- Android 8.0 及以上（`minSdk 26`）
- 当前版本：`0.2.0`（`versionCode 7`）
- 默认服务器：`http://43.108.14.102/`

应用登录前会访问 `/api/health`，只有数据库正常且 API 合约版本为 `1` 时才允许登录。服务器地址仍可在登录页修改，但必须是合法的 `http://` 或 `https://` 地址。

## 业务范围

- 厂家到货：按货品数量入库，不扫描条码。
- 终端退换货：登记货品、终端店铺、生产日期并扫码入库。
- 扫码出库：统一支持分配销售人员或发往另一个仓库。
- 销售退回：扫描销售人员名下条码并回流仓库。
- 扫码查询：查询条码当前归属与流转记录。

每次扫码业务最多提交 500 个条码。所有条码必须完成服务端校验后才能提交。

## 提交可靠性

`v0.2.0` 会为每次业务提交生成 `clientRequestId`。网络中断、服务器超时、会话失效或服务器仍在处理时，原请求会保存在设备上；重新登录或重启应用后可继续确认，重试不会重复改变库存。

账号和密码使用 Android Keystore + AES-GCM 加密保存。应用禁止系统备份账号、Cookie 和待确认业务。网络日志不会输出请求体、Cookie 或条码。

> 当前版本按项目决定继续允许 HTTP。HTTP 不能防止同一网络中的窃听或篡改，后续应在服务器配置域名和 HTTPS 后关闭明文流量。

## 本地构建

使用 Android Studio 或 JDK 17 + Android SDK：

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

有模拟器或真机连接时可运行设备测试：

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 正式签名

正式签名配置保存在项目外部：

`~/.config/warehouse-pda/signing/keystore.properties`

文件格式：

```properties
storeFile=/absolute/path/to/warehouse-pda-release.jks
storePassword=...
keyAlias=warehouse-pda
keyPassword=...
```

签名文件和密码不得提交 GitHub。首次正式发布后必须长期保留同一签名文件，否则已安装应用无法覆盖更新。

生成正式包：

```bash
./gradlew :app:assembleRelease
```

`v0.1.x` 使用 Debug 签名，不能直接覆盖安装 `v0.2.0` 正式签名版。现有设备需要卸载旧版并安装一次 `v0.2.0`；之后继续使用同一正式签名即可覆盖更新。
