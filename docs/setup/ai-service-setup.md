# AI 服务配置步骤

## 1. 安装依赖

### 使用命令行
```bash
# 进入 AI 服务目录
cd /path/to/ai-service

# 安装依赖
pip install fastapi uvicorn

# 安装其他可能需要的依赖
pip install scikit-learn lightgbm numpy pandas joblib
```

## 2. 创建 AI 服务文件

### 创建 main.py 文件
```python
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import joblib
import numpy as np

app = FastAPI()

# 加载模型（如果有）
# model = joblib.load("model.joblib")

class HotspotRequest(BaseModel):
    keys: list
    frequencies: list

class FaultPredictionRequest(BaseModel):
    metrics: list

class MessageTraceRequest(BaseModel):
    trace_id: str
    logs: list

@app.get("/health")
def health_check():
    return {"status": "healthy"}

@app.post("/hotspot/predict")
def predict_hotspot(request: HotspotRequest):
    # 这里实现热点识别逻辑
    # 示例返回
    return {
        "hotspots": request.keys[:2],
        "confidence": [0.9, 0.8]
    }

@app.post("/fault/predict")
def predict_fault(request: FaultPredictionRequest):
    # 这里实现故障预测逻辑
    # 示例返回
    return {
        "is_fault": False,
        "confidence": 0.95,
        "suggestions": ["系统运行正常"]
    }

@app.post("/message/trace/analyze")
def analyze_message_trace(request: MessageTraceRequest):
    # 这里实现消息轨迹分析逻辑
    # 示例返回
    return {
        "trace_id": request.trace_id,
        "status": "completed",
        "analysis": "消息处理正常",
        "suggestions": []
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

## 3. 启动 AI 服务

### 使用命令行
```bash
# 进入 AI 服务目录
cd /path/to/ai-service

# 启动服务
uvicorn main:app --host 0.0.0.0 --port 8000

# 或者使用 Python 直接运行
python main.py
```

## 4. 验证 AI 服务

### 使用命令行
```bash
# 测试健康检查
curl http://192.168.100.102:8000/health

# 测试热点识别
curl -X POST http://192.168.100.102:8000/hotspot/predict \
  -H "Content-Type: application/json" \
  -d '{"keys": ["key1", "key2", "key3"], "frequencies": [100, 50, 20]}'

# 测试故障预测
curl -X POST http://192.168.100.102:8000/fault/predict \
  -H "Content-Type: application/json" \
  -d '{"metrics": [0.5, 0.3, 0.2]}'

# 测试消息轨迹分析
curl -X POST http://192.168.100.102:8000/message/trace/analyze \
  -H "Content-Type: application/json" \
  -d '{"trace_id": "trace-123", "logs": ["log1", "log2"]}'
```

### 使用浏览器
1. 打开浏览器
2. 访问 `http://192.168.100.102:8000/docs`
3. 可以在 Swagger UI 中测试各个 API 端点

## 5. 注意事项

- 确保虚拟机的防火墙允许 8000 端口的访问
- 确保 Python 环境已经安装
- 确保所有依赖已经正确安装
- 服务地址要与 `application-ms-middleware.yml` 中的配置一致