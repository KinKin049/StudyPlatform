from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = ROOT / "docs" / "report-assets"

INK = "#263238"
MUTED = "#667085"
BG = "#ffffff"
LINE = "#506070"
DASH = "#8A94A6"

PALETTE = {
    "user": ("#EAF3F8", "#2F6F9F", "#1F4E70"),
    "course": ("#F2F8EE", "#5B9B64", "#2F5F38"),
    "textbook": ("#FFF7EA", "#A97920", "#6C4916"),
    "question": ("#F3F0FA", "#7568A7", "#453D73"),
    "oj": ("#EEF7F5", "#2A8C82", "#225E59"),
    "game": ("#FFF2EF", "#C65D4D", "#7A342D"),
    "experiment": ("#EDF5FF", "#4E79A7", "#2E4E73"),
    "neutral": ("#F7F8FA", "#8A94A6", "#4B5563"),
}


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    names = ["msyhbd.ttc", "msyh.ttc", "simhei.ttf"] if bold else ["msyh.ttc", "simhei.ttf"]
    for name in names:
        path = Path("C:/Windows/Fonts") / name
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


FONT_TITLE = font(48, True)
FONT_SUBTITLE = font(27)
FONT_GROUP = font(31, True)
FONT_CARD = font(26, True)
FONT_FIELD = font(22)
FONT_FIELD_BOLD = font(22, True)
FONT_BADGE = font(17, True)
FONT_LABEL = font(19, True)
FONT_NOTE = font(20)


@dataclass
class Card:
    key: str
    title: str
    fields: list[tuple[str, str]]
    x: int
    y: int
    w: int
    domain: str
    h: int = 0
    note: str | None = None

    def __post_init__(self) -> None:
        self.h = self.h or 84 + len(self.fields) * 36 + (30 if self.note else 0)

    @property
    def box(self) -> tuple[int, int, int, int]:
        return self.x, self.y, self.x + self.w, self.y + self.h

    def point(self, side: str) -> tuple[int, int]:
        x1, y1, x2, y2 = self.box
        if side == "left":
            return x1, (y1 + y2) // 2
        if side == "right":
            return x2, (y1 + y2) // 2
        if side == "top":
            return (x1 + x2) // 2, y1
        if side == "bottom":
            return (x1 + x2) // 2, y2
        return (x1 + x2) // 2, (y1 + y2) // 2


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


def rounded_label(draw: ImageDraw.ImageDraw, x: int, y: int, text: str, fill: str = BG) -> None:
    w, h = text_size(draw, text, FONT_LABEL)
    box = (x - w // 2 - 14, y - h // 2 - 8, x + w // 2 + 14, y + h // 2 + 8)
    draw.rounded_rectangle((box[0] + 3, box[1] + 3, box[2] + 3, box[3] + 3), radius=8, fill="#D9DEE8")
    draw.rounded_rectangle(box, radius=8, fill=fill, outline="#AEB7C6", width=2)
    draw.text((x - w // 2, y - h // 2), text, font=FONT_LABEL, fill=INK)


def dashed_line(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: str, width: int = 4) -> None:
    dash_len = 18
    gap = 12
    for start, end in zip(points[:-1], points[1:]):
        x1, y1 = start
        x2, y2 = end
        length = ((x2 - x1) ** 2 + (y2 - y1) ** 2) ** 0.5
        if length == 0:
            continue
        distance = 0
        while distance < length:
            next_distance = min(distance + dash_len, length)
            sx = x1 + (x2 - x1) * distance / length
            sy = y1 + (y2 - y1) * distance / length
            ex = x1 + (x2 - x1) * next_distance / length
            ey = y1 + (y2 - y1) * next_distance / length
            draw.line((sx, sy, ex, ey), fill=fill, width=width)
            distance += dash_len + gap


def draw_polyline(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], dashed: bool = False, fill: str = LINE) -> None:
    if dashed:
        dashed_line(draw, points, fill, 4)
    else:
        draw.line(points, fill=fill, width=4, joint="curve")


def badge(draw: ImageDraw.ImageDraw, x: int, y: int, text: str, kind: str) -> int:
    colors = {
        "PK": ("#263238", "#ffffff"),
        "FK": ("#5165A4", "#ffffff"),
        "UK": ("#A97920", "#ffffff"),
        "LOG": ("#687076", "#ffffff"),
    }
    fill, text_fill = colors.get(kind, ("#E5E7EB", INK))
    w = max(34, text_size(draw, text, FONT_BADGE)[0] + 14)
    draw.rounded_rectangle((x, y, x + w, y + 22), radius=6, fill=fill)
    center_text(draw, (x, y - 1, x + w, y + 22), text, FONT_BADGE, text_fill, line_gap=0)
    return w


def draw_card(draw: ImageDraw.ImageDraw, card: Card) -> None:
    fill, outline, header_text = PALETTE[card.domain]
    x1, y1, x2, y2 = card.box
    draw.rounded_rectangle((x1 + 5, y1 + 5, x2 + 5, y2 + 5), radius=18, fill="#DDE3EA")
    draw.rounded_rectangle(card.box, radius=18, fill=BG, outline=outline, width=3)
    draw.rounded_rectangle((x1, y1, x2, y1 + 54), radius=18, fill=fill, outline=outline, width=3)
    draw.rectangle((x1, y1 + 28, x2, y1 + 54), fill=fill)
    center_text(draw, (x1 + 18, y1 + 4, x2 - 18, y1 + 52), card.title, FONT_CARD, header_text, line_gap=3)
    y = y1 + 70
    for kind, field in card.fields:
        bx = x1 + 18
        if kind:
            b_width = badge(draw, bx, y + 1, kind, kind)
            bx += b_width + 10
        draw.text((bx, y), field, font=FONT_FIELD_BOLD if kind == "PK" else FONT_FIELD, fill=INK if kind != "FK" else "#334E8A")
        y += 36
    if card.note:
        draw.line((x1 + 16, y, x2 - 16, y), fill="#E5E7EB", width=2)
        draw.text((x1 + 18, y + 8), card.note, font=FONT_NOTE, fill=MUTED)


def relation(
    draw: ImageDraw.ImageDraw,
    cards: dict[str, Card],
    source: str,
    target: str,
    label: str,
    source_side: str = "right",
    target_side: str = "left",
    via: list[tuple[int, int]] | None = None,
    dashed: bool = False,
    label_at: tuple[int, int] | None = None,
) -> None:
    points = [cards[source].point(source_side), *(via or []), cards[target].point(target_side)]
    draw_polyline(draw, points, dashed=dashed, fill=DASH if dashed else LINE)
    if label_at is None:
        label_at = points[len(points) // 2]
    rounded_label(draw, label_at[0], label_at[1], label)


def draw_legend(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    draw.rounded_rectangle((x, y, x + 590, y + 150), radius=16, fill="#F7F8FA", outline="#CBD2DD", width=2)
    draw.text((x + 24, y + 18), "图例", font=FONT_GROUP, fill=INK)
    badge(draw, x + 24, y + 68, "PK", "PK")
    draw.text((x + 78, y + 67), "主键", font=FONT_NOTE, fill=MUTED)
    badge(draw, x + 160, y + 68, "FK", "FK")
    draw.text((x + 214, y + 67), "外键", font=FONT_NOTE, fill=MUTED)
    draw.line((x + 310, y + 80, x + 390, y + 80), fill=LINE, width=4)
    draw.text((x + 404, y + 67), "物理外键", font=FONT_NOTE, fill=MUTED)
    dashed_line(draw, [(x + 310, y + 118), (x + 390, y + 118)], DASH, 4)
    draw.text((x + 404, y + 105), "逻辑关联", font=FONT_NOTE, fill=MUTED)


def base_image(width: int, height: int, title: str, subtitle: str = "") -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (width, height), BG)
    draw = ImageDraw.Draw(image)
    title_w, _ = text_size(draw, title, FONT_TITLE)
    draw.text(((width - title_w) / 2, 34), title, font=FONT_TITLE, fill=INK)
    if subtitle:
        sub_w, _ = text_size(draw, subtitle, FONT_SUBTITLE)
        draw.text(((width - sub_w) / 2, 94), subtitle, font=FONT_SUBTITLE, fill=MUTED)
    return image, draw


def save_all(image: Image.Image, name: str) -> None:
    png = OUTPUT_DIR / f"{name}.png"
    jpg = OUTPUT_DIR / f"{name}.jpg"
    image.save(png, dpi=(220, 220))
    image.save(jpg, quality=95, dpi=(220, 220))
    print(png)
    print(jpg)


def draw_overview() -> Image.Image:
    image, draw = base_image(
        3600,
        2100,
        "StudyPlatform 数据库 ER 总览图",
        "合并后以 users 为统一用户主表；auth_users 为兼容视图，不作为独立实体表绘制",
    )
    groups = [
        ("用户认证与画像", "user", (120, 220, 830, 840), ["users", "password_reset_codes", "profile_user_profiles", "profile_learning_events", "profile_learning_time_records"]),
        ("课程与教材交易", "course", (1020, 220, 1860, 840), ["learning_content_blocks", "online_open_courses", "general_courses", "micro_major_courses", "teacher_published_courses", "academy_course_enrollments", "academy_course_reviews", "excellent_textbooks", "academy_textbook_*"]),
        ("题库作业考试", "question", (2050, 220, 3460, 840), ["course_question_bank_categories", "course_question_bank_sets", "course_question_bank_questions", "course_question_bank_mistakes/favorites", "question_bank_subjects/tags/problems", "academy_assignments/*", "academy_exams/*"]),
        ("OJ 在线评测", "oj", (120, 1120, 950, 1720), ["oj_problems", "oj_test_cases", "oj_submissions", "oj_submission_cases"]),
        ("游戏金币与实验", "game", (1140, 1120, 2260, 1720), ["game_ladder_jump_records", "game_type_warrior_records", "coin_reward_records", "production_*_record", "well_log_record", "well_log_template"]),
        ("后台管理视角", "neutral", (2450, 1120, 3460, 1720), ["管理 users / courses / question banks", "管理 textbook orders / reviews", "调整 profile_user_profiles.admin_coin_adjustment", "统计 learning / game / experiment records"]),
    ]
    centers = {}
    boxes = {}
    for title, domain, box, table_names in groups:
        boxes[title] = box
        x1, y1, x2, y2 = box
        centers[title] = ((x1 + x2) // 2, (y1 + y2) // 2)

    # Draw relationship corridors first; cards are drawn after this so lines never cover table lists.
    overview_labels: list[tuple[int, int, str]] = []

    def corridor(points: list[tuple[int, int]], label: str, label_at: tuple[int, int], dashed: bool = False) -> None:
        draw_polyline(draw, points, dashed=dashed, fill=DASH if dashed else LINE)
        overview_labels.append((label_at[0], label_at[1], label))

    corridor([(830, 530), (1020, 530)], "users 1:N", (925, 500))
    corridor([(1860, 530), (2050, 530)], "课程编码逻辑关联", (1955, 500), dashed=True)
    corridor([(475, 840), (475, 1120)], "提交/创建", (475, 980))
    corridor([(830, 760), (900, 950), (2760, 950), (2760, 840)], "学习行为 1:N", (1800, 925))
    corridor([(650, 840), (650, 1015), (1700, 1015), (1700, 1120)], "记录归属", (1175, 990))
    corridor([(950, 1120), (950, 1045), (2760, 1045), (2760, 840)], "作业/考试代码题引用", (2590, 1020), dashed=True)
    corridor([(2260, 1420), (2450, 1420)], "统计与金币调整", (2355, 1390), dashed=True)

    for title, domain, box, table_names in groups:
        fill, outline, text_color = PALETTE[domain]
        x1, y1, x2, y2 = box
        draw.rounded_rectangle((x1 + 6, y1 + 6, x2 + 6, y2 + 6), radius=24, fill="#DDE3EA")
        draw.rounded_rectangle(box, radius=24, fill=BG, outline=outline, width=4)
        draw.rounded_rectangle((x1, y1, x2, y1 + 68), radius=24, fill=fill, outline=outline, width=4)
        draw.rectangle((x1, y1 + 36, x2, y1 + 68), fill=fill)
        center_text(draw, (x1, y1 + 4, x2, y1 + 66), title, FONT_GROUP, text_color)
        y = y1 + 94
        for name in table_names:
            draw.rounded_rectangle((x1 + 36, y, x2 - 36, y + 52), radius=10, fill="#FFFFFF", outline="#E0E5EC", width=2)
            center_text(draw, (x1 + 48, y, x2 - 48, y + 52), name, FONT_FIELD, INK)
            y += 66
    for x, y, label in overview_labels:
        rounded_label(draw, x, y, label)
    draw_legend(draw, 120, 1850)
    note = "说明：总览图展示实体分域和关键跨域关系；详细字段与主外键见后续分域 ER 图。"
    draw.text((820, 1888), note, font=FONT_NOTE, fill=MUTED)
    return image


def render_cards(image: Image.Image, cards: dict[str, Card], rels: list[dict], legend_xy: tuple[int, int] | None = None) -> None:
    draw = ImageDraw.Draw(image)
    for rel in rels:
        relation(draw, cards, **rel)
    for card in cards.values():
        draw_card(draw, card)
    if legend_xy:
        draw_legend(draw, *legend_xy)


def draw_user_profile() -> Image.Image:
    image, draw = base_image(3000, 1700, "用户认证与学习画像 ER 图", "users 为统一用户主表，auth_users 为兼容视图")
    cards = {
        "users": Card("users", "users", [("PK", "id"), ("UK", "email"), ("", "username / password_hash"), ("", "role / role_type"), ("", "learning_goal / interests_json"), ("", "school / teacher_name"), ("", "pet_key / onboarding_completed")], 1120, 230, 620, "user"),
        "password": Card("password", "password_reset_codes", [("PK", "id"), ("", "email"), ("", "code_hash"), ("", "expires_at"), ("", "used / attempt_count")], 160, 300, 560, "user", note="按 email 找回密码"),
        "profile": Card("profile", "profile_user_profiles", [("PK", "user_id"), ("FK", "user_id -> users.id"), ("", "display_name / handle"), ("", "role_label / bio"), ("", "school / avatar_path"), ("", "admin_coin_adjustment")], 2180, 260, 620, "user"),
        "events": Card("events", "profile_learning_events", [("PK", "id"), ("FK", "user_id -> users.id"), ("FK", "question_id -> course_question_bank_questions.id"), ("", "event_type / set_code"), ("", "selected_answer / correct_answer"), ("", "is_correct / vocabulary_status")], 600, 1040, 690, "user"),
        "time": Card("time", "profile_learning_time_records", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "module_type"), ("", "target_code / target_title"), ("", "duration_seconds")], 1540, 1040, 640, "user"),
        "coin": Card("coin", "coin_reward_records", [("PK", "id"), ("FK", "user_id -> users.id"), ("UK", "user_id + source_type + source_key"), ("", "reason / amount"), ("", "reference_id")], 2260, 1040, 620, "game"),
    }
    rels = [
        dict(source="users", target="profile", label="1:1", source_side="right", target_side="left", label_at=(1945, 425)),
        dict(source="users", target="events", label="1:N", source_side="bottom", target_side="top", via=[(1430, 900), (945, 900)], label_at=(1180, 890)),
        dict(source="users", target="time", label="1:N", source_side="bottom", target_side="top", via=[(1430, 910), (1860, 910)], label_at=(1625, 900)),
        dict(source="users", target="coin", label="1:N", source_side="bottom", target_side="top", via=[(1430, 930), (2570, 930)], label_at=(2210, 920)),
        dict(source="password", target="users", label="email 逻辑", source_side="right", target_side="left", dashed=True, via=[(860, 470)], label_at=(910, 430)),
    ]
    render_cards(image, cards, rels, (140, 1370))
    draw.text((860, 1425), "备注：V51 合并后，登录注册字段归并到 users；auth_users 仅保留为兼容视图。", font=FONT_NOTE, fill=MUTED)
    return image


def draw_course_textbook() -> Image.Image:
    image, draw = base_image(3900, 2400, "课程、教材与交易 ER 图", "课程资源通过 resource_type + course_id 与业务表形成逻辑关联")
    cards = {
        "users": Card("users", "users", [("PK", "id"), ("", "username / role"), ("", "email")], 80, 250, 430, "user"),
        "blocks": Card("blocks", "learning_content_blocks", [("PK", "id"), ("UK", "block_code"), ("", "block_name"), ("", "storage_folder"), ("", "enabled")], 590, 210, 540, "course"),
        "online": Card("online", "online_open_courses", [("PK", "id"), ("UK", "external_course_id"), ("", "course_name"), ("", "teacher_name"), ("", "category / school_name"), ("", "participant_count"), ("", "certified")], 1220, 170, 590, "course"),
        "general": Card("general", "general_courses", [("PK", "id"), ("UK", "external_course_id"), ("", "course_name"), ("", "teacher_name"), ("", "category / school_name"), ("", "course_description"), ("", "certified")], 1910, 170, 590, "course"),
        "micro": Card("micro", "micro_major_courses", [("PK", "id"), ("UK", "external_course_id"), ("", "course_name"), ("", "teacher_name"), ("", "category / school_name"), ("", "course_description"), ("", "certified")], 2600, 170, 590, "course"),
        "teacher": Card("teacher", "teacher_published_courses", [("PK", "id"), ("UK", "course_id"), ("FK", "publisher_user_id -> users.id"), ("", "semester_plan"), ("", "course_overview / detail"), ("", "video_file_path")], 3260, 230, 590, "course"),
        "enroll": Card("enroll", "academy_course_enrollments", [("PK", "id"), ("FK", "user_id -> users.id"), ("LOG", "resource_type + course_id"), ("", "created_at")], 900, 920, 600, "course"),
        "reviews": Card("reviews", "academy_course_reviews", [("PK", "id"), ("LOG", "resource_type + course_id"), ("", "user_name"), ("", "rating / content"), ("", "created_at")], 1640, 920, 600, "course"),
        "textbook": Card("textbook", "excellent_textbooks", [("PK", "id"), ("UK", "external_textbook_id"), ("", "textbook_name"), ("", "chief_editor / publisher"), ("", "category / isbn"), ("", "cover_file_path")], 240, 1560, 620, "textbook"),
        "detail": Card("detail", "academy_textbook_details", [("PK", "id"), ("LOG", "textbook_id -> external_textbook_id"), ("", "recommendation"), ("", "price / reader_count"), ("", "overview / catalog_text")], 1020, 1560, 650, "textbook"),
        "cart": Card("cart", "academy_textbook_cart_items", [("PK", "id"), ("FK", "user_id -> users.id"), ("LOG", "textbook_id -> external_textbook_id"), ("", "quantity")], 1820, 1560, 620, "textbook"),
        "orders": Card("orders", "academy_textbook_orders", [("PK", "id"), ("FK", "user_id -> users.id"), ("UK", "order_no"), ("", "total_amount"), ("", "order_status")], 2580, 1510, 620, "textbook"),
        "items": Card("items", "academy_textbook_order_items", [("PK", "id"), ("FK", "order_id -> orders.id"), ("LOG", "textbook_id -> external_textbook_id"), ("", "textbook_name"), ("", "unit_price / quantity")], 3260, 1510, 600, "textbook"),
        "book_reviews": Card("book_reviews", "academy_textbook_reviews", [("PK", "id"), ("FK", "user_id -> users.id"), ("LOG", "textbook_id -> external_textbook_id"), ("", "user_name"), ("", "rating / content")], 2580, 1980, 620, "textbook"),
    }
    rels = [
        dict(source="users", target="teacher", label="1:N", source_side="right", target_side="left", via=[(520, 435), (3200, 435)], label_at=(2850, 398)),
        dict(source="users", target="enroll", label="1:N", source_side="bottom", target_side="left", via=[(300, 860), (760, 1090)], label_at=(565, 970)),
        dict(source="users", target="cart", label="1:N", source_side="bottom", target_side="left", via=[(300, 1450), (1760, 1730)], label_at=(1380, 1490)),
        dict(source="users", target="orders", label="1:N", source_side="bottom", target_side="left", via=[(300, 1485), (2520, 1680)], label_at=(2110, 1510)),
        dict(source="users", target="book_reviews", label="1:N", source_side="bottom", target_side="left", via=[(300, 2080), (2520, 2160)], label_at=(1990, 2075)),
        dict(source="online", target="enroll", label="逻辑 1:N", source_side="bottom", target_side="top", dashed=True, via=[(1515, 805), (1200, 805)], label_at=(1370, 790)),
        dict(source="general", target="reviews", label="逻辑 1:N", source_side="bottom", target_side="top", dashed=True, via=[(2205, 805), (1940, 805)], label_at=(2070, 790)),
        dict(source="micro", target="enroll", label="逻辑 1:N", source_side="bottom", target_side="top", dashed=True, via=[(2895, 835), (1200, 835)], label_at=(2470, 820)),
        dict(source="teacher", target="reviews", label="逻辑 1:N", source_side="bottom", target_side="top", dashed=True, via=[(3555, 865), (1940, 865)], label_at=(2870, 850)),
        dict(source="textbook", target="detail", label="逻辑 1:1", source_side="right", target_side="left", dashed=True, label_at=(940, 1710)),
        dict(source="textbook", target="cart", label="逻辑 1:N", source_side="right", target_side="left", dashed=True, via=[(920, 1825), (1760, 1825)], label_at=(1360, 1810)),
        dict(source="textbook", target="items", label="逻辑 1:N", source_side="right", target_side="left", dashed=True, via=[(960, 1880), (3200, 1880)], label_at=(2280, 1865)),
        dict(source="orders", target="items", label="1:N", source_side="right", target_side="left", label_at=(3235, 1620)),
        dict(source="textbook", target="book_reviews", label="逻辑 1:N", source_side="right", target_side="left", dashed=True, via=[(980, 2180), (2520, 2180)], label_at=(1840, 2165)),
    ]
    render_cards(image, cards, rels, (100, 2050))
    draw.text((770, 2115), "说明：课程与教材部分大量使用业务编码关联，图中以虚线表示逻辑关联。", font=FONT_NOTE, fill=MUTED)
    return image


def draw_question_exam() -> Image.Image:
    image, draw = base_image(4100, 2600, "题库、作业与考试 ER 图", "课程题库与通用题源并存；作业/考试题目可逻辑引用 OJ 题目")
    cards = {
        "users": Card("users", "users", [("PK", "id"), ("", "username / role")], 70, 1160, 420, "user"),
        "cat": Card("cat", "course_question_bank_categories", [("PK", "id"), ("UK", "category_code"), ("", "category_name"), ("", "sort_order")], 160, 250, 590, "question"),
        "sets": Card("sets", "course_question_bank_sets", [("PK", "id"), ("FK", "category_id -> categories.id"), ("UK", "set_code"), ("", "title / subtitle"), ("", "difficulty_label / status_label"), ("", "source_name / cover_file_path")], 920, 210, 640, "question"),
        "cq": Card("cq", "course_question_bank_questions", [("PK", "id"), ("FK", "set_id -> sets.id"), ("", "question_type"), ("", "stem / options_json"), ("", "answer / explanation"), ("", "difficulty_label")], 1720, 210, 660, "question"),
        "mistake": Card("mistake", "course_question_bank_mistakes", [("PK", "id"), ("FK", "user_id -> users.id"), ("FK", "question_id -> course_questions.id"), ("", "selected_answer / correct_answer"), ("", "wrong_count / mastered")], 2680, 180, 660, "question"),
        "favorite": Card("favorite", "course_question_bank_favorites", [("PK", "id"), ("FK", "user_id -> users.id"), ("FK", "question_id -> course_questions.id"), ("", "created_at")], 2680, 630, 660, "question"),
        "subjects": Card("subjects", "question_bank_subjects", [("PK", "id"), ("UK", "subject_code"), ("", "subject_name"), ("", "sort_order")], 160, 1580, 600, "question"),
        "tags": Card("tags", "question_bank_tags", [("PK", "id"), ("UK", "source + external_tag_id"), ("", "tag_name"), ("", "tag_type / parent_external_tag_id")], 920, 1580, 650, "question"),
        "problems": Card("problems", "question_bank_problems", [("PK", "id"), ("UK", "source + external_problem_id"), ("", "title / difficulty_label"), ("", "tag_ids / tag_names(JSON)"), ("", "description / hint"), ("", "total_submit / total_accepted")], 1720, 1550, 700, "question"),
        "ps": Card("ps", "question_bank_problem_subjects", [("PK", "problem_id + subject_id"), ("FK", "problem_id -> problems.id"), ("FK", "subject_id -> subjects.id")], 2680, 1620, 680, "question"),
        "assign": Card("assign", "academy_assignments", [("PK", "id"), ("UK", "assignment_code"), ("LOG", "course_resource_type + course_id"), ("", "assignment_title"), ("", "deadline_at / total_score")], 3440, 250, 610, "question"),
        "aq": Card("aq", "academy_assignment_questions", [("PK", "id"), ("FK", "assignment_id -> assignments.id"), ("", "question_order / question_type"), ("", "question_title / options"), ("", "score")], 3440, 760, 610, "question"),
        "asub": Card("asub", "academy_assignment_submissions", [("PK", "id"), ("FK", "assignment_id -> assignments.id"), ("FK", "user_id -> users.id"), ("", "submission_status"), ("", "answer_payload / score")], 3440, 1250, 610, "question"),
        "exam": Card("exam", "academy_exams", [("PK", "id"), ("UK", "exam_code"), ("LOG", "course_resource_type + course_id"), ("", "exam_title / exam_status"), ("", "starts_at / deadline_at")], 3440, 1740, 610, "question"),
        "eq": Card("eq", "academy_exam_questions", [("PK", "id"), ("FK", "exam_id -> exams.id"), ("LOG", "oj_problem_id -> oj_problems.id"), ("", "question_order / question_type"), ("", "correct_answer / auto_gradable")], 2680, 2140, 660, "question"),
        "esub": Card("esub", "academy_exam_submissions", [("PK", "id"), ("FK", "exam_id -> exams.id"), ("FK", "user_id -> users.id"), ("", "submission_status"), ("", "answer_payload / score")], 3440, 2140, 610, "question"),
    }
    rels = [
        dict(source="cat", target="sets", label="1:N", source_side="right", target_side="left", label_at=(835, 365)),
        dict(source="sets", target="cq", label="1:N", source_side="right", target_side="left", label_at=(1645, 365)),
        dict(source="cq", target="mistake", label="1:N", source_side="right", target_side="left", via=[(2520, 365)], label_at=(2495, 340)),
        dict(source="cq", target="favorite", label="1:N", source_side="right", target_side="left", via=[(2520, 565)], label_at=(2495, 545)),
        dict(source="users", target="mistake", label="1:N", source_side="right", target_side="bottom", via=[(2520, 1280), (3010, 1280)], label_at=(1960, 1265)),
        dict(source="users", target="favorite", label="1:N", source_side="right", target_side="bottom", via=[(2450, 1350), (3010, 1350)], label_at=(1920, 1335)),
        dict(source="subjects", target="ps", label="1:N", source_side="right", target_side="left", via=[(2420, 1740)], label_at=(1420, 1720)),
        dict(source="problems", target="ps", label="1:N", source_side="right", target_side="left", label_at=(2540, 1715)),
        dict(source="tags", target="problems", label="JSON 逻辑", source_side="right", target_side="left", dashed=True, label_at=(1645, 1720)),
        dict(source="assign", target="aq", label="1:N", source_side="bottom", target_side="top", label_at=(3745, 705)),
        dict(source="assign", target="asub", label="1:N", source_side="bottom", target_side="top", via=[(3820, 1180)], label_at=(3840, 1140)),
        dict(source="users", target="asub", label="1:N", source_side="right", target_side="left", via=[(3260, 1430)], label_at=(2840, 1410)),
        dict(source="exam", target="eq", label="1:N", source_side="left", target_side="right", via=[(3380, 2040)], label_at=(3340, 2010)),
        dict(source="exam", target="esub", label="1:N", source_side="bottom", target_side="top", label_at=(3745, 2110)),
        dict(source="users", target="esub", label="1:N", source_side="right", target_side="left", via=[(3300, 2390)], label_at=(2920, 2370)),
        dict(source="eq", target="problems", label="OJ 逻辑引用", source_side="left", target_side="right", dashed=True, via=[(2550, 2300), (2550, 1720)], label_at=(2540, 2000)),
    ]
    render_cards(image, cards, rels, (80, 2200))
    draw.text((780, 2260), "说明：course_question_bank_* 为课程题库；question_bank_* 为通用题源；作业/考试通过课程编码与课程资源逻辑关联。", font=FONT_NOTE, fill=MUTED)
    return image


def draw_oj() -> Image.Image:
    image, draw = base_image(3000, 1700, "OJ 在线评测 ER 图", "用户创建题目、提交代码，提交明细关联测试用例")
    cards = {
        "users": Card("users", "users", [("PK", "id"), ("", "username / role")], 160, 600, 480, "user"),
        "problems": Card("problems", "oj_problems", [("PK", "id"), ("UK", "slug"), ("FK", "created_by -> users.id"), ("", "title / difficulty"), ("", "time_limit_ms / memory_limit_kb"), ("", "status / tags(JSON)")], 980, 230, 660, "oj"),
        "cases": Card("cases", "oj_test_cases", [("PK", "id"), ("FK", "problem_id -> oj_problems.id"), ("", "input_data / expected_output"), ("", "sample / weight"), ("", "sort_order")], 2140, 230, 650, "oj"),
        "sub": Card("sub", "oj_submissions", [("PK", "id"), ("FK", "problem_id -> oj_problems.id"), ("FK", "user_id -> users.id"), ("", "language / source_code"), ("", "status / score"), ("", "time_used_ms / memory_used_kb")], 980, 1020, 660, "oj"),
        "subcase": Card("subcase", "oj_submission_cases", [("PK", "id"), ("FK", "submission_id -> submissions.id"), ("FK", "test_case_id -> test_cases.id"), ("", "status"), ("", "time_used_ms / memory_used_kb")], 2140, 1020, 650, "oj"),
    }
    rels = [
        dict(source="users", target="problems", label="1:N 创建", source_side="right", target_side="left", via=[(800, 520)], label_at=(800, 490)),
        dict(source="users", target="sub", label="1:N 提交", source_side="right", target_side="left", via=[(780, 1170)], label_at=(790, 1140)),
        dict(source="problems", target="cases", label="1:N", source_side="right", target_side="left", label_at=(1890, 390)),
        dict(source="problems", target="sub", label="1:N", source_side="bottom", target_side="top", label_at=(1310, 870)),
        dict(source="sub", target="subcase", label="1:N", source_side="right", target_side="left", label_at=(1890, 1180)),
        dict(source="cases", target="subcase", label="1:N", source_side="bottom", target_side="top", label_at=(2460, 870)),
    ]
    render_cards(image, cards, rels, (160, 1350))
    return image


def draw_game_experiment() -> Image.Image:
    image, draw = base_image(3800, 2300, "游戏金币、仿真实验与测井 ER 图", "金币奖励由学习时长、答题、游戏记录和后台调整共同形成")
    cards = {
        "users": Card("users", "users", [("PK", "id"), ("", "username / role")], 90, 950, 450, "user"),
        "ladder": Card("ladder", "game_ladder_jump_records", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "question_bank_code"), ("", "total_coins"), ("", "correct_count / wrong_count"), ("", "duration_seconds")], 780, 240, 650, "game"),
        "warrior": Card("warrior", "game_type_warrior_records", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "reached_wave / completed_wave_count"), ("", "score / max_combo"), ("", "solved_word_count / total_kill_count"), ("", "duration_seconds")], 1540, 240, 700, "game"),
        "coin": Card("coin", "coin_reward_records", [("PK", "id"), ("FK", "user_id -> users.id"), ("UK", "user_id + source_type + source_key"), ("", "source_type / source_key"), ("", "reason / amount"), ("", "reference_id")], 2460, 270, 700, "game"),
        "profile": Card("profile", "profile_user_profiles", [("PK", "user_id"), ("FK", "user_id -> users.id"), ("", "admin_coin_adjustment"), ("", "admin_data_note")], 3220, 350, 520, "user"),
        "reservoir": Card("reservoir", "production_reservoir_record", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "formation_pressure"), ("", "permeability / water_saturation"), ("", "daily_oil / daily_water")], 740, 1030, 650, "experiment"),
        "pump": Card("pump", "production_pump_record", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "stroke / stroke_times"), ("", "pump_diameter"), ("", "work_condition"), ("", "indicator_chart_data(JSON)")], 1500, 1030, 650, "experiment"),
        "water": Card("water", "production_waterflood_record", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "injection_rate"), ("", "effect_day / breakthrough_day"), ("", "peak_oil"), ("", "production_curve(JSON)")], 2260, 1030, 650, "experiment"),
        "stim": Card("stim", "production_stimulation_record", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "type"), ("", "sand_volume / acid_volume"), ("", "displacement / fracture_length"), ("", "stimulation_ratio")], 3020, 1030, 650, "experiment"),
        "well": Card("well", "well_log_record", [("PK", "id"), ("FK", "user_id -> users.id"), ("", "porosity"), ("", "oil_saturation"), ("", "report_json(JSON)")], 1020, 1760, 620, "experiment"),
        "template": Card("template", "well_log_template", [("PK", "id"), ("", "template_name"), ("", "depth_array(JSON)"), ("", "gr_base(JSON)"), ("", "remark")], 1980, 1760, 620, "experiment"),
    }
    rels = [
        dict(source="users", target="ladder", label="1:N", source_side="right", target_side="left", via=[(610, 600)], label_at=(620, 570)),
        dict(source="users", target="warrior", label="1:N", source_side="right", target_side="left", via=[(610, 675), (1480, 475)], label_at=(1130, 650)),
        dict(source="users", target="coin", label="1:N", source_side="right", target_side="left", via=[(610, 760), (2400, 520)], label_at=(1900, 720)),
        dict(source="users", target="profile", label="1:1 调整", source_side="right", target_side="left", via=[(610, 830), (3160, 560)], label_at=(2550, 800)),
        dict(source="ladder", target="coin", label="奖励来源", source_side="right", target_side="left", dashed=True, via=[(2350, 400)], label_at=(1980, 382)),
        dict(source="warrior", target="coin", label="奖励来源", source_side="right", target_side="left", dashed=True, label_at=(2350, 540)),
        dict(source="users", target="reservoir", label="1:N", source_side="right", target_side="left", via=[(610, 1190)], label_at=(635, 1160)),
        dict(source="users", target="pump", label="1:N", source_side="right", target_side="left", via=[(610, 1260), (1440, 1190)], label_at=(1120, 1240)),
        dict(source="users", target="water", label="1:N", source_side="right", target_side="left", via=[(610, 1330), (2200, 1190)], label_at=(1850, 1310)),
        dict(source="users", target="stim", label="1:N", source_side="right", target_side="left", via=[(610, 1400), (2960, 1190)], label_at=(2600, 1380)),
        dict(source="users", target="well", label="1:N", source_side="right", target_side="left", via=[(610, 1910), (960, 1910)], label_at=(780, 1885)),
        dict(source="template", target="well", label="选择模板/生成报告", source_side="left", target_side="right", dashed=True, label_at=(1810, 1900)),
    ]
    render_cards(image, cards, rels, (90, 1950))
    draw.text((760, 2010), "说明：production_* 为石油生产仿真实验记录；well_log_template 提供测井解释模板数据。", font=FONT_NOTE, fill=MUTED)
    return image


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    diagrams = {
        "studyplatform-er-overview": draw_overview(),
        "studyplatform-er-user-profile": draw_user_profile(),
        "studyplatform-er-course-textbook": draw_course_textbook(),
        "studyplatform-er-question-exam": draw_question_exam(),
        "studyplatform-er-oj": draw_oj(),
        "studyplatform-er-game-experiment": draw_game_experiment(),
    }
    for name, image in diagrams.items():
        save_all(image, name)
    # Keep the previous database logical design asset aligned with the new overview figure.
    diagrams["studyplatform-er-overview"].save(OUTPUT_DIR / "studyplatform-database-logical-design.png", dpi=(220, 220))
    print(OUTPUT_DIR / "studyplatform-database-logical-design.png")


if __name__ == "__main__":
    main()
