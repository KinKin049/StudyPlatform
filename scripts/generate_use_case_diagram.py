from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "docs" / "report-assets"
OUTPUT_PNG = OUTPUT_DIR / "studyplatform-use-case-diagram.png"
OUTPUT_V2_PNG = OUTPUT_DIR / "studyplatform-use-case-diagram-v2.png"
OUTPUT_JPG = OUTPUT_DIR / "studyplatform-use-case-diagram-v2.jpg"

WIDTH = 3400
HEIGHT = 2380

INK = "#1f2937"
MUTED = "#64748b"
LINE = "#475569"
RELATION_LINE = "#334155"
BLUE = "#2563eb"
GREEN = "#16a34a"
ORANGE = "#ea580c"
PURPLE = "#7c3aed"
TEAL = "#0d9488"
AMBER = "#d97706"
BORDER = "#94a3b8"
BG = "#ffffff"
BOUNDARY_BG = "#fcfdff"
SOFT_BLUE = "#eff6ff"
SOFT_GREEN = "#f0fdf4"
SOFT_ORANGE = "#fff7ed"
SOFT_PURPLE = "#faf5ff"
SOFT_TEAL = "#f0fdfa"
SOFT_AMBER = "#fffbeb"
SOFT_GRAY = "#f8fafc"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    names = ["msyhbd.ttc", "msyh.ttc", "simhei.ttf"] if bold else ["msyh.ttc", "simhei.ttf"]
    for name in names:
        path = Path("C:/Windows/Fonts") / name
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


FONT_TITLE = font(54, True)
FONT_SECTION = font(27, True)
FONT_ACTOR = font(31, True)
FONT_CASE = font(27)
FONT_CASE_SMALL = font(23)
FONT_LABEL = font(24, True)
FONT_SMALL = font(20)

RELATION_LABELS: list[tuple[float, float, str]] = []


def text_size(draw: ImageDraw.ImageDraw, text: str, text_font) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=text_font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def center_text(draw: ImageDraw.ImageDraw, box: tuple[float, float, float, float], text: str, text_font, fill=INK) -> None:
    lines = text.split("\n")
    sizes = [text_size(draw, line, text_font) for line in lines]
    total_height = sum(height for _, height in sizes) + max(0, len(lines) - 1) * 8
    x1, y1, x2, y2 = box
    y = y1 + ((y2 - y1) - total_height) / 2
    for line, (width, height) in zip(lines, sizes):
        draw.text((x1 + ((x2 - x1) - width) / 2, y), line, font=text_font, fill=fill)
        y += height + 8


def rounded_label(draw: ImageDraw.ImageDraw, x: float, y: float, text: str) -> None:
    width, height = text_size(draw, text, FONT_LABEL)
    pad_x = 18
    pad_y = 10
    box = (x - width / 2 - pad_x, y - height / 2 - pad_y, x + width / 2 + pad_x, y + height / 2 + pad_y)
    shadow = (box[0] + 4, box[1] + 4, box[2] + 4, box[3] + 4)
    draw.rounded_rectangle(shadow, radius=14, fill="#e2e8f0")
    draw.rounded_rectangle(box, radius=14, fill=BG, outline="#64748b", width=3)
    draw.text((x - width / 2, y - height / 2), text, font=FONT_LABEL, fill=INK)


def draw_actor(draw: ImageDraw.ImageDraw, x: int, y: int, name: str) -> None:
    draw.ellipse((x - 34, y, x + 34, y + 68), outline=INK, width=6)
    draw.line((x, y + 68, x, y + 178), fill=INK, width=6)
    draw.line((x - 78, y + 108, x + 78, y + 108), fill=INK, width=6)
    draw.line((x, y + 178, x - 72, y + 280), fill=INK, width=6)
    draw.line((x, y + 178, x + 72, y + 280), fill=INK, width=6)
    center_text(draw, (x - 175, y + 292, x + 175, y + 350), name, FONT_ACTOR)


def draw_inheritance(draw: ImageDraw.ImageDraw, x1: int, y1: int, x2: int, y2: int) -> None:
    draw.line((x1, y1, x2, y2), fill=LINE, width=4)
    triangle = [(x2, y2), (x2 - 24, y2 + 42), (x2 + 24, y2 + 42)]
    draw.polygon(triangle, fill=BG, outline=LINE)


def ellipse_edge(cx: float, cy: float, rx: float, ry: float, tx: float, ty: float) -> tuple[float, float]:
    dx = tx - cx
    dy = ty - cy
    if dx == 0 and dy == 0:
        return cx, cy
    scale = 1 / math.sqrt((dx * dx) / (rx * rx) + (dy * dy) / (ry * ry))
    return cx + dx * scale, cy + dy * scale


def case_edge(case: dict[str, float], target: tuple[float, float]) -> tuple[float, float]:
    return ellipse_edge(case["cx"], case["cy"], case["w"] / 2, case["h"] / 2, target[0], target[1])


def draw_case(draw: ImageDraw.ImageDraw, cases: dict[str, dict[str, float]], key: str, x: int, y: int, w: int, h: int, text: str, fill: str, outline: str = BLUE, small: bool = False) -> None:
    draw.ellipse((x, y, x + w, y + h), fill=fill, outline=outline, width=4)
    center_text(draw, (x + 12, y + 8, x + w - 12, y + h - 8), text, FONT_CASE_SMALL if small else FONT_CASE)
    cases[key] = {"x": x, "y": y, "w": w, "h": h, "cx": x + w / 2, "cy": y + h / 2}


def draw_section(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str, color: str) -> None:
    draw.rounded_rectangle(box, radius=24, fill="#ffffff", outline="#d7dee9", width=2)
    draw.rounded_rectangle((box[0], box[1], box[2], box[1] + 56), radius=24, fill=color, outline=color)
    draw.rectangle((box[0], box[1] + 30, box[2], box[1] + 56), fill=color)
    draw.text((box[0] + 24, box[1] + 13), title, font=FONT_SECTION, fill=INK)


def draw_polyline(draw: ImageDraw.ImageDraw, points: list[tuple[float, float]], fill: str, width: int = 4, dash: bool = False) -> None:
    if not dash:
        draw.line(points, fill=fill, width=width, joint="curve")
        return
    for start, end in zip(points[:-1], points[1:]):
        x1, y1 = start
        x2, y2 = end
        length = math.hypot(x2 - x1, y2 - y1)
        if length == 0:
            continue
        distance = 0
        dash_len = 18
        gap = 13
        while distance < length:
            next_distance = min(distance + dash_len, length)
            sx = x1 + (x2 - x1) * distance / length
            sy = y1 + (y2 - y1) * distance / length
            ex = x1 + (x2 - x1) * next_distance / length
            ey = y1 + (y2 - y1) * next_distance / length
            draw.line((sx, sy, ex, ey), fill=fill, width=width)
            distance += dash_len + gap


def arrowhead(draw: ImageDraw.ImageDraw, previous: tuple[float, float], end: tuple[float, float], fill: str) -> None:
    angle = math.atan2(end[1] - previous[1], end[0] - previous[0])
    size = 18
    left = (end[0] - size * math.cos(angle - math.pi / 6), end[1] - size * math.sin(angle - math.pi / 6))
    right = (end[0] - size * math.cos(angle + math.pi / 6), end[1] - size * math.sin(angle + math.pi / 6))
    draw.polygon([end, left, right], fill=fill)


def association(draw: ImageDraw.ImageDraw, actor_point: tuple[int, int], bus_x: int, case: dict[str, float], side: str = "left") -> None:
    if side == "left":
        end = case_edge(case, (bus_x, case["cy"]))
    else:
        end = case_edge(case, (bus_x, case["cy"]))
    points = [actor_point, (bus_x, actor_point[1]), (bus_x, case["cy"]), end]
    draw_polyline(draw, points, LINE, width=3)


def relation(
    draw: ImageDraw.ImageDraw,
    cases: dict[str, dict[str, float]],
    source_key: str,
    target_key: str,
    label: str,
    route: list[tuple[float, float]] | None = None,
    label_at: tuple[float, float] | None = None,
) -> None:
    source = cases[source_key]
    target = cases[target_key]
    if route:
        start = case_edge(source, route[0])
        end = case_edge(target, route[-1])
        points = [start, *route, end]
    else:
        start = case_edge(source, (target["cx"], target["cy"]))
        end = case_edge(target, (source["cx"], source["cy"]))
        points = [start, end]
    draw_polyline(draw, points, RELATION_LINE, width=5, dash=True)
    arrowhead(draw, points[-2], points[-1], RELATION_LINE)
    if label_at is None:
        if len(points) >= 4:
            label_at = points[len(points) // 2]
        else:
            label_at = ((points[0][0] + points[-1][0]) / 2, (points[0][1] + points[-1][1]) / 2)
    RELATION_LABELS.append((label_at[0], label_at[1], label))


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    RELATION_LABELS.clear()
    image = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(image)
    cases: dict[str, dict[str, float]] = {}

    title = "StudyPlatform 智慧学习平台用例图"
    title_width, _ = text_size(draw, title, FONT_TITLE)
    draw.text(((WIDTH - title_width) / 2, 42), title, font=FONT_TITLE, fill=INK)

    boundary = (470, 145, 2920, 2245)
    draw.rectangle(boundary, fill=BOUNDARY_BG, outline=BORDER, width=5)
    draw.text((515, 170), "系统边界：StudyPlatform 智慧学习平台", font=FONT_SMALL, fill=MUTED)

    draw_actor(draw, 185, 190, "访客")
    draw_actor(draw, 185, 825, "注册用户/学习者")
    draw_actor(draw, 185, 1650, "教师")
    draw_actor(draw, 3190, 900, "系统管理员")
    draw_inheritance(draw, 185, 820, 185, 620)
    draw_inheritance(draw, 185, 1645, 185, 1255)

    draw_section(draw, (545, 245, 1060, 1838), "学习者核心用例", SOFT_BLUE)
    draw_section(draw, (1135, 245, 1698, 1838), "扩展与被包含用例", SOFT_GREEN)
    draw_section(draw, (1775, 245, 2215, 1838), "金币系统用例", SOFT_AMBER)
    draw_section(draw, (640, 1900, 1508, 2172), "教师课程建设用例", SOFT_TEAL)
    draw_section(draw, (2310, 420, 2810, 1420), "管理员后台用例", SOFT_PURPLE)

    main_cases = [
        ("register", "注册账号", 330, SOFT_BLUE),
        ("login", "登录系统", 430, SOFT_BLUE),
        ("browse", "浏览课程与教材", 530, SOFT_BLUE),
        ("join", "加入课程", 630, SOFT_GREEN),
        ("watch", "观看课程", 730, SOFT_GREEN),
        ("my_class", "查看我的课程/班级", 830, SOFT_GREEN),
        ("assignment", "完成课程作业", 930, SOFT_GREEN),
        ("exam", "参加课程考试", 1030, SOFT_GREEN),
        ("question", "题库练习", 1130, SOFT_ORANGE),
        ("mistake", "错题复习", 1230, SOFT_ORANGE),
        ("favorite", "收藏题目", 1330, SOFT_ORANGE),
        ("textbook", "购买精品教材", 1430, SOFT_PURPLE),
        ("oj", "在线编程练习 OJ", 1530, SOFT_PURPLE),
        ("simulation", "使用仿真实验", 1630, SOFT_PURPLE),
        ("game", "学习游戏训练", 1730, SOFT_PURPLE),
    ]
    for key, text, cy, fill in main_cases:
        draw_case(draw, cases, key, 625, cy - 38, 340, 76, text, fill)

    draw_case(draw, cases, "ai", 1162, 340, 360, 76, "AI 学习助手问答", SOFT_PURPLE)
    draw_case(draw, cases, "learning_time", 1162, 660, 360, 76, "记录学习时长", SOFT_GREEN)
    draw_case(draw, cases, "submit_assignment", 1162, 865, 360, 76, "提交作业", SOFT_GREEN)
    draw_case(draw, cases, "submit_exam", 1162, 965, 360, 76, "提交考试", SOFT_GREEN)
    draw_case(draw, cases, "answer_feedback", 1162, 1088, 360, 76, "答题判定与解析", SOFT_ORANGE)
    draw_case(draw, cases, "textbook_order", 1162, 1392, 360, 76, "提交购书订单", SOFT_PURPLE)
    draw_case(draw, cases, "game_record", 1162, 1692, 360, 76, "保存游戏记录", SOFT_PURPLE)

    draw_case(draw, cases, "coin_earn", 1818, 505, 350, 78, "获得学习金币", SOFT_AMBER, AMBER)
    draw_case(draw, cases, "coin_balance", 1818, 630, 350, 78, "查看金币余额", SOFT_AMBER, AMBER)
    draw_case(draw, cases, "coin_rules", 1818, 755, 350, 78, "查看金币获取规则", SOFT_AMBER, AMBER)
    draw_case(draw, cases, "exchange", 1818, 880, 350, 78, "金币兑换中心", SOFT_AMBER, AMBER)
    draw_case(draw, cases, "exchange_items", 1818, 1005, 350, 78, "浏览兑换权益", SOFT_AMBER, AMBER)
    draw_case(draw, cases, "exchange_reserved", 1818, 1130, 350, 78, "兑换学习权益\n（预留）", SOFT_AMBER, AMBER, small=True)
    draw_case(draw, cases, "profile", 1818, 1370, 350, 78, "查看学习画像", SOFT_ORANGE, ORANGE)

    draw_case(draw, cases, "publish", 725, 1982, 360, 78, "发布在线开放课程", SOFT_TEAL, TEAL)
    draw_case(draw, cases, "own_courses", 1095, 1982, 360, 78, "管理本人发布课程", SOFT_TEAL, TEAL)

    admin_cases = [
        ("admin_user", "用户管理", 520),
        ("admin_course", "课程管理", 640),
        ("admin_bank", "题库管理", 760),
        ("admin_question", "题目管理", 880),
        ("admin_review", "评论管理", 1000),
        ("admin_order", "购书订单管理", 1120),
        ("admin_coin", "金币调整管理", 1240),
    ]
    for key, text, cy in admin_cases:
        draw_case(draw, cases, key, 2384, cy - 38, 330, 76, text, SOFT_PURPLE, PURPLE)

    # Actor associations are routed through side channels, not through use-case ellipses.
    guest_point = (258, 305)
    student_point = (272, 1060)
    teacher_point = (272, 1770)
    admin_point = (3098, 1080)
    left_bus = 520
    right_bus = 2860

    for key in ["register", "login", "browse"]:
        association(draw, guest_point, left_bus, cases[key])
    for key in [
        "browse",
        "join",
        "watch",
        "my_class",
        "assignment",
        "exam",
        "question",
        "mistake",
        "favorite",
        "textbook",
        "oj",
        "simulation",
        "game",
        "ai",
        "coin_earn",
        "exchange",
        "profile",
    ]:
        association(draw, student_point, left_bus, cases[key])
    for key in ["publish", "own_courses"]:
        association(draw, teacher_point, left_bus, cases[key])
    for key, _, _ in admin_cases:
        association(draw, admin_point, right_bus, cases[key], side="right")

    # Include/extend relations use open corridors and white label boxes.
    relation(draw, cases, "join", "browse", "<<include>>", [(1030, 630), (1030, 530)], label_at=(1032, 580))
    relation(draw, cases, "watch", "learning_time", "<<include>>", [(1080, 730), (1080, 698)], label_at=(1090, 716))
    relation(draw, cases, "assignment", "submit_assignment", "<<include>>", [(1085, 930), (1085, 903)], label_at=(1090, 916))
    relation(draw, cases, "exam", "submit_exam", "<<include>>", [(1085, 1030), (1085, 1003)], label_at=(1090, 1016))
    relation(draw, cases, "question", "answer_feedback", "<<include>>", [(1085, 1130), (1085, 1126)], label_at=(1088, 1128))
    relation(draw, cases, "mistake", "question", "<<extend>>", [(1045, 1230), (1045, 1130)], label_at=(1036, 1180))
    relation(draw, cases, "favorite", "question", "<<extend>>", [(1095, 1330), (1095, 1130)], label_at=(1100, 1238))
    relation(draw, cases, "textbook", "textbook_order", "<<include>>", [(1085, 1430), (1085, 1430)], label_at=(1088, 1430))
    relation(draw, cases, "game", "game_record", "<<include>>", [(1085, 1730), (1085, 1730)], label_at=(1088, 1730))

    relation(draw, cases, "learning_time", "coin_earn", "<<include>>", [(1605, 698), (1605, 544)], label_at=(1605, 635))
    relation(draw, cases, "answer_feedback", "coin_earn", "<<include>>", [(1605, 1126), (1605, 544)], label_at=(1605, 938))
    relation(draw, cases, "game_record", "coin_earn", "<<include>>", [(1605, 1730), (1605, 544)], label_at=(1605, 1362))
    relation(draw, cases, "exchange", "coin_balance", "<<include>>", [(1765, 919), (1765, 669)], label_at=(1765, 788))
    relation(draw, cases, "exchange", "coin_rules", "<<include>>", [(2205, 919), (2205, 794)], label_at=(2205, 856))
    relation(draw, cases, "exchange", "exchange_items", "<<include>>", [(2205, 919), (2205, 1044)], label_at=(2205, 980))
    relation(draw, cases, "exchange_reserved", "exchange", "<<extend>>", [(1765, 1169), (1765, 919)], label_at=(1765, 1042))
    relation(draw, cases, "profile", "coin_balance", "<<include>>", [(2208, 1409), (2208, 669)], label_at=(2208, 1255))

    relation(draw, cases, "own_courses", "publish", "<<include>>", [(1090, 2098), (905, 2098)], label_at=(998, 2085))
    relation(draw, cases, "admin_coin", "admin_user", "<<extend>>", [(2750, 1240), (2750, 520)], label_at=(2838, 820))

    # Use cases are redrawn after association lines so connectors never cover text.
    for key, text, cy, fill in main_cases:
        draw_case(draw, cases, key, 625, cy - 38, 340, 76, text, fill)
    for key, text, box_fill, outline, small in [
        ("ai", "AI 学习助手问答", SOFT_PURPLE, PURPLE, False),
        ("learning_time", "记录学习时长", SOFT_GREEN, GREEN, False),
        ("submit_assignment", "提交作业", SOFT_GREEN, GREEN, False),
        ("submit_exam", "提交考试", SOFT_GREEN, GREEN, False),
        ("answer_feedback", "答题判定与解析", SOFT_ORANGE, ORANGE, False),
        ("textbook_order", "提交购书订单", SOFT_PURPLE, PURPLE, False),
        ("game_record", "保存游戏记录", SOFT_PURPLE, PURPLE, False),
    ]:
        old = cases[key]
        draw_case(draw, cases, key, int(old["x"]), int(old["y"]), int(old["w"]), int(old["h"]), text, box_fill, outline, small)
    for key, text, small in [
        ("coin_earn", "获得学习金币", False),
        ("coin_balance", "查看金币余额", False),
        ("coin_rules", "查看金币获取规则", False),
        ("exchange", "金币兑换中心", False),
        ("exchange_items", "浏览兑换权益", False),
        ("exchange_reserved", "兑换学习权益\n（预留）", True),
        ("profile", "查看学习画像", False),
    ]:
        old = cases[key]
        draw_case(draw, cases, key, int(old["x"]), int(old["y"]), int(old["w"]), int(old["h"]), text, SOFT_AMBER if key.startswith("coin") or key.startswith("exchange") else SOFT_ORANGE, AMBER if key.startswith("coin") or key.startswith("exchange") else ORANGE, small)
    draw_case(draw, cases, "publish", 725, 1982, 360, 78, "发布在线开放课程", SOFT_TEAL, TEAL)
    draw_case(draw, cases, "own_courses", 1095, 1982, 360, 78, "管理本人发布课程", SOFT_TEAL, TEAL)
    for key, text, cy in admin_cases:
        draw_case(draw, cases, key, 2384, cy - 38, 330, 76, text, SOFT_PURPLE, PURPLE)

    for x, y, label in RELATION_LABELS:
        rounded_label(draw, x, y, label)

    # Legend.
    legend = (2290, 1560, 2820, 2040)
    draw.rounded_rectangle(legend, radius=18, fill=SOFT_GRAY, outline=BORDER, width=2)
    draw.text((2330, 1600), "图例", font=FONT_SECTION, fill=INK)
    draw.line((2340, 1665, 2490, 1665), fill=LINE, width=4)
    draw.text((2520, 1648), "实线：参与者与用例关联", font=FONT_SMALL, fill=MUTED)
    draw_inheritance(draw, 2365, 1758, 2365, 1700)
    draw.text((2520, 1705), "空心三角：角色泛化", font=FONT_SMALL, fill=MUTED)
    draw_polyline(draw, [(2340, 1818), (2490, 1818)], RELATION_LINE, width=5, dash=True)
    arrowhead(draw, (2440, 1818), (2490, 1818), RELATION_LINE)
    rounded_label(draw, 2415, 1860, "<<include>>")
    draw.text((2520, 1800), "包含关系", font=FONT_SMALL, fill=MUTED)
    draw_polyline(draw, [(2340, 1950), (2490, 1950)], RELATION_LINE, width=5, dash=True)
    arrowhead(draw, (2440, 1950), (2490, 1950), RELATION_LINE)
    rounded_label(draw, 2415, 1992, "<<extend>>")
    draw.text((2520, 1932), "扩展关系 / 预留能力", font=FONT_SMALL, fill=MUTED)

    note = "金币依据：学习时长、答题正确、游戏结算、后台调整；兑换权益当前按项目页面表现为展示/预留能力。"
    draw.text((560, 2195), note, font=FONT_SMALL, fill=MUTED)

    image.save(OUTPUT_PNG)
    image.save(OUTPUT_V2_PNG)
    image.save(OUTPUT_JPG, quality=95)
    print(OUTPUT_PNG)
    print(OUTPUT_V2_PNG)
    print(OUTPUT_JPG)


if __name__ == "__main__":
    main()
