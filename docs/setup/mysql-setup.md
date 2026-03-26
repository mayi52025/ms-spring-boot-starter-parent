# MySQL 配置步骤

## 1. 检查 MySQL 状态

### 使用命令行
```bash
# 检查 MySQL 服务状态
sudo systemctl status mysql

# 启动 MySQL 服务（如果未启动）
sudo systemctl start mysql
```

### 使用 DataGrip
1. 打开 DataGrip
2. 点击 "New Project"
3. 点击 "Add Data Source" → "MySQL"
4. 输入以下信息：
   - 主机：192.168.100.102
   - 端口：3306
   - 用户名：root
   - 密码：root
   - 数据库：ms_middleware
5. 点击 "Test Connection" 测试连接

## 2. 创建数据库和表

### 使用命令行
```bash
# 连接到 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS ms_middleware CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 使用数据库
USE ms_middleware;

# 创建消息幂等消费记录表
CREATE TABLE IF NOT EXISTS idempotent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(255) NOT NULL UNIQUE,
    consumer_group VARCHAR(255) NOT NULL,
    status TINYINT DEFAULT 0 COMMENT '0:处理中, 1:成功, 2:失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_message_id (message_id),
    INDEX idx_consumer_group (consumer_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# 创建消息轨迹记录表
CREATE TABLE IF NOT EXISTS message_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(255) NOT NULL,
    span_id VARCHAR(255) NOT NULL,
    message_id VARCHAR(255),
    producer_service VARCHAR(255),
    consumer_service VARCHAR(255),
    status TINYINT DEFAULT 0 COMMENT '0:发送中, 1:已发送, 2:已消费, 3:消费失败',
    error_msg TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_message_id (message_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# 退出 MySQL
EXIT;
```

### 使用 DataGrip
1. 连接到 MySQL 服务器
2. 在 "Database" 面板中，右键点击 "ms_middleware" 数据库
3. 选择 "New" → "Query Console"
4. 复制粘贴上述 SQL 语句
5. 点击 "Run" 执行 SQL 语句

## 3. 验证数据库和表

### 使用命令行
```bash
# 连接到 MySQL
mysql -u root -p

# 使用数据库
USE ms_middleware;

# 查看表结构
SHOW TABLES;

# 查看表结构详情
DESCRIBE idempotent_message;
DESCRIBE message_trace;

# 退出 MySQL
EXIT;
```

### 使用 DataGrip
- 在 "Database" 面板中，展开 "ms_middleware" 数据库
- 可以看到创建的表
- 右键点击表，选择 "View Table" 查看表结构

## 4. 注意事项

- 确保虚拟机的防火墙允许 3306 端口的访问
- 确保 MySQL 服务已经正常启动
- 用户名、密码和数据库名要与 `application-ms-middleware.yml` 中的配置一致