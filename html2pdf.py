#!/usr/bin/env python3
"""
HTML 转 PDF 工具
支持本地/远程 HTML 文件，完整保留 CSS 样式、图片、字体等资源。
优先使用 Playwright（Chromium 内核）实现最精准的渲染。

用法:
    python html2pdf.py input.html
    python html2pdf.py input.html -o output.pdf
    python html2pdf.py https://example.com -o example.pdf
    python html2pdf.py input1.html input2.html -o merged.pdf
    python html2pdf.py input.html --format A4 --landscape
    python html2pdf.py input.html --no-background
"""

import argparse
import os
import sys
from pathlib import Path
from typing import List, Optional


def check_playwright() -> bool:
    """检查 playwright 是否可用"""
    try:
        import playwright  # noqa: F401
        return True
    except ImportError:
        return False


def check_weasyprint() -> bool:
    """检查 weasyprint 是否可用"""
    try:
        import weasyprint  # noqa: F401
        return True
    except ImportError:
        return False


def convert_with_playwright(
    html_path: str,
    output_path: str,
    page_size: str = "A4",
    landscape: bool = False,
    margin: str = "10mm",
    background: bool = True,
    scale: float = 1.0,
    header_html: Optional[str] = None,
    footer_html: Optional[str] = None,
) -> bool:
    """使用 Playwright (Chromium) 进行转换，渲染效果最佳"""
    from playwright.sync_api import sync_playwright

    # 构建 HTML URL
    if html_path.startswith(("http://", "https://")):
        url = html_path
    else:
        abs_path = Path(html_path).resolve()
        if not abs_path.exists():
            print(f"错误: 文件不存在 - {html_path}")
            return False
        url = abs_path.as_uri()

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # 设置视口大小，影响媒体查询
        width_map = {"A3": "297mm", "A4": "210mm", "A5": "148mm", "Letter": "216mm", "Legal": "216mm"}
        height_map = {"A3": "420mm", "A4": "297mm", "A5": "210mm", "Letter": "279mm", "Legal": "356mm"}
        viewport_width = 1280
        if landscape:
            viewport_width = 1600
        page.set_viewport_size({"width": viewport_width, "height": 900})

        # 导航到页面，等待网络空闲
        page.goto(url, wait_until="networkidle", timeout=60000)

        # 额外等待字体和动态内容加载
        page.wait_for_timeout(1000)

        # 构建 PDF 参数
        pdf_options = {
            "path": output_path,
            "format": page_size,
            "landscape": landscape,
            "margin": {
                "top": margin,
                "bottom": margin,
                "left": margin,
                "right": margin,
            },
            "print_background": background,
            "scale": scale,
            "prefer_css_page_size": False,
        }

        if header_html:
            pdf_options["display_header_footer"] = True
            pdf_options["header_template"] = header_html
        if footer_html:
            pdf_options["display_header_footer"] = True
            pdf_options["footer_template"] = footer_html

        page.pdf(**pdf_options)
        browser.close()

    return True


def convert_with_weasyprint(
    html_path: str,
    output_path: str,
    page_size: str = "A4",
    landscape: bool = False,
    margin: str = "10mm",
    background: bool = True,
) -> bool:
    """使用 WeasyPrint 进行转换（纯 Python，无需浏览器）"""
    from weasyprint import HTML

    if html_path.startswith(("http://", "https://")):
        url = html_path
    else:
        abs_path = Path(html_path).resolve()
        if not abs_path.exists():
            print(f"错误: 文件不存在 - {html_path}")
            return False
        url = abs_path.as_uri()

    html = HTML(url=url)

    # 构建样式
    orientation = "landscape" if landscape else "portrait"
    size_str = f"{page_size} {orientation}" if landscape else page_size

    stylesheets = None
    if not background:
        # 如果不需要背景，添加一个覆盖背景的样式
        css_text = f"""
        @page {{
            size: {size_str};
            margin: {margin};
        }}
        * {{
            background: transparent !important;
            background-color: transparent !important;
        }}
        """
    else:
        css_text = f"""
        @page {{
            size: {size_str};
            margin: {margin};
        }}
        """

    from weasyprint import CSS
    stylesheets = [CSS(string=css_text)]

    html.write_pdf(output_path, stylesheets=stylesheets)
    return True


def merge_pdfs(pdf_paths: List[str], output_path: str) -> bool:
    """合并多个 PDF 文件"""
    try:
        from PyPDF2 import PdfMerger
    except ImportError:
        try:
            from pymupdf import open as pdf_open  # noqa: F401
        except ImportError:
            print("错误: 合并 PDF 需要 PyPDF2 或 PyMuPDF 库")
            print("  pip install PyPDF2")
            return False

    merger = PdfMerger()
    for pdf_path in pdf_paths:
        merger.append(pdf_path)
    merger.write(output_path)
    merger.close()
    return True


def main():
    parser = argparse.ArgumentParser(
        description="HTML 转 PDF 工具 - 完美保留样式、图片、字体",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  %(prog)s page.html                      # 转换单个文件
  %(prog)s page.html -o result.pdf        # 指定输出路径
  %(prog)s https://example.com -o out.pdf # 转换网页
  %(prog)s a.html b.html -o merged.pdf   # 多文件合并输出
  %(prog)s page.html --format A3 --landscape  # A3 横向
  %(prog)s page.html --margin 20mm        # 自定义边距
  %(prog)s page.html --engine weasyprint  # 指定引擎

首次使用 Playwright 引擎前，请先安装浏览器:
  pip install playwright
  playwright install chromium
        """,
    )

    parser.add_argument("input", nargs="+", help="HTML 文件路径或 URL（支持多个）")
    parser.add_argument("-o", "--output", help="输出 PDF 路径（默认与输入同名）")
    parser.add_argument(
        "--format",
        default="A4",
        choices=["A3", "A4", "A5", "Letter", "Legal"],
        help="页面大小（默认: A4）",
    )
    parser.add_argument("--landscape", action="store_true", help="横向排版")
    parser.add_argument("--margin", default="10mm", help="页边距（默认: 10mm）")
    parser.add_argument("--no-background", action="store_true", help="不打印背景色和背景图")
    parser.add_argument("--scale", type=float, default=1.0, help="缩放比例（默认: 1.0，仅 Playwright）")
    parser.add_argument(
        "--engine",
        choices=["auto", "playwright", "weasyprint"],
        default="auto",
        help="转换引擎（默认: auto 自动选择）",
    )

    args = parser.parse_args()

    # 选择引擎
    engine = args.engine
    if engine == "auto":
        if check_playwright():
            engine = "playwright"
        elif check_weasyprint():
            engine = "weasyprint"
        else:
            print("错误: 未找到可用的转换引擎，请安装其中一个：")
            print("  pip install playwright && playwright install chromium")
            print("  pip install weasyprint")
            sys.exit(1)

    print(f"使用引擎: {engine}")

    # 处理多文件输入
    input_files = args.input
    multiple = len(input_files) > 1

    temp_pdfs = []
    output_path = args.output

    for i, html_path in enumerate(input_files):
        # 确定输出路径
        if output_path and not multiple:
            cur_output = output_path
        elif output_path and multiple:
            # 多文件合并，先保存临时文件
            if i == 0 and len(input_files) > 1:
                import tempfile

                tmp_dir = tempfile.mkdtemp(prefix="html2pdf_")
            cur_output = os.path.join(tmp_dir if multiple else ".", f"temp_{i}.pdf")
            temp_pdfs.append(cur_output)
        else:
            base = Path(html_path)
            if html_path.startswith(("http://", "https://")):
                from urllib.parse import urlparse

                name = urlparse(html_path).path.strip("/").replace("/", "_") or "index"
                cur_output = f"{name}.pdf"
            else:
                cur_output = str(base.with_suffix(".pdf"))

        # 执行转换
        print(f"转换: {html_path} -> {cur_output}")

        success = False
        if engine == "playwright":
            success = convert_with_playwright(
                html_path,
                cur_output,
                page_size=args.format,
                landscape=args.landscape,
                margin=args.margin,
                background=not args.no_background,
                scale=args.scale,
            )
        elif engine == "weasyprint":
            success = convert_with_weasyprint(
                html_path,
                cur_output,
                page_size=args.format,
                landscape=args.landscape,
                margin=args.margin,
                background=not args.no_background,
            )

        if not success:
            print(f"转换失败: {html_path}")
            sys.exit(1)

        if not multiple:
            temp_pdfs.append(cur_output)

    # 多文件合并
    if multiple and output_path:
        print(f"合并 {len(temp_pdfs)} 个文件 -> {output_path}")
        if not merge_pdfs(temp_pdfs, output_path):
            sys.exit(1)
        # 清理临时文件
        for tmp in temp_pdfs:
            try:
                os.remove(tmp)
            except OSError:
                pass
        import shutil

        try:
            shutil.rmtree(tmp_dir)
        except (NameError, OSError):
            pass

    print("转换完成!")


if __name__ == "__main__":
    main()
