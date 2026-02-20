# CIM — Cross Instant Messaging

> 一款面向开发者的分布式即时通讯系统 | A distributed IM system for developers

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/reware-frame/Ware-Tim)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-1.5.6-green.svg)](https://spring.io/projects/spring-boot)

---

## 简介

`CIM (Cross-IM)` 是一款面向开发者的即时通讯系统，基于 **Netty + Spring Boot + Zookeeper + Redis** 构建，支持集群水平扩展。

**适用场景：**
- 即时通讯（IM）系统
- APP 消息推送中间件
- IOT 海量连接场景的消息透传

## 核心特性

| 特性 | 说明 |
|------|------|
| 群聊 / 私聊 | 支持全员广播与指定用户私信 |
| 集群部署 | cim-server 多实例，Zookeeper 自动注册发现 |
| 无状态路由 | cim-forward-route 无状态，可通过 Nginx 高可用 |
| 自动重连 | 客户端心跳检测，连接断开后自动重连 |
| 聊天记录 | 本地异步写入，支持关键字查询 |
| AI 模式 | 内置命令一键开启 AI 自动回复 |
| 前缀搜索 | 字典树实现用户名前缀模糊匹配 |
| Protocol Buffers | Google Protobuf 高效二进制编解码 |

## 系统架构

```
┌─────────────┐     HTTP/REST      ┌───────────────────┐
│  CIM Client ├───────────────────>│ cim-forward-route │
│  (Port 8082)│<───────────────────┤    (Port 8083)    │
└──────┬──────┘   Server Info      └────────┬──────────┘
       │                                    │  读写
       │ Netty 长连接                        ▼
       │                              ┌─────────────┐
       ▼                              │    Redis    │
┌─────────────┐                      └─────────────┘
│  cim-server │                            │
│  (Port 8081)│            ┌───────────────┘
│  集群部署    │            │  服务注册/发现
└─────────────┘            ▼
                     ┌─────────────┐
                     │  Zookeeper  │
                     └─────────────┘
```

> 详细架构说明见 [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md)

## 快速启动

### 方式一：Docker Compose（推荐）

```bash
git clone https://github.com/reware-frame/Ware-Tim.git
cd Ware-Tim

# 一键启动所有服务（Redis + Zookeeper + cim-server + cim-forward-route）
docker-compose up -d

# 启动客户端（注册账号后使用）
java -jar cim-client/target/cim-client-1.0.0-SNAPSHOT.jar \
  --cim.user.id=<userId> \
  --cim.user.userName=<userName>
```

### 方式二：手动启动

**前提条件：** Java 8+、Maven 3.x、Redis、Zookeeper

```bash
# 1. 构建
git clone https://github.com/reware-frame/Ware-Tim.git
cd Ware-Tim
mvn -Dmaven.test.skip=true clean package

# 2. 启动路由服务
java -jar cim-forward-route/target/cim-forward-route-1.0.0-SNAPSHOT.jar \
  --app.zk.addr=localhost:2181 \
  --spring.redis.host=localhost

# 3. 启动 IM 服务端
java -jar cim-server/target/cim-server-1.0.0-SNAPSHOT.jar \
  --app.zk.addr=localhost:2181

# 4. 注册账号
curl -X POST http://localhost:8083/registerAccount \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1001","timeStamp":0,"userName":"alice"}'

# 5. 启动客户端（使用注册返回的 userId）
java -jar cim-client/target/cim-client-1.0.0-SNAPSHOT.jar \
  --cim.user.id=<userId> --cim.user.userName=alice
```

> 完整部署文档见 [doc/DEPLOYMENT.md](doc/DEPLOYMENT.md)

## 客户端内置命令

| 命令 | 说明 |
|------|------|
| `:q!` | 退出客户端 |
| `:olu` | 获取所有在线用户列表 |
| `:all` | 显示所有命令帮助 |
| `:q <关键字>` | 查询聊天记录 |
| `:ai` | 开启 AI 模式（自动回复） |
| `:qai` | 关闭 AI 模式 |
| `:pu <前缀>` | 前缀模糊搜索用户名 |
| `:info` | 显示客户端连接信息 |

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| `cim-server` | 8081 / 11211 | Netty IM 服务端，支持集群 |
| `cim-forward-route` | 8083 | 消息路由服务，无状态 REST |
| `cim-client` | 8082 | 命令行 IM 客户端 |
| `cim-common` | — | 公共工具、协议、异常 |
| `cim-zk` | 9083 | Zookeeper 工具模块 |

## 文档

- [系统架构](doc/ARCHITECTURE.md) — 组件设计、数据流、技术选型
- [部署指南](doc/DEPLOYMENT.md) — 单机/集群/Docker 部署步骤
- [API 文档](doc/API.md) — cim-forward-route REST 接口说明
- [常见问题](doc/QA.md) — 部署和调试 FAQ
- [贡献指南](CONTRIBUTING.md) — 开发环境搭建、PR 流程

## TODO LIST

- [x] 群聊
- [x] 私聊
- [x] 内置命令
- [x] 聊天记录查询
- [x] AI 模式
- [x] Protocol Buffer 高效编解码
- [x] 水平扩容/缩容
- [x] 无状态路由（Nginx 高可用）
- [x] 服务端自动剔除离线客户端
- [x] 客户端自动重连
- [ ] 分组群聊
- [ ] Android SDK
- [ ] 离线消息
- [ ] 协议消息加密
- [ ] 更多客户端路由策略

## License

[MIT](LICENSE) © 2018 ten
