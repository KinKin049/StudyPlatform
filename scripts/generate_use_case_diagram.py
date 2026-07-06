from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "docs" / "report-assets"
OUTPUT_PATH = OUTPUT_DIR / "studyplatform-use-case-diagram.png"

WIDTH = 2400
HEIGHT = 1700

INK = "#1f2937"
MUTED = "#64748b"
LINE = "#475569"
BLUE = "#2563eb"
BORDER = "#94a3b8"
BG = "#ffffff"
BOUNDARY_BG = "#fcfdff"
SOFT_BLUE = "#eff6ff"
SOFT_GREEN = "#f0fdf4"
SOFT_ORANGE = "#fff7ed"
SOFT_PURPLE = "#faf5ff"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    names = ["msyhbd.ttc", "msyh.ttc", "simhei.ttf"] if bold else ["msyh.ttc", "simhei.ttf"]
    for name in names:
        path = Path("C:/Windows/Fonts") / name
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


FONT_TITLE = font(42, True)
FONT_ACTOR = font(26, True)
FONT_USE_CASE = font(24)
FONT_SMALL = font(18)


def center_text(draw: ImageDraw.ImageDraw, box: tuple[float, float, float, float], text: str, text_font, fill=INK) -> None:
    lines = text.split("\n")
    line_heights = []
    line_widths = []
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=text_font)
        line_widths.append(bbox[2] - bbox[0])
        line_heights.append(bbox[3] - bbox[1])
    total_height = sum(line_heights) + max(0, len(lines) - 1) * 6
    x1, y1, x2, y2 = box
    y = y1 + ((y2 - y1) - total_height) / 2
    for line, width, line_height in zip(lines, line_widths, line_heights):
        draw.text((x1 + ((x2 - x1) - width) / 2, y), line, font=text_font, fill=fill)
        y += line_height + 6


def draw_actor(draw: ImageDraw.ImageDraw, x: int, y: int, name: str) -> None:
    draw.ellipse((x - 30, y, x + 30, y + 60), outline=INK, width=5)
    draw.line((x, y + 60, x, y + 160), fill=INK, width=5)
    draw.line((x - 68, y + 95, x + 68, y + 95), fill=INK, width=5)
    draw.line((x, y + 160, x - 62, y + 250), fill=INK, width=5)
    draw.line((x, y + 160, x + 62, y + 250), fill=INK, width=5)
    center_text(draw, (x - 150, y + 260, x + 150, y + 310), name, FONT_ACTOR)


def draw_inheritance(draw: ImageDraw.ImageDraw, x1: int, y1: int, x2: int, y2: int) -> None:
    draw.line((x1, y1, x2, y2), fill=LINE, width=3)
    triangle = [(x2, y2), (x2 - 20, y2 + 38), (x2 + 20, y2 + 38)]
    draw.polygon(triangle, fill=BG, outline=LINE)


def ellipse_point(cx: float, cy: float, rx: float, ry: float, tx: float, ty: float) -> tuple[float, float]:
    dx = tx - cx
    dy = ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    scale = 1 / math.sqrt((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry))
    return cx + dx * scale, cy + dy * scale


def draw_dashed_line(draw: ImageDraw.ImageDraw, start: tuple[float, float], end: tuple[float, float], fill: str, width: int = 3) -> None:
    x1, y1 = start
    x2, y2 = end
    length = math.hypot(x2 - x1, y2 - y1)
    dash = 18
    gap = 12
    distance = 0
    while distance < length:
        next_distance = min(distance + dash, length)
        sx = x1 + (x2 - x1) * distance / length
        sy = y1 + (y2 - y1) * distance / length
        ex = x1 + (x2 - x1) * next_distance / length
        ey = y1 + (y2 - y1) * next_distance / length
        draw.line((sx, sy, ex, ey), fill=fill, width=width)
        distance += dash + gap


def draw_arrowhead(draw: ImageDraw.ImageDraw, start: tuple[float, float], end: tuple[float, float], fill: str) -> None:
    angle = math.atan2(end[1] - start[1], end[0] - start[0])
    size = 16
    left = (end[0] - size * math.cos(angle - math.pi / 6), end[1] - size * math.sin(angle - math.pi / 6))
    right = (end[0] - size * math.cos(angle + math.pi / 6), end[1] - size * math.sin(angle + math.pi / 6))
    draw.polygon([end, left, right], fill=fill)


def draw_arrow_between_cases(draw: ImageDraw.ImageDraw, cases: dict[str, dict[str, float]], from_key: str, to_key: str, label: str) -> None:
    start_case = cases[from_key]
    end_case = cases[to_key]
    start = ellipse_point(start_case["cx"], start_case["cy"], start_case["w"] / 2, start_case["h"] / 2, end_case["cx"], end_case["cy"])
    end = ellipse_point(end_case["cx"], end_case["cy"], end_case["w"] / 2, end_case["h"] / 2, start_case["cx"], start_case["cy"])
    draw_dashed_line(draw, start, end, MUTED)
    draw_arrowhead(draw, start, end, MUTED)
    mx = (start[0] + end[0]) / 2
    my = (start[1] + end[1]) / 2
    center_text(draw, (mx - 95, my - 22, mx + 95, my + 22), label, FONT_SMALL, MUTED)


def draw_use_case(
    draw: ImageDraw.ImageDraw,
    cases: dict[str, dict[str, float]],
    key: str,
    x: int,
    y: int,
    w: int,
    h: int,
    text: str,
    fill: str,
) -> None:
    draw.ellipse((x, y, x + w, y + h), fill=fill, outline=BLUE, width=3)
    center_text(draw, (x + 10, y + 8, x + w - 10, y + h - 8), text, FONT_USE_CASE)
    cases[key] = {"x": x, "y": y, "w": w, "h": h, "cx": x + w / 2, "cy": y + h / 2}


def draw_relation(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int]) -> None:
    draw.line((*start, *end), fill=LINE, width=3)


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(image)

    title = "StudyPlatform 智慧学习平台用例图"
    title_box = draw.textbbox((0, 0), title, font=FONT_TITLE)
    draw.text(((WIDTH - (title_box[2] - title_box[0])) / 2, 40), title, font=FONT_TITLE, fill=INK)

    draw.rectangle((410, 135, 1960, 1555), fill=BOUNDARY_BG, outline=BORDER, width=4)
    draw.text((445, 155), "系统边界：StudyPlatform 智慧学习平台", font=FONT_SMALL, fill=MUTED)

    cases: dict[str, dict[str, float]] = {}

    draw_actor(draw, 180, 180, "访客")
    draw_actor(draw, 180, 570, "注册用户/学习者")
    draw_actor(draw, 180, 980, "教师")
    draw_actor(draw, 2220, 565, "系统管理员")

    draw_inheritance(draw, 180, 570, 180, 468)
    draw_inheritance(draw, 180, 980, 180, 858)

    draw_use_case(draw, cases, "register", 530, 250, 240, 90, "注册账号", SOFT_BLUE)
    draw_use_case(draw, cases, "login", 530, 380, 240, 90, "登录系统", SOFT_BLUE)
    draw_use_case(draw, cases, "browse", 530, 510, 240, 90, "浏览课程与教材", SOFT_BLUE)

    draw_use_case(draw, cases, "join", 880, 250, 250, 90, "加入课程", SOFT_GREEN)
    draw_use_case(draw, cases, "watch", 880, 380, 250, 90, "观看课程", SOFT_GREEN)
    draw_use_case(draw, cases, "assignment", 880, 510, 250, 90, "完成课程作业", SOFT_GREEN)
    draw_use_case(draw, cases, "exam", 880, 640, 250, 90, "参加课程考试", SOFT_GREEN)

    draw_use_case(draw, cases, "question", 1230, 250, 250, 90, "题库练习", SOFT_ORANGE)
    draw_use_case(draw, cases, "mistake", 1230, 380, 250, 90, "错题复习", SOFT_ORANGE)
    draw_use_case(draw, cases, "favorite", 1230, 510, 250, 90, "收藏题目", SOFT_ORANGE)
    draw_use_case(draw, cases, "profile", 1230, 640, 250, 90, "查看学习画像", SOFT_ORANGE)

    draw_use_case(draw, cases, "textbook", 700, 840, 265, 90, "购买精品教材", SOFT_PURPLE)
    draw_use_case(draw, cases, "oj", 700, 970, 265, 90, "在线编程练习 OJ", SOFT_PURPLE)
    draw_use_case(draw, cases, "simulation", 700, 1100, 265, 90, "使用仿真实验", SOFT_PURPLE)
    draw_use_case(draw, cases, "game", 1030, 970, 265, 90, "学习游戏训练", SOFT_PURPLE)
    draw_use_case(draw, cases, "ai", 1030, 1100, 265, 90, "AI 学习助手问答", SOFT_PURPLE)

    draw_use_case(draw, cases, "publish", 530, 1280, 290, 90, "发布在线开放课程", SOFT_GREEN)
    draw_use_case(draw, cases, "ownCourses", 870, 1280, 290, 90, "管理本人发布课程", SOFT_GREEN)

    draw_use_case(draw, cases, "userAdmin", 1600, 430, 250, 85, "用户管理", SOFT_BLUE)
    draw_use_case(draw, cases, "courseAdmin", 1600, 545, 250, 85, "课程管理", SOFT_BLUE)
    draw_use_case(draw, cases, "bankAdmin", 1600, 660, 250, 85, "题库管理", SOFT_BLUE)
    draw_use_case(draw, cases, "questionAdmin", 1600, 775, 250, 85, "题目管理", SOFT_BLUE)
    draw_use_case(draw, cases, "reviewAdmin", 1600, 890, 250, 85, "评论管理", SOFT_BLUE)

    for end in [(530, 295), (530, 425), (530, 555)]:
        draw_relation(draw, (250, 295), end)

    student_lines = [
        (880, 295),
        (880, 425),
        (880, 555),
        (880, 685),
        (1230, 295),
        (1230, 425),
        (1230, 555),
        (1230, 685),
        (700, 885),
        (700, 1015),
        (700, 1145),
        (1030, 1015),
        (1030, 1145),
    ]
    for index, end in enumerate(student_lines):
        draw_relation(draw, (265, 700 + index * 20), end)

    draw_relation(draw, (265, 1090), (530, 1325))
    draw_relation(draw, (265, 1120), (870, 1325))

    for index, end in enumerate([(1850, 472), (1850, 587), (1850, 702), (1850, 817), (1850, 932)]):
        draw_relation(draw, (2155, 690 + index * 30), end)

    draw_arrow_between_cases(draw, cases, "join", "browse", "<<include>>")
    draw_arrow_between_cases(draw, cases, "watch", "profile", "<<include>>")
    draw_arrow_between_cases(draw, cases, "assignment", "profile", "<<include>>")
    draw_arrow_between_cases(draw, cases, "exam", "profile", "<<include>>")
    draw_arrow_between_cases(draw, cases, "mistake", "question", "<<extend>>")
    draw_arrow_between_cases(draw, cases, "favorite", "question", "<<extend>>")
    draw_arrow_between_cases(draw, cases, "ownCourses", "publish", "<<include>>")

    draw.rectangle((1420, 1185, 1875, 1430), fill="#f8fafc", outline=BORDER, width=2)
    draw.text((1450, 1210), "图例", font=FONT_ACTOR, fill=INK)
    draw.text((1450, 1270), "实线：参与者与用例的关联", font=FONT_SMALL, fill=MUTED)
    draw.text((1450, 1315), "空心三角：角色泛化关系", font=FONT_SMALL, fill=MUTED)
    draw.text((1450, 1360), "虚线箭头：包含 / 扩展关系", font=FONT_SMALL, fill=MUTED)

    image.save(OUTPUT_PATH)
    print(OUTPUT_PATH)


if __name__ == "__main__":
    main()
