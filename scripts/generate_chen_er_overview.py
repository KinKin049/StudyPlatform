from __future__ import annotations

from pathlib import Path
from typing import Iterable
import math

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "report-assets"
OUT_PATH = OUT_DIR / "studyplatform-chen-er-overview.png"

WIDTH = 6000
HEIGHT = 4000

BG = "#FFFFFF"
PANEL_FILL = "#FFFFFF"
PANEL_OUTLINE = "#111111"
PANEL_TITLE = "#111111"
TEXT = "#111111"
MUTED = "#555555"
LINE = "#5A5A5A"
ENTITY_FILL = "#FFFFFF"
USER_FILL = "#F1F1F1"
REL_FILL = "#FFFFFF"
ATTR_FILL = "#FFFFFF"
OUTLINE = "#111111"


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        Path(r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc"),
        Path(r"C:\Windows\Fonts\simhei.ttf"),
        Path(r"C:\Windows\Fonts\simsun.ttc"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size=size)
    return ImageFont.load_default()


TITLE_FONT = load_font(72, bold=True)
SUBTITLE_FONT = load_font(36)
PANEL_FONT = load_font(42, bold=True)
ENTITY_FONT = load_font(46, bold=True)
REL_FONT = load_font(40, bold=True)
LEGEND_FONT = load_font(34)
CARD_FONT = load_font(40, bold=True)


class Node:
    def __init__(
        self,
        key: str,
        label: str,
        kind: str,
        x: int,
        y: int,
        w: int = 420,
        h: int = 132,
    ) -> None:
        self.key = key
        self.label = label
        self.kind = kind
        self.x = x
        self.y = y
        self.w = w
        self.h = h

    @property
    def center(self) -> tuple[int, int]:
        return self.x, self.y


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def wrap_label(label: str, max_chars: int) -> list[str]:
    if label == "Type Warrior" or label.startswith("OJ "):
        return [label]
    if len(label) <= max_chars:
        return [label]
    lines: list[str] = []
    current = ""
    for char in label:
        current += char
        if len(current) >= max_chars:
            lines.append(current)
            current = ""
    if current:
        lines.append(current)
    return lines


def draw_centered_text(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    label: str,
    font: ImageFont.FreeTypeFont,
    max_chars: int,
    fill: str = TEXT,
) -> None:
    lines = wrap_label(label, max_chars)
    line_gap = 10
    heights = [text_size(draw, line, font)[1] for line in lines]
    total_h = sum(heights) + line_gap * (len(lines) - 1)
    y = center[1] - total_h / 2
    for line, line_h in zip(lines, heights):
        line_w, _ = text_size(draw, line, font)
        draw.text((center[0] - line_w / 2, y), line, font=font, fill=fill)
        y += line_h + line_gap


def rect_bounds(node: Node) -> tuple[int, int, int, int]:
    return (
        int(node.x - node.w / 2),
        int(node.y - node.h / 2),
        int(node.x + node.w / 2),
        int(node.y + node.h / 2),
    )


def diamond_points(node: Node) -> list[tuple[int, int]]:
    return [
        (node.x, int(node.y - node.h / 2)),
        (int(node.x + node.w / 2), node.y),
        (node.x, int(node.y + node.h / 2)),
        (int(node.x - node.w / 2), node.y),
    ]


def oval_bounds(node: Node) -> tuple[int, int, int, int]:
    return (
        int(node.x - node.w / 2),
        int(node.y - node.h / 2),
        int(node.x + node.w / 2),
        int(node.y + node.h / 2),
    )


def draw_panel(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=24, fill=PANEL_FILL, outline=PANEL_OUTLINE, width=4)
    draw.text((box[0] + 34, box[1] + 24), title, font=PANEL_FONT, fill=PANEL_TITLE)


def redraw_panel_title(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    title_w, title_h = text_size(draw, title, PANEL_FONT)
    title_box = (box[0] + 26, box[1] + 18, box[0] + 52 + title_w, box[1] + 36 + title_h)
    draw.rectangle(title_box, fill=PANEL_FILL)
    draw.text((box[0] + 34, box[1] + 24), title, font=PANEL_FONT, fill=PANEL_TITLE)


def draw_node(draw: ImageDraw.ImageDraw, node: Node) -> None:
    if node.kind == "relation":
        points = diamond_points(node)
        draw.polygon(points, fill=REL_FILL, outline=OUTLINE)
        draw.line(points + [points[0]], fill=OUTLINE, width=5)
        draw_centered_text(draw, node.center, node.label, REL_FONT, 5)
        return

    if node.kind == "attribute":
        draw.ellipse(oval_bounds(node), fill=ATTR_FILL, outline=OUTLINE, width=5)
        draw_centered_text(draw, node.center, node.label, LEGEND_FONT, 5)
        return

    fill = USER_FILL if node.kind == "user" else ENTITY_FILL
    draw.rounded_rectangle(rect_bounds(node), radius=10, fill=fill, outline=OUTLINE, width=5)
    draw_centered_text(draw, node.center, node.label, ENTITY_FONT, 7)


def draw_polyline(draw: ImageDraw.ImageDraw, points: Iterable[tuple[int, int]]) -> None:
    pts = list(points)
    if len(pts) < 2:
        return
    draw.line(pts, fill=LINE, width=4, joint="curve")


def draw_cardinality(
    draw: ImageDraw.ImageDraw,
    label: str,
    position: tuple[float, float],
    offset: tuple[int, int] = (0, 0),
) -> None:
    x = position[0] + offset[0]
    y = position[1] + offset[1]
    label_w, label_h = text_size(draw, label, CARD_FONT)
    padding_x = 12
    padding_y = 6
    draw.rectangle(
        (
            x - label_w / 2 - padding_x,
            y - label_h / 2 - padding_y,
            x + label_w / 2 + padding_x,
            y + label_h / 2 + padding_y,
        ),
        fill=BG,
    )
    draw.text((x - label_w / 2, y - label_h / 2), label, font=CARD_FONT, fill=TEXT)


def interpolate(
    start: tuple[int, int],
    end: tuple[int, int],
    ratio: float,
) -> tuple[float, float]:
    return (
        start[0] + (end[0] - start[0]) * ratio,
        start[1] + (end[1] - start[1]) * ratio,
    )


def unit_vector(start: tuple[int, int], end: tuple[int, int]) -> tuple[float, float]:
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    distance = math.hypot(dx, dy)
    if distance == 0:
        return (1.0, 0.0)
    return (dx / distance, dy / distance)


def cardinality_position_outside_entity(
    node: Node,
    entity_point: tuple[int, int],
    other_point: tuple[int, int],
    is_user_anchor: bool,
) -> tuple[float, float]:
    ux, uy = unit_vector(entity_point, other_point)
    if is_user_anchor:
        return (entity_point[0] + ux * 168, entity_point[1] + uy * 168)

    half_w = node.w / 2
    half_h = node.h / 2
    x_distance = half_w / abs(ux) if abs(ux) > 0.001 else float("inf")
    y_distance = half_h / abs(uy) if abs(uy) > 0.001 else float("inf")
    boundary_distance = min(x_distance, y_distance)
    label_distance = boundary_distance + 46
    return (
        node.x + ux * label_distance,
        node.y + uy * label_distance,
    )


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(image)

    title = "在线学堂系统 Chen ER 总览图（含基数）"
    title_w, _ = text_size(draw, title, TITLE_FONT)
    draw.text(((WIDTH - title_w) / 2, 72), title, font=TITLE_FONT, fill=TEXT)

    subtitle = "总览图展示核心实体、联系与 1 / n / m 基数；分模块图再展开数据库表级属性"
    subtitle_w, _ = text_size(draw, subtitle, SUBTITLE_FONT)
    draw.text(((WIDTH - subtitle_w) / 2, 170), subtitle, font=SUBTITLE_FONT, fill=MUTED)

    panels = [
        ((150, 360, 1540, 1460), "用户认证与学习画像"),
        ((1730, 360, 3920, 1460), "课程资源与教材交易"),
        ((4200, 360, 5850, 1660), "题库、作业与考试"),
        ((150, 2480, 1900, 3740), "游戏金币"),
        ((2120, 2480, 3860, 3740), "仿真实验与测井"),
        ((4100, 2100, 5850, 3740), "OJ 在线评测"),
    ]
    for panel, panel_title in panels:
        draw_panel(draw, panel, panel_title)

    # Legend
    draw.rounded_rectangle((170, 88, 1640, 230), radius=18, fill="#FFFFFF", outline=OUTLINE, width=3)
    legend_nodes = [
        Node("legend_entity", "实体", "entity", 330, 159, 220, 80),
        Node("legend_relation", "联系", "relation", 700, 159, 230, 110),
        Node("legend_attribute", "属性", "attribute", 1040, 159, 240, 86),
    ]
    for legend_node in legend_nodes:
        draw_node(draw, legend_node)
    draw.text((1200, 118), "线旁 1/n/m 表示基数", font=LEGEND_FONT, fill=MUTED)
    draw.text((1200, 164), "属性在分模块图展开", font=LEGEND_FONT, fill=MUTED)

    nodes: dict[str, Node] = {}

    def add(key: str, label: str, kind: str, x: int, y: int, w: int = 420, h: int = 132) -> None:
        nodes[key] = Node(key, label, kind, x, y, w, h)

    # Core entity
    add("user", "用户", "user", 3000, 1850, 430, 142)

    # User/profile
    add("profile", "用户画像", "entity", 520, 650)
    add("reset", "找回密码验证码", "entity", 520, 930, 490)
    add("learning_record", "学习记录", "entity", 520, 1210)
    add("own_profile", "拥有", "relation", 1260, 650, 260, 152)
    add("apply_reset", "申请", "relation", 1260, 930, 260, 152)
    add("record_learning", "记录", "relation", 1260, 1210, 260, 152)

    # Course/textbook
    add("course", "课程资源", "entity", 2460, 650)
    add("teacher_course", "教师发布课程", "entity", 3460, 650, 470)
    add("textbook", "精品教材", "entity", 2460, 1160)
    add("order", "教材订单", "entity", 3460, 1160)
    add("learn_course", "学习", "relation", 1960, 900, 260, 152)
    add("review_course", "评价", "relation", 2220, 1320, 260, 152)
    add("publish_course", "发布", "relation", 3460, 920, 260, 152)
    add("merge_course", "纳入", "relation", 2960, 650, 260, 152)
    add("buy_textbook", "购买", "relation", 3820, 1180, 260, 152)
    add("contain_textbook", "包含", "relation", 2960, 1160, 260, 152)
    add("support_course", "配套", "relation", 2460, 905, 260, 152)

    # Question/exam
    add("question_bank", "题库资源", "entity", 4510, 720)
    add("question", "题目资源", "entity", 5410, 720)
    add("assignment_exam", "作业与考试", "entity", 5410, 1280, 430)
    add("practice", "练习", "relation", 4200, 1050, 260, 152)
    add("course_bank", "配套题库", "relation", 4620, 1250, 330, 166)
    add("course_exam", "组织", "relation", 4960, 1280, 260, 152)
    add("question_part", "包含", "relation", 5410, 1000, 260, 152)

    # OJ
    add("oj_problem", "OJ 题目", "entity", 4470, 2380)
    add("oj_case", "OJ 测试用例", "entity", 5400, 2380, 450)
    add("submission", "代码提交", "entity", 4470, 3260)
    add("judge_result", "评测结果", "entity", 5400, 3260)
    add("create_oj", "创建", "relation", 4120, 2380, 260, 152)
    add("set_case", "设置用例", "relation", 4935, 2380, 330, 166)
    add("submit_code", "提交代码", "relation", 4120, 2860, 330, 166)
    add("make_result", "产生结果", "relation", 4935, 3260, 330, 166)
    add("ref_oj", "引用 OJ", "relation", 4470, 2820, 330, 166)

    # Game/coin
    add("ladder", "万题天梯跳", "entity", 540, 2820)
    add("type_warrior", "Type Warrior", "entity", 1320, 2820, 460)
    add("coin_reward", "金币奖励记录", "entity", 920, 3480, 470)
    add("play_game", "游玩", "relation", 1720, 2960, 260, 152)
    add("make_coin", "产生奖励", "relation", 920, 3140, 330, 166)
    add("gain_coin", "获得金币", "relation", 1740, 3480, 330, 166)

    # Simulation/well logging
    add("simulation", "仿真实验", "entity", 2440, 2820)
    add("well_template", "测井模板", "entity", 3480, 2820)
    add("well_record", "测井解释", "entity", 3480, 3480)
    add("run_sim", "执行", "relation", 2920, 2820, 260, 152)
    add("generate_well", "生成", "relation", 2920, 3480, 260, 152)
    add("template_help", "辅助生成", "relation", 3480, 3150, 330, 166)

    connections: list[tuple[str, str, list[tuple[int, int]], str]] = [
        # profile
        ("user", "own_profile", [], "1"),
        ("own_profile", "profile", [], "1"),
        ("user", "apply_reset", [], "1"),
        ("apply_reset", "reset", [], "n"),
        ("user", "record_learning", [], "1"),
        ("record_learning", "learning_record", [], "n"),
        # course
        ("user", "learn_course", [], "m"),
        ("learn_course", "course", [], "n"),
        ("user", "review_course", [], "m"),
        ("review_course", "course", [], "n"),
        ("user", "publish_course", [], "1"),
        ("publish_course", "teacher_course", [], "n"),
        ("teacher_course", "merge_course", [], "1"),
        ("merge_course", "course", [], "1"),
        ("user", "buy_textbook", [], "1"),
        ("buy_textbook", "order", [], "n"),
        ("order", "contain_textbook", [], "m"),
        ("contain_textbook", "textbook", [], "n"),
        ("textbook", "support_course", [], "m"),
        ("support_course", "course", [], "n"),
        # question/exam
        ("user", "practice", [], "m"),
        ("practice", "question_bank", [], "n"),
        ("course", "course_bank", [(3720, 820), (4300, 1160)], "1"),
        ("course_bank", "question_bank", [], "n"),
        ("course", "course_exam", [(3920, 690), (4700, 1180)], "1"),
        ("course_exam", "assignment_exam", [], "n"),
        ("assignment_exam", "question_part", [], "1"),
        ("question_part", "question", [], "n"),
        # OJ
        ("user", "create_oj", [], "1"),
        ("create_oj", "oj_problem", [], "n"),
        ("oj_problem", "set_case", [], "1"),
        ("set_case", "oj_case", [], "n"),
        ("user", "submit_code", [], "1"),
        ("submit_code", "oj_problem", [], "1"),
        ("submit_code", "submission", [], "n"),
        ("submission", "make_result", [], "1"),
        ("make_result", "judge_result", [], "n"),
        ("question", "ref_oj", [(5200, 1700), (4700, 2480)], "n"),
        ("assignment_exam", "ref_oj", [(5200, 1780), (4700, 2580)], "n"),
        ("ref_oj", "oj_problem", [], "1"),
        # game/coin
        ("user", "play_game", [], "m"),
        ("play_game", "ladder", [], "n"),
        ("play_game", "type_warrior", [], "n"),
        ("ladder", "make_coin", [], "1"),
        ("type_warrior", "make_coin", [], "1"),
        ("make_coin", "coin_reward", [], "n"),
        ("user", "gain_coin", [], "1"),
        ("gain_coin", "coin_reward", [], "n"),
        # simulation/well
        ("user", "run_sim", [], "m"),
        ("run_sim", "simulation", [], "n"),
        ("user", "generate_well", [], "1"),
        ("generate_well", "well_record", [], "n"),
        ("well_template", "template_help", [], "1"),
        ("template_help", "well_record", [], "n"),
    ]

    user_anchor = {
        "own_profile": (2792, 1782),
        "apply_reset": (2840, 1782),
        "record_learning": (2786, 1845),
        "learn_course": (2900, 1782),
        "review_course": (2950, 1782),
        "publish_course": (3000, 1782),
        "buy_textbook": (3050, 1782),
        "practice": (3214, 1794),
        "create_oj": (3214, 1845),
        "submit_code": (3214, 1900),
        "play_game": (2786, 1885),
        "gain_coin": (2786, 1918),
        "run_sim": (2925, 1918),
        "generate_well": (3060, 1918),
    }

    drawn_connections: list[tuple[str, str, list[tuple[int, int]], str, tuple[int, int], tuple[int, int]]] = []

    for start, end, waypoints, cardinality in connections:
        start_point = user_anchor.get(end, nodes[start].center) if start == "user" else nodes[start].center
        end_point = user_anchor.get(start, nodes[end].center) if end == "user" else nodes[end].center
        draw_polyline(draw, [start_point, *waypoints, end_point])
        drawn_connections.append((start, end, waypoints, cardinality, start_point, end_point))

    for node in nodes.values():
        draw_node(draw, node)

    label_offsets = {
        ("user", "own_profile"): (-20, -40),
        ("user", "apply_reset"): (-8, -26),
        ("user", "record_learning"): (-22, 32),
        ("user", "learn_course"): (-4, -38),
        ("user", "review_course"): (-2, -18),
        ("user", "publish_course"): (10, -40),
        ("user", "buy_textbook"): (20, -18),
        ("user", "practice"): (36, -36),
        ("user", "create_oj"): (48, -18),
        ("user", "submit_code"): (42, 14),
        ("user", "play_game"): (-38, 8),
        ("user", "gain_coin"): (-34, 36),
        ("user", "run_sim"): (-6, 44),
        ("user", "generate_well"): (34, 44),
        ("course", "learn_course"): (-26, -26),
        ("course", "support_course"): (0, 34),
        ("course", "merge_course"): (0, -34),
        ("course", "course_bank"): (34, -22),
        ("course", "course_exam"): (46, 28),
    }

    for start, end, waypoints, cardinality, start_point, end_point in drawn_connections:
        points = [start_point, *waypoints, end_point]
        if nodes[start].kind == "relation":
            entity_key = end
            relation_key = start
            entity_point = end_point
            other_point = points[-2]
        elif nodes[end].kind == "relation":
            entity_key = start
            relation_key = end
            entity_point = start_point
            other_point = points[1]
        else:
            continue
        label_position = cardinality_position_outside_entity(
            nodes[entity_key],
            entity_point,
            other_point,
            entity_key == "user",
        )
        draw_cardinality(
            draw,
            cardinality,
            label_position,
            label_offsets.get((entity_key, relation_key), (0, 0)),
        )

    for panel, panel_title in panels:
        redraw_panel_title(draw, panel, panel_title)

    footer = "说明：线旁 1/n/m 表示实体参与联系的基数；本总览图不展开属性，后续分模块图将覆盖具体数据库对象和属性。"
    footer_w, _ = text_size(draw, footer, SUBTITLE_FONT)
    draw.text(((WIDTH - footer_w) / 2, HEIGHT - 142), footer, font=SUBTITLE_FONT, fill=MUTED)

    image.save(OUT_PATH, quality=95)
    print(OUT_PATH)


if __name__ == "__main__":
    main()
