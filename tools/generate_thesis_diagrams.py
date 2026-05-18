from pathlib import Path
from typing import Iterable, Optional, Tuple

from PIL import Image, ImageDraw, ImageFont


OUTPUT_DIR = Path(r"E:\AndroidDevelopSave\OCRjizhang\docs\thesis\diagrams")
FONT_PATH = r"C:\Windows\Fonts\msyh.ttc"
TITLE_FONT = ImageFont.truetype(FONT_PATH, 30)
TEXT_FONT = ImageFont.truetype(FONT_PATH, 22)
SMALL_FONT = ImageFont.truetype(FONT_PATH, 18)

BG = "#FFFFFF"
LINE = "#5F6368"
TEXT = "#202124"
BOX = "#FFF7E9"
BOX_2 = "#FCE8E6"
BOX_3 = "#E8F0FE"
ACCENT = "#C75C3B"
ACCENT_2 = "#8C4A2F"
MUTED = "#F6F3EF"


def create_canvas(size: Tuple[int, int], title: str) -> Tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", size, BG)
    draw = ImageDraw.Draw(image)
    draw.text((40, 28), title, fill=TEXT, font=TITLE_FONT)
    draw.line((40, 74, size[0] - 40, 74), fill=ACCENT, width=3)
    return image, draw


def draw_box(
    draw: ImageDraw.ImageDraw,
    box: Tuple[int, int, int, int],
    text: str,
    fill: str = BOX,
    outline: str = LINE,
    radius: int = 18,
    font: ImageFont.FreeTypeFont = TEXT_FONT,
) -> None:
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=3)
    x1, y1, x2, y2 = box
    lines = wrap_text(draw, text, font, x2 - x1 - 26)
    line_height = font.size + 8
    total_height = len(lines) * line_height
    current_y = y1 + (y2 - y1 - total_height) / 2
    for line in lines:
        width = draw.textbbox((0, 0), line, font=font)[2]
        draw.text((x1 + (x2 - x1 - width) / 2, current_y), line, fill=TEXT, font=font)
        current_y += line_height


def wrap_text(
    draw: ImageDraw.ImageDraw,
    text: str,
    font: ImageFont.FreeTypeFont,
    max_width: int,
) -> Iterable[str]:
    parts = []
    for raw_line in text.split("\n"):
        current = ""
        for ch in raw_line:
            trial = current + ch
            width = draw.textbbox((0, 0), trial, font=font)[2]
            if width <= max_width or not current:
                current = trial
            else:
                parts.append(current)
                current = ch
        parts.append(current or " ")
    return parts


def draw_arrow(
    draw: ImageDraw.ImageDraw,
    start: Tuple[int, int],
    end: Tuple[int, int],
    color: str = ACCENT_2,
    width: int = 4,
) -> None:
    draw.line((start, end), fill=color, width=width)
    arrow_size = 12
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    if abs(dx) > abs(dy):
        direction = 1 if dx >= 0 else -1
        p1 = (end[0] - direction * arrow_size, end[1] - arrow_size // 2)
        p2 = (end[0] - direction * arrow_size, end[1] + arrow_size // 2)
    else:
        direction = 1 if dy >= 0 else -1
        p1 = (end[0] - arrow_size // 2, end[1] - direction * arrow_size)
        p2 = (end[0] + arrow_size // 2, end[1] - direction * arrow_size)
    draw.polygon([end, p1, p2], fill=color)


def draw_actor(draw: ImageDraw.ImageDraw, center: Tuple[int, int], label: str) -> None:
    cx, cy = center
    draw.ellipse((cx - 20, cy - 70, cx + 20, cy - 30), outline=LINE, width=3)
    draw.line((cx, cy - 30, cx, cy + 25), fill=LINE, width=3)
    draw.line((cx - 30, cy - 5, cx + 30, cy - 5), fill=LINE, width=3)
    draw.line((cx, cy + 25, cx - 28, cy + 65), fill=LINE, width=3)
    draw.line((cx, cy + 25, cx + 28, cy + 65), fill=LINE, width=3)
    width = draw.textbbox((0, 0), label, font=TEXT_FONT)[2]
    draw.text((cx - width / 2, cy + 78), label, fill=TEXT, font=TEXT_FONT)


def draw_ellipse(draw: ImageDraw.ImageDraw, box: Tuple[int, int, int, int], text: str, fill: str = MUTED) -> None:
    draw.ellipse(box, outline=LINE, width=3, fill=fill)
    x1, y1, x2, y2 = box
    lines = wrap_text(draw, text, TEXT_FONT, x2 - x1 - 24)
    line_height = TEXT_FONT.size + 6
    total_height = len(lines) * line_height
    y = y1 + (y2 - y1 - total_height) / 2
    for line in lines:
        width = draw.textbbox((0, 0), line, font=TEXT_FONT)[2]
        draw.text((x1 + (x2 - x1 - width) / 2, y), line, fill=TEXT, font=TEXT_FONT)
        y += line_height


def draw_diamond(
    draw: ImageDraw.ImageDraw,
    box: Tuple[int, int, int, int],
    text: str,
    fill: str = "#FFF2E0",
    outline: str = LINE,
) -> None:
    x1, y1, x2, y2 = box
    points = [
        ((x1 + x2) // 2, y1),
        (x2, (y1 + y2) // 2),
        ((x1 + x2) // 2, y2),
        (x1, (y1 + y2) // 2),
    ]
    draw.polygon(points, fill=fill, outline=outline)
    lines = wrap_text(draw, text, TEXT_FONT, x2 - x1 - 40)
    line_height = TEXT_FONT.size + 6
    total_height = len(lines) * line_height
    y = y1 + (y2 - y1 - total_height) / 2
    for line in lines:
        width = draw.textbbox((0, 0), line, font=TEXT_FONT)[2]
        draw.text((x1 + (x2 - x1 - width) / 2, y), line, fill=TEXT, font=TEXT_FONT)
        y += line_height


def draw_er_entity(
    draw: ImageDraw.ImageDraw,
    box: Tuple[int, int, int, int],
    text: str,
    fill: str = BOX,
) -> None:
    x1, y1, x2, y2 = box
    draw.rectangle(box, outline=LINE, width=3, fill=fill)
    width = draw.textbbox((0, 0), text, font=TEXT_FONT)[2]
    height = TEXT_FONT.size
    draw.text((x1 + (x2 - x1 - width) / 2, y1 + (y2 - y1 - height) / 2 - 3), text, fill=TEXT, font=TEXT_FONT)


def draw_er_attribute(
    draw: ImageDraw.ImageDraw,
    box: Tuple[int, int, int, int],
    text: str,
    fill: str = BG,
) -> None:
    draw.ellipse(box, outline=LINE, width=2, fill=fill)
    x1, y1, x2, y2 = box
    lines = wrap_text(draw, text, SMALL_FONT, x2 - x1 - 20)
    line_height = SMALL_FONT.size + 4
    total_height = len(lines) * line_height
    y = y1 + (y2 - y1 - total_height) / 2
    for line in lines:
        width = draw.textbbox((0, 0), line, font=SMALL_FONT)[2]
        draw.text((x1 + (x2 - x1 - width) / 2, y), line, fill=TEXT, font=SMALL_FONT)
        y += line_height


def draw_er_relationship(
    draw: ImageDraw.ImageDraw,
    box: Tuple[int, int, int, int],
    text: str,
    fill: str = "#FFF2E0",
) -> None:
    x1, y1, x2, y2 = box
    points = [
        ((x1 + x2) // 2, y1),
        (x2, (y1 + y2) // 2),
        ((x1 + x2) // 2, y2),
        (x1, (y1 + y2) // 2),
    ]
    draw.polygon(points, outline=LINE, fill=fill)
    lines = wrap_text(draw, text, SMALL_FONT, x2 - x1 - 24)
    line_height = SMALL_FONT.size + 3
    total_height = len(lines) * line_height
    y = y1 + (y2 - y1 - total_height) / 2
    for line in lines:
        width = draw.textbbox((0, 0), line, font=SMALL_FONT)[2]
        draw.text((x1 + (x2 - x1 - width) / 2, y), line, fill=TEXT, font=SMALL_FONT)
        y += line_height


def draw_er_link(
    draw: ImageDraw.ImageDraw,
    start: Tuple[int, int],
    end: Tuple[int, int],
    start_label: Optional[str] = None,
    end_label: Optional[str] = None,
) -> None:
    draw.line((start, end), fill=LINE, width=2)
    if start_label:
        draw.text((start[0] + 6, start[1] - 24), start_label, fill=ACCENT_2, font=SMALL_FONT)
    if end_label:
        draw.text((end[0] - 24, end[1] - 24), end_label, fill=ACCENT_2, font=SMALL_FONT)


def save(image: Image.Image, name: str) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT_DIR / name)


def draw_use_case() -> None:
    image, draw = create_canvas((1800, 1280), "普通用户详细用例图")
    draw_actor(draw, (165, 520), "普通用户")
    frame = (300, 130, 1670, 1120)
    draw.rounded_rectangle(frame, radius=24, outline=LINE, width=3)
    draw.text((340, 170), "小记OCR记账系统", fill=TEXT, font=TEXT_FONT)

    cases = [
        ((390, 230, 750, 320), "注册 / 登录"),
        ((860, 230, 1220, 320), "保持本地会话"),
        ((1330, 230, 1620, 320), "修改个人资料"),
        ((390, 395, 750, 485), "新增收入 / 支出"),
        ((860, 395, 1220, 485), "编辑 / 删除交易"),
        ((1330, 395, 1620, 485), "查看最近记账"),
        ((390, 560, 750, 650), "选择 / 管理分类"),
        ((860, 560, 1220, 650), "管理资产账户"),
        ((1330, 560, 1620, 650), "查看账户余额变化"),
        ((390, 725, 750, 815), "拍照 / 选图识别"),
        ((860, 725, 1220, 815), "带入OCR结果记账"),
        ((1330, 725, 1620, 815), "查看OCR历史"),
        ((390, 890, 750, 980), "查看统计分析"),
        ((860, 890, 1220, 980), "拉取云端最新数据"),
        ((1330, 890, 1620, 980), "退出登录"),
    ]
    for box, text in cases:
        draw_ellipse(draw, box, text)
        draw_arrow(draw, (210, 520), (box[0], (box[1] + box[3]) // 2), color=LINE, width=2)
    save(image, "fig3_1_use_case.png")


def draw_backend_use_case() -> None:
    image, draw = create_canvas((1700, 1100), "后端管理端用例图")
    draw_actor(draw, (170, 460), "管理端操作人员")
    frame = (320, 150, 1540, 930)
    draw.rounded_rectangle(frame, radius=24, outline=LINE, width=3)
    draw.text((360, 170), "本地后端管理系统", fill=TEXT, font=TEXT_FONT)

    cases = [
        ((470, 240, 860, 335), "登录后台面板"),
        ((980, 240, 1390, 335), "查看系统总览"),
        ((470, 420, 860, 515), "查看 / 管理用户"),
        ((980, 420, 1390, 515), "查看 / 管理分类"),
        ((470, 600, 860, 695), "查看 / 管理账户"),
        ((980, 600, 1390, 695), "查看 / 管理交易"),
        ((720, 780, 1140, 875), "为移动端提供同步数据"),
    ]
    for box, text in cases:
        draw_ellipse(draw, box, text, fill=BOX_3 if box[1] < 550 else MUTED)
        draw_arrow(draw, (210, 460), (box[0], (box[1] + box[3]) // 2), color=LINE, width=2)
    save(image, "fig3_2_backend_use_case.png")


def draw_architecture() -> None:
    image, draw = create_canvas((1700, 1200), "系统总体架构图")
    draw_box(draw, (120, 170, 1560, 280), "表现层  UI\nSplash / 登录注册 / 账本首页 / 资产 / 统计 / 我的 / 记账底部弹层 / OCR页面", fill=BOX_2)
    draw_box(draw, (120, 360, 1560, 470), "ViewModel层\nLoginViewModel / HomeViewModel / AssetViewModel / StatisticsViewModel / OcrViewModel / TransactionViewModel", fill=BOX_3)
    draw_box(draw, (120, 550, 1560, 700), "Repository层\nAuthRepository / TransactionRepository / CategoryRepository / AccountRepository / OcrRepository / StatisticsRepository / SyncRepository", fill=BOX)
    draw_box(draw, (120, 790, 760, 1040), "本地数据层\nRoom Database\nUserDao / CategoryDao / AccountDao / TransactionDao / OcrRecordDao / SyncOperationDao", fill=MUTED)
    draw_box(draw, (920, 790, 1560, 1040), "远程与识别层\nSpring Boot 本地后端\nAuthService / SyncService / CategoryService / TransactionService\nML Kit OCR / Paddle OCR兜底", fill=MUTED)
    for y1, y2 in [(280, 360), (470, 550), (700, 790)]:
        draw_arrow(draw, (840, y1), (840, y2))
    draw_arrow(draw, (760, 915), (920, 915))
    save(image, "fig4_1_architecture.png")


def draw_module() -> None:
    image, draw = create_canvas((1600, 1100), "功能模块图")
    draw_box(draw, (610, 150, 1010, 250), "小记OCR记账", fill=BOX_2)
    modules = [
        ((120, 380, 450, 520), "认证模块\n登录 / 注册 / 会话保持"),
        ((500, 380, 830, 520), "账本模块\n新增记账 / 编辑 / 删除 / 最近记录"),
        ((880, 380, 1210, 520), "分类模块\n默认分类 / 图标选择 / 迁移"),
        ((1260, 380, 1490, 520), "资产模块\n账户管理 / 余额维护"),
        ((300, 720, 630, 860), "OCR模块\n拍照 / 选图 / 解析 / 回填"),
        ((700, 720, 1030, 860), "统计模块\n周月年范围 / 柱图 / 饼图 / 资产趋势"),
        ((1100, 720, 1430, 860), "同步模块\n自动上传 / 手动拉取 / 后端面板"),
    ]
    for box, text in modules:
        draw_box(draw, box, text, fill=BOX if box[1] < 600 else BOX_3)
        draw_arrow(draw, (810, 250), ((box[0] + box[2]) // 2, box[1]))
    save(image, "fig4_2_module.png")


def draw_data_flow() -> None:
    image, draw = create_canvas((1700, 1100), "系统数据流图")
    draw_actor(draw, (130, 480), "普通用户")
    draw_box(draw, (280, 230, 620, 360), "输入层\n手动录入 / 拍照选图 / 分类与账户操作", fill=BOX)
    draw_box(draw, (720, 230, 1050, 360), "业务处理层\nViewModel + Repository", fill=BOX_2)
    draw_box(draw, (1140, 170, 1540, 360), "本地存储\nRoom + DataStore", fill=BOX_3)
    draw_box(draw, (720, 500, 1050, 650), "同步与识别层\nOCR解析 / Sync调度", fill=BOX)
    draw_box(draw, (1140, 500, 1540, 650), "本地后端\nREST API + 管理面板", fill=BOX_3)
    draw_box(draw, (720, 800, 1050, 940), "输出层\n首页摘要 / 统计图表 / 资产卡片 / OCR结果", fill=BOX_2)
    draw_arrow(draw, (180, 480), (280, 300))
    draw_arrow(draw, (620, 300), (720, 300))
    draw_arrow(draw, (1050, 300), (1140, 265))
    draw_arrow(draw, (885, 360), (885, 500))
    draw_arrow(draw, (1050, 575), (1140, 575))
    draw_arrow(draw, (885, 650), (885, 800))
    draw_arrow(draw, (1140, 300), (1050, 840))
    save(image, "fig4_3_data_flow.png")


def draw_er() -> None:
    image, draw = create_canvas((2500, 1700), "系统E-R图")

    user = (1080, 860, 1240, 930)
    transaction = (1080, 320, 1260, 390)
    category = (520, 460, 690, 530)
    account = (1620, 470, 1790, 540)
    ocr_record = (1620, 1080, 1810, 1150)
    sync_operation = (510, 1080, 730, 1150)

    draw_er_entity(draw, user, "用户", fill=BOX_2)
    draw_er_entity(draw, transaction, "交易", fill=BOX_3)
    draw_er_entity(draw, category, "分类", fill=BOX)
    draw_er_entity(draw, account, "账户", fill=BOX)
    draw_er_entity(draw, ocr_record, "OCR记录", fill=MUTED)
    draw_er_entity(draw, sync_operation, "同步操作", fill=MUTED)

    rel_record = (1050, 560, 1170, 650)
    rel_has_category = (760, 520, 900, 610)
    rel_has_account = (1370, 520, 1510, 610)
    rel_keep_ocr = (1400, 940, 1540, 1030)
    rel_keep_sync = (790, 940, 930, 1030)
    rel_use_category = (820, 350, 950, 440)
    rel_bind_account = (1370, 350, 1500, 440)
    rel_from_ocr = (1400, 760, 1540, 850)

    draw_er_relationship(draw, rel_record, "记录")
    draw_er_relationship(draw, rel_has_category, "拥有")
    draw_er_relationship(draw, rel_has_account, "拥有")
    draw_er_relationship(draw, rel_keep_ocr, "保存")
    draw_er_relationship(draw, rel_keep_sync, "维护")
    draw_er_relationship(draw, rel_use_category, "使用")
    draw_er_relationship(draw, rel_bind_account, "归属")
    draw_er_relationship(draw, rel_from_ocr, "来源于")

    draw_er_link(draw, (1160, 860), (1110, 650), "1", "n")
    draw_er_link(draw, (1080, 595), (1170, 390), "n", "1")

    draw_er_link(draw, (1080, 895), (900, 565), "1", "n")
    draw_er_link(draw, (760, 565), (690, 495), "n", "1")

    draw_er_link(draw, (1240, 895), (1370, 565), "1", "n")
    draw_er_link(draw, (1510, 565), (1620, 505), "n", "1")

    draw_er_link(draw, (1240, 905), (1400, 985), "1", "n")
    draw_er_link(draw, (1540, 985), (1620, 1115), "n", "1")

    draw_er_link(draw, (1080, 905), (930, 985), "1", "n")
    draw_er_link(draw, (790, 985), (730, 1115), "n", "1")

    draw_er_link(draw, (1080, 355), (950, 395), "n", "1")
    draw_er_link(draw, (820, 395), (690, 495), "n", "1")

    draw_er_link(draw, (1260, 355), (1370, 395), "n", "1")
    draw_er_link(draw, (1500, 395), (1620, 505), "n", "1")

    draw_er_link(draw, (1260, 390), (1400, 805), "n", "1")
    draw_er_link(draw, (1540, 805), (1715, 1080), "n", "1")

    # 用户属性
    draw_er_attribute(draw, (860, 790, 970, 845), "PK id")
    draw_er_attribute(draw, (820, 900, 970, 955), "username")
    draw_er_attribute(draw, (860, 1010, 980, 1065), "nickname")
    draw_er_attribute(draw, (1290, 790, 1430, 845), "email")
    draw_er_attribute(draw, (1300, 900, 1430, 955), "phone")
    draw_er_attribute(draw, (1245, 1000, 1405, 1055), "createdAt")
    draw_er_attribute(draw, (1035, 1110, 1205, 1165), "updatedAt")
    for start, end in [
        ((970, 820), (1080, 885)),
        ((970, 930), (1080, 900)),
        ((980, 1038), (1100, 930)),
        ((1290, 820), (1240, 885)),
        ((1300, 930), (1240, 900)),
        ((1245, 1028), (1230, 930)),
        ((1120, 1110), (1160, 930)),
    ]:
        draw_er_link(draw, start, end)

    # 分类属性
    draw_er_attribute(draw, (300, 410, 410, 465), "PK id")
    draw_er_attribute(draw, (270, 500, 430, 555), "name")
    draw_er_attribute(draw, (300, 590, 430, 645), "type")
    draw_er_attribute(draw, (520, 320, 680, 375), "icon")
    draw_er_attribute(draw, (530, 610, 700, 665), "color")
    draw_er_attribute(draw, (705, 410, 860, 465), "isDefault")
    draw_er_attribute(draw, (720, 500, 900, 555), "syncStatus")
    for start, end in [
        ((410, 438), (520, 485)),
        ((430, 528), (520, 500)),
        ((430, 618), (540, 515)),
        ((600, 375), (600, 460)),
        ((610, 610), (610, 530)),
        ((705, 438), (690, 485)),
        ((720, 528), (690, 500)),
    ]:
        draw_er_link(draw, start, end)

    # 账户属性
    draw_er_attribute(draw, (1730, 320, 1840, 375), "PK id")
    draw_er_attribute(draw, (1810, 430, 1940, 485), "name")
    draw_er_attribute(draw, (1810, 520, 1940, 575), "symbol")
    draw_er_attribute(draw, (1810, 610, 1980, 665), "balanceFen")
    draw_er_attribute(draw, (1570, 320, 1710, 375), "userId")
    draw_er_attribute(draw, (1450, 430, 1600, 485), "isDefault")
    draw_er_attribute(draw, (1450, 610, 1640, 665), "updatedAt")
    for start, end in [
        ((1730, 348), (1790, 500)),
        ((1810, 458), (1790, 505)),
        ((1810, 548), (1790, 515)),
        ((1810, 638), (1790, 525)),
        ((1710, 348), (1620, 500)),
        ((1600, 458), (1620, 505)),
        ((1640, 638), (1680, 540)),
    ]:
        draw_er_link(draw, start, end)

    # 交易属性
    draw_er_attribute(draw, (900, 190, 1010, 245), "PK id")
    draw_er_attribute(draw, (1040, 180, 1170, 235), "type")
    draw_er_attribute(draw, (1190, 180, 1370, 235), "amountFen")
    draw_er_attribute(draw, (1360, 290, 1560, 345), "transactionTime")
    draw_er_attribute(draw, (1340, 410, 1540, 465), "merchantName")
    draw_er_attribute(draw, (1040, 430, 1180, 485), "remark")
    draw_er_attribute(draw, (880, 420, 1010, 475), "source")
    draw_er_attribute(draw, (760, 290, 920, 345), "syncStatus")
    for start, end in [
        ((1010, 218), (1080, 335)),
        ((1105, 235), (1140, 320)),
        ((1190, 208), (1200, 320)),
        ((1360, 318), (1260, 350)),
        ((1340, 438), (1250, 380)),
        ((1110, 430), (1160, 390)),
        ((1010, 448), (1080, 385)),
        ((920, 318), (1080, 350)),
    ]:
        draw_er_link(draw, start, end)

    # OCR记录属性
    draw_er_attribute(draw, (1840, 1000, 1960, 1055), "PK id")
    draw_er_attribute(draw, (1860, 1090, 2020, 1145), "imageUri")
    draw_er_attribute(draw, (1860, 1180, 2030, 1235), "amountText")
    draw_er_attribute(draw, (1650, 1190, 1800, 1245), "amountFen")
    draw_er_attribute(draw, (1620, 1280, 1780, 1335), "dateText")
    draw_er_attribute(draw, (1830, 1280, 2010, 1335), "merchantName")
    draw_er_attribute(draw, (1680, 1370, 1850, 1425), "createdAt")
    for start, end in [
        ((1840, 1028), (1810, 1105)),
        ((1860, 1118), (1810, 1115)),
        ((1860, 1208), (1800, 1130)),
        ((1800, 1218), (1760, 1150)),
        ((1780, 1308), (1720, 1150)),
        ((1830, 1308), (1785, 1150)),
        ((1765, 1370), (1715, 1150)),
    ]:
        draw_er_link(draw, start, end)

    # 同步操作属性
    draw_er_attribute(draw, (300, 1000, 420, 1055), "PK id")
    draw_er_attribute(draw, (250, 1090, 450, 1145), "entityType")
    draw_er_attribute(draw, (250, 1180, 450, 1235), "entityId")
    draw_er_attribute(draw, (250, 1270, 470, 1325), "operationType")
    draw_er_attribute(draw, (740, 1010, 940, 1065), "payloadJson")
    draw_er_attribute(draw, (750, 1100, 910, 1155), "createdAt")
    draw_er_attribute(draw, (740, 1190, 920, 1245), "retryCount")
    for start, end in [
        ((420, 1028), (510, 1105)),
        ((450, 1118), (510, 1115)),
        ((450, 1208), (510, 1128)),
        ((470, 1298), (560, 1150)),
        ((740, 1038), (730, 1105)),
        ((750, 1128), (730, 1115)),
        ((740, 1218), (710, 1150)),
    ]:
        draw_er_link(draw, start, end)
    save(image, "fig4_4_er.png")


def draw_class_diagram() -> None:
    image, draw = create_canvas((1800, 1200), "核心类图")
    draw_box(draw, (80, 180, 430, 420), "TransactionEntryBottomSheet\n+ renderState()\n+ submitTransaction()\n+ launchOcr()", fill=BOX_2)
    draw_box(draw, (520, 180, 900, 420), "TransactionViewModel\n+ observeEditorState()\n+ saveTransaction()\n+ deleteTransaction()\n+ attachOcrResult()", fill=BOX)
    draw_box(draw, (990, 180, 1370, 420), "TransactionRepository\n+ createTransaction()\n+ updateTransaction()\n+ deleteTransaction()\n+ observeTransactions()", fill=BOX_3)
    draw_box(draw, (1450, 180, 1720, 420), "Room DAO\nTransactionDao\nAccountDao\nCategoryDao", fill=MUTED)

    draw_box(draw, (330, 620, 700, 860), "OcrViewModel\n+ recognizeImage()\n+ observeHistory()", fill=BOX_2)
    draw_box(draw, (790, 620, 1170, 860), "OcrRepository\n+ recognizeImage()\n+ saveRecord()\n+ observeRecentRecords()", fill=BOX)
    draw_box(draw, (1260, 620, 1720, 860), "MlKitOcrEngine / PaymentScreenshotParser\n+ recognize()\n+ parse()", fill=BOX_3)

    draw_box(draw, (520, 980, 900, 1130), "SyncRepository\n+ pushPendingChangesBestEffort()\n+ pullLatest()", fill=MUTED)

    for start, end in [
        ((430, 300), (520, 300)),
        ((900, 300), (990, 300)),
        ((1370, 300), (1450, 300)),
        ((700, 740), (790, 740)),
        ((1170, 740), (1260, 740)),
        ((1090, 420), (710, 980)),
        ((980, 740), (860, 980)),
    ]:
        draw_arrow(draw, start, end)
    save(image, "fig4_5_class.png")


def draw_sequence(name: str, title: str, participants: Iterable[str], steps: Iterable[Tuple[int, int, str]]) -> None:
    image, draw = create_canvas((1800, 1200), title)
    x_positions = [180, 520, 860, 1200, 1540]
    tops = 160
    bottom = 1080
    participant_list = list(participants)
    for index, participant in enumerate(participant_list):
        x = x_positions[index]
        draw_box(draw, (x - 110, tops, x + 110, tops + 70), participant, fill=BOX if index % 2 == 0 else BOX_2)
        draw.line((x, tops + 70, x, bottom), fill=LINE, width=2)

    current_y = 290
    for start_idx, end_idx, text in steps:
        start_x = x_positions[start_idx]
        end_x = x_positions[end_idx]
        draw_arrow(draw, (start_x, current_y), (end_x, current_y))
        label_x = min(start_x, end_x) + 24
        draw.text((label_x, current_y - 30), text, fill=TEXT, font=SMALL_FONT)
        current_y += 110
    save(image, name)


def draw_prototype() -> None:
    image, draw = create_canvas((1800, 1200), "主要界面运行截图")
    shots = [
        ("账本首页", Path(r"E:\AndroidDevelopSave\OCRjizhang\docs\thesis\screenshots\01-home.png")),
        ("新增记账弹层", Path(r"E:\AndroidDevelopSave\OCRjizhang\docs\thesis\screenshots\05-entry-bottom-sheet.png")),
        ("资产页面", Path(r"E:\AndroidDevelopSave\OCRjizhang\docs\thesis\screenshots\02-asset.png")),
        ("统计页面", Path(r"E:\AndroidDevelopSave\OCRjizhang\docs\thesis\screenshots\03-statistics.png")),
    ]
    slots = [
        (60, 150, 450, 1080),
        (490, 150, 880, 1080),
        (920, 150, 1310, 1080),
        (1350, 150, 1740, 1080),
    ]
    for (label, path), (x1, y1, x2, y2) in zip(shots, slots):
        frame = Image.open(path).convert("RGB")
        frame_ratio = frame.width / frame.height
        slot_w = x2 - x1
        slot_h = y2 - y1 - 44
        target_ratio = slot_w / slot_h
        if frame_ratio > target_ratio:
            new_w = slot_w
            new_h = int(slot_w / frame_ratio)
        else:
            new_h = slot_h
            new_w = int(slot_h * frame_ratio)
        resized = frame.resize((new_w, new_h))
        paste_x = x1 + (slot_w - new_w) // 2
        paste_y = y1 + 40 + (slot_h - new_h) // 2
        image.paste(resized, (paste_x, paste_y))
        draw.rounded_rectangle((paste_x - 8, paste_y - 8, paste_x + new_w + 8, paste_y + new_h + 8), radius=18, outline="#D8C8BE", width=3)
        draw.rounded_rectangle((x1 + 86, y1, x2 - 86, y1 + 34), radius=12, fill="#FFFDF9", outline="#DCC9BE", width=2)
        tw = draw.textbbox((0, 0), label, font=SMALL_FONT)[2]
        draw.text((x1 + (slot_w - tw) / 2, y1 + 7), label, fill=TEXT, font=SMALL_FONT)
    save(image, "fig4_9_runtime_screens.png")


def draw_flowchart_box(
    draw: ImageDraw.ImageDraw,
    x: int,
    y: int,
    text: str,
    width: int = 520,
    height: int = 92,
    fill: str = BOX,
) -> Tuple[int, int, int, int]:
    box = (x, y, x + width, y + height)
    draw_box(draw, box, text, fill=fill, radius=20)
    return box


def connect_vertical(draw: ImageDraw.ImageDraw, upper: Tuple[int, int, int, int], lower: Tuple[int, int, int, int]) -> None:
    start = ((upper[0] + upper[2]) // 2, upper[3])
    end = ((lower[0] + lower[2]) // 2, lower[1])
    draw_arrow(draw, start, end)


def draw_login_flow() -> None:
    image, draw = create_canvas((1500, 1700), "登录与会话判断流程图")
    steps = [
        draw_flowchart_box(draw, 490, 130, "应用启动，进入 Splash / 启动判断", fill=BOX_2),
        draw_flowchart_box(draw, 490, 290, "读取 DataStore 中的 token、userId、username、nickname", fill=BOX),
        (560, 450, 940, 560),
        draw_flowchart_box(draw, 170, 660, "无有效会话：跳转登录注册页，等待用户输入账号密码", fill=MUTED),
        draw_flowchart_box(draw, 810, 660, "有有效会话：直接进入账本首页并装载用户快照", fill=BOX_3),
        draw_flowchart_box(draw, 170, 840, "登录请求发送到 AuthRepository，校验演示账号或本地后端返回结果", fill=BOX),
        (240, 1020, 620, 1130),
        draw_flowchart_box(draw, 70, 1240, "失败：提示错误信息并停留在认证页面", fill=MUTED),
        draw_flowchart_box(draw, 720, 1240, "成功：保存 SessionSnapshot，写入本地用户快照，预置默认分类", fill=BOX_2),
        draw_flowchart_box(draw, 720, 1420, "进入主界面，后续页面通过 Flow 读取当前会话状态", fill=BOX_3),
    ]

    connect_vertical(draw, steps[0], steps[1])
    draw_diamond(draw, steps[2], "本地是否存在有效会话")
    connect_vertical(draw, steps[1], steps[2])
    draw_arrow(draw, (560, 505), (430, 705))
    draw_arrow(draw, (940, 505), (1070, 705))
    connect_vertical(draw, steps[3], steps[5])
    draw_diamond(draw, steps[6], "登录校验是否通过")
    connect_vertical(draw, steps[5], steps[6])
    draw_arrow(draw, (240, 1075), (210, 1285))
    draw_arrow(draw, (620, 1075), (980, 1285))
    connect_vertical(draw, steps[8], steps[9])
    save(image, "fig5_2_login_flow.png")


def draw_transaction_flow() -> None:
    image, draw = create_canvas((1500, 1840), "记账提交与余额联动流程图")
    boxes = [
        draw_flowchart_box(draw, 490, 120, "用户点击账本页 FAB，展开新增记账底部弹层", fill=BOX_2),
        draw_flowchart_box(draw, 490, 280, "在弹层中选择收支类型、分类、账户，输入金额、时间、备注", fill=BOX),
        (560, 450, 940, 560),
        draw_flowchart_box(draw, 100, 660, "信息不完整：高亮必填项并阻止提交", fill=MUTED),
        draw_flowchart_box(draw, 800, 660, "信息完整：组装 TransactionEntity，记录 categoryName 与 accountName 快照", fill=BOX_3),
        draw_flowchart_box(draw, 800, 840, "在 Room 事务中写入交易记录", fill=BOX),
        draw_flowchart_box(draw, 800, 1000, "根据收入 / 支出计算 signedAmount，并差额更新账户余额", fill=BOX_2),
        draw_flowchart_box(draw, 800, 1160, "写入 sync_operations 待同步队列", fill=MUTED),
        draw_flowchart_box(draw, 800, 1320, "触发 pushPendingChangesBestEffort 尝试上传本地改动", fill=BOX_3),
        draw_flowchart_box(draw, 800, 1480, "Flow 推送最新交易列表、账本摘要与资产余额", fill=BOX),
        draw_flowchart_box(draw, 800, 1640, "界面关闭弹层并回到账本页，最近记账与总览立即刷新", fill=BOX_2),
    ]
    connect_vertical(draw, boxes[0], boxes[1])
    draw_diamond(draw, boxes[2], "金额、分类、账户是否齐全")
    connect_vertical(draw, boxes[1], boxes[2])
    draw_arrow(draw, (560, 505), (360, 705))
    draw_arrow(draw, (940, 505), (1060, 705))
    for idx in range(4, len(boxes) - 1):
        connect_vertical(draw, boxes[idx], boxes[idx + 1])
    save(image, "fig5_5_transaction_flow.png")


def draw_asset_flow() -> None:
    image, draw = create_canvas((1500, 1700), "资产账户维护流程图")
    boxes = [
        draw_flowchart_box(draw, 490, 120, "用户进入资产页，系统读取 AccountDao 并展示总资产与账户卡片", fill=BOX_2),
        draw_flowchart_box(draw, 490, 280, "点击新增或编辑账户，填写账户名称、图标符号与余额", fill=BOX),
        (560, 450, 940, 560),
        draw_flowchart_box(draw, 150, 660, "校验失败：提示名称为空或输入不合法", fill=MUTED),
        draw_flowchart_box(draw, 820, 660, "校验通过：写入 / 更新 AccountEntity", fill=BOX_3),
        draw_flowchart_box(draw, 820, 840, "若为默认账户升级，则保留默认标记并刷新卡片顺序", fill=BOX),
        draw_flowchart_box(draw, 820, 1020, "若删除账户，则同步清空历史交易中的 accountId / accountName 绑定", fill=BOX_2),
        draw_flowchart_box(draw, 820, 1200, "账户改动进入同步队列，等待自动上传或手动拉取后回写", fill=MUTED),
        draw_flowchart_box(draw, 820, 1380, "资产页重新计算总资产、账户数量与更新时间摘要", fill=BOX_3),
    ]
    connect_vertical(draw, boxes[0], boxes[1])
    draw_diamond(draw, boxes[2], "账户名称与余额是否有效")
    connect_vertical(draw, boxes[1], boxes[2])
    draw_arrow(draw, (560, 505), (360, 705))
    draw_arrow(draw, (940, 505), (1080, 705))
    for idx in range(4, len(boxes) - 1):
        connect_vertical(draw, boxes[idx], boxes[idx + 1])
    save(image, "fig5_7_asset_flow.png")


def draw_ocr_flow() -> None:
    image, draw = create_canvas((1500, 1920), "OCR识别与回填流程图")
    boxes = [
        draw_flowchart_box(draw, 490, 120, "用户在 OCR 页选择拍照或相册图片，进入预览页面", fill=BOX_2),
        draw_flowchart_box(draw, 490, 280, "执行图片旋转修正、路径确认与识别前预处理", fill=BOX),
        draw_flowchart_box(draw, 490, 440, "优先调用 ML Kit 获取 rawText 与行坐标 structuredLines", fill=BOX_3),
        (560, 610, 940, 720),
        draw_flowchart_box(draw, 120, 820, "ML Kit 无可用文本：切换到本地 Native OCR 兜底识别", fill=MUTED),
        draw_flowchart_box(draw, 820, 820, "ML Kit 成功：直接进入规则解析阶段", fill=BOX_2),
        draw_flowchart_box(draw, 490, 1000, "通用票据解析器提取金额、日期、商户候选", fill=BOX),
        draw_flowchart_box(draw, 490, 1160, "支付截图解析器根据标签位置、同排值、候选打分修正结果", fill=BOX_3),
        draw_flowchart_box(draw, 490, 1320, "生成 ParsedReceiptData，并写入 OCR 历史记录表", fill=MUTED),
        draw_flowchart_box(draw, 490, 1480, "页面展示金额、日期、商户和原始识别文本", fill=BOX),
        (560, 1650, 940, 1760),
        draw_flowchart_box(draw, 120, 1810, "用户不带入：停留在结果页，可重新识别或返回", fill=MUTED),
        draw_flowchart_box(draw, 820, 1810, "用户点击带入：跳转 / 回填到新增记账弹层", fill=BOX_2),
    ]
    connect_vertical(draw, boxes[0], boxes[1])
    connect_vertical(draw, boxes[1], boxes[2])
    draw_diamond(draw, boxes[3], "主识别是否返回可用文本")
    connect_vertical(draw, boxes[2], boxes[3])
    draw_arrow(draw, (560, 665), (380, 865))
    draw_arrow(draw, (940, 665), (1080, 865))
    draw_arrow(draw, (380, 912), (750, 1046))
    draw_arrow(draw, (1080, 912), (750, 1046))
    connect_vertical(draw, boxes[6], boxes[7])
    connect_vertical(draw, boxes[7], boxes[8])
    connect_vertical(draw, boxes[8], boxes[9])
    draw_diamond(draw, boxes[10], "是否将识别结果带入记账")
    connect_vertical(draw, boxes[9], boxes[10])
    draw_arrow(draw, (560, 1705), (360, 1855))
    draw_arrow(draw, (940, 1705), (1080, 1855))
    save(image, "fig5_9_ocr_flow.png")


def draw_statistics_flow() -> None:
    image, draw = create_canvas((1500, 1700), "统计聚合处理流程图")
    boxes = [
        draw_flowchart_box(draw, 490, 120, "用户进入统计页或切换周 / 月 / 年 / 全部 / 范围", fill=BOX_2),
        draw_flowchart_box(draw, 490, 280, "StatisticsViewModel 依据当前粒度计算起止时间", fill=BOX),
        draw_flowchart_box(draw, 490, 440, "从 TransactionDao 读取指定区间的交易流", fill=BOX_3),
        draw_flowchart_box(draw, 490, 600, "StatisticsRepository 计算收入、支出、结余、日均值与分类占比", fill=MUTED),
        draw_flowchart_box(draw, 490, 760, "生成柱状图数据、饼图数据与资产趋势数据", fill=BOX),
        draw_flowchart_box(draw, 490, 920, "ViewModel 把聚合结果分发给摘要卡片与 MPAndroidChart 组件", fill=BOX_2),
        draw_flowchart_box(draw, 490, 1080, "页面刷新图表与数值摘要，不直接在界面层做重计算", fill=BOX_3),
    ]
    for idx in range(len(boxes) - 1):
        connect_vertical(draw, boxes[idx], boxes[idx + 1])
    save(image, "fig5_11_statistics_flow.png")


def draw_sync_flow() -> None:
    image, draw = create_canvas((1500, 1840), "本地优先同步流程图")
    boxes = [
        draw_flowchart_box(draw, 490, 120, "本地交易 / 分类 / 账户发生改动，写入 sync_operations 队列", fill=BOX_2),
        draw_flowchart_box(draw, 490, 280, "自动上传阶段：尝试 pushPendingChangesBestEffort", fill=BOX),
        (560, 450, 940, 560),
        draw_flowchart_box(draw, 120, 660, "网络异常或后端未开启：保留待同步记录，等待下次继续上传", fill=MUTED),
        draw_flowchart_box(draw, 820, 660, "上传成功：删除已完成的待同步操作", fill=BOX_3),
        draw_flowchart_box(draw, 820, 840, "用户在“我的”页面点击拉取云端最新数据", fill=BOX),
        draw_flowchart_box(draw, 820, 1000, "先再次补传本地待同步改动，再请求 pullChanges", fill=BOX_2),
        draw_flowchart_box(draw, 820, 1160, "后端返回账户、分类、交易快照", fill=MUTED),
        draw_flowchart_box(draw, 820, 1320, "客户端覆盖本地快照，并补齐默认账户与分类绑定", fill=BOX_3),
        draw_flowchart_box(draw, 820, 1480, "首页、资产页、统计页重新读取本地数据并刷新显示", fill=BOX),
    ]
    connect_vertical(draw, boxes[0], boxes[1])
    draw_diamond(draw, boxes[2], "自动上传是否成功")
    connect_vertical(draw, boxes[1], boxes[2])
    draw_arrow(draw, (560, 505), (360, 705))
    draw_arrow(draw, (940, 505), (1080, 705))
    for idx in range(4, len(boxes) - 1):
        connect_vertical(draw, boxes[idx], boxes[idx + 1])
    save(image, "fig5_12_sync_flow.png")


def main() -> None:
    draw_use_case()
    draw_backend_use_case()
    draw_architecture()
    draw_module()
    draw_data_flow()
    draw_er()
    draw_class_diagram()
    draw_sequence(
        "fig4_6_manual_sequence.png",
        "手动记账时序图",
        ["用户", "记账弹层", "TransactionViewModel", "TransactionRepository", "Room数据库"],
        [
            (0, 1, "输入金额、分类、账户"),
            (1, 2, "提交记账请求"),
            (2, 3, "校验参数并组装交易"),
            (3, 4, "写入交易并更新账户余额"),
            (4, 3, "返回持久化结果"),
            (3, 2, "推送最新列表状态"),
            (2, 1, "刷新界面并关闭弹层"),
        ],
    )
    draw_sequence(
        "fig4_7_ocr_sequence.png",
        "OCR识别并回填时序图",
        ["用户", "OCR页面", "OcrViewModel", "OcrRepository", "OCR引擎/解析器"],
        [
            (0, 1, "拍照或选择图片"),
            (1, 2, "请求开始识别"),
            (2, 3, "启动识别流程"),
            (3, 4, "提取原始文字与结构化字段"),
            (4, 3, "返回金额、时间、商户"),
            (3, 2, "封装识别结果"),
            (2, 1, "展示识别内容并支持带入"),
        ],
    )
    draw_sequence(
        "fig4_8_sync_sequence.png",
        "手动同步时序图",
        ["用户", "我的页面", "SyncViewModel/调用层", "SyncRepository", "本地后端"],
        [
            (0, 1, "点击立即同步"),
            (1, 2, "发起同步动作"),
            (2, 3, "先推送本地待同步操作"),
            (3, 4, "push本地变更"),
            (4, 3, "返回写入结果"),
            (3, 4, "pull最新快照"),
            (4, 3, "返回账户/分类/交易"),
            (3, 2, "覆盖本地并回传统计"),
            (2, 1, "提示同步完成"),
        ],
    )
    draw_prototype()
    draw_login_flow()
    draw_transaction_flow()
    draw_asset_flow()
    draw_ocr_flow()
    draw_statistics_flow()
    draw_sync_flow()


if __name__ == "__main__":
    main()
