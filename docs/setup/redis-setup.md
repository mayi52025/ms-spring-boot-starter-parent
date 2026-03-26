# Redis 配置步骤

## 1. 检查 Redis 状态

### 使用命令行
```bash
# 连接到 Redis
redis-cli -h 192.168.100.102 -p 6379

# 检查是否设置了密码
AUTH 1234

# 查看 Redis 信息
INFO
```

### 使用 Another Redis Desktop Manager
1. 打开 Another Redis Desktop Manager
2. 点击 "连接到 Redis 服务器"
3. 输入以下信息：
   - 名称：MS Middleware Redis
   - 主机：192.168.100.102
   - 端口：6379
   - 密码：1234
   - 数据库：0
4. 点击 "连接"

## 2. 配置 Redis 密码（如果需要）

### 编辑 Redis 配置文件
```bash
# 编辑 Redis 配置文件
sudo vim /etc/redis/redis.conf

# 找到并修改以下配置
requirepass 1234

# 重启 Redis 服务
sudo systemctl restart redis
```

## 3. 验证连接

### 使用命令行
```bash
redis-cli -h 192.168.100.102 -p 6379 -a 1234 ping
# 应该返回: PONG
```

### 使用 Another Redis Desktop Manager
- 连接成功后，会显示 Redis 服务器的信息
- 可以在 "控制台" 标签页执行 Redis 命令

## 4. 配置持久化（可选）

### 编辑 Redis 配置文件
```bash
# 编辑 Redis 配置文件
sudo vim /etc/redis/redis.conf

# 找到并修改以下配置
# 开启 RDB 持久化
save 900 1
save 300 10
save 60 10000

# 开启 AOF 持久化
appendonly yes
appendfsync everysec

# 重启 Redis 服务
sudo systemctl restart redis
```

## 5. 注意事项

- 确保虚拟机的防火墙允许 6379 端口的访问
- 确保 Redis 服务已经正常启动
- 密码设置要与 `application-ms-middleware.yml` 中的配置一致