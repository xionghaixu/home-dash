# home-dash 后端开发规范

## 1. 文档定位

本文件是 `home-dash` 后端**唯一规范入口**，用于定义：

- 工程结构与分层约定
- 编码规范与注释规范
- 异常、日志、接口约束
- 文档维护规则

说明：`README.md` 只放项目介绍与启动方式；`OPTIMIZATION_SUGGESTIONS.md` 只放阶段规划与验收边界。

## 2. 项目基线

- 技术栈：JDK 21、Spring Boot 3.3.4、MyBatis-Plus、Maven
- 数据库：H2（默认）/ MySQL（可切换）
- API 前缀：`/v1`
- 当前阶段：阶段一已完全完成，阶段二至六为规划中

## 3. 当前项目结构（按现状）

```text
home-dash/
├── src/main/java/com/hd/
│   ├── biz/                 # 业务层（接口与实现）
│   ├── common/              # 常量、异常、全局处理、配置、工具
│   ├── controller/          # 控制器
│   ├── dao/
│   │   ├── entity/          # 实体
│   │   ├── mapper/          # Mapper（MyBatis-Plus BaseMapper）
│   │   └── service/         # 数据服务层
│   ├── model/               # DTO/VO
│   └── HomeDashApplication.java
├── src/main/resources/
├── data/
├── deploy/
└── pom.xml
```

## 4. 阶段一标准能力基线（用于开发对照）

已稳定交付：

- 文件列表、目录切换、面包屑
- 文件/文件夹创建、重命名、删除、移动、复制
- 单文件上传、分块上传、断点续传、分块合并
- MD5 预检、秒传、文件/分块完整性校验
- 传输任务查询与清理
- 系统信息统计
- 统一响应 `ResponseDto` 与全局异常处理

说明：阶段一能力不再扩展，只做基线标准化引用；新增能力统一进入后续阶段。

## 5. 编码规范（融合版）

### 5.1 代码风格

- 缩进统一为 4 个空格
- 类名使用 PascalCase
- 方法名和变量名使用 camelCase
- 常量名使用 UPPER_SNAKE_CASE
- 类引用优先使用 `import`，禁止在业务代码中直接使用全限定类名
- 统一使用 UTF-8 编码

### 5.2 框架约定

- 控制器规范：
  - 新增控制器统一继承 `BaseController`
  - 存量控制器按迭代逐步迁移，不在本次文档整理中强制改造
- 服务规范：服务实现类必须标注 `@Service`
- 数据访问规范：Mapper 接口统一继承 `BaseMapper<T>`
- 实体规范：实体类优先使用 Lombok 降低样板代码

### 5.3 分层约定

- 严格遵循 `Controller -> Biz/Service -> Dao` 分层
- `Controller` 不直接访问 `Mapper`
- `Service/Biz` 不处理 HTTP 响应对象
- 公共能力优先沉淀到 `common` 或可复用组件

## 6. 异常处理规范

- 统一使用全局异常处理器：`com.hd.common.handler.GlobalExceptionHandler`
- 业务异常使用 `BusinessException` / `HomeDashException` 体系
- 系统异常使用 `SystemException` 或框架异常统一兜底
- 对外响应必须使用统一错误码 `ErrorCode`
- 错误信息要求：
  - 对用户：可理解、可操作
  - 对日志：包含上下文，便于排查

## 7. 日志规范

- 日志门面统一为 SLF4J
- 级别顺序：`DEBUG < INFO < WARN < ERROR`
- 推荐格式：`[操作类型] 操作描述 - 参数信息`
- 约束：
  - `INFO` 记录关键业务路径
  - `WARN` 记录可恢复异常与参数问题
  - `ERROR` 记录系统异常并携带堆栈
  - 禁止输出密码、密钥、令牌等敏感信息

## 8. 接口与响应规范

- REST 风格，统一前缀 `/v1`
- 统一响应结构 `ResponseDto`
- 列表接口统一支持分页/排序/筛选扩展
- 参数校验失败返回 `400`，并给出明确字段信息
- 业务错误通过错误码表达，不使用魔法字符串

## 9. 文档注释规范

- 所有 `public` 方法必须有 JavaDoc
- 类注释必须包含：作者、创建时间、功能描述
- 方法注释必须说明：功能、参数含义、返回值
- 复杂业务分支可加简短行内注释，避免无意义注释

## 10. 文档治理与去重规则

- `README.md`：只维护项目定位与运行方式
- `SKILLS_SPEC.md`：只维护规范，不写阶段路线细节
- `OPTIMIZATION_SUGGESTIONS.md`：只维护阶段规划与验收口径
- 不在多个文档重复维护同一规范条款
- 结构、接口、阶段状态发生变化时，必须同步更新对应单一文档

## 11. 其他强制规则

- 保持依赖轻量化，避免无必要第三方库
- 以稳定性优先于“功能堆叠”
- 关键改动需具备可回滚方案
- 涉及数据库写操作时，必须考虑事务边界与幂等性
