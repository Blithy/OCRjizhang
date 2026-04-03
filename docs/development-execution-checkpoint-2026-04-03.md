# Development Checkpoint - 2026-04-03

本文件用于承接 `docs/development-execution-plan.md` 与 `docs/current-delivery-order-2026-04-01.md`，记录今天（2026-04-03）结束时的实际进度，供明天直接续做。

## 对照主线（H9）

当前阶段仍为 H9（同步链路下账户与交易挂靠一致性修复），未切换到 H10。

## 今日已完成

1. 明确并复现了核心问题：  
   - 新增支出后，账户 `SYNC1` 本地余额先正确从 `B0 -> B1`。  
   - 点击“我的 -> 立即同步”后，在不清本地的情况下余额回滚 `B1 -> B0`。
2. 完成根因定位：  
   - 后端 `DemoStore` 在交易创建/更新/删除时未同步更新 `accounts.balanceFen`。  
   - 同步 pull 时旧余额覆盖本地，导致回滚。
3. 完成后端修复（交易驱动余额变化）：  
   - `createOrUpdateTransaction`：更新交易前先回滚旧交易影响，再应用新交易影响。  
   - `deleteTransaction`：删除交易时回滚该交易对账户余额的影响。  
   - 新增统一的余额增量计算方法（按 `INCOME/EXPENSE` 计算 signed amount）。
4. 补充回归测试：  
   - 新增 `transactionSyncShouldUpdateAccountBalanceAndRollbackOnUpdateDelete`，覆盖创建/更新/删除交易后的账户余额变化。  
5. 本地验证：  
   - 使用本地 Gradle 缓存执行测试通过：  
     - `:backend:test --tests '*DemoStoreTest*'`  
     - 结果：`BUILD SUCCESSFUL`
6. 代码已提交并推送到 GitHub：  
   - commit: `1ffc1e2`  
   - message: `fix(sync): keep account balances consistent after transaction sync`

## 当前状态快照（收工时）

1. 代码已在远端 `origin/main`。  
2. 后端进程当前未运行（`127.0.0.1:8080` 不可连）。  
3. 真机最终回归尚未完成，等待明天按步骤复测。

## 明日待办（按顺序）

1. 启动后端（Android Studio 运行 `DemoBackendApplication`）。  
2. 确认真机使用的 `base.url` 指向电脑可访问地址（当前配置示例：`http://192.168.50.197:8080/api/`）。  
3. 重新安装 debug 包到真机。  
4. 执行核心回归用例（必须记录数值）：  
   - 记录 `SYNC1` 初始余额 `B0`  
   - 新增支出金额 `A` 后应为 `B1 = B0 - A`  
   - 点击“立即同步”后余额应保持 `B1`（不应回滚 `B0`）
5. 重启 App 后再次同步，确认余额仍保持 `B1`。  
6. 若仍异常，收集以下信息后继续修复：  
   - `B0/A/B1` 三个值与操作时间  
   - 当次同步前后资产页截图  
   - 对应时间段的后端请求日志（accounts/transactions/sync）

## 明日第一条执行命令（建议）

先在仓库根目录执行一次后端单测，确保环境一致：

```powershell
$env:GRADLE_USER_HOME='C:\Users\Blithy\.gradle'
.\gradlew.bat :backend:test --tests '*DemoStoreTest*'
```

通过后再开始真机回归。
