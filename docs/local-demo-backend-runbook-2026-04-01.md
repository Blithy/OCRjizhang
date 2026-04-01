# 本地演示后端联调说明

## 本次新增内容

1. 仓库新增 `backend` 模块，基于 Spring Boot 3.2.x。
2. 已提供演示所需最小接口：
   - `POST /api/auth/register`
   - `POST /api/auth/login`
   - `GET /api/user/me`
   - `PUT /api/user/me`
   - `GET /api/categories`
   - `POST /api/categories`
   - `PUT /api/categories/{id}`
   - `DELETE /api/categories/{id}`
   - `GET /api/transactions`
   - `POST /api/transactions`
   - `PUT /api/transactions/{id}`
   - `DELETE /api/transactions/{id}`
   - `POST /api/sync/push`
   - `POST /api/sync/pull`
3. 后端当前使用本地内存存储，适合毕设演示，不依赖 MySQL。
4. 默认演示账号已内置：
   - 用户名：`demo`
   - 密码：`123456`

## 如何启动后端

在仓库根目录执行：

```powershell
./gradlew.bat :backend:bootRun
```

默认监听地址：

```text
http://127.0.0.1:8080/api
```

## Android 端如何连接

### 模拟器

如果你用 Android 模拟器，`local.properties` 里保持：

```properties
base.url=http://10.0.2.2:8080/api/
```

### 真机

如果你用 Pixel 3 真机，`base.url` 不能写 `10.0.2.2`，要改成电脑局域网 IP，例如：

```properties
base.url=http://192.168.1.10:8080/api/
```

可在 Windows PowerShell 执行：

```powershell
ipconfig
```

优先查看当前正在联网的网卡 IPv4 地址。

## 当前同步行为

1. 分类和交易的本地增删改都会写入 `sync_operations` 队列。
2. 个人中心新增“立即同步”按钮。
3. 点击后会执行：
   - 先上传本地待同步操作
   - 再从后端全量拉取当前用户的分类和交易
   - 将拉回的数据回写到本地 Room
4. 如果后端还没有任何数据，并且本地也没有新的待同步改动，界面会给出提示，不会清空现有本地数据。

## 当前限制

1. 后端使用内存存储，重启后数据会丢失。
2. 同步当前走“演示级最小闭环”，优先保证联调稳定，不追求生产级冲突合并。
3. OCR 历史仍然只保存在本地，不参与同步。

## 明天继续时优先验证

1. 启动 `backend` 模块。
2. 在手机上把 `base.url` 指向电脑局域网 IP。
3. 打开 App，进入“我的”页点击“立即同步”。
4. 新增一条交易和一个分类，再点一次“立即同步”。
5. 退出重进或清本地后，验证后端数据是否能拉回。
