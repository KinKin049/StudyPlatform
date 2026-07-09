# 跨电脑运行说明

这份说明用于把整个 `StudyPlatform` 文件夹复制到另一台 Windows 电脑后，尽量稳定地重新跑通前后端。

## 必需环境

- Windows 10/11
- Node.js `22.18+` 或 `24.12+`
- JDK `21`
- MySQL `8.x` 或兼容版本
- 可访问 npm 与 Maven 仓库的网络

> 如果新电脑不能联网，需要额外复制 `studyplatform-vue/node_modules` 以及 Maven 本地仓库缓存；更推荐联网后重新安装依赖。

## 推荐迁移方式

复制项目时可以不带这些目录，它们会在新电脑重新生成：

```text
studyplatform-vue/node_modules
studyplatform-vue/dist
StudyPlatform-back/target
target
```

必须保留这些文件：

```text
studyplatform-vue/package.json
studyplatform-vue/package-lock.json
StudyPlatform-back/mvnw.cmd
StudyPlatform-back/.mvn/wrapper/maven-wrapper.properties
StudyPlatform-back/src/main/resources/db/migration
```

`markdown-it` 和 `dompurify` 已经写入 `studyplatform-vue/package.json` 与 `studyplatform-vue/package-lock.json`，所以复制源码后执行 `npm ci` 会自动恢复 Markdown 渲染能力。

## 第一次初始化

在项目根目录打开 PowerShell：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\setup-windows.ps1
```

脚本会完成：

- 检查 `node`、`npm`、`java`
- 根据 `package-lock.json` 安装前端依赖
- 复制 `StudyPlatform-back/application-local.example.properties` 为本机配置
- 预下载并编译后端 Maven 依赖

## 数据库准备

在 MySQL 中创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS study_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

然后修改：

```text
StudyPlatform-back/application-local.properties
```

至少确认：

```properties
DB_URL=jdbc:mysql://localhost:3306/study_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
```

数据库表由 Flyway 在后端启动时自动迁移。

## 启动项目

方式一：分别启动，方便观察日志。

```powershell
.\scripts\start-backend.ps1
.\scripts\start-frontend.ps1
```

方式二：双击或运行一键启动脚本。

```cmd
scripts\start-dev-windows.cmd
```

访问：

```text
http://localhost:5173
```

## 可选配置

AI 宠物聊天需要在 `StudyPlatform-back/application-local.properties` 配置：

```properties
AI_PET_BASE_URL=https://yunwu.ai
AI_PET_API_KEY=your_token_here
AI_PET_MODEL=deepseek-v4-flash
```

OJ 判题沙箱是可选服务。如需开启，单独启动 `judge-sandbox`，并配置：

```properties
OJ_SANDBOX_URL=http://localhost:9000
```

## 常见问题

### `npm ci` 失败

确认 Node.js 版本满足 `22.18+` 或 `24.12+`，并且新电脑可以访问 npm 源。

### 后端提示数据库连接失败

确认 MySQL 已启动、数据库 `study_platform` 已创建、`application-local.properties` 中的账号密码正确。

### AI 宠物可以打开但聊天失败

通常是 `AI_PET_API_KEY` 未配置或代理服务不可访问。该问题不影响普通页面运行。

### Markdown 仍然显示星号

确认新电脑执行过：

```powershell
cd studyplatform-vue
npm ci
```

并确认 `node_modules/markdown-it` 和 `node_modules/dompurify` 存在。
