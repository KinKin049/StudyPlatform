from __future__ import annotations

import math
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "report-assets"

BG = "#FFFFFF"
TEXT = "#111111"
MUTED = "#555555"
LINE = "#565656"
OUTLINE = "#111111"
USER_FILL = "#F1F1F1"


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


TITLE_FONT = load_font(68, bold=True)
SUBTITLE_FONT = load_font(34)
ENTITY_FONT = load_font(43, bold=True)
REL_FONT = load_font(38, bold=True)
ATTR_FONT = load_font(32)
ATTR_KEY_FONT = load_font(32, bold=True)
CARD_FONT = load_font(38, bold=True)
LEGEND_FONT = load_font(31)


class Node:
    def __init__(
        self,
        key: str,
        label: str,
        kind: str,
        x: int,
        y: int,
        w: int = 430,
        h: int = 132,
        is_key: bool = False,
    ) -> None:
        self.key = key
        self.label = label
        self.kind = kind
        self.x = x
        self.y = y
        self.w = w
        self.h = h
        self.is_key = is_key

    @property
    def center(self) -> tuple[int, int]:
        return self.x, self.y


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def wrap_label(label: str, max_chars: int) -> list[str]:
    if label == "Type Warrior":
        return [label]
    if label.startswith("Type Warrior"):
        rest = label.replace("Type Warrior", "", 1).strip()
        return ["Type Warrior"] + ([rest] if rest else [])
    if label in {"OJ 题目", "OJ 测试用例"}:
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
    line_gap = 8
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


def oval_bounds(node: Node) -> tuple[int, int, int, int]:
    return rect_bounds(node)


def diamond_points(node: Node) -> list[tuple[int, int]]:
    return [
        (node.x, int(node.y - node.h / 2)),
        (int(node.x + node.w / 2), node.y),
        (node.x, int(node.y + node.h / 2)),
        (int(node.x - node.w / 2), node.y),
    ]


def draw_node(draw: ImageDraw.ImageDraw, node: Node) -> None:
    if node.kind == "relation":
        points = diamond_points(node)
        draw.polygon(points, fill=BG, outline=OUTLINE)
        draw.line(points + [points[0]], fill=OUTLINE, width=5)
        draw_centered_text(draw, node.center, node.label, REL_FONT, 5)
        return

    if node.kind == "attribute":
        draw.ellipse(oval_bounds(node), fill=BG, outline=OUTLINE, width=4)
        draw_centered_text(
            draw,
            node.center,
            node.label,
            ATTR_KEY_FONT if node.is_key else ATTR_FONT,
            6,
        )
        if node.is_key:
            underline_y = node.y + int(node.h * 0.22)
            draw.line(
                (node.x - int(node.w * 0.26), underline_y, node.x + int(node.w * 0.26), underline_y),
                fill=TEXT,
                width=3,
            )
        return

    fill = USER_FILL if node.kind == "user" else BG
    draw.rounded_rectangle(rect_bounds(node), radius=8, fill=fill, outline=OUTLINE, width=5)
    draw_centered_text(draw, node.center, node.label, ENTITY_FONT, 7)


def draw_polyline(draw: ImageDraw.ImageDraw, points: Iterable[tuple[int, int]]) -> None:
    pts = list(points)
    if len(pts) < 2:
        return
    draw.line(pts, fill=LINE, width=4, joint="curve")


def unit_vector(start: tuple[int, int], end: tuple[int, int]) -> tuple[float, float]:
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    distance = math.hypot(dx, dy)
    if distance == 0:
        return (1.0, 0.0)
    return (dx / distance, dy / distance)


def cardinality_position(node: Node, entity_point: tuple[int, int], relation_point: tuple[int, int]) -> tuple[float, float]:
    ux, uy = unit_vector(entity_point, relation_point)
    half_w = node.w / 2
    half_h = node.h / 2
    x_distance = half_w / abs(ux) if abs(ux) > 0.001 else float("inf")
    y_distance = half_h / abs(uy) if abs(uy) > 0.001 else float("inf")
    boundary_distance = min(x_distance, y_distance)
    return (node.x + ux * (boundary_distance + 44), node.y + uy * (boundary_distance + 44))


def draw_cardinality(
    draw: ImageDraw.ImageDraw,
    label: str,
    position: tuple[float, float],
    offset: tuple[int, int] = (0, 0),
) -> None:
    x = position[0] + offset[0]
    y = position[1] + offset[1]
    label_w, label_h = text_size(draw, label, CARD_FONT)
    pad_x = 10
    pad_y = 5
    draw.rectangle(
        (
            x - label_w / 2 - pad_x,
            y - label_h / 2 - pad_y,
            x + label_w / 2 + pad_x,
            y + label_h / 2 + pad_y,
        ),
        fill=BG,
    )
    draw.text((x - label_w / 2, y - label_h / 2), label, font=CARD_FONT, fill=TEXT)


def add_node(
    nodes: dict[str, Node],
    key: str,
    label: str,
    kind: str,
    x: int,
    y: int,
    w: int = 430,
    h: int = 132,
) -> None:
    nodes[key] = Node(key, label, kind, x, y, w, h)


def add_attr(
    attrs: list[tuple[str, Node]],
    owner: str,
    key: str,
    label: str,
    x: int,
    y: int,
    is_key: bool = False,
    w: int = 360,
    h: int = 94,
) -> None:
    attrs.append((owner, Node(f"{owner}_{key}", label, "attribute", x, y, w, h, is_key=is_key)))


def draw_legend(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    draw.rounded_rectangle((x, y, x + 1480, y + 142), radius=18, fill=BG, outline=OUTLINE, width=3)
    samples = [
        Node("legend_entity", "实体", "entity", x + 160, y + 72, 220, 78),
        Node("legend_relation", "联系", "relation", x + 520, y + 72, 230, 110),
        Node("legend_attribute", "属性", "attribute", x + 860, y + 72, 250, 86),
    ]
    for sample in samples:
        draw_node(draw, sample)
    draw.text((x + 1050, y + 31), "线旁 1/n/m 表示基数", font=LEGEND_FONT, fill=MUTED)
    draw.text((x + 1050, y + 76), "下划线属性表示主键", font=LEGEND_FONT, fill=MUTED)


def render_diagram(
    title: str,
    filename: str,
    width: int,
    height: int,
    nodes: dict[str, Node],
    connections: list[tuple[str, str, str, list[tuple[int, int]], tuple[int, int]]],
    attrs: list[tuple[str, Node]],
    note: str,
) -> Path:
    image = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(image)

    title_w, _ = text_size(draw, title, TITLE_FONT)
    draw.text(((width - title_w) / 2, 62), title, font=TITLE_FONT, fill=TEXT)
    subtitle = "传统 Chen ER：矩形=实体，菱形=联系，椭圆=属性，线旁 1/n/m=基数"
    subtitle_w, _ = text_size(draw, subtitle, SUBTITLE_FONT)
    draw.text(((width - subtitle_w) / 2, 154), subtitle, font=SUBTITLE_FONT, fill=MUTED)
    draw_legend(draw, 100, 70)

    drawn_connections: list[tuple[str, str, str, tuple[int, int], tuple[int, int], tuple[int, int]]] = []
    for entity_key, relation_key, card, waypoints, offset in connections:
        start = nodes[entity_key].center
        end = nodes[relation_key].center
        draw_polyline(draw, [start, *waypoints, end])
        drawn_connections.append((entity_key, relation_key, card, start, end, offset))

    for owner_key, attr in attrs:
        draw_polyline(draw, [nodes[owner_key].center, attr.center])

    for node in nodes.values():
        draw_node(draw, node)

    for _, attr in attrs:
        draw_node(draw, attr)

    for entity_key, relation_key, card, start, end, offset in drawn_connections:
        position = cardinality_position(nodes[entity_key], start, end)
        draw_cardinality(draw, card, position, offset)

    note_w, _ = text_size(draw, note, SUBTITLE_FONT)
    draw.text(((width - note_w) / 2, height - 116), note, font=SUBTITLE_FONT, fill=MUTED)

    out_path = OUT_DIR / filename
    image.save(out_path, quality=95)
    return out_path


def user_profile_diagram() -> Path:
    nodes: dict[str, Node] = {}
    attrs: list[tuple[str, Node]] = []

    add_node(nodes, "user", "用户", "user", 2800, 1750, 460, 142)
    add_node(nodes, "profile", "用户画像", "entity", 920, 900, 430, 132)
    add_node(nodes, "reset", "找回密码验证码", "entity", 920, 1750, 520, 132)
    add_node(nodes, "event", "学习事件记录", "entity", 4680, 920, 480, 132)
    add_node(nodes, "time", "学习时长记录", "entity", 4680, 2350, 480, 132)
    add_node(nodes, "own", "拥有", "relation", 1840, 1050, 280, 164)
    add_node(nodes, "apply", "申请", "relation", 1840, 1750, 280, 164)
    add_node(nodes, "produce", "产生", "relation", 3740, 1120, 280, 164)
    add_node(nodes, "accumulate", "累计", "relation", 3740, 2350, 280, 164)

    connections = [
        ("user", "own", "1", [], (-10, -26)),
        ("profile", "own", "1", [], (0, -20)),
        ("user", "apply", "1", [], (-10, 18)),
        ("reset", "apply", "n", [], (0, -18)),
        ("user", "produce", "1", [], (10, -18)),
        ("event", "produce", "n", [], (0, -24)),
        ("user", "accumulate", "1", [], (10, 24)),
        ("time", "accumulate", "n", [], (0, -24)),
    ]

    for label, x, y, key in [
        ("用户编号", 2460, 930, True),
        ("用户名", 2860, 900, False),
        ("昵称", 3260, 930, False),
        ("邮箱", 2260, 1540, False),
        ("角色", 3340, 1540, False),
        ("学习目标", 2800, 2580, False),
    ]:
        add_attr(attrs, "user", label, label, x, y, key)

    for item in [
        ("用户编号", 430, 560, True),
        ("显示名称", 920, 520, False),
        ("个人简介", 1380, 560, False),
        ("所在地", 420, 1260, False),
        ("学校", 920, 1320, False),
        ("后台金币调整值", 1460, 1260, False, 470),
    ]:
        if len(item) == 4:
            label, x, y, key = item
            add_attr(attrs, "profile", label, label, x, y, key)
        else:
            label, x, y, key, attr_w = item
            add_attr(attrs, "profile", label, label, x, y, key, w=470)

    for label, x, y, key in [
        ("验证码编号", 390, 1480, True),
        ("邮箱", 920, 1410, False),
        ("验证码散列", 1450, 1480, False),
        ("过期时间", 390, 2020, False),
        ("是否已使用", 920, 2090, False),
        ("尝试次数", 1450, 2020, False),
    ]:
        add_attr(attrs, "reset", label, label, x, y, key)

    for label, x, y, key in [
        ("事件编号", 4220, 570, True),
        ("事件类型", 4680, 500, False),
        ("题目类型", 5140, 570, False),
        ("用户作答", 4180, 1280, False),
        ("是否正确", 4680, 1380, False),
        ("掌握状态", 5160, 1280, False),
    ]:
        add_attr(attrs, "event", label, label, x, y, key)

    for label, x, y, key in [
        ("记录编号", 4210, 2050, True),
        ("模块类型", 4680, 1980, False),
        ("目标标题", 5160, 2050, False),
        ("学习时长", 4420, 2720, False),
        ("创建时间", 4960, 2720, False),
    ]:
        add_attr(attrs, "time", label, label, x, y, key)

    return render_diagram(
        "用户认证与学习画像 Chen ER 图",
        "studyplatform-chen-er-user-profile.png",
        5600,
        3400,
        nodes,
        connections,
        attrs,
        "说明：找回密码、学习事件和学习时长均按用户维度形成 1:n 记录。",
    )


def course_textbook_diagram() -> Path:
    nodes: dict[str, Node] = {}
    attrs: list[tuple[str, Node]] = []

    add_node(nodes, "user", "用户", "user", 3800, 2300, 430, 142)
    add_node(nodes, "block", "学习内容板块", "entity", 950, 780, 500, 132)
    add_node(nodes, "course", "课程资源", "entity", 2450, 900, 430, 132)
    add_node(nodes, "teacher_course", "教师发布课程", "entity", 4300, 780, 500, 132)
    add_node(nodes, "textbook", "精品教材", "entity", 2450, 3400, 430, 132)
    add_node(nodes, "detail", "教材详情", "entity", 950, 3400, 430, 132)
    add_node(nodes, "order", "教材订单", "entity", 5600, 3400, 430, 132)

    add_node(nodes, "block_course", "包含", "relation", 1700, 820, 280, 164)
    add_node(nodes, "publish", "发布", "relation", 4660, 1230, 280, 164)
    add_node(nodes, "merge", "纳入", "relation", 3400, 780, 280, 164)
    add_node(nodes, "enroll", "加入课程", "relation", 2920, 1600, 360, 180)
    add_node(nodes, "review_course", "评价课程", "relation", 2600, 2050, 360, 180)
    add_node(nodes, "detail_rel", "拥有详情", "relation", 1700, 3400, 360, 180)
    add_node(nodes, "cart", "加入购物车", "relation", 2920, 3000, 430, 190)
    add_node(nodes, "review_book", "评价教材", "relation", 2500, 2700, 360, 180)
    add_node(nodes, "place_order", "下单购买", "relation", 4920, 2860, 380, 180)
    add_node(nodes, "order_item", "包含教材", "relation", 3900, 3740, 380, 180)

    connections = [
        ("block", "block_course", "1", [], (0, -20)),
        ("course", "block_course", "n", [], (0, -26)),
        ("user", "publish", "1", [], (10, -20)),
        ("teacher_course", "publish", "n", [], (0, 18)),
        ("teacher_course", "merge", "1", [], (0, -22)),
        ("course", "merge", "1", [], (0, -22)),
        ("user", "enroll", "m", [], (-12, -18)),
        ("course", "enroll", "n", [], (0, 22)),
        ("user", "review_course", "m", [], (-12, 20)),
        ("course", "review_course", "n", [], (-20, 24)),
        ("textbook", "detail_rel", "1", [], (0, -22)),
        ("detail", "detail_rel", "1", [], (0, -22)),
        ("user", "cart", "m", [], (-10, 20)),
        ("textbook", "cart", "n", [], (0, -20)),
        ("user", "review_book", "m", [], (-16, 28)),
        ("textbook", "review_book", "n", [], (0, -18)),
        ("user", "place_order", "1", [], (10, -20)),
        ("order", "place_order", "n", [], (0, -20)),
        ("order", "order_item", "m", [], (0, 22)),
        ("textbook", "order_item", "n", [], (0, 24)),
    ]

    for owner, data in {
        "block": [("板块编号", 500, 470, True), ("板块编码", 950, 430, False), ("板块名称", 1400, 470, False), ("排序号", 950, 1160, False)],
        "course": [("课程编号", 2050, 520, True), ("课程名称", 2450, 470, False), ("教师姓名", 2850, 520, False), ("课程类别", 2070, 1280, False), ("学校名称", 2450, 1340, False), ("开课时间", 2860, 1280, False)],
        "teacher_course": [("发布课程编号", 3880, 430, True), ("学期计划", 4300, 390, False), ("课程概述", 4720, 430, False), ("视频路径", 4300, 1120, False)],
        "textbook": [("教材编号", 2050, 3120, True), ("教材名称", 2450, 3050, False), ("主编", 2850, 3120, False), ("出版社", 2050, 3680, False), ("ISBN", 2450, 3780, False), ("教材类别", 2850, 3680, False)],
        "detail": [("详情编号", 520, 3120, True), ("推荐语", 950, 3050, False), ("原价", 1380, 3120, False), ("折扣价", 520, 3680, False), ("阅读人数", 950, 3780, False)],
        "order": [("订单编号", 5200, 3120, True), ("订单号", 5600, 3050, False), ("订单总额", 6000, 3120, False), ("订单状态", 5600, 3780, False)],
        "enroll": [("加入记录编号", 3180, 1360, True), ("创建时间", 3180, 1820, False)],
        "review_course": [("评价编号", 2180, 1860, True), ("评分", 2180, 2220, False), ("评价内容", 2920, 2260, False)],
        "cart": [("购物车项编号", 3300, 2790, True), ("购买数量", 3300, 3220, False)],
        "review_book": [("教材评价编号", 2080, 2500, True), ("评分", 2080, 2870, False), ("评价内容", 2920, 2560, False)],
        "order_item": [("明细编号", 3550, 3440, True), ("单价", 3950, 4050, False), ("数量", 4300, 3440, False)],
    }.items():
        for item in data:
            label, x, y, key = item
            add_attr(attrs, owner, label, label, x, y, key, w=430 if len(label) >= 6 else 360)

    return render_diagram(
        "课程资源与教材交易 Chen ER 图",
        "studyplatform-chen-er-course-textbook.png",
        6800,
        4400,
        nodes,
        connections,
        attrs,
        "说明：课程加入、课程评价、购物车和教材评价均按用户与资源之间的 m:n 联系建模。",
    )


def question_exam_diagram() -> Path:
    nodes: dict[str, Node] = {}
    attrs: list[tuple[str, Node]] = []

    add_node(nodes, "user", "用户", "user", 3950, 2600, 430, 142)
    add_node(nodes, "category", "课程题库分类", "entity", 820, 720, 500, 132)
    add_node(nodes, "set", "课程题库套题", "entity", 2100, 720, 500, 132)
    add_node(nodes, "course_q", "课程题库题目", "entity", 3380, 720, 500, 132)
    add_node(nodes, "subject", "通用题源学科", "entity", 4920, 720, 500, 132)
    add_node(nodes, "problem", "通用题源题目", "entity", 6200, 720, 500, 132)
    add_node(nodes, "tag", "通用题源标签", "entity", 7420, 720, 500, 132)
    add_node(nodes, "assignment", "课程作业", "entity", 980, 3850, 430, 132)
    add_node(nodes, "assignment_q", "作业题目", "entity", 2240, 3850, 430, 132)
    add_node(nodes, "exam", "课程考试", "entity", 5200, 3850, 430, 132)
    add_node(nodes, "exam_q", "考试题目", "entity", 6440, 3850, 430, 132)
    add_node(nodes, "oj", "OJ 题目", "entity", 7420, 2600, 430, 132)

    for key, label, x, y, w in [
        ("cat_set", "包含", 1460, 720, 280),
        ("set_q", "包含", 2740, 720, 280),
        ("mistake", "记录错题", 3280, 1650, 360),
        ("favorite", "收藏题目", 3280, 2050, 360),
        ("problem_subject", "归属", 5560, 720, 280),
        ("problem_tag", "标记", 6810, 720, 280),
        ("assignment_contains", "包含", 1610, 3850, 280),
        ("exam_contains", "包含", 5820, 3850, 280),
        ("submit_assignment", "提交作业", 2640, 3200, 360),
        ("submit_exam", "参加考试", 4760, 3200, 360),
        ("quote_oj", "引用 OJ", 6980, 3220, 360),
    ]:
        add_node(nodes, key, label, "relation", x, y, w, 170)

    connections = [
        ("category", "cat_set", "1", [], (0, -20)),
        ("set", "cat_set", "n", [], (0, -20)),
        ("set", "set_q", "1", [], (0, 22)),
        ("course_q", "set_q", "n", [], (0, -20)),
        ("user", "mistake", "m", [], (-10, -18)),
        ("course_q", "mistake", "n", [], (0, 20)),
        ("user", "favorite", "m", [], (-10, 22)),
        ("course_q", "favorite", "n", [], (0, 20)),
        ("problem", "problem_subject", "m", [], (0, -20)),
        ("subject", "problem_subject", "n", [], (0, -20)),
        ("problem", "problem_tag", "m", [], (0, 22)),
        ("tag", "problem_tag", "n", [], (0, -20)),
        ("assignment", "assignment_contains", "1", [], (0, -20)),
        ("assignment_q", "assignment_contains", "n", [], (0, -20)),
        ("exam", "exam_contains", "1", [], (0, -20)),
        ("exam_q", "exam_contains", "n", [], (0, -20)),
        ("user", "submit_assignment", "m", [], (-10, 20)),
        ("assignment", "submit_assignment", "n", [], (0, -18)),
        ("user", "submit_exam", "m", [], (10, 20)),
        ("exam", "submit_exam", "n", [], (0, -18)),
        ("assignment_q", "quote_oj", "n", [(4800, 4100), (6500, 3420)], (0, 26)),
        ("exam_q", "quote_oj", "n", [], (0, 26)),
        ("oj", "quote_oj", "1", [], (0, -20)),
    ]

    attr_specs = {
        "category": [("分类编号", 420, 430, True), ("分类编码", 820, 370, False), ("分类名称", 1220, 430, False), ("排序号", 820, 1060, False)],
        "set": [("套题编号", 1700, 430, True), ("套题编码", 2100, 370, False), ("标题", 2500, 430, False), ("难度标签", 1840, 1060, False), ("题目数量", 2360, 1060, False)],
        "course_q": [("题目编号", 3000, 430, True), ("题目类型", 3380, 370, False), ("题干", 3760, 430, False), ("答案", 3160, 1060, False), ("解析", 3600, 1060, False)],
        "subject": [("学科编号", 4540, 430, True), ("学科编码", 4920, 370, False), ("学科名称", 5300, 430, False), ("排序号", 4920, 1060, False)],
        "problem": [("题源题目编号", 5800, 430, True), ("来源", 6200, 370, False), ("标题", 6600, 430, False), ("难度", 5980, 1060, False), ("提交总数", 6420, 1060, False)],
        "tag": [("标签编号", 7040, 430, True), ("标签名称", 7420, 370, False), ("标签类型", 7800, 430, False), ("来源", 7420, 1060, False)],
        "assignment": [("作业编号", 580, 3560, True), ("作业编码", 980, 3480, False), ("作业标题", 1380, 3560, False), ("截止时间", 780, 4200, False), ("总分", 1180, 4200, False)],
        "assignment_q": [("作业题目编号", 1840, 3560, True), ("题目序号", 2240, 3480, False), ("题目类型", 2640, 3560, False), ("分值", 2020, 4200, False), ("正确答案", 2460, 4200, False)],
        "exam": [("考试编号", 4820, 3560, True), ("考试编码", 5200, 3480, False), ("考试标题", 5580, 3560, False), ("开始时间", 5000, 4200, False), ("总分", 5400, 4200, False)],
        "exam_q": [("考试题目编号", 6040, 3560, True), ("题目序号", 6440, 3480, False), ("题目类型", 6840, 3560, False), ("分值", 6220, 4200, False), ("正确答案", 6660, 4200, False)],
        "oj": [("OJ题目编号", 7420, 2300, True), ("题目标题", 7420, 2900, False)],
        "mistake": [("错题记录编号", 2920, 1460, True), ("错误次数", 2920, 1840, False), ("是否掌握", 3640, 1840, False)],
        "favorite": [("收藏编号", 2920, 2240, True), ("创建时间", 3640, 2240, False)],
        "submit_assignment": [("提交编号", 2280, 2980, True), ("答案内容", 2280, 3420, False), ("得分", 3060, 3420, False)],
        "submit_exam": [("提交编号", 4380, 2980, True), ("开始作答时间", 4380, 3420, False), ("得分", 5140, 3420, False)],
    }
    for owner, items in attr_specs.items():
        for label, x, y, key in items:
            add_attr(attrs, owner, label, label, x, y, key, w=430 if len(label) >= 6 else 360)

    return render_diagram(
        "课程题库、作业与考试 Chen ER 图",
        "studyplatform-chen-er-question-exam.png",
        8200,
        4700,
        nodes,
        connections,
        attrs,
        "说明：错题、收藏、作业提交和考试提交均作为带属性的联系；题目可引用 OJ 题目。",
    )


def oj_diagram() -> Path:
    nodes: dict[str, Node] = {}
    attrs: list[tuple[str, Node]] = []

    add_node(nodes, "user", "用户", "user", 760, 1800, 430, 142)
    add_node(nodes, "problem", "OJ 题目", "entity", 2700, 860, 430, 132)
    add_node(nodes, "case", "OJ 测试用例", "entity", 5000, 860, 500, 132)
    add_node(nodes, "submission", "代码提交", "entity", 2700, 2880, 430, 132)
    add_node(nodes, "result", "单用例评测结果", "entity", 5000, 2880, 560, 132)
    add_node(nodes, "create", "创建", "relation", 1650, 1160, 280, 164)
    add_node(nodes, "set_case", "设置用例", "relation", 3850, 860, 360, 180)
    add_node(nodes, "submit", "提交代码", "relation", 1650, 2520, 360, 180)
    add_node(nodes, "target", "针对", "relation", 2700, 1880, 280, 164)
    add_node(nodes, "make_result", "产生评测", "relation", 3850, 2880, 360, 180)
    add_node(nodes, "case_result", "参与评测", "relation", 5000, 1880, 360, 180)

    connections = [
        ("user", "create", "1", [], (0, -18)),
        ("problem", "create", "n", [], (0, 20)),
        ("problem", "set_case", "1", [], (0, -20)),
        ("case", "set_case", "n", [], (0, -20)),
        ("user", "submit", "1", [], (0, 18)),
        ("submission", "submit", "n", [], (0, -20)),
        ("submission", "target", "n", [], (0, -18)),
        ("problem", "target", "1", [], (0, 20)),
        ("submission", "make_result", "1", [], (0, 22)),
        ("result", "make_result", "n", [], (0, 22)),
        ("case", "case_result", "1", [], (0, 20)),
        ("result", "case_result", "n", [], (0, -18)),
    ]

    attr_specs = {
        "problem": [("题目编号", 2300, 520, True), ("题目标题", 2700, 450, False), ("题目标识", 3100, 520, False), ("难度", 2300, 1180, False), ("时间限制", 2700, 1260, False), ("内存限制", 3100, 1180, False)],
        "case": [("用例编号", 4560, 520, True), ("输入数据", 5000, 450, False), ("期望输出", 5440, 520, False), ("是否样例", 4780, 1180, False), ("权重", 5220, 1180, False)],
        "submission": [("提交编号", 2300, 2540, True), ("编程语言", 2700, 2460, False), ("源代码", 3100, 2540, False), ("评测状态", 2300, 3220, False), ("得分", 2700, 3300, False), ("耗时", 3100, 3220, False)],
        "result": [("评测明细编号", 4540, 2540, True), ("评测状态", 5000, 2460, False), ("耗时", 5460, 2540, False), ("内存占用", 4780, 3220, False), ("消息", 5220, 3220, False)],
    }
    for owner, items in attr_specs.items():
        for label, x, y, key in items:
            add_attr(attrs, owner, label, label, x, y, key, w=430 if len(label) >= 6 else 360)

    return render_diagram(
        "OJ 在线评测 Chen ER 图",
        "studyplatform-chen-er-oj.png",
        6200,
        3600,
        nodes,
        connections,
        attrs,
        "说明：代码提交作为事件实体承接用户、OJ 题目和单用例评测结果之间的关系。",
    )


def game_experiment_diagram() -> Path:
    nodes: dict[str, Node] = {}
    attrs: list[tuple[str, Node]] = []

    add_node(nodes, "user", "用户", "user", 3800, 2250, 430, 142)
    add_node(nodes, "ladder", "万题天梯跳成绩", "entity", 980, 850, 560, 132)
    add_node(nodes, "type", "Type Warrior 成绩", "entity", 2860, 850, 580, 132)
    add_node(nodes, "coin", "金币奖励记录", "entity", 1900, 1720, 520, 132)
    add_node(nodes, "reservoir", "油藏动态仿真记录", "entity", 5200, 760, 620, 132)
    add_node(nodes, "pump", "抽油泵工况仿真记录", "entity", 6720, 760, 670, 132)
    add_node(nodes, "water", "注水开发仿真记录", "entity", 5200, 1900, 620, 132)
    add_node(nodes, "stim", "压裂酸化仿真记录", "entity", 6720, 1900, 620, 132)
    add_node(nodes, "template", "测井模板", "entity", 5200, 3500, 430, 132)
    add_node(nodes, "well", "测井解释", "entity", 6720, 3500, 430, 132)

    for key, label, x, y, w in [
        ("play_ladder", "游玩", 2200, 1120, 280),
        ("play_type", "游玩", 3100, 1320, 280),
        ("make_coin", "产生奖励", 1900, 1280, 360),
        ("gain_coin", "获得金币", 2860, 1860, 360),
        ("run_reservoir", "执行", 4780, 1240, 280),
        ("run_pump", "执行", 6280, 1240, 280),
        ("run_water", "执行", 4780, 2080, 280),
        ("run_stim", "执行", 6280, 2080, 280),
        ("generate_well", "生成解释", 5940, 3000, 360),
        ("template_help", "辅助生成", 5940, 3500, 360),
    ]:
        add_node(nodes, key, label, "relation", x, y, w, 170)

    connections = [
        ("user", "play_ladder", "1", [], (-34, -48)),
        ("ladder", "play_ladder", "n", [], (0, -18)),
        ("user", "play_type", "1", [], (-30, -10)),
        ("type", "play_type", "n", [], (0, -18)),
        ("ladder", "make_coin", "1", [], (0, 20)),
        ("type", "make_coin", "1", [], (0, 20)),
        ("coin", "make_coin", "n", [], (0, 20)),
        ("user", "gain_coin", "1", [], (-32, 36)),
        ("coin", "gain_coin", "n", [], (0, -18)),
        ("user", "run_reservoir", "1", [], (32, -86)),
        ("reservoir", "run_reservoir", "n", [], (0, 20)),
        ("user", "run_pump", "1", [], (36, -42)),
        ("pump", "run_pump", "n", [], (0, 20)),
        ("user", "run_water", "1", [], (38, 18)),
        ("water", "run_water", "n", [], (0, -18)),
        ("user", "run_stim", "1", [], (42, 64)),
        ("stim", "run_stim", "n", [], (0, -18)),
        ("user", "generate_well", "1", [], (52, 116)),
        ("well", "generate_well", "n", [], (0, -18)),
        ("template", "template_help", "1", [], (0, -18)),
        ("well", "template_help", "n", [], (0, -18)),
    ]

    attr_specs = {
        "ladder": [("天梯跳记录编号", 520, 520, True), ("题库编码", 980, 450, False), ("获得金币总数", 1440, 520, False), ("正确数", 740, 1160, False), ("错误数", 1220, 1160, False)],
        "type": [("Type记录编号", 2400, 520, True), ("到达波次", 2860, 450, False), ("得分", 3320, 520, False), ("最大连击", 2620, 1160, False), ("击杀数", 3100, 1160, False)],
        "coin": [("奖励编号", 1460, 1480, True), ("奖励来源类型", 1900, 1400, False), ("奖励原因", 2340, 1480, False), ("金币数量", 1660, 2020, False), ("创建时间", 2140, 2020, False)],
        "reservoir": [("油藏记录编号", 4740, 450, True), ("地层压力", 5200, 380, False), ("渗透率", 5660, 450, False), ("日产油量", 4980, 1080, False), ("日产水量", 5420, 1080, False)],
        "pump": [("抽油泵记录编号", 6260, 450, True), ("冲程", 6720, 380, False), ("冲次", 7180, 450, False), ("工况类型", 6480, 1080, False), ("示功图数据", 6960, 1080, False)],
        "water": [("注水记录编号", 4740, 1640, True), ("注入速度", 5200, 1560, False), ("见效天数", 5660, 1640, False), ("峰值产油量", 4980, 2220, False), ("产量曲线", 5420, 2220, False)],
        "stim": [("压裂酸化编号", 6260, 1640, True), ("施工类型", 6720, 1560, False), ("加砂量", 7180, 1640, False), ("裂缝长度", 6480, 2220, False), ("增产倍数", 6960, 2220, False)],
        "template": [("模板编号", 4820, 3240, True), ("模板名称", 5200, 3160, False), ("自然伽马基线", 5580, 3240, False), ("备注", 5200, 3800, False)],
        "well": [("测井记录编号", 6340, 3240, True), ("孔隙度", 6720, 3160, False), ("含油饱和度", 7100, 3240, False), ("解释报告", 6720, 3800, False)],
    }
    for owner, items in attr_specs.items():
        for label, x, y, key in items:
            add_attr(attrs, owner, label, label, x, y, key, w=460 if len(label) >= 6 else 360)

    return render_diagram(
        "游戏金币、仿真实验与测井 Chen ER 图",
        "studyplatform-chen-er-game-experiment.png",
        7600,
        4300,
        nodes,
        connections,
        attrs,
        "说明：游戏、仿真和测井均按用户产生记录建模；金币奖励由游戏成绩或用户行为产生。",
    )


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    paths = [
        user_profile_diagram(),
        course_textbook_diagram(),
        question_exam_diagram(),
        oj_diagram(),
        game_experiment_diagram(),
    ]
    for path in paths:
        print(path)


if __name__ == "__main__":
    main()
