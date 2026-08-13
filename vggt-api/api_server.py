"""
智观·古建 — VGGT 古建结构解析 API 服务
FastAPI 监听 8000 端口，接收图片并模拟 VGGT 模型进行结构推断
"""
import io
import random
import time
from pathlib import Path

from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image

app = FastAPI(title="Zhiguan VGGT API", version="1.0")

# 允许 Spring Boot 后端跨域调用
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 上传文件保存目录
UPLOAD_DIR = Path(__file__).parent / "api_uploads"
UPLOAD_DIR.mkdir(exist_ok=True)


def simulate_vggt_analysis(image_path: str) -> dict:
    """
    模拟 VGGT 模型对古建图片进行结构推断。
    在实际部署时，此处替换为真实的 VGGT 模型推理调用。
    VGGT (Visual Geometry Grounded Transformer) 可识别古建筑的
    斗栱、柱础、梁架、屋顶等关键结构元素。

    返回包含 structural_elements 的字典，每个元素包含：
    - name: 构件名称
    - bbox: 归一化边界框 [x1, y1, x2, y2]
    - confidence: 识别置信度
    - cultural_note: 文化解读（通俗易懂）
    """
    # 模拟识别耗时（VGGT 实际推理约 2-8 秒）
    time.sleep(random.uniform(1.5, 3.0))

    # 模拟 VGGT 输出 — 古建关键结构元素的定位与文化解读
    structural_elements = [
        {
            "name": "斗栱",
            "bbox": [0.15, 0.30, 0.85, 0.55],
            "confidence": round(random.uniform(0.82, 0.96), 3),
            "cultural_note": (
                "斗栱是中国古建筑最独特的构件，位于立柱与横梁之间，"
                "由斗形木块（斗）和弓形横木（栱）层层叠合而成。"
                "它不仅是结构传力的关键节点，更是古代匠人智慧的结晶——"
                "斗栱层数越多，建筑等级越高。唐宋斗栱宏大雄健，明清则趋于装饰纤巧。"
                "您图中的斗栱呈现典型的出挑造型，承载着千年的榫卯技艺。"
            ),
        },
        {
            "name": "柱础",
            "bbox": [0.08, 0.72, 0.25, 0.88],
            "confidence": round(random.uniform(0.80, 0.94), 3),
            "cultural_note": (
                "柱础是木柱底部的石质基座，宋代《营造法式》称其为'柱础'或'磉墩'。"
                "它的核心作用是隔绝地面潮气、防止木柱腐朽，同时将屋顶荷载均匀传至地基。"
                "柱础的雕饰往往反映建筑等级——皇家用莲花覆盆式，民间则多为素面鼓镜式。"
                "您图中的柱础雕刻精美，体现了匠人对细节的极致追求。"
            ),
        },
        {
            "name": "檐椽",
            "bbox": [0.05, 0.18, 0.95, 0.32],
            "confidence": round(random.uniform(0.78, 0.91), 3),
            "cultural_note": (
                "檐椽是屋顶出檐部分的椽子，排列在檐檩之上，向外挑出形成深远出檐。"
                "古人云'上栋下宇'，檐椽正是'宇'的关键构件——"
                "唐代出檐可达柱高一半，气势恢宏；宋代以后出檐逐渐缩短。"
                "出檐深远不仅能遮挡风雨，更营造出'如鸟斯革，如翚斯飞'的飞翔美感。"
            ),
        },
        {
            "name": "鸱吻",
            "bbox": [0.40, 0.02, 0.60, 0.14],
            "confidence": round(random.uniform(0.75, 0.89), 3),
            "cultural_note": (
                "鸱吻是正脊两端的兽形装饰，相传为龙之九子之一，性好吞火，"
                "故置于屋脊以镇火灾。唐代鸱吻造型简洁有力，仅以尾部上翘；"
                "明清则演变为完整的龙首吞脊形象，尾部卷曲上扬，愈显威严。"
                "这一构件既具装饰作用，也起到加固屋脊端部的结构功能。"
            ),
        },
    ]

    return {
        "success": True,
        "analysis_id": f"vggt_{int(time.time())}_{random.randint(1000, 9999)}",
        "image_info": {
            "filename": Path(image_path).name,
            "architectural_style": random.choice(["唐代殿堂", "宋代楼阁", "明清官式", "江南园林"]),
        },
        "structural_elements": structural_elements,
        "summary": (
            "经 VGGT 深度结构解析，该古建筑图像呈现出典型的中国传统木构架体系特征。"
            "从斗栱的雄大尺度与出檐的深远程度判断，建筑风格偏向唐风遗韵，"
            "体现了'墙倒屋不塌'的榫卯结构精髓。"
        ),
    }


@app.get("/health")
async def health_check():
    """健康检查端点，供 Spring Boot 后端启动时探活"""
    return {"status": "ok", "service": "Zhiguan VGGT API"}


@app.post("/v1/analyze")
async def analyze_image(image: UploadFile = File(...)):
    """
    VGGT 古建结构分析接口。
    接收 multipart/form-data 中的 image 文件，
    调用 VGGT 模型进行结构推断，返回结构元素定位与文化解读。

    Spring Boot 后端通过 RestTemplate 调用此接口：
        POST http://vggt-api:8000/v1/analyze
    """
    # 校验文件类型
    allowed_types = {"image/jpeg", "image/png", "image/webp", "image/bmp"}
    if image.content_type not in allowed_types:
        return {
            "success": False,
            "error": f"不支持的文件类型: {image.content_type}，请上传 JPEG/PNG/WebP/BMP 格式图片",
        }

    # 保存上传文件到本地
    contents = await image.read()
    file_ext = Path(image.filename).suffix if image.filename else ".jpg"
    save_path = UPLOAD_DIR / f"zhixi_{int(time.time())}_{random.randint(100, 999)}{file_ext}"
    save_path.write_bytes(contents)

    # 验证是否为有效图片
    try:
        with Image.open(save_path) as img:
            img.verify()
    except Exception:
        save_path.unlink(missing_ok=True)
        return {"success": False, "error": "无法解析图片文件，请确认上传的是有效图片"}

    # 调用 VGGT 模型进行结构分析（当前为模拟）
    result = simulate_vggt_analysis(str(save_path))

    return result


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
