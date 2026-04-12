# home-dash 后端项目 Skills 规范

## 1. 项目概述

`home-dash` 是当前后端项目目录名，对应原 PND（Personal Network Disk）后端工程，基于 Spring Boot 构建，提供文件管理、大文件上传/下载、视频播放等服务端支持。

## 2. 技术栈

- **框架**: Spring Boot 3.3.4
- **数据访问**: MyBatis
- **嵌入式数据库**: H2 2.2.224 (默认启用)
- **生产数据库**: MySQL (可选，通过配置切换)
- **构建工具**: Maven
- **JDK**: JDK 21

## 3. 项目结构

```
home-dash/
├── .mvn/                  # Maven wrapper 配置
├── distribution/          # 部署文件
│   ├── bin/               # 启动/停止脚本
│   ├── conf/              # 配置文件
│   ├── Dockerfile         # Docker 构建文件
│   └── pom.xml            # Distribution 模块 Maven 配置
├── doc/                   # 文档和图片资源
├── web/                   # Web 应用模块
│   ├── src/               # 源代码
│   │   ├── main/java/     # Java 源代码
│   │   ├── main/resources/ # 资源文件
│   │   └── test/          # 测试代码
│   └── pom.xml            # Web 模块 Maven 配置
├── .gitignore             # Git 忽略文件
├── README.md              # 项目文档
├── mvnw                   # Maven wrapper 脚本 (Unix)
├── mvnw.cmd               # Maven wrapper 脚本 (Windows)
└── pom.xml                # 根 Maven 配置
```

### 3.1 Web 模块结构

```
web/src/main/java/site/bitinit/pnd/web/
├── config/                # 配置类
├── controller/            # 控制器
├── dao/                   # 数据访问对象
├── entity/                # 实体类
├── exception/             # 异常处理
├── service/               # 服务层
├── util/                  # 工具类
├── Constants.java         # 常量定义
└── PndWebApplication.java # 应用入口
```

## 4. 开发规范

### 4.1 代码规范

- **代码风格检查**: 使用 CheckStyle、PMD 等代码风格检查工具，确保代码风格一致
- **命名规范**:
  - 类名：使用 PascalCase，如 `FileController`
  - 方法名：使用 camelCase，如 `getFileList`
  - 变量名：使用 camelCase，如 `fileId`
  - 常量名：使用 UPPER_SNAKE_CASE，如 `MAX_FILE_SIZE`
- **注释规范**:
  - 类注释：使用 Javadoc 格式描述类的功能和用途
  - 方法注释：使用 Javadoc 格式描述方法功能、参数、返回值和异常
  - 代码注释：复杂逻辑和关键代码添加注释，提高可读性

### 4.2 异常处理

- **统一异常处理**: 实现全局异常处理器，统一处理系统和业务异常
- **异常层次结构**:
  - 定义基础异常类 `PndException`
  - 业务异常继承 `PndException`，如 `FileNotFoundException`
  - 系统异常使用 Spring 内置异常类
- **异常信息**: 异常消息应包含详细的错误原因和上下文信息，便于问题排查

### 4.3 日志管理

- **日志框架**: 使用 SLF4J 作为日志门面，Logback 或 Log4j2 作为底层实现
- **日志级别**:
  - ERROR: 记录系统错误和异常
  - WARN: 记录警告信息
  - INFO: 记录系统运行状态和重要操作
  - DEBUG: 记录调试信息
  - TRACE: 记录详细跟踪信息
- **日志格式**: 统一日志格式，包括时间、级别、类名、方法名和日志内容
- **日志分类**: 按模块和功能划分日志，便于问题排查

### 4.4 API 设计

- **API 风格**: 使用 RESTful 规范，如 `GET /api/files` 获取文件列表
- **API 版本控制**: 在 URL 中添加版本号，如 `GET /api/v1/files`
- **API 文档**: 使用 Swagger 或类似工具生成 API 文档，便于前端开发和测试
- **请求参数**: 使用对象封装请求参数，避免方法参数过多
- **响应格式**: 统一响应格式，包括状态码、消息和数据

### 4.5 代码组织

- **分层架构**: 严格遵循 Controller → Service → DAO 分层架构
- **模块划分**: 按功能模块划分代码，如文件管理、资源管理
- **依赖注入**: 使用 Spring 依赖注入机制，减少硬编码依赖
- **代码复用**: 提取公共代码到工具类或父类，减少代码冗余

### 4.6 跨域策略（CORS）

#### 策略原则

**跨域问题由前端统一解决，后端不配置 CORS。**

#### 原因

1. **职责分离**: 跨域是浏览器安全策略，属于前端关注点，后端 API 应保持协议无关性
2. **部署灵活性**: 后端不绑定特定前端域名/端口，便于多端接入（Web、App、小程序等）
3. **安全性**: 避免后端开放 `Access-Control-Allow-Origin: *` 导致的安全风险
4. **开发便利**: 前端通过代理（Vite/Webpack devServer）统一处理开发环境跨域

#### 前端解决方案（参考）

- **开发环境**: 使用 Vite/Webpack devServer proxy 配置代理转发
- **生产环境**: 使用 Nginx 反向代理统一入口

#### 后端约束

- **禁止**在后端添加 `@CrossOrigin` 注解或 `CorsConfig` 配置类
- **禁止**在响应头中手动设置 `Access-Control-Allow-*` 字段
- 如确实需要后端支持（如第三方回调），需经架构评审后单独配置

### 4.7 数据库选型规范

#### 嵌入式数据库选型对比（JDK 21 环境）

| 对比项 | H2 2.2.224 | Apache Derby 10.17 |
|--------|------------|-------------------|
| **JDK兼容性** | ✅ JRE 11+ (含JDK 21) | ⚠️ JDK 17+ (Derby 10.17+) |
| **维护状态** | 🟢 活跃维护 | 🔴 已标记Retired(退休) |
| **最新版本** | 2.2.224 (2024) | 10.17.1.0 (2025) |
| **MySQL兼容模式** | ✅ MODE=MySQL | ❌ 不支持 |
| **Spring Boot集成** | ✅ 官方starter支持 | ⚠️ 需手动配置 |
| **社区活跃度** | 1333+项目使用 | Spark已移除依赖 |
| **性能** | 更优 | 一般 |
| **体积** | ~2MB | ~3MB |

#### 最终选型：H2 2.2.224

**选择理由**：

1. **JDK 21 完全兼容**: 官方明确支持 JRE 11 及以上版本
2. **活跃维护**: 持续更新，社区活跃
3. **MySQL 兼容模式**: `MODE=MySQL` 可模拟 MySQL 语法，降低迁移成本
4. **轻量高效**: 启动快、内存占用小、性能优秀
5. **Spring Boot 友好**: 开箱即用，无需额外配置

**Derby 淘汰原因**:

- Apache 官方已将 Derby 标记为"退休"(Retired) 状态
- Spark 等主流项目已移除 Derby 依赖
- 虽然 Derby 10.17 支持 JDK 21，但长期维护存疑

#### 配置说明

- 默认使用 H2 嵌入式数据库，数据存储在 `{project}/web/data/data/` 目录
- 通过 `pnd.useMysql=true` 配置可切换到 MySQL 生产环境
- JDBC URL 格式：`jdbc:h2:file:{dataDir}/pnd;MODE=MySQL;DB_CLOSE_DELAY=-1`

## 5. 重要规则

1. **禁止增加新的 MD 文件**: 为了保持项目文档的简洁性和一致性，禁止在项目中增加新的 Markdown 文件。

2. **必须读取 Skills 规范**: 在进行任何开发或优化工作之前，必须先读取本 Skills 规范文件，确保所有工作都符合项目规范和要求。

3. **自动化代码编辑**: 代码编辑应避免手动确认，全程自动完成，确保开发过程的高效性和一致性。

4. **轻量化原则**: 保持项目轻量化，只添加必要依赖，避免引入过多第三方库。

5. **稳定性优先**: 确保功能稳定性和可靠性，避免引入不稳定的功能和依赖。

6. **性能优化**: 关注系统性能，优化关键路径代码，提高系统响应速度。

7. **部署便利性**: 保持编译打包和部署的便捷性，确保优化后的项目仍然能够实现一站式部署。

8. **代码质量**: 定期进行代码审查，确保代码质量和可维护性。

9. **安全性**: 关注系统安全性，避免引入安全漏洞，如 SQL 注入、XSS 攻击等。

10. **兼容性**: 确保代码与指定的 JDK 和 Spring Boot 版本兼容。

11. **文档更新**: 当项目结构或功能发生变化时，及时更新相关文档，确保文档与代码保持一致。

## 6. 技术栈详细信息

### 6.1 JDK 版本

- **当前版本**: JDK 21
- **最低要求**: JDK 17 (Spring Boot 3.x 要求)
- **推荐版本**: JDK 21 LTS (长期支持版本)

### 6.2 Spring Boot 版本

- **当前版本**: 3.3.4
- **主要特性**:
  - 原生支持 JDK 21
  - 改进的 AOT 编译支持
  - 更好的容器化支持
  - 性能优化

### 6.3 数据库

- **嵌入式**: H2 2.2.224 (默认)
- **生产环境**: MySQL 8.0+ (可选)

### 6.4 构建工具

- **Maven**: 3.6+
- **Maven Wrapper**: 已包含在项目中

## 7. 开发环境设置

### 7.1 环境要求

- JDK 21
- Maven 3.6+
- IDE (IntelliJ IDEA / Eclipse)

### 7.2 构建命令

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 打包
mvn clean package

# 运行
mvn spring-boot:run
```

## 8. 注意事项

1. 所有新增代码必须符合本规范中的代码风格和命名规范
2. 所有公共类和方法必须包含完整的 Javadoc 注释
3. 所有异常必须通过全局异常处理器处理
4. 所有日志必须使用 SLF4J API
5. 所有 API 必须使用统一的响应格式
6. 禁止在代码中硬编码配置值，应使用配置文件或 @ConfigurationProperties
7. 禁止在代码中直接操作数据库，应通过 DAO 层
8. 禁止在 Service 层处理 HTTP 响应，应在 Controller 层处理
9. 禁止在代码中记录敏感信息（如密码、密钥等）
10. 所有数据库操作必须使用事务管理
