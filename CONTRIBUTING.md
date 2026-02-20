# 贡献指南

感谢你对 CIM 项目的关注！本文档说明如何参与贡献代码、文档或 Bug 报告。

## 目录

- [开发环境搭建](#开发环境搭建)
- [代码规范](#代码规范)
- [提交 Issue](#提交-issue)
- [提交 Pull Request](#提交-pull-request)
- [代码审查清单](#代码审查清单)
- [模块开发说明](#模块开发说明)

---

## 开发环境搭建

### 方式一：Docker Compose（推荐）

```bash
# 克隆项目
git clone https://github.com/reware-frame/Ware-Tim.git
cd Ware-Tim

# 构建项目
mvn -Dmaven.test.skip=true clean package

# 启动基础组件（Redis + Zookeeper）
docker-compose up -d redis zookeeper

# 在 IDE 中直接运行各模块的 Application 类
```

### 方式二：手动安装

**系统要求：**
- JDK 8+（推荐 OpenJDK 8 或 Oracle JDK 8）
- Maven 3.3+
- Redis 3.0+（`brew install redis` / `apt-get install redis-server`）
- Zookeeper 3.4+（下载并运行 `zkServer.sh start`）

**构建：**
```bash
mvn clean install -DskipTests
```

**运行各模块（按顺序）：**
1. 启动 Redis 和 Zookeeper
2. 运行 `cim-forward-route` 的 `RouteApplication`
3. 运行 `cim-server` 的 `CIMServerApplication`
4. 运行 `cim-client` 的 `CIMClientApplication`（至少两个实例）

---

## 代码规范

### Java 代码规范

- **包名**：全小写，遵循现有 `com.ten.cim.*` 结构
- **类名**：UpperCamelCase，如 `CIMServerHandle`
- **方法名**：lowerCamelCase，如 `loadRouteRelated`
- **常量**：全大写下划线分隔，如 `ROUTE_PREFIX`
- **日志**：使用 SLF4J，使用参数化日志（`LOGGER.info("key={}", key)`），不使用字符串拼接
- **异常**：不要吞掉异常，必须记录日志或向上抛出

### 注释规范

- 类级注释：说明职责和使用场景
- 方法注释：说明参数、返回值、异常
- 行内注释：只在逻辑不自明处添加
- 禁止提交无意义的 `// TODO` 注释，应先完成实现再提交

### 测试规范

- 所有新增的 Service 层方法必须有对应单元测试
- 测试类名格式：`<ClassName>Test`
- 测试方法名格式：`test<MethodName>_<Scenario>`
- 使用 JUnit 4（当前框架），不引入新的测试框架

---

## 提交 Issue

在提交 Issue 前，请先搜索现有 Issue 确认是否重复。

### Bug Report

使用项目提供的 Bug Report 模板（`.github/ISSUE_TEMPLATE/bug_report.md`），填写：

- **CIM 版本**：`1.0.0-SNAPSHOT` 或具体 commit hash
- **操作系统和 Java 版本**
- **问题描述**：清晰描述预期行为和实际行为
- **复现步骤**：提供最小化复现步骤
- **日志输出**：粘贴相关错误日志（注意删除敏感信息）

### Feature Request

描述：
- 使用场景（我在 XXX 场景需要 XXX 功能）
- 期望行为
- 可能的实现思路（可选）

---

## 提交 Pull Request

### 工作流程

1. **Fork** 项目到你的账号
2. **创建分支**：以功能或 Bug 命名分支
   ```bash
   git checkout -b feature/offline-message
   # 或
   git checkout -b fix/redis-atomic-offline
   ```
3. **开发并提交**：遵循 Commit Message 规范（见下）
4. **推送分支**：`git push origin feature/offline-message`
5. **创建 PR**：向 `master` 分支提交 Pull Request

### Commit Message 规范

格式：`<type>(<scope>): <subject>`

| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `refactor` | 重构（不影响功能） |
| `test` | 测试相关 |
| `chore` | 构建/依赖/工具更新 |
| `perf` | 性能优化 |

**示例：**
```
feat(client): 实现断线重连成功后关闭定时任务
fix(route): 修复下线操作非原子性导致的数据不一致
docs(readme): 添加 Docker Compose 快速启动说明
test(server): 补充 SessionSocketHolder 单元测试
```

### PR 描述模板

```markdown
## 改动说明

简要描述本次 PR 的改动内容和原因。

## 改动范围

- [ ] cim-server
- [ ] cim-client
- [ ] cim-forward-route
- [ ] cim-common
- [ ] 文档

## 测试情况

- [ ] 添加了单元测试
- [ ] 所有现有测试通过（`mvn test`）
- [ ] 手动验证过功能

## 关联 Issue

Closes #<issue_number>
```

---

## 代码审查清单

在提交 PR 前，请自检以下项目：

**功能正确性：**
- [ ] 功能实现符合 Issue 描述
- [ ] 边界条件已处理（null 检查、空集合等）
- [ ] 异常情况有日志记录

**代码质量：**
- [ ] 没有硬编码的 IP 地址、密码等敏感信息
- [ ] 没有无意义的 TODO 注释
- [ ] 没有注释掉的死代码
- [ ] 日志使用参数化格式（不拼接字符串）

**安全性：**
- [ ] Redis 操作在需要原子性时使用 Lua 脚本或事务
- [ ] 没有 SQL 注入、命令注入等安全漏洞
- [ ] HTTP 接口参数有基本校验

**测试：**
- [ ] 新增代码有对应测试
- [ ] 现有测试未被破坏

---

## 模块开发说明

### 添加新的客户端命令

1. 在 `cim-client/.../command/` 包下创建新的 `Command` 实现类
2. 实现 `InnerCommand` 接口的 `process(String msg)` 方法
3. 在 `InnerCommandContext` 中注册命令（`commandContext.put(":newcmd", new NewCommand())`）
4. 在 `README.md` 和 `doc/QA.md` 中更新命令文档

### 添加新的路由服务接口

1. 在 `cim-forward-route/.../controller/RouteController` 中添加 `@RequestMapping`
2. 在对应 Service 接口和 `AccountServiceRedisImpl` 实现类中添加业务逻辑
3. 在 `doc/API.md` 中补充接口文档
4. 添加对应的 Controller 集成测试

### 修改 Protobuf 协议

1. 修改 `protocol/` 目录下的 `.proto` 文件
2. 重新生成 Java 代码（使用 `protoc` 工具）
3. 将生成的 Java 文件放入 `cim-common/src/main/java/` 对应包下
4. 更新所有使用该协议的代码

---

## 联系

- 提交 Issue：https://github.com/reware-frame/Ware-Tim/issues
- 原作者邮箱：ten@gmail.com
