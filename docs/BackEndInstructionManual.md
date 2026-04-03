后端已经重启成功并可用。

当前状态：

监听端口：8080
进程 PID：27652
登录接口探活成功（/api/auth/login 返回 code=0）
你自己常用这几条 PowerShell 命令就够了：

启动后端（前台看实时日志）
cd E:\AndroidDevelopSave\OCRjizhang
.\gradlew.bat :backend:bootRun
启动后端（后台运行）
$repo='E:\AndroidDevelopSave\OCRjizhang'
Start-Process -FilePath "$repo\gradlew.bat" -ArgumentList ":backend:bootRun" -WorkingDirectory $repo
关闭后端（按端口杀进程）
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }
重启后端
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }
cd E:\AndroidDevelopSave\OCRjizhang
.\gradlew.bat :backend:bootRun
看是否启动成功
netstat -ano | Select-String ":8080"
看日志文件（如果你用后台启动）
标准输出：bootRun.out.log
错误输出：bootRun.err.log
Get-Content E:\AndroidDevelopSave\OCRjizhang\backend\build\bootRun.out.log -Tail 80
Get-Content E:\AndroidDevelopSave\OCRjizhang\backend\build\bootRun.err.log -Tail 80