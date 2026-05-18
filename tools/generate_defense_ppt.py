from __future__ import annotations

import copy
import os
import struct
import zipfile
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple
from xml.etree import ElementTree as ET
from xml.sax.saxutils import escape


BASE_DIR = Path(r"E:\AndroidDevelopSave\OCRjizhang")
TEMPLATE_PATH = Path(r"C:\Program Files\Microsoft Office\root\Templates\2052\WidescreenPresentation.potx")
OUTPUT_PATH = BASE_DIR / "docs" / "thesis" / "小记OCR记账-本科毕业设计答辩PPT.pptx"

SCREENSHOT_DIR = BASE_DIR / "docs" / "thesis" / "screenshots"
DIAGRAM_DIR = BASE_DIR / "docs" / "thesis" / "diagrams"


NS = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "cp": "http://schemas.openxmlformats.org/package/2006/metadata/core-properties",
    "dc": "http://purl.org/dc/elements/1.1/",
    "dcterms": "http://purl.org/dc/terms/",
    "ep": "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties",
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
    "r": "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
    "rel": "http://schemas.openxmlformats.org/package/2006/relationships",
    "vt": "http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes",
    "ct": "http://schemas.openxmlformats.org/package/2006/content-types",
}

for prefix, uri in NS.items():
    ET.register_namespace(prefix if prefix != "rel" else "", uri)
ET.register_namespace("", NS["ct"])


SLIDE_W = 9144000
SLIDE_H = 5143500

BG = "FFFFFF"
PAPER = "F8FAFC"
CARD = "FFFFFF"
TEXT = "1F2937"
TEXT_SOFT = "374151"
MUTED = "6B7280"
NAVY = "1E3A8A"
BLUE = "3B82F6"
BLUE_SOFT = "DBEAFE"
AMBER = "F59E0B"
AMBER_SOFT = "FEF3C7"
SLATE_SOFT = "F3F4F6"
LINE = "D1D5DB"
WHITE = "FFFFFF"
SUCCESS = "10B981"
SUCCESS_SOFT = "D1FAE5"


def serialize_xml(element: ET.Element) -> bytes:
    return b'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' + ET.tostring(element, encoding="utf-8")


def emu_from_px(px: int, dpi: int = 96) -> int:
    return int(px * 914400 / dpi)


def read_png_size(path: Path) -> Tuple[int, int]:
    with path.open("rb") as f:
        header = f.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path} is not a PNG image")
    width, height = struct.unpack(">II", header[16:24])
    return width, height


def fit_box(src_w: int, src_h: int, box_w: int, box_h: int) -> Tuple[int, int]:
    ratio = min(box_w / src_w, box_h / src_h)
    return int(src_w * ratio), int(src_h * ratio)


def xml_shape(
    shape_id: int,
    name: str,
    x: int,
    y: int,
    w: int,
    h: int,
    *,
    fill: Optional[str] = None,
    line: Optional[str] = None,
    radius: str = "rect",
    tx: Optional[str] = None,
    font_size: int = 1800,
    color: str = TEXT,
    bold: bool = False,
    align: str = "l",
    margin_left: int = 91440,
    margin_top: int = 45720,
    margin_right: int = 91440,
    margin_bottom: int = 45720,
    no_fill: bool = False,
    font_face: str = "微软雅黑",
) -> str:
    text_body = ""
    if tx is not None:
        paragraphs = []
        for raw_line in tx.split("\n"):
            line_text = escape(raw_line)
            if line_text:
                paragraphs.append(
                    f'<a:p><a:pPr algn="{align}"/><a:r><a:rPr lang="zh-CN" sz="{font_size}"'
                    + (' b="1"' if bold else "")
                    + f'><a:solidFill><a:srgbClr val="{color}"/></a:solidFill><a:latin typeface="{font_face}"/><a:ea typeface="{font_face}"/><a:cs typeface="{font_face}"/></a:rPr><a:t>{line_text}</a:t></a:r>'
                    + f'<a:endParaRPr lang="zh-CN" sz="{font_size}"><a:solidFill><a:srgbClr val="{color}"/></a:solidFill><a:latin typeface="{font_face}"/><a:ea typeface="{font_face}"/><a:cs typeface="{font_face}"/></a:endParaRPr></a:p>'
                )
            else:
                paragraphs.append(
                    f'<a:p><a:pPr algn="{align}"/><a:endParaRPr lang="zh-CN" sz="{font_size}"><a:solidFill><a:srgbClr val="{color}"/></a:solidFill><a:latin typeface="{font_face}"/><a:ea typeface="{font_face}"/><a:cs typeface="{font_face}"/></a:endParaRPr></a:p>'
                )
        text_body = (
            "<p:txBody>"
            f'<a:bodyPr wrap="square" lIns="{margin_left}" tIns="{margin_top}" rIns="{margin_right}" bIns="{margin_bottom}"><a:spAutoFit/></a:bodyPr>'
            "<a:lstStyle/>"
            + "".join(paragraphs)
            + "</p:txBody>"
        )
    else:
        text_body = (
            "<p:txBody>"
            "<a:bodyPr/>"
            "<a:lstStyle/>"
            "<a:p><a:endParaRPr lang=\"zh-CN\"/></a:p>"
            "</p:txBody>"
        )

    if no_fill:
        fill_xml = "<a:noFill/>"
    elif fill:
        fill_xml = f'<a:solidFill><a:srgbClr val="{fill}"/></a:solidFill>'
    else:
        fill_xml = "<a:noFill/>"

    if line:
        line_xml = f'<a:ln w="12700"><a:solidFill><a:srgbClr val="{line}"/></a:solidFill></a:ln>'
    else:
        line_xml = "<a:ln><a:noFill/></a:ln>"

    return (
        "<p:sp>"
        "<p:nvSpPr>"
        f'<p:cNvPr id="{shape_id}" name="{escape(name)}"/>'
        '<p:cNvSpPr txBox="1"/>'
        "<p:nvPr/>"
        "</p:nvSpPr>"
        "<p:spPr>"
        f'<a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm>'
        f'<a:prstGeom prst="{radius}"><a:avLst/></a:prstGeom>'
        f"{fill_xml}{line_xml}"
        "</p:spPr>"
        f"{text_body}"
        "</p:sp>"
    )


def xml_picture(shape_id: int, name: str, x: int, y: int, w: int, h: int, rel_id: str) -> str:
    return (
        "<p:pic>"
        "<p:nvPicPr>"
        f'<p:cNvPr id="{shape_id}" name="{escape(name)}"/>'
        '<p:cNvPicPr><a:picLocks noChangeAspect="1" noGrp="1"/></p:cNvPicPr>'
        "<p:nvPr/>"
        "</p:nvPicPr>"
        f'<p:blipFill><a:blip r:embed="{rel_id}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>'
        "<p:spPr>"
        f'<a:xfrm><a:off x="{x}" y="{y}"/><a:ext cx="{w}" cy="{h}"/></a:xfrm>'
        '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom>'
        "</p:spPr>"
        "</p:pic>"
    )


def xml_slide(sp_tree_items: Sequence[str], bg_color: str = BG) -> str:
    return (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
        '<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" '
        'xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">'
        "<p:cSld>"
        "<p:bg>"
        f'<p:bgPr><a:solidFill><a:srgbClr val="{bg_color}"/></a:solidFill></p:bgPr>'
        "</p:bg>"
        "<p:spTree>"
        '<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>'
        f'<p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{SLIDE_W}" cy="{SLIDE_H}"/><a:chOff x="0" y="0"/><a:chExt cx="{SLIDE_W}" cy="{SLIDE_H}"/></a:xfrm></p:grpSpPr>'
        + "".join(sp_tree_items)
        + "</p:spTree>"
        "</p:cSld>"
        "<p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>"
        "</p:sld>"
    )


@dataclass
class SlideSpec:
    title: str
    shapes: List[str]
    images: List[Tuple[Path, str, int, int, int, int]] = field(default_factory=list)
    bg_color: str = BG


def title_block(title: str, subtitle: Optional[str] = None) -> List[str]:
    items = [
        xml_shape(2, "title", 457200, 274320, 8229600, 731520, tx=title, font_size=3200, color=NAVY, bold=True),
    ]
    if subtitle:
        items.append(xml_shape(3, "subtitle", 457200, 914400, 8229600, 365760, tx=subtitle, font_size=1400, color=TEXT_SOFT, bold=True))
    return items


def bullet_lines(lines: Sequence[str]) -> str:
    return "\n".join(f"• {line}" for line in lines)


def step_box(shape_id: int, x: int, y: int, w: int, h: int, title: str, body: str, *, fill: str = CARD) -> List[str]:
    return [
        xml_shape(shape_id, f"step_card_{shape_id}", x, y, w, h, fill=fill, line=BLUE, radius="roundRect"),
        xml_shape(shape_id + 1, f"step_title_{shape_id}", x + 90000, y + 70000, w - 180000, 180000,
                  tx=title, font_size=1220, color=NAVY, bold=True, align="ctr"),
        xml_shape(shape_id + 2, f"step_body_{shape_id}", x + 70000, y + 250000, w - 140000, h - 320000,
                  tx=body, font_size=980, color=TEXT_SOFT, align="ctr", margin_left=45720, margin_right=45720),
    ]


def build_slides() -> List[SlideSpec]:
    slides: List[SlideSpec] = []

    # 1 cover
    shapes = [
        xml_shape(2, "university", 457200, 731520, 8229600, 548640, tx="四川师范大学", font_size=2400, color=WHITE, align="ctr"),
        xml_shape(3, "subtitle", 457200, 1280160, 8229600, 457200, tx="本科毕业设计答辩", font_size=1800, color=BLUE, align="ctr"),
        xml_shape(4, "title", 457200, 2011680, 8229600, 1371600,
                  tx="基于 Android 的小记 OCR 记账应用\n设计与实现",
                  font_size=3600, color=WHITE, bold=True, align="ctr"),
        xml_shape(5, "info", 457200, 3840480, 8229600, 960000,
                  tx="答辩人：犹正扬    学号：2022120741\n专业：计算机科学与技术    指导教师：李贵洋\n学院：计算机科学学院    时间：2026 年 5 月",
                  font_size=1600, color=BLUE, align="ctr"),
    ]
    slides.append(SlideSpec("封面", shapes, bg_color=NAVY))

    # 2 toc
    shapes = [
        xml_shape(2, "toc_title", 457200, 274320, 8229600, 731520, tx="目录", font_size=3200, color=NAVY, bold=True),
    ]
    toc_items = [
        ("01", "研究背景与选题意义"),
        ("02", "系统设计与总体架构"),
        ("03", "核心功能实现"),
        ("04", "数据库与关键技术"),
        ("05", "系统测试与运行效果"),
        ("06", "总结与展望"),
    ]
    y = 1188720
    sid = 10
    for num, label in toc_items:
        shapes.append(xml_shape(sid, f"num_{num}", 731520, y, 731520, 457200, tx=num, font_size=2000, color=BLUE, bold=True))
        shapes.append(xml_shape(sid + 1, f"label_{num}", 1645920, y, 6400800, 457200, tx=label, font_size=1800, color=TEXT))
        y += 594360
        sid += 2
    slides.append(SlideSpec("目录", shapes))

    # 3 background
    shapes = title_block("研究背景与选题意义", "围绕生活记账场景中的高频录入痛点，设计一款更适合日常使用的移动端应用")
    stat_specs = [
        ("高频场景", "移动支付截图、小票、账单消息常常分散保存", BLUE_SOFT, BLUE),
        ("录入痛点", "传统记账需要反复切换分类、金额、账户与时间", AMBER_SOFT, AMBER),
        ("研究目标", "让记账过程更快、更直观，并保留完整分析能力", SLATE_SOFT, NAVY),
    ]
    x = 457200
    sid = 10
    for title, body, fill, border in stat_specs:
        shapes.append(xml_shape(sid, f"stat_{sid}", x, 1234440, 2468880, 822960, fill=fill, line=border, radius="roundRect"))
        shapes.append(xml_shape(sid + 1, f"stat_title_{sid}", x + 91440, 1325880, 2286000, 228600, tx=title, font_size=1200, color=NAVY, bold=True))
        shapes.append(xml_shape(sid + 2, f"stat_body_{sid}", x + 91440, 1645920, 2286000, 365760, tx=body, font_size=1000, color=TEXT_SOFT))
        x += 2743200
        sid += 3
    shapes += [
        xml_shape(30, "problem_card", 457200, 2286000, 3840480, 1828800, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(31, "problem_title", 548640, 2423160, 1737360, 274320, tx="问题背景", font_size=1500, color=NAVY, bold=True),
        xml_shape(32, "problem_body", 548640, 2788920, 3566160, 1200000,
                  tx=bullet_lines(
                      [
                          "生活记账需要兼顾快速录入、分类归档与收支回顾。",
                          "真实消费常常先以支付截图或票据图片的形式出现。",
                          "仅靠纯手动输入会拉长记账路径，影响持续使用意愿。",
                          "Android 设备具备拍照、相册和本地计算能力，适合承载 OCR 辅助录入。 ",
                      ]
                  ),
                  font_size=1150, color=TEXT),
        xml_shape(33, "meaning_card", 4754880, 2286000, 3931920, 1828800, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(34, "meaning_title", 4937760, 2423160, 1737360, 274320, tx="选题意义", font_size=1500, color=NAVY, bold=True),
        xml_shape(35, "meaning_body", 4937760, 2788920, 3474720, 1005840,
                  tx=bullet_lines(
                      [
                          "构建一条更自然的生活记账主链路，让“记账”更接近日常消费后的真实动作。",
                          "把 OCR 识别做成辅助入口，在降低输入成本的同时保留用户确认环节。",
                          "在毕业设计范围内形成 Android 客户端、数据存储、统计分析和本地演示后端的完整闭环。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
        xml_shape(36, "meaning_note", 4937760, 3794760, 3474720, 228600,
                  tx="目标并非复杂财务管理，而是面向个人生活账本的轻量化智能体验。", font_size=980, color=MUTED),
    ]
    slides.append(SlideSpec("研究背景与选题意义", shapes))

    # 4 technology
    shapes = title_block("技术选型", "开发方案以原生 Android 为核心，兼顾离线可用、演示闭环和后续扩展")
    tech_cards = [
        ("客户端", "Kotlin + MVVM + Hilt", BLUE_SOFT, BLUE),
        ("界面层", "Material Design 3 + Navigation", SLATE_SOFT, NAVY),
        ("本地存储", "Room + DataStore", BLUE_SOFT, BLUE),
        ("图表统计", "MPAndroidChart", SLATE_SOFT, NAVY),
        ("OCR 能力", "ML Kit + Paddle 本地回退", AMBER_SOFT, AMBER),
        ("演示后端", "Spring Boot + REST API", SLATE_SOFT, NAVY),
    ]
    positions = [
        (457200, 1234440), (3291840, 1234440),
        (6126480, 1234440), (457200, 2468880),
        (3291840, 2468880), (6126480, 2468880),
    ]
    sid = 40
    for (title, body, fill, border), (x, y) in zip(tech_cards, positions):
        shapes.append(xml_shape(sid, f"tech_{sid}", x, y, 2468880, 914400, fill=fill, line=border, radius="roundRect"))
        shapes.append(xml_shape(sid + 1, f"tech_title_{sid}", x + 91440, y + 91440, 2286000, 228600, tx=title, font_size=1200, color=NAVY, bold=True))
        shapes.append(xml_shape(sid + 2, f"tech_body_{sid}", x + 91440, y + 365760, 2286000, 320040, tx=body, font_size=1050, color=TEXT_SOFT))
        sid += 3
    shapes += [
        xml_shape(70, "env_card", 457200, 3749040, 8229600, 548640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(71, "env_body", 640080, 3886200, 7863840, 274320,
                  tx="开发环境：Android Studio + Gradle 8.2 + JDK 17，最低兼容版本已调整为 Android 8.0，以便真机 Pixel 3 调试与毕业设计演示。", font_size=1080, color=TEXT_SOFT, align="ctr"),
    ]
    slides.append(SlideSpec("技术选型", shapes))

    # 5 architecture
    shapes = title_block("系统总体架构", "整体采用前后端分离思路，客户端负责主要体验，后端承担本地演示同步与后台管理")
    shapes += [
        xml_shape(10, "client_block", 457200, 1234440, 2560320, 2194560, fill=BLUE_SOFT, line=BLUE, radius="roundRect"),
        xml_shape(11, "client_title", 548640, 1325880, 2377440, 365760, tx="客户端层", font_size=1400, color=NAVY, bold=True, align="ctr"),
        xml_shape(12, "client_body", 548640, 1783080, 2377440, 1280160,
                  tx="账本首页\n新增记账底部弹层\n资产账户管理\n统计图表分析\n我的页面与分类管理",
                  font_size=1100, color=TEXT_SOFT, align="ctr"),
        xml_shape(13, "business_block", 3291840, 1234440, 2560320, 2194560, fill=AMBER_SOFT, line=AMBER, radius="roundRect"),
        xml_shape(14, "business_title", 3383280, 1325880, 2377440, 365760, tx="业务与识别层", font_size=1400, color=NAVY, bold=True, align="ctr"),
        xml_shape(15, "business_body", 3383280, 1783080, 2377440, 1280160,
                  tx="ViewModel 状态管理\nRepository 业务编排\nOCR 文本识别与规则解析\n账户余额联动\n同步队列生成",
                  font_size=1100, color=TEXT_SOFT, align="ctr"),
        xml_shape(16, "data_block", 6126480, 1234440, 2560320, 2194560, fill=SLATE_SOFT, line=BLUE, radius="roundRect"),
        xml_shape(17, "data_title", 6217920, 1325880, 2377440, 365760, tx="数据与服务层", font_size=1400, color=NAVY, bold=True, align="ctr"),
        xml_shape(18, "data_body", 6217920, 1783080, 2377440, 1280160,
                  tx="Room 本地数据库\nDataStore 会话信息\nSpring Boot REST 接口\n后台演示面板\n手动拉取远端快照",
                  font_size=1100, color=TEXT_SOFT, align="ctr"),
        xml_shape(19, "arrow1", 2880360, 2194560, 274320, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(20, "arrow2", 5715000, 2194560, 274320, 91440, fill=AMBER, line=None, radius="chevron"),
        xml_shape(21, "arch_note", 457200, 3749040, 8229600, 548640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(22, "arch_note_text", 640080, 3886200, 7863840, 274320,
                  tx="客户端承担主流程和交互反馈，OCR 作为辅助录入能力嵌入新增记账链路，后端只负责本机演示同步与后台数据查看。", font_size=1080, color=TEXT_SOFT, align="ctr"),
    ]
    slides.append(SlideSpec("系统总体架构", shapes))

    # 6 ui design
    shapes = title_block("UI 设计说明", "界面遵循原生 Material Design 规范，并结合生活账本场景进行配色与层级调整")
    shapes += [
        xml_shape(10, "ui_left", 457200, 1234440, 2560320, 3017520, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "ui_left_title", 548640, 1371600, 1645920, 274320, tx="设计要点", font_size=1500, color=NAVY, bold=True),
        xml_shape(12, "ui_left_body", 548640, 1737360, 2286000, 1508760,
                  tx=bullet_lines(
                      [
                          "使用 Material 组件、FAB、卡片与 Bottom Sheet 组织高频操作。",
                          "主应用配色采用暖红与中性色，避免工具感过强的金融类视觉。",
                          "大标题、卡片信息和交互动效尽量贴近 Android 原生应用体验。",
                          "新增记账采用底部大弹层形式，保留底层账本界面，提高操作连贯性。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
        xml_shape(13, "ui_palette_title", 548640, 3429000, 1371600, 228600, tx="界面配色", font_size=1200, color=NAVY, bold=True),
        xml_shape(14, "ui_c1", 548640, 3749040, 320040, 182880, fill="B75B56", line=LINE, radius="roundRect"),
        xml_shape(15, "ui_c2", 960120, 3749040, 320040, 182880, fill="F5EFEA", line=LINE, radius="roundRect"),
        xml_shape(16, "ui_c3", 1371600, 3749040, 320040, 182880, fill="2F2623", line=LINE, radius="roundRect"),
        xml_shape(17, "ui_c4", 1783080, 3749040, 320040, 182880, fill="E7D5CE", line=LINE, radius="roundRect"),
        xml_shape(18, "ui_palette_text", 548640, 4010760, 1920240, 182880, tx="主色 / 背景 / 文本 / 软强调色", font_size=980, color=MUTED),
        xml_shape(19, "phone1", 3474720, 1234440, 1280160, 2651760, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(20, "phone2", 4937760, 1234440, 1280160, 2651760, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(21, "phone3", 6400800, 1234440, 1280160, 2651760, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(22, "phone4", 7863840, 1234440, 822960, 2651760, fill=CARD, line=LINE, radius="roundRect"),
    ]
    slides.append(
        SlideSpec(
            "UI 设计说明",
            shapes,
            [
                (SCREENSHOT_DIR / "01-home.png", "ui_home", 3566160, 1325880, 1097280, 2468880),
                (SCREENSHOT_DIR / "05-entry-bottom-sheet.png", "ui_entry", 5029200, 1325880, 1097280, 2468880),
                (SCREENSHOT_DIR / "03-statistics.png", "ui_stats", 6492240, 1325880, 1097280, 2468880),
                (SCREENSHOT_DIR / "04-profile.png", "ui_profile", 7955280, 1325880, 640080, 2468880),
            ],
        )
    )

    # 7 transaction flow
    shapes = title_block("核心功能实现：新增记账流程", "把高频记账动作压缩为一条更短的交互链路，兼顾录入效率与手动确认")
    shapes += [
        xml_shape(10, "flow_frame", 457200, 1188720, 5394960, 2926080, fill=CARD, line=LINE, radius="roundRect"),
    ]
    shapes += step_box(20, 594360, 1874520, 731520, 1097280, "步骤 1", "首页点击\nFAB", fill=SLATE_SOFT)
    shapes += step_box(30, 1463040, 1874520, 731520, 1097280, "步骤 2", "切换收入\n或支出", fill=SLATE_SOFT)
    shapes += step_box(40, 2331720, 1874520, 731520, 1097280, "步骤 3", "选择分类\n与账户", fill=SLATE_SOFT)
    shapes += step_box(50, 3200400, 1874520, 731520, 1097280, "步骤 4", "输入金额\n时间备注", fill=SLATE_SOFT)
    shapes += step_box(60, 4069080, 1874520, 731520, 1097280, "步骤 5", "写入本地\n异步同步", fill=AMBER_SOFT)
    shapes += [
        xml_shape(70, "arrow_a", 1361448, 2322576, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(71, "arrow_b", 2230128, 2322576, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(72, "arrow_c", 3098808, 2322576, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(73, "arrow_d", 3967488, 2322576, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(74, "flow_right", 5294880, 1188720, 3403600, 2926080, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(75, "flow_right_title", 5477760, 1325880, 1645920, 274320, tx="实现特点", font_size=1500, color=NAVY, bold=True),
        xml_shape(76, "flow_right_body", 5477760, 1691640, 3017520, 1097280,
                  tx=bullet_lines(
                      [
                          "新增记账不是普通跳页，而是覆盖在账本页上的底部大弹层。",
                          "账户、分类、金额与时间在同一条操作链里完成，减少来回切换。",
                          "保存成功后先关闭面板，再在后台尝试同步，避免影响前端演示。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
        xml_shape(77, "flow_right_note", 5477760, 3246120, 3017520, 365760, fill=BLUE_SOFT, line=BLUE, radius="roundRect",
                  tx="界面形态更接近原生 Android 中的 Bottom Sheet 交互语义。", font_size=1020, color=TEXT_SOFT, align="ctr"),
    ]
    slides.append(
        SlideSpec(
            "核心功能实现：新增记账流程",
            shapes,
            [(SCREENSHOT_DIR / "05-entry-bottom-sheet.png", "entry_sheet", 7010400, 1874520, 1310640, 1270000)],
        )
    )

    # 8 OCR
    shapes = title_block("核心功能实现：OCR 识别流程", "OCR 作为辅助录入入口接入账单流程，重点提升截图和票据场景下的输入效率")
    shapes += [
        xml_shape(10, "ocr_left", 457200, 1188720, 4302760, 2560320, fill=CARD, line=LINE, radius="roundRect"),
    ]
    shapes += step_box(20, 594360, 1691640, 731520, 914400, "输入图片", "拍照或从\n相册选择", fill=SLATE_SOFT)
    shapes += step_box(30, 1463040, 1691640, 731520, 914400, "文本识别", "ML Kit\n优先执行", fill=SLATE_SOFT)
    shapes += step_box(40, 2331720, 1691640, 731520, 914400, "回退识别", "Paddle\n本地补充", fill=SLATE_SOFT)
    shapes += step_box(50, 3200400, 1691640, 731520, 914400, "规则解析", "提取金额\n日期商户", fill=AMBER_SOFT)
    shapes += [
        xml_shape(60, "ocr_arrow1", 1361448, 2057400, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(61, "ocr_arrow2", 2230128, 2057400, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(62, "ocr_arrow3", 3098808, 2057400, 91440, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(63, "ocr_right", 5212560, 1188720, 3474720, 2560320, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(64, "ocr_right_title", 5395440, 1325880, 1645920, 274320, tx="识别策略", font_size=1500, color=NAVY, bold=True),
        xml_shape(65, "ocr_right_body", 5395440, 3154680, 3110000, 411480,
                  tx="先识别文本，再基于支付截图规则与票据关键词提取金额、日期和商户，最后回填到记账面板供用户确认。", font_size=1040, color=TEXT_SOFT),
    ]
    slides.append(
        SlideSpec(
            "核心功能实现：OCR 识别流程",
            shapes,
            [(SCREENSHOT_DIR / "06-ocr-page.png", "ocr_page", 5486880, 1645920, 2926080, 1371600)],
        )
    )

    # 9 asset statistics
    shapes = title_block("核心功能实现：资产与统计模块", "除了基础记账外，系统还提供资产账户与多维统计，用于形成完整的账本回顾能力")
    shapes += [
        xml_shape(10, "asset_card", 457200, 1234440, 3840480, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "asset_title", 548640, 1371600, 1463040, 274320, tx="资产账户", font_size=1500, color=NAVY, bold=True),
        xml_shape(12, "asset_body", 548640, 3337560, 3474720, 502920,
                  tx=bullet_lines(
                      [
                          "维护现金、微信、支付宝、银行卡等账户余额。",
                          "交易保存、编辑和删除时会自动联动账户金额。",
                          "账户页支持新增、修改和删除，适合作为“钱包”概念展示。 ",
                      ]
                  ),
                  font_size=1040, color=TEXT),
        xml_shape(13, "stats_card", 4754880, 1234440, 3931920, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(14, "stats_title", 4937760, 1371600, 1645920, 274320, tx="统计分析", font_size=1500, color=NAVY, bold=True),
        xml_shape(15, "stats_body", 4937760, 3337560, 3566160, 502920,
                  tx=bullet_lines(
                      [
                          "支持周、月、年、全部与范围统计。",
                          "通过柱状图、饼图与资产趋势图展示账本变化。",
                          "统计结果直接基于本地数据聚合，离线状态也能正常查看。 ",
                      ]
                  ),
                  font_size=1040, color=TEXT),
    ]
    slides.append(
        SlideSpec(
            "核心功能实现：资产与统计模块",
            shapes,
            [
                (SCREENSHOT_DIR / "02-asset.png", "asset_screen", 685800, 1737360, 2834640, 1463040),
                (SCREENSHOT_DIR / "03-statistics.png", "stats_screen", 5212080, 1737360, 2834640, 1463040),
            ],
        )
    )

    # 10 sync
    shapes = title_block("核心功能实现：同步策略", "同步策略采用本地优先模式，既保证演示稳定性，也保留前后端联动能力")
    shapes += [
        xml_shape(10, "sync_left", 457200, 1234440, 5037840, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "sync_tag1", 640080, 1508760, 1463040, 365760, fill=BLUE, line=None, radius="roundRect", tx="本地保存", font_size=1300, color=WHITE, bold=True, align="ctr"),
        xml_shape(12, "sync_tag2", 2423160, 1508760, 1463040, 365760, fill=BLUE, line=None, radius="roundRect", tx="同步队列", font_size=1300, color=WHITE, bold=True, align="ctr"),
        xml_shape(13, "sync_tag3", 4206240, 1508760, 1463040, 365760, fill=BLUE, line=None, radius="roundRect", tx="演示后端", font_size=1300, color=WHITE, bold=True, align="ctr"),
        xml_shape(14, "sync_box1", 640080, 2148840, 1463040, 868680, fill=SLATE_SOFT, line=BLUE, radius="roundRect",
                  tx="交易新增、编辑、删除\n先写入 Room\n保证前端立即成功", font_size=1060, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(15, "sync_box2", 2423160, 2148840, 1463040, 868680, fill=SLATE_SOFT, line=BLUE, radius="roundRect",
                  tx="记录待同步操作\n后台异步推送\n不阻塞界面关闭", font_size=1060, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(16, "sync_box3", 4206240, 2148840, 1463040, 868680, fill=AMBER_SOFT, line=AMBER, radius="roundRect",
                  tx="后端可查看同步结果\n后台改动需手动拉取\n保持演示过程可控", font_size=1060, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(17, "sync_arrow1", 1920240, 2550160, 182880, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(18, "sync_arrow2", 3703320, 2550160, 182880, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(19, "sync_right", 5486400, 1234440, 3200400, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(20, "sync_right_title", 5669280, 1371600, 1554480, 274320, tx="策略说明", font_size=1500, color=NAVY, bold=True),
        xml_shape(21, "sync_right_body", 5669280, 1737360, 2834640, 1508760,
                  tx=bullet_lines(
                      [
                          "自动同步只负责把本地变更尽快推送到后端，属于 best-effort 行为。",
                          "后端网页中的人工改动不会强推回前端，需要用户主动点击同步按钮拉取。",
                          "这种方案更适合毕业设计演示，不会因为后端异常影响本地操作成功率。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
    ]
    slides.append(SlideSpec("核心功能实现：同步策略", shapes))

    # 11 database
    shapes = title_block("数据库设计", "本地数据库强调离线可用，后端数据库强调规范化关联，两者通过同步 DTO 完成映射")
    shapes += [
        xml_shape(10, "db_local", 457200, 1234440, 3657600, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "db_local_tag", 594360, 1371600, 1188720, 274320, fill=BLUE_SOFT, line=None, radius="roundRect", tx="本地 Room", font_size=1200, color=NAVY, bold=True, align="ctr"),
        xml_shape(12, "db_u", 640080, 1828800, 1097280, 731520, fill=SLATE_SOFT, line=BLUE, radius="roundRect", tx="users\n账号信息\n会话绑定", font_size=980, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(13, "db_a", 2057400, 1828800, 1097280, 731520, fill=SLATE_SOFT, line=BLUE, radius="roundRect", tx="accounts\n账户名\n余额", font_size=980, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(14, "db_c", 640080, 2872740, 1097280, 731520, fill=SLATE_SOFT, line=BLUE, radius="roundRect", tx="categories\n名称\n类型图标", font_size=980, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(15, "db_t", 2057400, 2872740, 1097280, 731520, fill=AMBER_SOFT, line=AMBER, radius="roundRect", tx="transactions\n金额 时间\n账户 分类", font_size=980, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(16, "db_arrow1", 1767840, 2148840, 182880, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(17, "db_arrow2", 1767840, 3192780, 182880, 91440, fill=BLUE, line=None, radius="chevron"),
        xml_shape(18, "db_arrow3", 1234440, 2590800, 91440, 182880, fill=BLUE, line=None, radius="chevron", tx="↓", font_size=900, color=WHITE, align="ctr"),
        xml_shape(19, "db_remote", 4480560, 1234440, 4206240, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(20, "db_remote_tag", 4663440, 1371600, 1463040, 274320, fill=AMBER_SOFT, line=None, radius="roundRect", tx="后端 MySQL", font_size=1200, color=NAVY, bold=True, align="ctr"),
        xml_shape(21, "db_remote_body", 4663440, 1828800, 3840480, 1508760,
                  tx=bullet_lines(
                      [
                          "核心表采用 tb_user、tb_account、tb_category、tb_transaction 四张主表。",
                          "交易表只保留 amount、occurred_at、type、account_id、category_id 等交易相关字段。",
                          "账户与分类信息通过外键关联读取，避免在交易表中冗余账户名、分类名和用户昵称。",
                          "本地 OCR 历史与同步队列表主要服务于客户端演示，不作为后端核心业务表。 ",
                      ]
                  ),
                  font_size=1080, color=TEXT),
        xml_shape(22, "db_remote_note", 4663440, 3520440, 3840480, 274320, tx="这种拆分方式更符合关系型数据库的规范化设计，也便于论文中的 E-R 图表达。", font_size=980, color=MUTED),
    ]
    slides.append(SlideSpec("数据库设计", shapes))

    # 12 key tech
    shapes = title_block("关键技术点", "系统的完成重点不在单纯页面堆叠，而在于业务链路、状态流转与识别规则的协同")
    tech_points = [
        ("01", "MVVM 分层", "通过 ViewModel、Repository、DAO 和 Room 组织状态与数据流。", BLUE_SOFT, BLUE),
        ("02", "OCR 规则解析", "文本识别之后再结合支付截图规则提取金额、日期和商户。", AMBER_SOFT, AMBER),
        ("03", "余额联动", "交易的新增、编辑、删除都会同步影响对应账户余额。", SLATE_SOFT, BLUE),
        ("04", "同步解耦", "保存成功与后端同步解耦，避免前端操作依赖后端状态。", BLUE_SOFT, BLUE),
    ]
    positions = [(457200, 1234440), (4663440, 1234440), (457200, 2743200), (4663440, 2743200)]
    sid = 50
    for (no, head, body, fill, border), (x, y) in zip(tech_points, positions):
        shapes.append(xml_shape(sid, f"kt_{sid}", x, y, 3840480, 1280160, fill=fill, line=border, radius="roundRect"))
        shapes.append(xml_shape(sid + 1, f"kt_no_{sid}", x + 137160, y + 137160, 548640, 228600, tx=no, font_size=1100, color=NAVY, bold=True, align="ctr"))
        shapes.append(xml_shape(sid + 2, f"kt_head_{sid}", x + 137160, y + 457200, 2286000, 274320, tx=head, font_size=1450, color=NAVY, bold=True))
        shapes.append(xml_shape(sid + 3, f"kt_body_{sid}", x + 137160, y + 822960, 3474720, 274320, tx=body, font_size=1050, color=TEXT_SOFT))
        sid += 4
    slides.append(SlideSpec("关键技术点", shapes))

    # 13 test and effect
    shapes = title_block("系统测试与运行效果", "主要功能已经在真机环境完成联调，可支撑毕业设计展示与论文中的运行章节")
    shapes += [
        xml_shape(10, "run1", 457200, 1234440, 1828800, 2103120, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "run2", 2550160, 1234440, 1828800, 2103120, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(12, "run3", 4648200, 1234440, 1828800, 2103120, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(13, "run4", 6746240, 1234440, 1828800, 2103120, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(14, "cap1", 548640, 3383280, 1645920, 228600, tx="首页账本", font_size=1050, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(15, "cap2", 2641600, 3383280, 1645920, 228600, tx="资产账户", font_size=1050, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(16, "cap3", 4739640, 3383280, 1645920, 228600, tx="统计分析", font_size=1050, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(17, "cap4", 6837680, 3383280, 1645920, 228600, tx="OCR 识别", font_size=1050, color=TEXT_SOFT, bold=True, align="ctr"),
        xml_shape(18, "test_note", 457200, 3794760, 8229600, 411480, fill=SUCCESS_SOFT, line=SUCCESS, radius="roundRect",
                  tx="测试结论：登录、交易增删改查、OCR 回填、账户余额联动、统计切换与同步按钮均已通过真机演示验证。", font_size=1060, color=TEXT_SOFT, align="ctr"),
    ]
    slides.append(
        SlideSpec(
            "系统测试与运行效果",
            shapes,
            [
                (SCREENSHOT_DIR / "01-home.png", "run_home", 548640, 1325880, 1645920, 1965960),
                (SCREENSHOT_DIR / "02-asset.png", "run_asset", 2641600, 1325880, 1645920, 1965960),
                (SCREENSHOT_DIR / "03-statistics.png", "run_stats", 4739640, 1325880, 1645920, 1965960),
                (SCREENSHOT_DIR / "06-ocr-page.png", "run_ocr", 6837680, 1325880, 1645920, 1965960),
            ],
        )
    )

    # 14 highlights
    shapes = title_block("项目特色与创新点", "在毕业设计要求范围内，系统兼顾原生体验、OCR 辅助录入与演示型前后端闭环")
    highlights = [
        ("原生交互体验", "首页 FAB、卡片布局、底部大弹层和动效反馈均尽量贴近 Android 原生软件逻辑。", BLUE_SOFT, BLUE),
        ("OCR 辅助录入", "不是把识别结果直接当真值，而是作为账单预填结果，兼顾效率与可控性。", AMBER_SOFT, AMBER),
        ("演示型同步闭环", "本地优先 + 后台异步 + 手动拉取的同步方式更适合毕业设计答辩场景。", SLATE_SOFT, BLUE),
    ]
    x_positions = [457200, 3291840, 6126480]
    sid = 70
    for (head, body, fill, border), x in zip(highlights, x_positions):
        shapes.append(xml_shape(sid, f"hl_{sid}", x, 1508760, 2377440, 1828800, fill=fill, line=border, radius="roundRect"))
        shapes.append(xml_shape(sid + 1, f"hl_head_{sid}", x + 137160, 1691640, 2103120, 320040, tx=head, font_size=1450, color=NAVY, bold=True, align="ctr"))
        shapes.append(xml_shape(sid + 2, f"hl_body_{sid}", x + 137160, 2148840, 2103120, 914400, tx=body, font_size=1020, color=TEXT_SOFT))
        sid += 3
    slides.append(SlideSpec("项目特色与创新点", shapes))

    # 15 future
    shapes = title_block("不足与展望", "说明系统当前边界，并给出后续可继续深入的技术方向")
    shapes += [
        xml_shape(10, "future_left", 457200, 1234440, 3840480, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(11, "future_left_title", 548640, 1371600, 1645920, 274320, tx="当前不足", font_size=1500, color=NAVY, bold=True),
        xml_shape(12, "future_left_body", 548640, 1737360, 3474720, 1508760,
                  tx=bullet_lines(
                      [
                          "OCR 在复杂票据、遮挡图像和特殊排版场景下仍可能出现识别误差。",
                          "后端目前主要服务于本机演示，尚未延伸到正式部署与复杂安全策略。",
                          "同步冲突处理采用简化方案，没有引入更细粒度的多端协同机制。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
        xml_shape(13, "future_right", 4754880, 1234440, 3931920, 2834640, fill=CARD, line=LINE, radius="roundRect"),
        xml_shape(14, "future_right_title", 4937760, 1371600, 1645920, 274320, tx="后续展望", font_size=1500, color=NAVY, bold=True),
        xml_shape(15, "future_right_body", 4937760, 1737360, 3566160, 1508760,
                  tx=bullet_lines(
                      [
                          "继续优化 OCR 图像预处理和金额候选排序，提高更多支付截图场景的准确率。",
                          "扩展预算管理、搜索筛选、账单导出等更完整的个人账本能力。",
                          "在需要时可把演示后端替换为正式数据库和更完整的接口鉴权体系。",
                      ]
                  ),
                  font_size=1120, color=TEXT),
    ]
    slides.append(SlideSpec("不足与展望", shapes))

    # 16 thanks
    shapes = [
        xml_shape(2, "thanks", 457200, 1737360, 8229600, 640080, tx="答辩完毕", font_size=3400, color=WHITE, bold=True, align="ctr"),
        xml_shape(3, "thanks_sub", 457200, 2468880, 8229600, 365760, tx="感谢各位老师聆听，敬请批评指正", font_size=1700, color=BLUE, align="ctr"),
        xml_shape(4, "thanks_foot", 457200, 3474720, 8229600, 274320, tx="小记 OCR 记账 · 本科毕业设计答辩", font_size=1200, color=WHITE, align="ctr"),
    ]
    slides.append(SlideSpec("结束页", shapes, bg_color=NAVY))

    return slides


def build_slide_rels(layout_target: str, image_targets: Sequence[str]) -> str:
    parts = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
        f'<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="{layout_target}"/>',
    ]
    for idx, target in enumerate(image_targets, start=2):
        parts.append(
            f'<Relationship Id="rId{idx}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="{target}"/>'
        )
    parts.append("</Relationships>")
    return "".join(parts)


def update_content_types(content_xml: bytes, slide_count: int) -> bytes:
    root = ET.fromstring(content_xml)
    overrides = root.findall("{%s}Override" % NS["ct"])
    existing_parts = {ov.attrib["PartName"] for ov in overrides}
    for part in [f"/ppt/slides/slide{i}.xml" for i in range(1, slide_count + 1)]:
        if part not in existing_parts:
            ET.SubElement(
                root,
                "{%s}Override" % NS["ct"],
                PartName=part,
                ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml",
            )
    return serialize_xml(root)


def update_presentation_xml(presentation_xml: bytes, slide_rel_ids: Sequence[str]) -> bytes:
    root = ET.fromstring(presentation_xml)
    sld_id_lst = root.find("{%s}sldIdLst" % NS["p"])
    if sld_id_lst is None:
        raise RuntimeError("presentation.xml missing sldIdLst")
    for child in list(sld_id_lst):
        sld_id_lst.remove(child)
    slide_id = 256
    for rel_id in slide_rel_ids:
        ET.SubElement(
            sld_id_lst,
            "{%s}sldId" % NS["p"],
            {"id": str(slide_id), "{%s}id" % NS["r"]: rel_id},
        )
        slide_id += 1
    return serialize_xml(root)


def update_presentation_rels(rels_xml: bytes, slide_count: int) -> bytes:
    root = ET.fromstring(rels_xml)
    relationships = list(root.findall("{%s}Relationship" % NS["rel"]))
    preserved = [
        rel
        for rel in relationships
        if rel.attrib["Type"] != "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide"
    ]
    root.clear()
    for rel in preserved:
        root.append(copy.deepcopy(rel))
    insert_rel_ids = ["rId2", "rId3", "rId4", "rId5", "rId6", "rId7", "rId8", "rId9"]
    next_id = 15
    while len(insert_rel_ids) < slide_count:
        insert_rel_ids.append(f"rId{next_id}")
        next_id += 1
    for index in range(slide_count):
        ET.SubElement(
            root,
            "{%s}Relationship" % NS["rel"],
            Id=insert_rel_ids[index],
            Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide",
            Target=f"slides/slide{index + 1}.xml",
        )
    return serialize_xml(root), insert_rel_ids


def update_core_xml(core_xml: bytes) -> bytes:
    root = ET.fromstring(core_xml)
    title = root.find("{%s}title" % NS["dc"])
    if title is not None:
        title.text = "小记OCR记账本科毕业设计答辩PPT"
    creator = root.find("{%s}creator" % NS["dc"])
    if creator is not None:
        creator.text = "OpenAI Codex"
    return serialize_xml(root)


def update_app_xml(app_xml: bytes, slide_count: int) -> bytes:
    root = ET.fromstring(app_xml)
    slides = root.find("{%s}Slides" % NS["ep"])
    if slides is not None:
        slides.text = str(slide_count)
    return serialize_xml(root)


def generate_ppt() -> Path:
    slides = build_slides()
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(TEMPLATE_PATH, "r") as zin:
        file_data: Dict[str, bytes] = {name: zin.read(name) for name in zin.namelist()}

    presentation_rels_bytes, slide_rel_ids = update_presentation_rels(file_data["ppt/_rels/presentation.xml.rels"], len(slides))
    file_data["ppt/_rels/presentation.xml.rels"] = presentation_rels_bytes
    file_data["ppt/presentation.xml"] = update_presentation_xml(file_data["ppt/presentation.xml"], slide_rel_ids)
    file_data["[Content_Types].xml"] = update_content_types(file_data["[Content_Types].xml"], len(slides))
    file_data["docProps/core.xml"] = update_core_xml(file_data["docProps/core.xml"])
    file_data["docProps/app.xml"] = update_app_xml(file_data["docProps/app.xml"], len(slides))

    media_index = 100
    for slide_number, spec in enumerate(slides, start=1):
        shape_xml = list(spec.shapes)
        image_rel_targets: List[str] = []
        shape_id = 100
        for image_path, image_name, x, y, box_w, box_h in spec.images:
            src_w, src_h = read_png_size(image_path)
            fit_w, fit_h = fit_box(src_w, src_h, box_w, box_h)
            place_x = x + (box_w - fit_w) // 2
            place_y = y + (box_h - fit_h) // 2
            media_name = f"ppt/media/generated_{media_index}.png"
            media_index += 1
            file_data[media_name] = image_path.read_bytes()
            rel_target = f"../media/{Path(media_name).name}"
            image_rel_targets.append(rel_target)
            shape_xml.append(xml_picture(shape_id, image_name, place_x, place_y, fit_w, fit_h, f"rId{len(image_rel_targets) + 1}"))
            shape_id += 1

        file_data[f"ppt/slides/slide{slide_number}.xml"] = xml_slide(shape_xml, bg_color=spec.bg_color).encode("utf-8")
        file_data[f"ppt/slides/_rels/slide{slide_number}.xml.rels"] = build_slide_rels("../slideLayouts/slideLayout7.xml", image_rel_targets).encode("utf-8")

    # remove obsolete notes relationships from template slides not reused
    for name in list(file_data.keys()):
        if name.startswith("ppt/slides/slide"):
            continue

    with zipfile.ZipFile(OUTPUT_PATH, "w", compression=zipfile.ZIP_DEFLATED) as zout:
        for name, data in file_data.items():
            zout.writestr(name, data)

    return OUTPUT_PATH


if __name__ == "__main__":
    result = generate_ppt()
    print(result)
