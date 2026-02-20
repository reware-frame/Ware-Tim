# CIM API 文档

## cim-forward-route REST 接口

**Base URL：** `http://<route-host>:8083`

**Content-Type：** `application/json`

**统一响应格式：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": {}
}
```

**响应码说明：**

| code | 说明 |
|------|------|
| 9000 | 成功 |
| 4040 | 账号不存在或账号与用户名不匹配 |
| 4041 | 用户已在线（重复登录） |
| 5000 | 用户不在线（离线状态） |

---

## 1. 注册账号

**接口：** `POST /registerAccount`

**说明：** 注册一个新用户账号，返回系统分配的唯一 userId。

**请求体：**
```json
{
  "reqNo": "1001",
  "timeStamp": 0,
  "userName": "alice"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reqNo | String | 是 | 请求流水号（幂等标识） |
| timeStamp | Long | 否 | 请求时间戳 |
| userName | String | 是 | 用户名（同名用户返回同一 userId） |

**响应示例：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": {
    "userId": 1547028929407,
    "userName": "alice"
  }
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8083/registerAccount \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1001","timeStamp":0,"userName":"alice"}'
```

---

## 2. 用户登录

**接口：** `POST /login`

**说明：** 验证用户身份，返回可用的 cim-server 连接信息（IP + 端口）。

**请求体：**
```json
{
  "reqNo": "1002",
  "timeStamp": 0,
  "userId": 1547028929407,
  "userName": "alice"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reqNo | String | 是 | 请求流水号 |
| timeStamp | Long | 否 | 请求时间戳 |
| userId | Long | 是 | 用户 ID（注册时获取） |
| userName | String | 是 | 用户名 |

**响应示例（成功）：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": {
    "ip": "127.0.0.1",
    "httpPort": 8081,
    "cimServerPort": 11211
  }
}
```

**响应示例（账号不匹配）：**
```json
{
  "code": "4040",
  "message": "账号不存在或账号与用户名不匹配",
  "reqNo": null,
  "dataBody": null
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8083/login \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1002","timeStamp":0,"userId":1547028929407,"userName":"alice"}'
```

---

## 3. 群聊消息路由

**接口：** `POST /groupRoute`

**说明：** 将消息广播给所有在线用户。cim-forward-route 查找所有在线用户的 cim-server，逐一推送消息。

**请求体：**
```json
{
  "reqNo": "1003",
  "timeStamp": 0,
  "userId": 1547028929407,
  "msg": "大家好！"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reqNo | String | 是 | 请求流水号 |
| userId | Long | 是 | 发送者 userId |
| msg | String | 是 | 消息内容 |

**响应示例：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": null
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8083/groupRoute \
  -H 'Content-Type: application/json' \
  -d '{"reqNo":"1003","timeStamp":0,"userId":1547028929407,"msg":"大家好！"}'
```

---

## 4. 私聊消息路由

**接口：** `POST /p2pRoute`

**说明：** 向指定用户发送私聊消息。如果目标用户不在线，返回错误。

**请求体：**
```json
{
  "reqNo": "1004",
  "timeStamp": 0,
  "userId": 1547028929407,
  "receiveUserId": 1547028929408,
  "msg": "你好，Bob！"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reqNo | String | 是 | 请求流水号 |
| userId | Long | 是 | 发送者 userId |
| receiveUserId | Long | 是 | 接收者 userId |
| msg | String | 是 | 消息内容 |

**响应示例（成功）：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": null
}
```

**响应示例（目标用户不在线）：**
```json
{
  "code": "5000",
  "message": "用户不在线",
  "reqNo": null,
  "dataBody": null
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8083/p2pRoute \
  -H 'Content-Type: application/json' \
  -d '{
    "reqNo":"1004",
    "timeStamp":0,
    "userId":1547028929407,
    "receiveUserId":1547028929408,
    "msg":"你好，Bob！"
  }'
```

**客户端私聊格式：** 在 CIM 客户端命令行中，私聊格式为：
```
<receiveUserId>;;消息内容
```
例如：`1547028929408;;你好，Bob！`

---

## 5. 用户下线

**接口：** `POST /offLine`

**说明：** 清除用户的路由信息和登录状态。通常由 cim-server 在检测到客户端断开时自动调用。

**请求体：**
```json
{
  "userId": 1547028929407,
  "msg": "offLine"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | 是 | 下线用户 ID |
| msg | String | 否 | 下线原因（固定值 "offLine"） |

**响应示例：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": null
}
```

**curl 示例：**
```bash
curl -X POST http://localhost:8083/offLine \
  -H 'Content-Type: application/json' \
  -d '{"userId":1547028929407,"msg":"offLine"}'
```

---

## 6. 获取在线用户列表

**接口：** `GET /onlineUser`

**说明：** 返回当前所有在线用户的信息列表。

**请求参数：** 无

**响应示例：**
```json
{
  "code": "9000",
  "message": "成功",
  "reqNo": null,
  "dataBody": [
    {
      "userId": 1547028929407,
      "userName": "alice"
    },
    {
      "userId": 1547028929408,
      "userName": "bob"
    }
  ]
}
```

**curl 示例：**
```bash
curl http://localhost:8083/onlineUser
```

---

## Swagger UI

启动 cim-forward-route 后，可通过以下地址访问交互式 API 文档：

```
http://localhost:8083/swagger-ui.html
```

cim-server Swagger：
```
http://localhost:8081/swagger-ui.html
```
