# 常见问题 (FAQ)

> 以下问题由社区反馈整理而来。

---

## 部署相关

### Q1：部署 cim-server 要不要加端口号？

`cim-server` 有两个端口：
- `server.port`：Spring Boot HTTP 管理端口（默认 8081），用于 Actuator 等
- `cim.server.port`：Netty 监听端口（默认 11211），客户端连接此端口

同一台服务器启动多个 `cim-server` 实例时，两个端口都需要唯一：

```bash
# 实例 1
java -jar cim-server.jar --server.port=8081 --cim.server.port=11211

# 实例 2
java -jar cim-server.jar --server.port=8082 --cim.server.port=11212
```

---

### Q2：部署路由服务器，ZK 和 Redis 地址需要加端口号吗？

需要按如下格式配置：

```bash
# ZK 地址格式：host:port
--app.zk.addr=192.168.1.100:2181

# Redis 地址分开配置
--spring.redis.host=192.168.1.100
--spring.redis.port=6379
```

配置不加端口时会使用 `application.properties` 中的默认值（现已改为 `localhost`）。

---

### Q3：本地启动路由服务器写 `127.0.0.1` 还是 `localhost`？

本地开发时，ZK 和 Redis 运行在本机，使用 `127.0.0.1` 或 `localhost` 均可（效果相同）。

默认的 `application.properties` 已配置为 `localhost`，本地无需额外指定。

---

### Q4：如何在本地注册账号？

启动 `cim-forward-route` 后，调用注册接口：

```bash
curl -X POST http://localhost:8083/registerAccount \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1001","timeStamp":0,"userName":"alice"}'
```

从响应的 `dataBody.userId` 获取用户 ID，启动客户端时使用：

```bash
java -jar cim-client.jar --cim.user.id=<userId> --cim.user.userName=alice
```

账号信息存储在 Redis 中，重启后不会丢失。

---

### Q5：本地如何模拟完整调试？

至少需要启动以下服务（按顺序）：

1. **Redis**（路由/账号/在线状态存储）
2. **Zookeeper**（服务注册发现）
3. **cim-forward-route**（消息路由服务）
4. **cim-server**（IM 服务端）
5. **至少两个 cim-client**（需要两个客户端才能互发消息）

推荐使用 `docker-compose up -d` 一键启动基础组件，详见 [DEPLOYMENT.md](DEPLOYMENT.md)。

---

### Q6：使用 Docker Compose 时客户端无法连接到 cim-server？

检查以下几点：

1. **网络模式**：Docker Compose 默认创建的网络与宿主机隔离，需确保客户端能访问容器端口。
2. **端口映射**：确认 `docker-compose.yml` 中已映射端口（如 `11211:11211`）。
3. **客户端配置**：`cim.server.route.request.url` 应指向宿主机 IP 而非容器内部 IP。

---

## 连接问题

### Q7：Zookeeper 连接失败怎么排查？

**症状：** 启动 cim-server 或 cim-forward-route 时日志报 `KeeperException` 或连接超时。

**排查步骤：**

```bash
# 1. 确认 ZK 服务在运行
echo ruok | nc localhost 2181
# 返回 "imok" 表示 ZK 正常

# 2. 确认端口可访问
telnet localhost 2181

# 3. 检查防火墙
sudo iptables -L | grep 2181
```

**常见原因：**
- ZK 未启动（`zkServer.sh start` 后检查状态）
- 端口被防火墙拦截
- `app.zk.addr` 配置的地址/端口不正确

---

### Q8：Redis 连接失败怎么排查？

**症状：** cim-forward-route 启动后日志报 `RedisConnectionFailureException`。

**排查步骤：**

```bash
# 1. 确认 Redis 运行
redis-cli ping
# 返回 PONG 表示正常

# 2. 确认端口可访问
telnet localhost 6379

# 3. 如果 Redis 有密码，需添加配置
--spring.redis.password=<your_password>
```

---

### Q9：客户端启动后一直显示"重连中"？

**可能原因：**

1. **cim-forward-route 未启动**：检查路由服务是否正常运行
   ```bash
   curl http://localhost:8083/onlineUser
   ```

2. **cim-server 未注册到 ZK**：检查 ZK 中的节点
   ```bash
   echo "ls /route" | zkCli.sh -server localhost:2181
   ```

3. **userId 不存在**：确认使用的是注册接口返回的有效 userId

4. **账号不匹配**：userId 与 userName 必须对应（注册时的组合）

---

### Q10：心跳超时后如何配置重连行为？

`cim-client` 的心跳和重连参数：

```properties
# 检测多少秒未收到服务端心跳后重新登录（默认 60 秒）
cim.heartbeat.time=60

# 连接失败最大重连次数（默认 3 次）
cim.reconnect.count=3
```

`cim-server` 的心跳超时参数：

```properties
# 检测多少秒未收到客户端心跳后断开连接（默认 30 秒）
cim.heartbeat.time=30
```

> 建议：客户端心跳间隔应小于服务端超时时间，避免被误判为离线。

---

## 功能使用

### Q11：如何发送私聊消息？

1. 使用命令 `:olu` 查看在线用户列表，获取目标用户的 userId
2. 输入格式：`<目标userId>;;消息内容`

例如：
```
1547028929408;;你好！这是私聊消息
```

---

### Q12：聊天记录存在哪里？如何查询？

聊天记录默认存储在 `/opt/logs/cim/<userName>/` 目录下，按日期分文件（如 `2026220.log`）。

自定义存储路径：
```bash
--cim.msg.logger.path=/my/custom/path/
```

查询命令：
```
:q <关键字>
```
例如：`:q 你好` 会高亮显示所有包含"你好"的历史消息。

---

### Q13：AI 模式如何工作？

输入 `:ai` 开启 AI 模式后，所有收到的消息都会自动由 AI 回复。输入 `:qai` 关闭。

> 注意：AI 模式依赖外部 AI 服务，需确保网络可访问对应的 AI 接口。

---

### Q14：`:pu` 命令如何使用？

前缀搜索用户名命令：

```
:pu <前缀>
```

例如：`:pu ali` 会返回所有用户名以 "ali" 开头的用户信息。

该功能基于字典树（Trie Tree）实现，时间复杂度为 O(k)（k 为前缀长度）。

---

## 集群部署

### Q15：集群部署时需要注意什么？

1. **所有 cim-server 使用同一个 Zookeeper**：确保 `app.zk.addr` 相同
2. **所有 cim-forward-route 使用同一个 Redis 和 ZK**：确保状态共享
3. **cim-server 端口唯一**：不同机器上可以相同，同机需不同
4. **网络互通**：cim-forward-route 需能访问所有 cim-server 的 HTTP 端口（推送消息）

---

### Q16：如何优雅关闭服务？

```bash
# 查找进程
ps aux | grep cim-server

# 发送 SIGTERM 信号（触发 Spring Boot 优雅关机）
kill -15 <pid>

# 或使用 Actuator（需 management.security.enabled=false）
curl -X POST http://localhost:8081/shutdown
```
