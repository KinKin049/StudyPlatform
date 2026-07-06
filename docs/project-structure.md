# StudyPlatform 项目结构文档

## 项目概述

这是一个**学习平台系统**，采用前后端分离架构，包含在线评测(OJ)、课程学习、题库练习、游戏化学习、石油专业模拟等多个功能模块。

## 整体目录结构

```
studyPlatform/                    # 项目根目录
├── .idea/                        # IntelliJ IDEA配置
└── StudyPlatform/                # 主项目目录
    ├── .idea/                    # 子项目IDE配置
    ├── CET46/                    # 四六级词汇SQL数据
    ├── docs/                     # 项目文档
    ├── StudyPlatform-back/       # Spring Boot后端服务
    ├── judge-sandbox/            # 代码评测沙箱(独立服务)
    ├── studyplatform-vue/        # Vue前端应用
    └── README.md
```

## 后端服务 - StudyPlatform-back

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.16 | 应用框架 |
| Java | 21 | 编程语言 |
| MySQL | - | 数据库 |
| Flyway | 11.7.2 | 数据库迁移 |
| Spring Security | - | 安全认证 |
| Spring Mail | - | 邮件发送 |
| Jsoup | 1.18.3 | HTML解析 |
| Lombok | - | 代码简化 |

### 模块划分（按业务领域）

```
src/main/java/com/cupk/
├── StudyPlatformBackApplication.java    # 启动类
├── academy/                             # 学院学习模块
│   ├── controller/                      # 课程、题库、作业、考试
│   ├── service/                         # 学习服务 + 题库种子数据
│   ├── repository/                      # 数据访问
│   └── dto/                             # 数据传输对象
├── admin/                               # 管理员模块
│   ├── controller/                      # 管理员接口
│   ├── service/                         # 管理服务
│   ├── repository/                      # 数据访问
│   └── dto/                             # 管理DTO
├── auth/                                # 认证模块
│   ├── controller/                      # 登录/注册/密码重置
│   ├── service/                         # 认证服务
│   ├── repository/                      # 用户/重置码数据
│   └── dto/                             # 认证DTO
├── oj/                                  # 在线评测模块
│   ├── controller/                      # 题目/提交接口
│   ├── service/                         # 评测服务(含沙箱客户端)
│   ├── repository/                      # 题目/提交数据
│   ├── model/                           # 数据库实体
│   ├── dto/                             # 评测DTO
│   └── config/                          # OJ配置
├── games/                               # 游戏化学习模块
│   ├── controller/                      # 天梯跳跃/打字战士
│   ├── service/                         # 游戏服务
│   ├── repository/                      # 游戏记录
│   └── dto/                             # 游戏DTO
├── production/                          # 生产模拟模块
│   ├── controller/                      # 生产记录接口
│   ├── service/                         # 生产服务
│   ├── repository/                      # 生产数据
│   ├── model/                           # 生产实体
│   └── dto/                             # 生产DTO
├── welllog/                             # 测井模拟模块
│   ├── controller/                      # 测井记录/模板
│   ├── service/                         # 测井服务
│   ├── repository/                      # 测井数据
│   ├── model/                           # 测井实体
│   └── dto/                             # 测井DTO
└── config/                              # 全局配置
    ├── SecurityConfig.java              # Spring Security配置
    ├── WebMvcConfig.java                # Web MVC配置
    └── ApiExceptionHandler.java         # 全局异常处理
```

### 资源文件

```
src/main/resources/
├── application.properties               # 应用配置
├── db/migration/                        # Flyway数据库迁移脚本(V1-V43)
└── question-bank-sources/               # 题库数据源文件
    ├── cet4-vocabulary.json
    ├── cet6-vocabulary.json
    ├── database-mysql.md
    ├── ideology-law.html
    ├── maoism.html
    ├── marxism.json
    ├── modern-history.html
    ├── ncre.md
    └── python-study-note.md
```

### 外部目录

```
StudyPlatform-back/
├── src/main/resources/db/migration/      # Flyway自动迁移SQL
├── src/main/resources/db/manual/         # 手工执行SQL，如建库脚本
├── storage/                             # 文件存储(教材图片、题库封面)
├── logs/                                # 日志文件
└── docs/                                # 模块文档
```

## 评测沙箱 - judge-sandbox

独立的代码评测沙箱服务，基于 Node.js 实现，使用 MinGW64 作为编译工具链。

```
judge-sandbox/
├── src/
│   └── server.js                        # 沙箱服务入口
├── toolchains/
│   └── mingw64/                         # MinGW64编译工具链
│       └── bin/                         # gcc/g++/gdb等
├── .env.example
├── Dockerfile
├── package.json
└── README.md
```

## 前端应用 - studyplatform-vue

Vue 前端项目，按功能模块组织页面：

### 页面结构

```
src/pages/
├── HomePage.vue                         # 首页
├── LoginPage.vue                        # 登录页
├── RegisterPage.vue                     # 注册页
├── ProfilePage.vue                      # 个人中心
├── AdminPage.vue                        # 管理员页面
├── OjPlatform.vue                       # OJ评测平台
├── GamePlatform.vue                     # 游戏平台
│   ├── LadderJumpGame.vue               # 天梯跳跃
│   └── TypeWarriorGame.vue              # 打字战士
├── AcademyPage.vue                      # 学院首页
│   ├── AcademyHome.vue                  # 学院首页
│   ├── AcademyGeneralCourses.vue        # 通识课程
│   ├── AcademyMicroMajors.vue           # 微专业
│   ├── AcademyOpenCourses.vue           # 在线开放课程
│   ├── AcademyTextbooks.vue             # 教材
│   ├── AcademyQuestionBank.vue          # 题库
│   ├── AcademyCourseDetail.vue          # 课程详情
│   ├── AcademyAssignmentDetail.vue      # 作业详情
│   └── AcademyExamDetail.vue            # 考试详情
├── VisualizationHome.vue                # 可视化中心
│   ├── DataStructureVisualization.vue   # 数据结构可视化
│   ├── AlgorithmDemoViewer.vue          # 算法演示
│   ├── FunctionGraph2D.vue              # 函数图像
│   └── SpaceModel3D.vue                 # 空间模型3D
├── PetroleumSimulation.vue              # 石油模拟
│   ├── WellLogPanel.vue                 # 测井面板
│   ├── ReservoirDynamicsPanel.vue       # 油藏动态
│   ├── WaterfloodPanel.vue              # 注水开发
│   └── PumpIndicatorPanel.vue           # 泵况指示
└── LabPlatform.vue                      # 实验平台
```

## 功能模块总结

| 模块 | 功能描述 |
|------|----------|
| **Academy** | 课程学习、作业、考试、题库练习、错题本、收藏夹 |
| **OJ** | 在线编程评测、代码提交、判题系统 |
| **Games** | 天梯跳跃(答题游戏)、打字战士(英语单词) |
| **Production** | 石油生产模拟(注水、压裂、油藏、泵况) |
| **WellLog** | 测井记录管理、模板管理 |
| **Visualization** | 数据结构可视化、算法演示、函数图像 |
| **Auth** | 用户注册、登录、密码重置、邮箱验证 |
| **Admin** | 课程管理、用户管理、题库管理 |

## 数据库迁移

项目使用 Flyway 管理数据库版本，共 **43 个迁移脚本**，涵盖：
- 初始化架构
- OJ题目表
- 学习内容表(课程、教材)
- 题库表(多种题型)
- 用户认证表
- 作业/考试表
- 游戏记录表
- 生产模拟表

## 配置说明

后端服务通过 `application.properties` 配置：
- 数据库连接：MySQL `localhost:3306/study_platform`
- 邮箱服务：QQ邮箱 SMTP
- 文件上传限制：500MB
- OJ沙箱地址：通过环境变量 `OJ_SANDBOX_URL` 配置

项目支持通过 `application-local.properties` 覆盖本地开发配置。
