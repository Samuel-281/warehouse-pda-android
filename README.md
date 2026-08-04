# 箱码流向追踪 PDA 安卓客户端

这是箱码流向追踪系统的原生 Android PDA 客户端，面向扫码枪和安卓手机，重点支持混合箱码快速出库、统一回库、签收状态查询与可靠提交。

## 技术与版本

- Kotlin + Jetpack Compose
- Retrofit + OkHttp
- Android 8.0 及以上（`minSdk 26`）
- 当前版本：`0.3.1`（`versionCode 9`）
- 默认服务器：`http://43.108.14.102/`

应用登录前会访问 `/api/health`，只有数据库正常且 API 合约版本为 `1` 时才允许登录。服务器地址仍可在登录页修改，但必须是合法的 `http://` 或 `https://` 地址。

## 业务范围

- 快速出库：选择来源仓库和去向后连续扫描混合箱码，不选择商品、不维护数量库存。
- 扫码回库：未售货物、店铺退货和未知外部退货统一扫码回仓，不登记商品、店铺或生产日期。
- 条码查询：查询当前仓库、销售人员或终端店铺归属，以及待签收、已签收、签收异常状态。
- 勤策回填：商品名称、单位、签收店铺和签收时间由 Web 后端同步的勤策数据补充。
- 扫码反馈：校验成功后语音播报当前有效件数；重复、校验失败或超过上限时使用三连错误音并播报错误。

每次扫码业务最多提交 500 个条码。所有条码必须完成服务端校验后才能提交。

## 提交可靠性

应用会为每次业务提交生成 `clientRequestId`。网络中断、服务器超时、会话失效或服务器仍在处理时，原请求会保存在设备上；重新登录或重启应用后可继续确认，重试不会重复记录条码流转。

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

`v0.1.x` 使用 Debug 签名，不能直接覆盖安装正式签名版。已经安装 `v0.2.0` 或 `v0.3.0` 正式版的设备可以直接覆盖升级到 `v0.3.1`；首次从旧 Debug 版升级时仍需卸载重装一次。
