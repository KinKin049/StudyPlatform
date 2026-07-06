from __future__ import annotations

from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "docs" / "report-assets"

FUNCTION_PNG = OUTPUT_DIR / "studyplatform-function-architecture.png"
FUNCTION_V2_PNG = OUTPUT_DIR / "studyplatform-function-architecture-v2.png"
FUNCTION_V2_JPG = OUTPUT_DIR / "studyplatform-function-architecture-v2.jpg"
TECH_PNG = OUTPUT_DIR / "studyplatform-technical-architecture.png"
TECH_V2_PNG = OUTPUT_DIR / "studyplatform-technical-architecture-v2.png"
TECH_V2_JPG = OUTPUT_DIR / "studyplatform-technical-architecture-v2.jpg"

INK = "#2f2f2f"
MUTED = "#58616f"
GRID = "#e5e7eb"
BG = "#ffffff"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    names = ["msyhbd.ttc", "msyh.ttc", "simhei.ttf"] if bold else ["msyh.ttc", "simhei.ttf"]
    for name in names:
        path = Path("C:/Windows/Fonts") / name
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


FONT_TITLE = font(42, True)
FONT_NODE = font(28)
FONT_NODE_BOLD = font(28, True)
FONT_LEAF = font(24)
FONT_LEAF_BOLD = font(24, True)
FONT_SMALL = font(20)
FONT_TECH_TITLE = font(40, True)
FONT_LAYER = font(26, True)
FONT_TECH = font(24, True)
FONT_TECH_SMALL = font(20, True)


def text_size(draw: ImageDraw.ImageDraw, text: str, text_font: ImageFont.FreeTypeFont) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=text_font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def center_text(
    draw: ImageDraw.ImageDraw,
    box: tuple[float, float, float, float],
    text: str,
    text_font: ImageFont.FreeTypeFont,
    fill: str = INK,
    line_gap: int = 6,
) -> None:
    lines = text.split("\n")
    sizes = [text_size(draw, line, text_font) for line in lines]
    total_height = sum(height for _, height in sizes) + max(0, len(lines) - 1) * line_gap
    x1, y1, x2, y2 = box
    y = y1 + ((y2 - y1) - total_height) / 2
    for line, (width, height) in zip(lines, sizes):
        draw.text((x1 + ((x2 - x1) - width) / 2, y), line, font=text_font, fill=fill)
        y += height + line_gap


def wrap_text(draw: ImageDraw.ImageDraw, text: str, text_font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    result: list[str] = []
    for paragraph in text.split("\n"):
        line = ""
        for ch in paragraph:
            candidate = line + ch
            if text_size(draw, candidate, text_font)[0] <= max_width or not line:
                line = candidate
            else:
                result.append(line)
                line = ch
        if line:
            result.append(line)
    return result


def draw_rect(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    outline: str = INK,
    fill: str = BG,
    text_font: ImageFont.FreeTypeFont = FONT_NODE,
    width: int = 4,
) -> None:
    draw.rectangle(box, fill=fill, outline=outline, width=width)
    center_text(draw, box, text, text_font)


def draw_arrow_line(
    draw: ImageDraw.ImageDraw,
    start: tuple[int, int],
    end: tuple[int, int],
    fill: str = INK,
    width: int = 4,
    arrow: bool = True,
) -> None:
    draw.line((start, end), fill=fill, width=width)
    if not arrow:
        return
    x1, y1 = start
    x2, y2 = end
    if abs(x2 - x1) < abs(y2 - y1):
        direction = 1 if y2 >= y1 else -1
        points = [(x2, y2), (x2 - 12, y2 - direction * 22), (x2 + 12, y2 - direction * 22)]
    else:
        direction = 1 if x2 >= x1 else -1
        points = [(x2, y2), (x2 - direction * 22, y2 - 12), (x2 - direction * 22, y2 + 12)]
    draw.polygon(points, fill=fill)


def draw_polyline(
    draw: ImageDraw.ImageDraw,
    points: Iterable[tuple[int, int]],
    fill: str = INK,
    width: int = 4,
    arrow: bool = False,
) -> None:
    pts = list(points)
    if len(pts) < 2:
        return
    draw.line(pts, fill=fill, width=width, joint="curve")
    if arrow:
        draw_arrow_line(draw, pts[-2], pts[-1], fill=fill, width=width, arrow=True)


def vertical_text(text: str) -> str:
    return "\n".join(text)


def draw_function_architecture() -> Image.Image:
    width, height = 3880, 1480
    image = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(image)

    title_box = (340, 28, 3540, 96)
    draw_rect(draw, title_box, "StudyPlatform 智慧学习平台功能架构", text_font=FONT_TITLE, width=3)

    trunk_x = width // 2
    draw_arrow_line(draw, (trunk_x, 96), (trunk_x, 164), arrow=False)

    columns = [
        {
            "title": "用户与账号",
            "x": 80,
            "w": 280,
            "subs": ["用户登录", "账号注册", "找回密码", "入驻引导"],
        },
        {
            "title": "在线学堂",
            "x": 420,
            "w": 630,
            "subs": ["学堂首页", "开放课程", "通识课程", "微专业", "精品教材", "我的课程班级", "课程作业", "课程考试"],
        },
        {
            "title": "题库练习",
            "x": 1110,
            "w": 390,
            "subs": ["课程题库", "答题判定", "错题本", "收藏夹", "词汇/专项题库"],
        },
        {
            "title": "实验平台",
            "x": 1560,
            "w": 330,
            "subs": ["OJ编程练习", "石油气仿真", "测井解释"],
        },
        {
            "title": "可视化实验",
            "x": 1950,
            "w": 410,
            "subs": ["算法结构可视化", "函数图像实验室", "空间模型实验室"],
        },
        {
            "title": "游戏与金币",
            "x": 2420,
            "w": 390,
            "subs": ["打字战士", "万题天梯跳", "游戏记录", "学习金币", "金币兑换中心"],
        },
        {
            "title": "个人与AI",
            "x": 2870,
            "w": 360,
            "subs": ["个人资料", "学习画像", "学习时长", "金币余额", "AI学习助手"],
        },
        {
            "title": "后台管理",
            "x": 3290,
            "w": 520,
            "subs": ["用户管理", "课程管理", "题库管理", "题目管理", "评论管理", "购书订单", "金币调整"],
        },
    ]

    top_y = 220
    module_h = 72
    bus_y = 168
    min_x = min(c["x"] + c["w"] // 2 for c in columns)
    max_x = max(c["x"] + c["w"] // 2 for c in columns)
    draw.line((min_x, bus_y, max_x, bus_y), fill=INK, width=4)
    draw_arrow_line(draw, (trunk_x, 164), (trunk_x, bus_y), arrow=False)

    leaf_top = 430
    leaf_h = 880
    gap = 16
    for column in columns:
        x = int(column["x"])
        w = int(column["w"])
        center = x + w // 2
        draw_arrow_line(draw, (center, bus_y), (center, top_y), arrow=True)
        draw_rect(draw, (x, top_y, x + w, top_y + module_h), column["title"], text_font=FONT_NODE_BOLD, width=4)
        draw_arrow_line(draw, (center, top_y + module_h), (center, 360), arrow=False)

        subs = column["subs"]
        sub_count = len(subs)
        leaf_w = max(54, int((w - (sub_count - 1) * gap) / sub_count))
        sub_bus_y = 360
        first_center = x + leaf_w // 2
        last_center = x + (leaf_w + gap) * (sub_count - 1) + leaf_w // 2
        draw.line((first_center, sub_bus_y, last_center, sub_bus_y), fill=INK, width=4)
        for index, sub in enumerate(subs):
            lx = x + index * (leaf_w + gap)
            cx = lx + leaf_w // 2
            draw_arrow_line(draw, (cx, sub_bus_y), (cx, leaf_top), arrow=True)
            label = sub if "\n" in sub else vertical_text(sub)
            draw_rect(
                draw,
                (lx, leaf_top, lx + leaf_w, leaf_top + leaf_h),
                label,
                text_font=FONT_LEAF if len(sub) <= 7 else FONT_SMALL,
                width=3,
            )

    note = "注：金币兑换中心依据当前项目页面标注为展示/预留能力；金币获取、余额展示与后台调整已纳入功能分解。"
    draw.text((90, 1378), note, font=FONT_SMALL, fill=MUTED)
    return image


def rounded_rect(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    fill: str,
    outline: str,
    radius: int = 0,
    width: int = 4,
) -> None:
    if radius:
        draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)
    else:
        draw.rectangle(box, fill=fill, outline=outline, width=width)


def draw_chip(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fill: str,
    outline: str,
    text_fill: str,
    text_font: ImageFont.FreeTypeFont = FONT_TECH_SMALL,
) -> None:
    rounded_rect(draw, box, fill=fill, outline=outline, radius=2, width=4)
    wrapped = "\n".join(wrap_text(draw, text, text_font, box[2] - box[0] - 18))
    center_text(draw, box, wrapped, text_font, fill=text_fill, line_gap=4)


def draw_layer_label(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    fill: str,
    outline: str,
) -> None:
    rounded_rect(draw, box, fill=fill, outline=outline, radius=0, width=4)
    center_text(draw, box, text, FONT_LAYER, fill=BG)


def draw_technical_architecture() -> Image.Image:
    width, height = 2500, 1740
    image = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(image)

    title = "StudyPlatform 技术架构设计总览"
    title_width, _ = text_size(draw, title, FONT_TECH_TITLE)
    draw.text(((width - title_width) / 2, 34), title, font=FONT_TECH_TITLE, fill="#253041")

    left_x = 38
    label_w = 180
    content_x = 285
    content_w = 1740
    right_x = 2070

    palette = [
        ("访问层", "#7FB3D5", "#2F6F9F", 115, 222),
        ("前端框架", "#8FCBC2", "#2A8C82", 285, 425),
        ("前端模块", "#A9D6A5", "#5B9B64", 485, 710),
        ("业务接口", "#B8ABD7", "#7568A7", 775, 960),
        ("后端服务", "#DDB967", "#A97920", 1025, 1165),
        ("业务支撑", "#8FC7B5", "#2F8F7E", 1215, 1390),
        ("数据与外部服务", "#EFA99A", "#C65D4D", 1448, 1640),
    ]

    for name, fill, outline, y1, y2 in palette:
        draw_layer_label(draw, (left_x, y1, left_x + label_w, y2), name, fill, outline)

    # Access layer.
    y1, y2 = 112, 232
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#EFF7FC", outline="#2F6F9F", width=5)
    browser_chips = [
        ("Chrome", "#ffffff"),
        ("Edge", "#ffffff"),
        ("Firefox", "#ffffff"),
        ("Safari", "#ffffff"),
        ("移动/PC Web", "#ffffff"),
        ("小程序/移动端预留", "#ffffff"),
    ]
    bx = content_x + 35
    for label, fill in browser_chips:
        chip_w = 230 if len(label) > 8 else 170
        draw_chip(draw, (bx, y1 + 25, bx + chip_w, y2 - 25), label, fill, "#2F6F9F", "#1F4E70", FONT_TECH_SMALL)
        bx += chip_w + 22
    draw_arrow_line(draw, (right_x + 120, y1 + 60), (content_x + content_w + 28, y1 + 60), fill="#253041", width=5)
    draw_chip(draw, (right_x, y1 + 12, right_x + 155, y1 + 56), "PC端", "#E5F0F7", "#2F6F9F", "#1F4E70")
    draw_chip(draw, (right_x, y1 + 66, right_x + 155, y1 + 110), "浏览器", "#E5F0F7", "#2F6F9F", "#1F4E70")

    # Frontend framework.
    y1, y2 = 278, 435
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#F1FAF8", outline="#2A8C82", width=5)
    frameworks = ["Vue 3", "Vite", "Vue Router", "Element Plus", "ECharts", "Three.js", "HTML5", "CSS3", "JavaScript"]
    chip_w = 250
    chip_h = 44
    for index, item in enumerate(frameworks):
        row = index // 5
        col = index % 5
        x = content_x + 40 + col * 330
        y = y1 + 24 + row * 68
        w = chip_w if item not in {"Element Plus", "Vue Router", "JavaScript"} else 290
        draw_chip(draw, (x, y, x + w, y + chip_h), item, "#FFFFFF", "#2A8C82", "#225E59", FONT_TECH_SMALL)

    # Frontend modules.
    y1, y2 = 480, 720
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#F4FBF3", outline="#5B9B64", width=5)
    frontend_rows = [
        "在线学堂：学堂首页、开放课程、通识课程、微专业、精品教材、作业考试、题库练习",
        "实验与可视化：OJ 编程平台、石油气仿真、测井解释、算法结构、函数图像、空间模型",
        "学习拓展：Type Warrior、万题天梯跳、AI 学习助手、个人中心、金币兑换中心",
        "管理端：用户管理、课程管理、题库/题目管理、评论管理、购书订单、金币调整",
    ]
    row_h = 50
    for index, text in enumerate(frontend_rows):
        y = y1 + 24 + index * row_h
        draw_chip(draw, (content_x + 35, y, content_x + content_w - 35, y + 40), text, "#DDEFD8", "#6CA874", "#2F5F38", FONT_TECH_SMALL)

    # Business interface layer.
    y1, y2 = 770, 972
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#F6F3FB", outline="#7568A7", width=5)
    api_items = [
        "REST API / JSON",
        "X-Auth-User-Id 用户上下文",
        "静态资源 URL 解析",
        "AI 后端代理",
        "OJ 沙箱调用契约",
        "统一异常与参数校验",
    ]
    for index, item in enumerate(api_items):
        x = content_x + 35 + (index % 3) * 560
        y = y1 + 26 + (index // 3) * 76
        draw_chip(draw, (x, y, x + 500, y + 50), item, "#E5DFF3", "#7568A7", "#453D73", FONT_TECH_SMALL)

    # Backend service layer.
    y1, y2 = 1024, 1177
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#FFF8E8", outline="#A97920", width=5)
    service_items = ["Spring Boot 3.5", "Spring Security", "Spring MVC", "Validation", "JdbcTemplate", "Flyway", "Mail", "配置/异常处理"]
    for index, item in enumerate(service_items):
        x = content_x + 35 + index * 210
        draw_chip(draw, (x, y1 + 48, x + 188, y1 + 100), item, "#FFFFFF", "#A97920", "#6C4916", FONT_TECH_SMALL)

    # Business support layer.
    y1, y2 = 1210, 1405
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#F1FAF7", outline="#2F8F7E", width=5)
    support_items = [
        "认证/用户画像",
        "课程/教材/订单",
        "作业/考试",
        "题库/错题/收藏",
        "OJ 评测",
        "仿真实验/测井",
        "游戏记录",
        "金币奖励",
        "后台管理",
        "AI 问答",
    ]
    for index, item in enumerate(support_items):
        x = content_x + 35 + (index % 5) * 335
        y = y1 + 28 + (index // 5) * 78
        draw_chip(draw, (x, y, x + 295, y + 50), item, "#FFFFFF", "#2F8F7E", "#225E54", FONT_TECH_SMALL)

    # Data and external service layer.
    y1, y2 = 1450, 1645
    rounded_rect(draw, (content_x, y1, content_x + content_w, y2), fill="#FFF5F1", outline="#C65D4D", width=5)
    data_items = [
        "MySQL\nstudy_platform",
        "Flyway\n版本迁移",
        "本地资源存储\n图片/视频/教材封面",
        "Judge Sandbox\nC++ 代码评测",
        "AI 模型服务\nOpenAI兼容接口",
        "SMTP 邮件\n找回密码",
    ]
    for index, item in enumerate(data_items):
        col = index % 3
        row = index // 3
        x = content_x + 35 + col * 560
        y = y1 + 24 + row * 78
        draw_chip(draw, (x, y, x + 500, y + 55), item, "#FFFFFF", "#C65D4D", "#7A342D", FONT_TECH_SMALL)

    # Vertical orchestration lane.
    rounded_rect(draw, (right_x + 150, 280, right_x + 270, 1408), fill="#D9F0EA", outline="#2F8F7E", width=4)
    center_text(draw, (right_x + 150, 280, right_x + 270, 1408), "前后端分离\n请求/响应\n运行主链路", FONT_LAYER, fill="#225E54", line_gap=12)
    draw_arrow_line(draw, (right_x + 315, 180), (right_x + 315, 1530), fill="#D77A61", width=16)
    center_text(draw, (right_x + 334, 700, right_x + 398, 1040), vertical_text("服务运行环境"), FONT_LAYER, fill="#D77A61", line_gap=2)

    # Cross-layer arrows.
    arrow_color = "#2B7A78"
    arrow_x = content_x + content_w + 80
    layer_centers = [172, 356, 600, 870, 1100, 1305, 1530]
    for start, end in zip(layer_centers[:-1], layer_centers[1:]):
        draw_arrow_line(draw, (arrow_x, start + 60), (arrow_x, end - 60), fill=arrow_color, width=5)
        draw_arrow_line(draw, (arrow_x, end - 60), (content_x + content_w, end - 60), fill=arrow_color, width=5)

    note = "说明：架构内容来自当前项目文件；未引入 Redis、Nginx 等项目中未配置的组件。"
    draw.text((content_x, 1682), note, font=FONT_SMALL, fill=MUTED)
    return image


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    function_image = draw_function_architecture()
    function_image.save(FUNCTION_PNG)
    function_image.save(FUNCTION_V2_PNG)
    function_image.save(FUNCTION_V2_JPG, quality=95)

    technical_image = draw_technical_architecture()
    technical_image.save(TECH_PNG)
    technical_image.save(TECH_V2_PNG)
    technical_image.save(TECH_V2_JPG, quality=95)

    for path in [FUNCTION_PNG, FUNCTION_V2_PNG, FUNCTION_V2_JPG, TECH_PNG, TECH_V2_PNG, TECH_V2_JPG]:
        print(path)


if __name__ == "__main__":
    main()
