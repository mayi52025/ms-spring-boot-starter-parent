# RabbitMQ 配置步骤

## 1. 检查 RabbitMQ 状态

### 使用命令行
```bash
# 检查 RabbitMQ 服务状态
sudo systemctl status rabbitmq-server

# 启动 RabbitMQ 服务（如果未启动）
sudo systemctl start rabbitmq-server

# 启用 RabbitMQ 管理插件
sudo rabbitmq-plugins enable rabbitmq_management
```

### 使用浏览器访问管理界面
1. 打开浏览器
2. 访问 `http://192.168.100.102:15672`
3. 使用默认账号登录：`guest/guest`

## 2. 创建交换机和队列

### 使用 RabbitMQ 管理界面

**创建死信交换机：**
1. 点击 "Exchanges" → "Add a new exchange"
2. 输入以下信息：
   - Name: `ms.dead.letter.exchange`
   - Type: `direct`
   - Durability: `Durable`
   - Auto delete: `No`
   - Internal: `No`
3. 点击 "Add exchange"

**创建死信队列：**
1. 点击 "Queues" → "Add a new queue"
2. 输入以下信息：
   - Name: `ms.dead.letter.queue`
   - Durability: `Durable`
   - Auto delete: `No`
   - Exclusive: `No`
3. 点击 "Add queue"

**绑定死信交换机和队列：**
1. 点击 "Exchanges" → `ms.dead.letter.exchange`
2. 在 "Bindings" 部分，输入以下信息：
   - To queue: `ms.dead.letter.queue`
   - Routing key: `ms.dead.letter.key`
3. 点击 "Bind"

**创建业务交换机：**
1. 点击 "Exchanges" → "Add a new exchange"
2. 输入以下信息：
   - Name: `ms.business.exchange`
   - Type: `topic`
   - Durability: `Durable`
   - Auto delete: `No`
   - Internal: `No`
3. 点击 "Add exchange"

**创建业务队列：**
1. 点击 "Queues" → "Add a new queue"
2. 输入以下信息：
   - Name: `ms.business.queue`
   - Durability: `Durable`
   - Auto delete: `No`
   - Exclusive: `No`
3. 在 "Arguments" 部分，点击 "Add argument"，输入以下信息：
   - Key: `x-dead-letter-exchange`
   - Value: `ms.dead.letter.exchange`
   - Type: `string`
4. 点击 "Add argument"，输入以下信息：
   - Key: `x-dead-letter-routing-key`
   - Value: `ms.dead.letter.key`
   - Type: `string`
5. 点击 "Add queue"

**绑定业务交换机和队列：**
1. 点击 "Exchanges" → `ms.business.exchange`
2. 在 "Bindings" 部分，输入以下信息：
   - To queue: `ms.business.queue`
   - Routing key: `#`
3. 点击 "Bind"

### 使用命令行
```bash
# 创建死信交换机
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare exchange name=ms.dead.letter.exchange type=direct durable=true

# 创建死信队列
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare queue name=ms.dead.letter.queue durable=true

# 绑定死信交换机和队列
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare binding source=ms.dead.letter.exchange destination=ms.dead.letter.queue routing_key=ms.dead.letter.key

# 创建业务交换机
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare exchange name=ms.business.exchange type=topic durable=true

# 创建业务队列
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare queue name=ms.business.queue durable=true arguments='{"x-dead-letter-exchange":"ms.dead.letter.exchange","x-dead-letter-routing-key":"ms.dead.letter.key"}'

# 绑定业务交换机和队列
rabbitmqadmin -H 192.168.100.102 -u guest -p guest declare binding source=ms.business.exchange destination=ms.business.queue routing_key=#
```

## 3. 验证配置

### 使用 RabbitMQ 管理界面
- 点击 "Exchanges" 查看创建的交换机
- 点击 "Queues" 查看创建的队列
- 点击 "Bindings" 查看绑定关系

### 使用命令行
```bash
# 查看交换机
rabbitmqadmin -H 192.168.100.102 -u guest -p guest list exchanges

# 查看队列
rabbitmqadmin -H 192.168.100.102 -u guest -p guest list queues

# 查看绑定
rabbitmqadmin -H 192.168.100.102 -u guest -p guest list bindings
```

## 4. 注意事项

- 确保虚拟机的防火墙允许 5672 和 15672 端口的访问
- 确保 RabbitMQ 服务已经正常启动
- 交换机和队列的名称要与 `application-ms-middleware.yml` 中的配置一致