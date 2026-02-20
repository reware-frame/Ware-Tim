# CIM 部署指南

## 环境要求

| 组件 | 最低版本 | 说明 |
|------|----------|------|
| Java | 8+ | 运行 CIM 各服务 |
| Maven | 3.3+ | 构建项目 |
| Redis | 3.0+ | 路由/账号/在线状态存储 |
| Zookeeper | 3.4+ | 服务注册与发现 |
| Docker (可选) | 19+ | 容器化部署 |

**建议内存配置：**
- cim-server：512MB+（每个长连接约占 ~40KB）
- cim-forward-route：256MB+
- Redis：512MB+（视在线用户规模调整）

---

## 方式一：Docker Compose 部署（推荐本地开发）

### 1. 克隆并构建

```bash
git clone https://github.com/reware-frame/Ware-Tim.git
cd Ware-Tim
mvn -Dmaven.test.skip=true clean package
```

### 2. 一键启动

```bash
docker-compose up -d
```

启动后服务端口：

| 服务 | 地址 |
|------|------|
| cim-forward-route | http://localhost:8083 |
| cim-server (HTTP) | http://localhost:8081 |
| cim-server (CIM)  | localhost:11211 |
| Redis | localhost:6379 |
| Zookeeper | localhost:2181 |

### 3. 注册账号并启动客户端

```bash
# 注册账号
curl -X POST http://localhost:8083/registerAccount \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1001","timeStamp":0,"userName":"alice"}'

# 响应示例：{"code":"9000","dataBody":{"userId":1547028929407,"userName":"alice"}}

# 启动客户端（替换 userId）
java -jar cim-client/target/cim-client-1.0.0-SNAPSHOT.jar \
  --cim.user.id=1547028929407 \
  --cim.user.userName=alice
```

### 4. 停止服务

```bash
docker-compose down
```

---

## 方式二：手动部署（单机）

### 步骤 1：安装依赖

**Ubuntu/Debian：**
```bash
# Redis
sudo apt-get install redis-server
sudo systemctl start redis

# Zookeeper
wget https://downloads.apache.org/zookeeper/zookeeper-3.8.3/apache-zookeeper-3.8.3-bin.tar.gz
tar -xzf apache-zookeeper-3.8.3-bin.tar.gz
cd apache-zookeeper-3.8.3-bin
cp conf/zoo_sample.cfg conf/zoo.cfg
./bin/zkServer.sh start
```

### 步骤 2：构建项目

```bash
git clone https://github.com/reware-frame/Ware-Tim.git
cd Ware-Tim
mvn -Dmaven.test.skip=true clean package
```

构建产物位于各模块 `target/` 目录下：
- `cim-server/target/cim-server-1.0.0-SNAPSHOT.jar`
- `cim-forward-route/target/cim-forward-route-1.0.0-SNAPSHOT.jar`
- `cim-client/target/cim-client-1.0.0-SNAPSHOT.jar`

### 步骤 3：启动路由服务

```bash
nohup java -jar cim-forward-route/target/cim-forward-route-1.0.0-SNAPSHOT.jar \
  --app.zk.addr=localhost:2181 \
  --spring.redis.host=localhost \
  --spring.redis.port=6379 \
  > /var/log/cim/route.log 2>&1 &
```

验证启动：`curl http://localhost:8083/onlineUser`

### 步骤 4：启动 IM 服务端

```bash
nohup java -jar cim-server/target/cim-server-1.0.0-SNAPSHOT.jar \
  --app.zk.addr=localhost:2181 \
  --cim.server.port=11211 \
  > /var/log/cim/server.log 2>&1 &
```

### 步骤 5：注册账号并启动客户端

```bash
# 注册
curl -X POST http://localhost:8083/registerAccount \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1001","timeStamp":0,"userName":"alice"}'

# 启动（至少启动两个客户端才能互发消息）
java -jar cim-client/target/cim-client-1.0.0-SNAPSHOT.jar \
  --server.port=8082 \
  --cim.user.id=<alice_userId> \
  --cim.user.userName=alice

java -jar cim-client/target/cim-client-1.0.0-SNAPSHOT.jar \
  --server.port=8084 \
  --cim.user.id=<bob_userId> \
  --cim.user.userName=bob
```

---

## 方式三：集群部署

### 架构图

```
          Nginx (负载均衡)
         /               \
cim-forward-route:8083  cim-forward-route:8084
         |               |
         +─────┬─────────+
               |
    ┌──────────+──────────┐
    ▼                     ▼
cim-server:11211     cim-server:11212
(HTTP:8081)          (HTTP:8082)
    |                     |
    +──────────┬──────────+
               ▼
           Zookeeper
```

### 步骤 1：部署多个 cim-server

```bash
# 实例 1（服务器 192.168.1.10）
nohup java -jar cim-server-1.0.0-SNAPSHOT.jar \
  --cim.server.port=11211 \
  --server.port=8081 \
  --app.zk.addr=<zk_host>:2181 \
  > server1.log 2>&1 &

# 实例 2（服务器 192.168.1.11）
nohup java -jar cim-server-1.0.0-SNAPSHOT.jar \
  --cim.server.port=11211 \
  --server.port=8081 \
  --app.zk.addr=<zk_host>:2181 \
  > server2.log 2>&1 &
```

> 每个 cim-server 实例自动向 Zookeeper 注册，cim-forward-route 自动感知新节点。

### 步骤 2：部署多个 cim-forward-route

```bash
# 实例 1
nohup java -jar cim-forward-route-1.0.0-SNAPSHOT.jar \
  --server.port=8083 \
  --app.zk.addr=<zk_host>:2181 \
  --spring.redis.host=<redis_host> \
  > route1.log 2>&1 &

# 实例 2
nohup java -jar cim-forward-route-1.0.0-SNAPSHOT.jar \
  --server.port=8084 \
  --app.zk.addr=<zk_host>:2181 \
  --spring.redis.host=<redis_host> \
  > route2.log 2>&1 &
```

### 步骤 3：配置 Nginx

```nginx
upstream cim_route {
    server 192.168.1.10:8083;
    server 192.168.1.11:8083;
    keepalive 32;
}

server {
    listen 80;
    server_name cim-route.example.com;

    location / {
        proxy_pass http://cim_route;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }
}
```

---

## 配置参数说明

### cim-server 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8081 | HTTP 管理端口 |
| `cim.server.port` | 11211 | Netty 监听端口（客户端连接此端口） |
| `app.zk.addr` | localhost:2181 | Zookeeper 地址 |
| `app.zk.root` | /route | Zookeeper 注册根节点 |
| `cim.heartbeat.time` | 30 | 心跳超时秒数（超过后断开连接） |
| `cim.clear.route.request.url` | http://localhost:8083/offLine | 下线通知 URL |

### cim-forward-route 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8083 | HTTP 端口 |
| `app.zk.addr` | localhost:2181 | Zookeeper 地址 |
| `spring.redis.host` | localhost | Redis 主机 |
| `spring.redis.port` | 6379 | Redis 端口 |
| `spring.redis.pool.max-active` | 100 | Redis 连接池最大活跃连接数 |

### cim-client 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `server.port` | 8082 | 客户端 HTTP 端口（多实例时需不同） |
| `cim.user.id` | 1545574841528 | 用户唯一 ID（注册时获取） |
| `cim.user.userName` | zhangsan | 用户名 |
| `cim.group.route.request.url` | http://localhost:8083/groupRoute | 群聊路由地址 |
| `cim.p2p.route.request.url` | http://localhost:8083/p2pRoute | 私聊路由地址 |
| `cim.server.route.request.url` | http://localhost:8083/login | 登录获取服务器地址 |
| `cim.msg.logger.path` | /opt/logs/cim/ | 聊天记录本地存储路径 |
| `cim.heartbeat.time` | 60 | 心跳检测间隔秒数 |
| `cim.reconnect.count` | 3 | 断线重连最大尝试次数 |

---

## 监控与运维

### Spring Boot Actuator 端点

各服务均启用了 Actuator（`management.security.enabled=false`）：

| 端点 | 说明 |
|------|------|
| `GET /health` | 健康检查 |
| `GET /info` | 应用信息 |
| `GET /metrics` | 性能指标 |
| `GET /channelMap` | cim-server 自定义端点：在线 Channel 数量 |

### 日志管理

所有服务使用 Logback，默认级别为 `INFO`。

调整日志级别（不重启）：
```bash
curl -X POST http://localhost:8081/loggers/com.ten.cim \
  -H 'Content-Type: application/json' \
  -d '{"configuredLevel":"DEBUG"}'
```

### 查看在线用户

```bash
curl http://localhost:8083/onlineUser
```

---

## 常见问题

见 [QA.md](QA.md)
