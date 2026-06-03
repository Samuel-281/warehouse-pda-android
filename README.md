# 仓库 PDA 安卓客户端

这是面向安卓 PDA 扫码枪的原生客户端工程骨架，目标是：

1. 以安装包形式运行，不依赖用户手动打开浏览器。
2. 联网调用现有 `warehouse-web` 后端 API。
3. 聚焦扫码作业：厂家到货入库、终端退换货入库、挪仓、销售出库、销售退回。

## 当前实现

- `Kotlin + Jetpack Compose`
- `Retrofit + OkHttp`
- 复用现有后端登录、会话、主数据、条码校验、入库、出库、销售退回接口
- 服务端会话通过 Cookie 保持
- 登录页支持填写服务器地址

## 默认假设

- 服务器接口沿用当前 `warehouse-web/app/api/*`
- PDA 扫码枪表现为键盘输入，并支持回车提交
- 当前首版仍然是联网模式，不做离线同步

## 本地打开

建议使用 Android Studio 打开 `warehouse-pda-android/` 目录。

由于当前这台机器缺少 Java Runtime 和 Gradle，本仓库里暂未生成 Gradle Wrapper，也未在当前环境完成 APK 编译验证。打开前请先准备：

1. Android Studio
2. JDK 17
3. Android SDK

## 需要你后续确认的设备联调点

1. PDA 扫码后是否自动回车
2. 设备分辨率和屏幕方向
3. 是否必须锁定横屏或竖屏
4. 是否需要厂商扫码 SDK，而不是仅靠键盘输入
