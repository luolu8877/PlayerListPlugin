
# PlayerListPlugin

### 一个轻量级 Paper-1.21 插件，支持 **游戏内命令** 和 **Web API** 实时查看在线玩家及系统资源（CPU / 内存 / 磁盘 / 网络）。

---

## ✨ 功能

- **`/online`**：列出当前在线玩家及数量。
- **`/plp api port <1024-65535>`**：OP 专属命令，动态更改 Web API 端口，立即生效。
- **Web API**：`GET /api/status` 返回 JSON，包含在线玩家列表和实时系统使用率。
- **实时采样**：所有系统指标在请求时实时采样，无后台线程。
- **Fat-Jar**：包含 Spark、Gson 和 OSHI，直接运行无需额外依赖。

---

## 📥 安装

1. 从 [Releases](../../releases) 下载 `PlayerListPlugin-1.0.0.jar`。
2. 将其放入服务器的 `plugins/` 文件夹。
3. 启动或重新加载 Paper-1.21（或更高版本）。
4. 默认 Web API 地址：`http://localhost:9960/api/status`。

---

## 🕹 命令与权限

| 命令 | 权限 | 描述 |
|------|------|------|
| `/online` | 无 | 列出当前在线玩家及数量 |
| `/plp api port <port>` | `plp.admin`（默认 OP） | 动态更改 API 端口 |

---

## 🌐 API 响应示例

`GET http://your-server:9960/api/status`

```json
{
  "players": ["Steve", "Alex"],
  "count": 2,
  "system": {
    "cpu": "12.50",
    "memory": {
      "totalMB": 8192,
      "usedMB": 4096,
      "usage": "50.00"
    },
    "disk": {
      "totalGB": 256,
      "usedGB": 128,
      "usage": "50.00"
    },
    "network": {
      "rxMbps": "0.83",
      "txMbps": "1.24"
    }
  }
}
```

---

## ⚙ 配置

`plugins/PlayerListPlugin/config.yml`

```yaml
# Web API 监听端口（1024-65535）
api-port: 9960
```

通过命令更改的配置会自动保存到这里。

---

## 🔨 自行编译

```bash
git clone https://github.com/yourname/PlayerListPlugin.git
cd PlayerListPlugin
mvn clean package
# 输出：target/PlayerListPlugin-1.0.0.jar
```

需要 Java 21 和 Maven 3.9+。

---

## 📄 许可证

MIT © 2024 ApplePeo  
可以自由 fork、PR 或重新分发。