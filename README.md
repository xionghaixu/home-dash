![logo](deploy/image/pic.png)

# home-dash

家庭媒体中心与文件管理系统，支持文件管理、媒体播放、数据治理等功能。

## 功能特性

### 文件管理
- 文件列表、目录切换、面包屑导航
- 文件上传（支持分块上传、断点续传、秒传）
- 文件下载、重命名、删除、移动、复制
- 文件搜索与筛选

### 媒体中心
- 图片时间线浏览、相册管理
- 视频在线播放（支持倍速、画中画、键盘快捷键）
- 音频播放、专辑管理、播放列表
- 媒体元数据提取与展示

### 数据治理
- 回收站：文件恢复、批量清空
- 重复文件检测与清理
- 大文件识别与清理
- 空目录检测与清理
- 存储空间分析与可视化

### 任务中心
- 统一任务管理（系统任务、媒体任务）
- 任务状态监控、进度展示
- 失败任务重试、批量重试

## 快速开始

### 环境要求

- JDK 21+
- Node.js 20+（仅前端开发需要）

### 拉取代码

```bash
git clone https://github.com/xionghaixu/home-dash.git
cd home-dash
```

## 部署方式

### 方式一：下载发布包（推荐）

从 [Releases](https://github.com/xionghaixu/home-dash/releases) 下载部署包：
- `home-dash-x.x.x.tar.gz` - Linux/macOS
- `home-dash-x.x.x.zip` - Windows

部署包已包含前端文件，无需单独构建前端。

**Linux / macOS / WSL：**

```bash
# 解压
tar -zxvf home-dash-1.0.0.tar.gz
cd home-dash-1.0.0

# 启动
sh bin/startup.sh

# 查看日志
tail -f data/logs/home-dash.log

# 停止
sh bin/shutdown.sh
```

**Windows：**

```cmd
REM 解压 zip 文件
REM 进入目录
cd home-dash-1.0.0

REM 启动
bin\startup.bat

REM 停止
bin\shutdown.bat
```

启动后访问 http://localhost:8190

### 方式二：源码构建

```bash
# 克隆代码
git clone https://github.com/xionghaixu/home-dash.git
cd home-dash

# 构建前端
cd ../home-dash-web
npm install
npm run build
cp -r dist/* ../home-dash/src/main/resources/static/

# 打包后端
cd ../home-dash
mvn clean package -DskipTests

# 运行
java -jar target/home-dash.jar
```

**目录结构：**

```
home-dash-1.0.0/
├── bin/                    # 启动/停止脚本
│   ├── startup.sh          # Linux/macOS/WSL 启动
│   ├── startup.bat         # Windows 启动
│   ├── shutdown.sh         # Linux/macOS/WSL 停止
│   ├── shutdown.bat        # Windows 停止
│   └── docker-startup.sh   # Docker 启动
├── conf/                   # 配置文件
│   └── application.yml
├── lib/                    # JAR 包（已包含前端）
│   └── home-dash.jar
├── data/                   # 数据目录（自动创建）
│   ├── resources/          # 上传文件
│   └── logs/               # 日志文件
└── Dockerfile              # Docker 构建文件
```

### 方式二：源码构建

```bash
# 克隆代码
git clone https://github.com/xionghaixu/home-dash.git
cd home-dash

# Maven 打包
mvn clean package -DskipTests

# 运行
java -jar target/home-dash-1.0-SNAPSHOT.jar
```

## 脚本说明

| 脚本 | 用途 | 平台 |
|------|------|------|
| `startup.sh` | 后台启动服务 | Linux/macOS/WSL |
| `startup.bat` | 后台启动服务 | Windows |
| `shutdown.sh` | 停止服务 | Linux/macOS/WSL |
| `shutdown.bat` | 停止服务 | Windows |
| `docker-startup.sh` | Docker 容器启动 | Docker |

### Linux / macOS / WSL

```bash
# 启动
sh bin/startup.sh

# 查看日志
tail -f data/logs/home-dash.log

# 停止
sh bin/shutdown.sh
```

### Windows

```cmd
REM 启动
bin\startup.bat

REM 停止
bin\shutdown.bat
```

### Docker

```bash
docker run -d \
  -p 8190:8190 \
  -e USE_MYSQL=true \
  -e MYSQL_HOST=localhost \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DB_NAME=home_dash \
  -e MYSQL_USERNAME=root \
  -e MYSQL_PASSWORD=password \
  -v /path/to/data:/app/data \
  home-dash
```

## 平台兼容性

| 平台 | 脚本 | 说明 |
|------|------|------|
| Linux | `startup.sh` / `shutdown.sh` | 推荐部署环境 |
| macOS | `startup.sh` / `shutdown.sh` | 开发测试 |
| WSL/WSL2 | `startup.sh` / `shutdown.sh` | Windows 下的 Linux 环境 |
| Windows | `startup.bat` / `shutdown.bat` | 原生批处理脚本 |
| Docker | `docker-startup.sh` | 容器化部署 |
# Windows CMD 直接运行
java -jar lib/home-dash.jar --spring.config.location=conf/application.yml
```

## 配置说明

### 端口配置

默认端口 **8190**，访问地址：http://localhost:8190

修改端口：编辑 `conf/application.yml`

```yaml
server:
  port: 8190  # 修改为其他端口
```

### 数据库配置

默认使用 H2 内置数据库，数据文件位于 `./data` 目录。

如需使用 MySQL，修改 `conf/application.yml`：

```yaml
home-dash:
  useMysql: true
  mysql:
    url: jdbc:mysql://localhost:3306/home_dash
    username: root
    password: your-password
```

或通过环境变量（Docker）：

```bash
-e USE_MYSQL=true
-e MYSQL_HOST=localhost
-e MYSQL_PORT=3306
-e MYSQL_DB_NAME=home_dash
-e MYSQL_USERNAME=root
-e MYSQL_PASSWORD=password
```

### 存储路径

文件存储路径默认为 `./data/resources`，可通过配置修改：

```yaml
home-dash:
  homeDir: /path/to/storage
```

## 技术栈

- JDK 21
- Spring Boot 3.3.4
- MyBatis-Plus
- H2（默认）/ MySQL（可选）
- Maven

## 相关项目

- [home-dash-web](https://github.com/xionghaixu/home-dash-web) - 前端项目

## 许可证

MIT License
