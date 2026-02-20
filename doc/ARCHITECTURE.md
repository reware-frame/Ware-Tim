# CIM 系统架构文档

## 1. 系统概览

CIM（Cross Instant Messaging）是一个分布式即时通讯系统，由五个核心模块组成，采用 Netty 处理长连接、Redis 存储路由状态、Zookeeper 实现服务发现。

## 2. 整体架构图

```
                           ┌─────────────────────────────────────────┐
                           │           External Clients               │
                           │  CIM Client 1    CIM Client 2  ...      │
                           │  (Port 8082)     (Port 8082)            │
                           └────────┬──────────────┬─────────────────┘
                                    │ HTTP /login  │ HTTP /groupRoute
                                    │ /p2pRoute    │ /onlineUser ...
                                    ▼              ▼
                        ┌───────────────────────────────┐
                        │      cim-forward-route        │
                        │      (无状态 REST 服务)         │
                        │         Port: 8083             │
                        │  可多实例部署，Nginx 负载均衡   │
                        └────────┬──────────┬───────────┘
                                 │          │
                      读写路由信息│          │读取服务器列表
                                 ▼          ▼
                        ┌────────────┐  ┌────────────┐
                        │   Redis    │  │ Zookeeper  │
                        │            │  │            │
                        │ 路由信息    │  │ 服务注册    │
                        │ 账号信息    │  │ 服务发现    │
                        │ 在线状态    │  │            │
                        └────────────┘  └─────┬──────┘
                                              │ 服务列表监听
                                              ▼
                        ┌─────────────────────────────────┐
                        │         cim-server 集群          │
                        │  Server 1       Server 2  ...   │
                        │  HTTP:8081      HTTP:8081        │
                        │  CIM:11211      CIM:11212        │
                        └────────────────┬────────────────┘
                                         │ Netty 长连接
                                         │ (Protobuf 编解码)
                                         │ 心跳保活
                        ┌────────────────▼────────────────┐
                        │          CIM Clients             │
                        │  Client 1        Client 2       │
                        └─────────────────────────────────┘
```

## 3. 核心组件说明

### 3.1 cim-forward-route（消息路由服务）

**职责：**
- 用户注册与账号管理
- 用户登录（从 Zookeeper 选择可用 cim-server，返回连接信息）
- 群聊消息路由（广播到所有在线用户的 cim-server）
- 私聊消息路由（点对点转发到目标用户的 cim-server）
- 用户下线（清除 Redis 中的路由信息和登录状态）
- 查询在线用户列表

**设计亮点：** 完全无状态，所有状态存储在 Redis，可水平扩展，配合 Nginx 实现高可用。

**关键类：**
| 类名 | 说明 |
|------|------|
| `RouteController` | REST 接口入口 |
| `AccountServiceRedisImpl` | 账号/路由数据 Redis 存储实现 |
| `UserInfoCacheServiceImpl` | 用户信息本地缓存 + Redis 存储 |
| `ServerListListener` | 监听 Zookeeper 服务列表变化 |
| `ServerCache` | 本地缓存 cim-server 列表 |

### 3.2 cim-server（IM 服务端）

**职责：**
- 维护客户端长连接（Netty NIO）
- 接收客户端消息并推送给目标用户
- 心跳检测（READER_IDLE 11s）
- 客户端离线时通知 cim-forward-route 清除路由

**关键类：**
| 类名 | 说明 |
|------|------|
| `CIMServer` | Netty 服务器启动，绑定 cim.server.port |
| `CIMServerInitializer` | Pipeline 配置（Protobuf 编解码 + 心跳） |
| `CIMServerHandle` | 消息处理逻辑（登录/心跳/掉线） |
| `SessionSocketHolder` | userId ↔ NioSocketChannel 映射管理 |
| `RegistryZK` | 启动时向 Zookeeper 注册服务节点 |
| `ServerHeartBeatHandlerImpl` | 心跳超时处理 |

**Netty Pipeline 顺序：**
```
[入站方向] ─────────────────────────────────────────────────────────>
  IdleStateHandler(11s)
    → ProtobufVarint32FrameDecoder   # 处理 TCP 粘包
    → ProtobufDecoder                # 解码为 CIMReqProtocol 对象
    → CIMServerHandle                # 业务处理
<───────────────────────────────────────────────────────── [出站方向]
  CIMServerHandle
    → ProtobufVarint32LengthFieldPrepender  # 添加帧长度头
    → ProtobufEncoder                        # 编码为字节流
```

### 3.3 cim-client（IM 客户端）

**职责：**
- 向 cim-forward-route 登录，获取 cim-server 连接信息
- 建立与 cim-server 的 Netty 长连接
- 发送群聊/私聊消息
- 接收并展示消息
- 心跳保活（WRITER_IDLE 触发）
- 断线自动重连（每 10 秒尝试一次）
- 本地异步写入聊天记录

**内置命令处理：** 命令格式为 `:xxx`，由 `InnerCommandContext` 通过策略模式分发到各 `Command` 实现类。

### 3.4 cim-common（公共模块）

提供各模块共用的类：
- `CIMRequestProto` / `CIMResponseProto`：Protobuf 生成的协议类
- `Constants.CommandType`：消息类型常量（LOGIN=0x01, PING=0x03）
- `TrieTree`：字典树，实现用户名前缀模糊搜索
- `NettyAttrUtil`：Channel Attribute 工具（存储最后心跳时间）
- `HeartBeatHandler`：心跳处理抽象接口

### 3.5 cim-zk（Zookeeper 工具模块）

提供 Zookeeper 节点管理功能，供 cim-server 启动时注册自身信息。

## 4. 数据流说明

### 4.1 用户登录流程

```
Client                  cim-forward-route          Zookeeper         Redis
  │                            │                       │                │
  │─── POST /login ───────────>│                       │                │
  │                            │─── 查询账号 ──────────────────────────>│
  │                            │<── 账号信息 ──────────────────────────-│
  │                            │─── 获取服务器列表 ────>│                │
  │                            │<── [server1, server2] │                │
  │                            │─── 负载均衡选择 server1               │
  │                            │─── 保存路由信息 ────────────────────-->│
  │                            │─── 更新登录状态 ────────────────────-->│
  │<── {ip, httpPort, cimPort} │                       │                │
  │                            │                       │                │
  │─── Netty connect(cimPort) ──────────────────────> cim-server       │
  │<── 连接成功，开始心跳保活 ──────────────────────────                 │
```

### 4.2 群聊消息流程

```
Client A            cim-forward-route         cim-server-1      Client B
  │                        │                       │                │
  │─── POST /groupRoute ──>│                       │                │
  │                        │─── 查询所有在线用户路由                  │
  │                        │─── HTTP push ─────────>│               │
  │                        │                        │─── push msg ──>│
  │<── 200 OK ─────────────│                        │               │
```

### 4.3 私聊消息流程

```
Client A            cim-forward-route         cim-server-2      Client B
  │                        │                       │                │
  │─── POST /p2pRoute ────>│                       │                │
  │    {targetUserId, msg} │                       │                │
  │                        │─── 查询 targetUserId 路由               │
  │                        │─── HTTP push ─────────────────────────>│
  │                        │                                        │─── 收到消息
  │<── 200 OK ─────────────│                                        │
```

### 4.4 客户端下线流程

```
Client              cim-forward-route              Redis
  │                        │                        │
  │─── POST /offLine ─────>│                        │
  │                        │─── Lua 脚本原子执行 ───>│
  │                        │   DEL cim-route:{uid}  │
  │                        │   SREM login-status    │
  │<── 200 OK ─────────────│<── 1 ──────────────────│
```

## 5. 数据存储设计

### Redis Key 设计

| Key 格式 | 类型 | 值格式 | 说明 |
|----------|------|--------|------|
| `cim-account:{userId}` | String | `{userName}` | 账号信息（userId → userName） |
| `{userName}` | String | `cim-account:{userId}` | 反向索引（userName → accountKey） |
| `cim-route:{userId}` | String | `{ip}:{httpPort}:{cimPort}` | 用户路由信息 |
| `login-status` | Set | `{userId}` | 在线用户集合 |

### Zookeeper 节点设计

```
/route                          # 根节点（app.zk.root 配置）
  ├── {ip}:{httpPort}:{cimPort} # cim-server 实例节点（临时节点）
  └── ...
```

## 6. 协议设计

使用 Google Protocol Buffers 进行序列化，详见 `protocol/` 目录。

### CIMReqProtocol（客户端 → 服务端）

```protobuf
message CIMReqProtocol {
  int64 requestId = 1;   // 用户 ID
  int32 type = 2;        // 消息类型：LOGIN=0x01, PING=0x03
  string reqMsg = 3;     // 消息内容（登录时为用户名）
}
```

### CIMResProtocol（服务端 → 客户端）

```protobuf
message CIMResProtocol {
  int32 type = 1;        // 消息类型：PING=0x03
  string resMsg = 2;     // 响应内容
}
```

## 7. 技术选型说明

| 技术 | 选型原因 |
|------|----------|
| Netty | 高性能 NIO 框架，成熟的心跳/粘包处理机制，ChannelHandler 线程模型清晰 |
| Protocol Buffers | 二进制序列化，比 JSON 体积小 3-10x，序列化性能更高 |
| Zookeeper | 成熟的分布式协调工具，支持节点监听、临时节点（服务宕机自动摘除） |
| Redis | 高性能 KV 存储，Set/String 原生支持路由信息的 CRUD，Pub/Sub 可扩展 |
| Spring Boot | 简化配置，Actuator 提供监控端点，便于生产运维 |

## 8. 扩展性设计

### 水平扩展 cim-server

1. 启动新的 cim-server 实例，指定唯一的 `cim.server.port`
2. 实例自动向 Zookeeper 注册
3. cim-forward-route 监听到新节点，自动纳入负载均衡池
4. 新连接请求即可路由到新实例

### 水平扩展 cim-forward-route

cim-forward-route 完全无状态，直接启动多实例，通过 Nginx 进行负载均衡：

```nginx
upstream cim_route {
    server 127.0.0.1:8083;
    server 127.0.0.1:8084;
}
```
