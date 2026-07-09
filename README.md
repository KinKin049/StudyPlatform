# StudyPlatform

StudyPlatform 是一个包含 Spring Boot 后端、Vue/Vite 前端、OJ 沙箱与学习宠物模块的综合学习平台。

## 跨电脑运行

把整个项目文件夹复制到另一台 Windows 电脑后，优先阅读：

```text
docs/cross-computer-setup.md
```

最短启动流程：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\setup-windows.ps1
.\scripts\start-backend.ps1
.\scripts\start-frontend.ps1
```

也可以用一键启动脚本：

```cmd
scripts\start-dev-windows.cmd
```

必需环境：

- Node.js 22.18+ 或 24.12+
- JDK 21
- MySQL 8.x

访问首页：

```text
http://localhost:5173
```

## OJ Page

Start backend and frontend, then open:

```text
http://127.0.0.1:5173/oj.html
```

For the OJ file layout and file responsibilities, see:

```text
docs/oj-file-map.md
```

## C++ Judge Sandbox

The development C++ judge service lives in:

```text
judge-sandbox
```

It requires `g++` or Docker. Start it on port `9000`, then run the backend with:

```properties
oj.sandbox-url=http://localhost:9000
```
