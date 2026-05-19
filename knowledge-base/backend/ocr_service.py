#!/usr/bin/env python3
"""RapidOCR 服务 - 接收 base64 图片，返回识别文本"""
import sys
import base64
import json
import tempfile
import os

def ocr_image(base64_data: str) -> str:
    """对 base64 编码的图片执行 OCR，返回识别文本"""
    from rapidocr_onnxruntime import RapidOCR
    engine = RapidOCR()
    
    # 解码 base64，去掉 data:image/...;base64, 前缀
    if ',' in base64_data:
        base64_data = base64_data.split(',', 1)[1]
    
    img_bytes = base64.b64decode(base64_data)
    
    # 写入临时文件（RapidOCR 需要文件路径或 numpy 数组）
    with tempfile.NamedTemporaryFile(suffix='.png', delete=False) as f:
        f.write(img_bytes)
        tmp_path = f.name
    
    try:
        result, _ = engine(tmp_path)
        if result:
            # result 是 list of [bbox, text, confidence]
            texts = [item[1] for item in result]
            return '\n'.join(texts)
        return ''
    finally:
        os.unlink(tmp_path)

if __name__ == '__main__':
    # 从 stdin 读取 base64 数据
    data = sys.stdin.read().strip()
    if not data:
        print(json.dumps({'error': 'No input data'}, ensure_ascii=False))
        sys.exit(1)
    
    try:
        text = ocr_image(data)
        print(json.dumps({'text': text}, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({'error': str(e)}, ensure_ascii=False))
        sys.exit(1)
