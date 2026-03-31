# Android OCR 记账 App 毕设演示版正式规格

## 1. 文档定位

本文档用于指导本项目后续 Android 客户端与本地 Spring Boot 演示后端的实际开发，目标是将前期需求收敛为一份可以直接开工的实现规格。

本文档优先级高于前期口头讨论与零散说明；若后续实现中发现与本文档不一致，以本文档为准，再做增量修订。

## 2. 项目目标

### 2.1 项目名称

Android 智能 OCR 记账 App

### 2.2 项目定位

本项目是毕业设计演示项目，重点在于 Android 记账 App 的完整功能展示，而非生产环境部署能力。

### 2.3 核心目标

实现一款具备以下能力的 Android 记账应用：

1. 用户注册、登录、退出与基础资料管理。
2. 收支交易增删改查。
3. 分类管理。
4. 收支统计与图表可视化。
5. 基于百度 OCR API 的票据识别记账。
6. 本地数据存储与本地 Spring Boot 后端同步演示。

### 2.4 非目标

以下内容不作为本项目第一版必须实现的目标：

1. 生产级安全加固。
2. 正式公网部署、域名、HTTPS 证书与运维体系。
3. 管理员后台页面。
4. 多账本、多币种、多人协作。
5. 消息推送、邮箱验证、短信验证、找回密码。
6. OCR 结果云端同步。

## 3. 已确认的关键决策

### 3.1 演示场景

1. Spring Boot 后端仅用于本机开发与演示联调。
2. 后端无需正式上线部署。
3. 功能实现优先级高于安全细节。

### 3.2 OCR 调用方式

1. OCR 由 Android App 直接调用百度 OCR API。
2. 不通过 Spring Boot 后端中转 OCR 请求。
3. 百度 OCR 的 `API Key` 与 `Secret Key` 不直接硬编码在源码中。
4. 本地开发阶段通过 `local.properties` 或 `gradle.properties` 注入到 `BuildConfig`。

### 3.3 认证方案

1. 保留注册与登录功能。
2. 仅支持普通用户角色，不设计管理员角色。
3. 使用简单 `JWT` 或自定义 `token` 方案均可，开发实现优先选择最简单稳定方案。
4. 第一版不实现 `refresh token`。
5. 客户端仅保存 `token` 与必要的用户摘要信息，不保存明文密码。
6. 密码在后端使用哈希存储。

### 3.4 数据权限

1. 用户只能访问自己的交易和分类数据。
2. 后端所有交易与分类接口必须依据当前登录用户进行数据隔离。

### 3.5 同步原则

1. App 以本地可用优先。
2. 联网时执行自动同步。
3. 冲突处理采用简单策略：`updatedAt` 较新的数据覆盖较旧数据；若时间完全相同，以本地数据为准。
4. 删除采用物理删除。
5. 为支持离线后补同步，客户端需要维护本地待同步操作记录。

### 3.6 业务限制

1. 不支持游客模式。
2. 分类删除后，历史交易自动迁移到默认分类“未分类”。
3. OCR 历史仅本地保存，不同步到云端。
4. 统计默认展示“本月”数据。

## 4. 技术栈与版本基线

## 4.1 Android 客户端

1. 开发语言：Kotlin
2. 架构模式：MVVM
3. 依赖注入：Hilt
4. 本地数据库：Room
5. 异步方案：Coroutines + Flow
6. 网络请求：Retrofit2 + OkHttp3 + Gson
7. UI 体系：Material Design 3 + ViewBinding
8. 导航：Navigation Component
9. 图表：MPAndroidChart
10. OCR：百度智能云 OCR API

### 4.2 Android 版本基线

1. `minSdk = 34`
2. `targetSdk = 34`
3. `compileSdk = 36`
4. `JDK = 17`

说明：

当前工程依赖版本要求 `compileSdk` 至少为 36，因此正式规格采用 `compileSdk 36 / targetSdk 34 / minSdk 34` 组合。

### 4.3 后端技术栈

1. Spring Boot 3.2.x
2. MyBatis-Plus
3. MySQL 8.0
4. Lombok
5. Spring Web
6. Spring Validation
7. JWT 或简单 token 鉴权

## 5. 开发范围

### 5.1 第一版必须完成

1. 用户注册、登录、退出。
2. 用户信息查看与简单修改。
3. 收入与支出交易管理。
4. 收支分类管理。
5. 日、周、月统计。
6. 饼图与柱状图展示。
7. 拍照或相册选图进行 OCR 识别。
8. OCR 结果自动填充记账表单。
9. 本地 Room 存储。
10. 与本地 Spring Boot 后端的数据同步。

### 5.2 第一版可延后

1. OCR 原图长期缓存管理优化。
2. 更细粒度同步提示。
3. 分类图标与颜色自定义。
4. 完整搜索与筛选。
5. 数据导出。

## 6. 总体架构

### 6.1 Android 架构分层

1. `ui`：页面、Dialog、Adapter、ViewModel、UI 状态。
2. `data/local`：Room 数据库、DAO、本地实体。
3. `data/remote`：Retrofit Service、远程 DTO、请求响应模型。
4. `repository`：统一封装本地与远程数据来源。
5. `di`：Hilt 模块。
6. `utils`：图片、OCR、网络、时间、金额等工具。

### 6.2 数据流原则

1. UI 层只与 ViewModel 交互。
2. ViewModel 不直接操作 DAO 或 Retrofit。
3. Repository 是数据访问唯一入口。
4. 本地数据通过 Flow 驱动列表和统计页面刷新。
5. 网络请求统一通过 RemoteDataSource 或 Repository 发起。

### 6.3 推荐目录结构

```text
app/src/main/java/com/example/ocrjizhang/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── database/
│   │   ├── entity/
│   │   └── converter/
│   ├── remote/
│   │   ├── api/
│   │   ├── dto/
│   │   ├── request/
│   │   ├── response/
│   │   └── service/
│   ├── mapper/
│   └── repository/
├── di/
├── ui/
│   ├── auth/
│   ├── home/
│   ├── transaction/
│   ├── category/
│   ├── statistics/
│   ├── profile/
│   └── ocr/
├── model/
├── utils/
└── MainActivity.kt
```

## 7. 核心数据模型

### 7.1 User

用途：用户账号信息。

建议字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 用户主键 |
| username | String | 是 | 登录名，唯一 |
| passwordHash | String | 是 | 仅后端保存哈希值 |
| email | String | 否 | 邮箱 |
| phone | String | 否 | 手机号 |
| nickname | String | 否 | 昵称 |
| createdAt | Long | 是 | 创建时间戳 |
| updatedAt | Long | 是 | 修改时间戳 |

客户端本地缓存不保存 `passwordHash`。

### 7.2 Category

用途：收支分类。

建议字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 分类主键 |
| userId | Long | 是 | 所属用户 |
| name | String | 是 | 分类名称 |
| type | String | 是 | `INCOME` 或 `EXPENSE` |
| icon | String | 否 | 图标标识，可延后实现 |
| color | String | 否 | 颜色值，可延后实现 |
| isDefault | Boolean | 是 | 是否默认分类 |
| createdAt | Long | 是 | 创建时间 |
| updatedAt | Long | 是 | 修改时间 |
| syncStatus | String | 是 | 同步状态 |

固定默认分类：

1. 支出默认分类至少包含：餐饮、交通、购物、日用、娱乐、医疗、住房、未分类。
2. 收入默认分类至少包含：工资、奖金、兼职、理财、其他、未分类。

### 7.3 Transaction

用途：收支交易记录。

建议字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 交易主键 |
| userId | Long | 是 | 所属用户 |
| type | String | 是 | `INCOME` 或 `EXPENSE` |
| amountFen | Long | 是 | 金额，单位分 |
| categoryId | Long | 是 | 分类主键 |
| categoryName | String | 是 | 冗余分类名，便于展示 |
| remark | String | 否 | 备注 |
| merchantName | String | 否 | 商户名 |
| transactionTime | Long | 是 | 交易发生时间 |
| source | String | 是 | `MANUAL` 或 `OCR` |
| createdAt | Long | 是 | 创建时间 |
| updatedAt | Long | 是 | 修改时间 |
| syncStatus | String | 是 | 同步状态 |

金额统一用“分”存储，不使用 `Float` 或 `Double`。

### 7.4 OcrRecord

用途：本地 OCR 识别历史。

建议字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | OCR 记录主键 |
| userId | Long | 是 | 所属用户 |
| imageUri | String | 是 | 原图或缓存图路径 |
| amountText | String | 否 | OCR 原始金额文本 |
| amountFen | Long | 否 | 解析后的金额 |
| dateText | String | 否 | OCR 原始日期文本 |
| merchantName | String | 否 | 商户名称 |
| rawJson | String | 否 | OCR 原始响应，便于调试 |
| createdAt | Long | 是 | 识别时间 |

OCR 历史仅保留最近 50 条。

### 7.5 SyncOperation

用途：支持离线后补同步。

建议字段：

| 字段名 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | Long | 是 | 队列主键 |
| entityType | String | 是 | `TRANSACTION` 或 `CATEGORY` |
| entityId | Long | 是 | 业务数据主键 |
| operationType | String | 是 | `CREATE`、`UPDATE`、`DELETE` |
| payloadJson | String | 否 | 删除前快照或待上传内容 |
| createdAt | Long | 是 | 入队时间 |
| retryCount | Int | 是 | 重试次数 |

## 8. 本地数据库设计

### 8.1 Room 表

第一版建议至少包含以下表：

1. `users`
2. `categories`
3. `transactions`
4. `ocr_records`
5. `sync_operations`

### 8.2 Room 设计要求

1. 时间统一使用 `Long` 时间戳毫秒值。
2. 金额统一使用 `Long`，单位分。
3. Flow 用于交易列表、分类列表、统计结果的监听。
4. DAO 命名统一使用 `xxxDao`。
5. 数据库升级使用显式 Migration，不允许开发中长期依赖 `fallbackToDestructiveMigration`。

## 9. 后端数据库设计

### 9.1 MySQL 表

第一版建议至少包含以下表：

1. `user`
2. `category`
3. `transaction_record`

### 9.2 数据库规范

1. 主键使用 `BIGINT`。
2. 字符集使用 `utf8mb4`。
3. 时间字段使用 `BIGINT` 时间戳或 `DATETIME`，实现时二选一后保持全局统一。
4. `username` 唯一。
5. 可选：`email` 和 `phone` 唯一。

## 10. 用户认证与登录状态设计

### 10.1 业务规则

1. App 启动后若本地存在有效 `token`，直接进入主页。
2. 若无有效 `token`，进入登录页面。
3. 第一版必须支持注册、登录、退出登录。
4. 第一版不支持游客模式。

### 10.2 token 方案

1. 登录成功后后端返回 `token`、`userId`、`username`、`nickname`。
2. 客户端使用 `SharedPreferences` 或 `DataStore` 保存 `token` 与当前用户摘要信息。
3. 第一版不实现 `refresh token`。
4. 当后端返回未登录或 token 失效时，客户端清空登录状态并跳转登录页。

### 10.3 密码规则

1. 注册时密码长度建议不少于 6 位。
2. 后端对密码进行哈希处理。
3. 客户端不缓存明文密码。

## 11. 交易与分类业务规则

### 11.1 交易规则

1. 每条交易必须选择收入或支出类型。
2. 每条交易必须有金额。
3. 每条交易必须归属一个分类。
4. 默认交易时间为当前时间。
5. 允许添加备注。
6. OCR 填充后用户仍可手动修改任意字段。

### 11.2 分类规则

1. 用户可以新增、编辑、删除自定义分类。
2. 默认分类不可删除。
3. 删除自定义分类时，该分类下所有历史交易自动迁移到同类型默认“未分类”。

## 12. 统计模块规则

### 12.1 统计周期

支持以下统计周期：

1. 日
2. 周
3. 月

默认进入统计页时显示“本月”。

### 12.2 展示内容

1. 总收入
2. 总支出
3. 结余
4. 支出分类饼图
5. 收支趋势柱状图

### 12.3 第一版口径

1. 饼图优先展示支出分类占比。
2. 柱状图展示当前周期内收入与支出汇总。
3. 暂不实现同比、环比、自定义区间。

## 13. OCR 模块规格

### 13.1 第一版目标

支持消费小票与通用票据识别，并从识别结果中提取以下字段：

1. 金额
2. 日期
3. 商户名称

### 13.2 图片来源

1. 相机拍照
2. 相册选图

### 13.3 识别流程

1. 用户选择拍照或相册图片。
2. App 对图片进行压缩。
3. App 将图片转为 Base64。
4. App 调用百度 OCR 接口。
5. 解析金额、日期、商户名。
6. 将结果带入记账录入页。
7. 用户确认并保存交易。

### 13.4 失败处理

1. OCR 接口失败时提示“识别失败，请重试或手动输入”。
2. 若未识别到金额，则不允许直接保存为 OCR 自动结果，需用户手动补全金额。
3. 若只识别出部分字段，允许进入记账页手动补全。

### 13.5 图片策略

1. 上传前压缩至 4MB 内。
2. 建议最长边限制在 1280 至 1920 像素之间。
3. OCR 原图仅本地缓存，允许后续手动清理。

## 14. 数据同步设计

### 14.1 同步范围

同步以下数据：

1. 用户账号基础信息
2. 分类
3. 交易

不同步以下数据：

1. OCR 历史
2. OCR 原图

### 14.2 自动同步策略

1. 登录成功后拉取一次云端数据。
2. 新增、编辑、删除交易后尝试自动同步。
3. 新增、编辑、删除分类后尝试自动同步。
4. 网络不可用时仅落本地，并写入 `sync_operations`。

### 14.3 手动同步策略

提供“立即同步”入口，执行步骤如下：

1. 先上传本地待同步操作。
2. 再拉取云端最新分类与交易。
3. 依据 `updatedAt` 合并数据。

### 14.4 冲突处理

1. 比较本地与云端的 `updatedAt`。
2. `updatedAt` 较新的记录覆盖较旧记录。
3. 若时间完全一致，本地记录优先。

### 14.5 删除同步

由于业务要求使用物理删除，因此删除时必须将删除动作写入 `sync_operations`，确保离线删除在恢复联网后仍可同步到后端。

## 15. 后端 API 规范

### 15.1 通用规则

1. 所有业务接口统一返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

2. `code = 0` 表示成功。
3. 非 0 表示业务失败。
4. 需要登录的接口统一在请求头传 `Authorization: Bearer <token>`。

### 15.2 Base URL

本地联调使用：

1. Android 模拟器：`http://10.0.2.2:8080/api`
2. 真机调试：`http://<电脑局域网IP>:8080/api`

### 15.3 认证接口

#### 注册

- 方法：`POST`
- 路径：`/auth/register`

请求示例：

```json
{
  "username": "demo_user",
  "password": "123456",
  "email": "demo@example.com",
  "phone": "13800000000",
  "nickname": "演示用户"
}
```

#### 登录

- 方法：`POST`
- 路径：`/auth/login`

请求示例：

```json
{
  "username": "demo_user",
  "password": "123456"
}
```

响应示例：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "token": "token_string",
    "userId": 1,
    "username": "demo_user",
    "nickname": "演示用户"
  }
}
```

### 15.4 用户接口

#### 获取当前用户信息

- 方法：`GET`
- 路径：`/user/me`

#### 更新用户信息

- 方法：`PUT`
- 路径：`/user/me`

#### 修改密码

- 方法：`PUT`
- 路径：`/user/password`

### 15.5 分类接口

#### 查询分类列表

- 方法：`GET`
- 路径：`/categories`

#### 新增分类

- 方法：`POST`
- 路径：`/categories`

#### 编辑分类

- 方法：`PUT`
- 路径：`/categories/{id}`

#### 删除分类

- 方法：`DELETE`
- 路径：`/categories/{id}`

### 15.6 交易接口

#### 查询交易列表

- 方法：`GET`
- 路径：`/transactions`
- 参数：支持按开始时间、结束时间、类型筛选

#### 新增交易

- 方法：`POST`
- 路径：`/transactions`

#### 更新交易

- 方法：`PUT`
- 路径：`/transactions/{id}`

#### 删除交易

- 方法：`DELETE`
- 路径：`/transactions/{id}`

### 15.7 同步接口

#### 上传待同步变更

- 方法：`POST`
- 路径：`/sync/push`

请求体包含：

1. 待新增分类
2. 待更新分类
3. 待删除分类 ID
4. 待新增交易
5. 待更新交易
6. 待删除交易 ID

#### 拉取最新数据

- 方法：`POST`
- 路径：`/sync/pull`

请求示例：

```json
{
  "lastSyncTime": 0
}
```

响应中返回：

1. 分类列表
2. 交易列表
3. 服务器当前时间

## 16. Android 权限与系统配置

### 16.1 Manifest 必要权限

1. `android.permission.INTERNET`
2. `android.permission.CAMERA`
3. Android 13+ 如使用媒体读取，申请 `READ_MEDIA_IMAGES`

### 16.2 推荐实现方式

1. 相册优先使用系统 Photo Picker，降低权限复杂度。
2. 拍照使用系统相机 Intent 或 CameraX，第一版优先选择实现成本更低的方案。
3. 如使用本地 HTTP 联调，调试环境开启明文流量支持。

### 16.3 网络配置

调试环境允许访问本地 HTTP 服务；如采用 `usesCleartextTraffic` 或 `network_security_config`，需在文档和代码中保持一致。

## 17. UI 与交互规范

### 17.1 整体风格

1. 基于 Material Design 3。
2. 风格简洁、清晰，适合毕业设计演示。
3. 优先保证信息结构清楚，而非追求复杂动画。

### 17.2 基本页面

第一版至少包含：

1. 启动页
2. 登录页
3. 注册页
4. 首页总览
5. 交易列表页
6. 新增或编辑交易页
7. 分类管理页
8. 统计页
9. OCR 识别页
10. 个人中心页

### 17.3 状态页要求

每个核心页面至少考虑：

1. 加载中
2. 空数据
3. 请求失败
4. 无网络

## 18. 开发规范

### 18.1 Kotlin 代码规范

1. 采用 Kotlin 官方代码风格。
2. 类名使用大驼峰。
3. 方法与变量使用小驼峰。
4. 常量使用全大写下划线命名。
5. 避免在 Activity 或 Fragment 中直接写业务逻辑。
6. ViewModel 使用 `UiState` 驱动界面。

### 18.2 分层规范

1. `Entity` 仅用于数据库层。
2. `DTO/Request/Response` 仅用于网络层。
3. UI 不直接依赖 Retrofit Response。
4. 需要通过 mapper 将网络模型或数据库实体转换为 UI 可用模型。

### 18.3 资源命名规范

1. 布局文件：`activity_xxx`、`fragment_xxx`、`dialog_xxx`、`item_xxx`
2. 字符串：`page_xxx`、`action_xxx`、`hint_xxx`、`message_xxx`
3. 图片资源：`ic_xxx`、`bg_xxx`

### 18.4 Git 提交规范

采用 Angular Commit Message 风格，例如：

1. `feat: 添加 OCR 识别页`
2. `fix: 修复交易同步失败问题`
3. `refactor: 重构交易仓库结构`
4. `test: 补充登录 ViewModel 单元测试`
5. `docs: 更新毕设开发规格`

### 18.5 测试规范

第一版至少补充单元测试，重点覆盖：

1. 金额与时间工具类
2. Repository 核心逻辑
3. ViewModel 状态流转
4. 同步策略关键逻辑

### 18.6 日志规范

1. Debug 日志不得打印明文密码。
2. 不打印完整 token。
3. OCR 原始响应日志仅在调试环境允许打印，发布前应关闭。

## 19. 异常处理规范

### 19.1 客户端异常提示

统一用户可见文案方向：

1. 网络异常：`网络连接失败，请稍后重试`
2. 登录失效：`登录已失效，请重新登录`
3. OCR 失败：`识别失败，请重试或手动输入`
4. 同步失败：`同步失败，已保留本地数据`

### 19.2 后端异常返回

建议业务错误码：

1. `1001` 用户名已存在
2. `1002` 用户名或密码错误
3. `1003` token 无效
4. `2001` 分类不存在
5. `3001` 交易不存在
6. `4001` 同步失败

## 20. 里程碑与开发顺序

推荐按以下顺序开发：

1. 调整工程基础配置与依赖。
2. 搭建 MVVM + Hilt + Room 基础结构。
3. 完成用户注册、登录、退出。
4. 完成分类管理。
5. 完成交易增删改查。
6. 完成本地统计模块。
7. 集成 OCR 识别与结果填充。
8. 完成后端接口与同步逻辑。
9. 补充单元测试与演示优化。

## 21. 验收标准

### 21.1 用户模块

1. 可注册新用户。
2. 可登录并保持登录状态。
3. 可退出登录并清空本地登录状态。

### 21.2 记账模块

1. 可新增收入和支出。
2. 可编辑和删除交易。
3. 可按分类查看交易。

### 21.3 分类模块

1. 可新增和编辑自定义分类。
2. 删除分类后历史交易自动归入“未分类”。

### 21.4 统计模块

1. 可查看本日、本周、本月汇总。
2. 可展示饼图和柱状图。

### 21.5 OCR 模块

1. 可从相机或相册获取图片。
2. 可识别出金额、日期或商户中的至少部分信息。
3. 可将识别结果带入新增交易页。

### 21.6 同步模块

1. 联网时新增、编辑、删除交易可以同步到后端。
2. 联网时分类变更可以同步到后端。
3. 断网时可正常使用本地记账，恢复联网后可补同步。

## 22. 当前版本结论

基于当前毕设目标，项目后续开发采用以下固定方案：

1. Android 客户端为主，Spring Boot 后端仅作本地演示联调。
2. OCR 由 App 直连百度 OCR API。
3. 登录保留，但采用简单 token 方案，不做复杂安全体系。
4. 同步采用简化的 `updatedAt` 冲突处理规则。
5. `compileSdk = 36`，`targetSdk = 34`，`minSdk = 34`。

后续开发默认按照本文档执行，除非用户明确要求修订。
