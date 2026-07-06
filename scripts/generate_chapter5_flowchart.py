from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "report-assets"
OUT_PATH = OUT_DIR / "studyplatform-module-detail-design.png"

WIDTH = 3600
HEIGHT = 2240

BG = "#FFFFFF"
TEXT = "#111111"
MUTED = "#555555"
LINE = "#222222"
OUTLINE = "#111111"
LANE = "#777777"


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


TITLE_FONT = load_font(68, True)
NODE_FONT = load_font(37, True)
SMALL_FONT = load_font(28)
LANE_FONT = load_font(30, True)
NOTE_FONT = load_font(30)


def text_size(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont) -> tuple[int, int]:
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0], bbox[3] - bbox[1]


def wrap_label(text: str, max_chars: int = 9) -> list[str]:
    if "\n" in text:
        return text.splitlines()
    if len(text) <= max_chars:
        return [text]
    lines: list[str] = []
    current = ""
    for char in text:
        current += char
        if len(current) >= max_chars:
            lines.append(current)
            current = ""
    if current:
        lines.append(current)
    return lines


def draw_centered(
    draw: ImageDraw.ImageDraw,
    box: tuple[int, int, int, int],
    text: str,
    font: ImageFont.FreeTypeFont,
) -> None:
    lines = wrap_label(text)
    gap = 8
    heights = [text_size(draw, line, font)[1] for line in lines]
    total_h = sum(heights) + gap * (len(lines) - 1)
    y = (box[1] + box[3]) / 2 - total_h / 2
    for line, line_h in zip(lines, heights):
        line_w, _ = text_size(draw, line, font)
        draw.text(((box[0] + box[2]) / 2 - line_w / 2, y), line, font=font, fill=TEXT)
        y += line_h + gap


def rounded(
    draw: ImageDraw.ImageDraw,
    center: tuple[int, int],
    size: tuple[int, int],
    text: str,
    radius: int = 36,
) -> tuple[int, int, int, int]:
    x, y = center
    w, h = size
    box = (x - w // 2, y - h // 2, x + w // 2, y + h // 2)
    draw.rounded_rectangle(box, radius=radius, fill=BG, outline=OUTLINE, width=5)
    draw_centered(draw, box, text, NODE_FONT)
    return box


def process(draw: ImageDraw.ImageDraw, center: tuple[int, int], text: str) -> tuple[int, int, int, int]:
    return rounded(draw, center, (440, 150), text)


def terminator(draw: ImageDraw.ImageDraw, center: tuple[int, int], text: str) -> tuple[int, int, int, int]:
    return rounded(draw, center, (440, 150), text, radius=75)


def decision(draw: ImageDraw.ImageDraw, center: tuple[int, int], text: str) -> tuple[int, int, int, int]:
    x, y = center
    w, h = 500, 230
    points = [(x, y - h // 2), (x + w // 2, y), (x, y + h // 2), (x - w // 2, y)]
    draw.polygon(points, fill=BG, outline=OUTLINE)
    draw.line(points + [points[0]], fill=OUTLINE, width=5)
    draw_centered(draw, (x - w // 2, y - h // 2, x + w // 2, y + h // 2), text, NODE_FONT)
    return (x - w // 2, y - h // 2, x + w // 2, y + h // 2)


def arrowhead(draw: ImageDraw.ImageDraw, end: tuple[int, int], direction: tuple[float, float]) -> None:
    ux, uy = direction
    px, py = -uy, ux
    size = 18
    points = [
        end,
        (int(end[0] - ux * size + px * size * 0.65), int(end[1] - uy * size + py * size * 0.65)),
        (int(end[0] - ux * size - px * size * 0.65), int(end[1] - uy * size - py * size * 0.65)),
    ]
    draw.polygon(points, fill=LINE)


def draw_label(draw: ImageDraw.ImageDraw, position: tuple[int, int], label: str) -> None:
    if not label:
        return
    label_w, label_h = text_size(draw, label, SMALL_FONT)
    x, y = position
    draw.rectangle((x - label_w / 2 - 10, y - label_h / 2 - 6, x + label_w / 2 + 10, y + label_h / 2 + 6), fill=BG)
    draw.text((x - label_w / 2, y - label_h / 2), label, font=SMALL_FONT, fill=TEXT)


def arrow(
    draw: ImageDraw.ImageDraw,
    start: tuple[int, int],
    end: tuple[int, int],
    label: str = "",
) -> None:
    draw.line([start, end], fill=LINE, width=5)
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    length = max((dx * dx + dy * dy) ** 0.5, 1)
    arrowhead(draw, end, (dx / length, dy / length))
    if label:
        draw_label(draw, ((start[0] + end[0]) // 2, (start[1] + end[1]) // 2 - 30), label)


def poly_arrow(
    draw: ImageDraw.ImageDraw,
    points: list[tuple[int, int]],
    label: str = "",
    label_at: tuple[int, int] | None = None,
) -> None:
    draw.line(points, fill=LINE, width=5, joint="curve")
    start, end = points[-2], points[-1]
    dx = end[0] - start[0]
    dy = end[1] - start[1]
    length = max((dx * dx + dy * dy) ** 0.5, 1)
    arrowhead(draw, end, (dx / length, dy / length))
    if label:
        draw_label(draw, label_at or points[len(points) // 2], label)


def draw_lane(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], title: str) -> None:
    draw.rounded_rectangle(box, radius=28, fill=BG, outline=LANE, width=3)
    draw.text((box[0] + 34, box[1] + 24), title, font=LANE_FONT, fill=MUTED)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw = ImageDraw.Draw(image)

    title = "金币激励与学习模块详细设计流程图"
    title_w, _ = text_size(draw, title, TITLE_FONT)
    draw.text(((WIDTH - title_w) / 2, 70), title, font=TITLE_FONT, fill=TEXT)

    draw_lane(draw, (150, 220, 3450, 610), "学习行为来源")
    draw_lane(draw, (150, 690, 3450, 1080), "接口接收与身份校验")
    draw_lane(draw, (150, 1160, 3450, 1550), "规则匹配与奖励结算")
    draw_lane(draw, (150, 1630, 3450, 2020), "数据沉淀与激励反馈")

    terminator(draw, (390, 425), "学习者")
    process(draw, (900, 425), "课程学习\n视频观看")
    process(draw, (1410, 425), "题库练习\n作业考试")
    process(draw, (1920, 425), "游戏训练\n实验/OJ")
    process(draw, (2430, 425), "AI宠物\n待办专注")
    process(draw, (2940, 425), "前端提交\n学习事件")

    process(draw, (2940, 895), "REST接口\n接收请求")
    process(draw, (2410, 895), "解析用户ID\n来源目标")
    decision(draw, (1820, 895), "身份参数\n是否有效")
    process(draw, (1220, 895), "异常提示\n拒绝结算")

    process(draw, (620, 1365), "规则引擎\n匹配奖励")
    process(draw, (1160, 1365), "时长/答题\n游戏折算")
    decision(draw, (1740, 1365), "是否重复\n或超限")
    process(draw, (2360, 1365), "生成奖励\n结算结果")
    process(draw, (2960, 1365), "不重复发放\n保留记录")

    process(draw, (620, 1835), "写入学习\n事件/时长")
    process(draw, (1160, 1835), "写入金币\n奖励流水")
    process(draw, (1740, 1835), "更新用户\n金币余额")
    process(draw, (2360, 1835), "同步画像\n学习统计")
    terminator(draw, (2960, 1835), "前端展示\n兑换资产")

    arrow(draw, (610, 425), (680, 425))
    arrow(draw, (1120, 425), (1190, 425))
    arrow(draw, (1630, 425), (1700, 425))
    arrow(draw, (2140, 425), (2210, 425))
    arrow(draw, (2650, 425), (2720, 425))
    poly_arrow(draw, [(2940, 500), (2940, 745), (2940, 820)])

    arrow(draw, (2720, 895), (2630, 895))
    arrow(draw, (2190, 895), (2070, 895))
    arrow(draw, (1570, 895), (1440, 895), "否")
    poly_arrow(draw, [(1820, 1010), (1820, 1115), (620, 1115), (620, 1290)], "是", (1740, 1085))

    arrow(draw, (840, 1365), (940, 1365))
    arrow(draw, (1380, 1365), (1490, 1365))
    arrow(draw, (1990, 1365), (2140, 1365), "否")
    arrow(draw, (2610, 1365), (2740, 1365), "是")
    poly_arrow(draw, [(2360, 1440), (2360, 1600), (620, 1600), (620, 1760)])
    poly_arrow(draw, [(2960, 1440), (2960, 1600), (620, 1600), (620, 1760)])

    arrow(draw, (840, 1835), (940, 1835))
    arrow(draw, (1380, 1835), (1520, 1835))
    arrow(draw, (1960, 1835), (2140, 1835))
    arrow(draw, (2580, 1835), (2740, 1835))

    notes = [
        "奖励规则：视频学习每10分钟5金币；可视化3金币；油气仿真4金币；题库、错题、OJ、作业和考试2金币。",
        "幂等控制：金币记录使用 user_id + source_type + source_key 去重，前端只提交行为，后端统一结算奖励数量。",
    ]
    y = 2070
    for note in notes:
        note_w, _ = text_size(draw, note, NOTE_FONT)
        draw.text(((WIDTH - note_w) / 2, y), note, font=NOTE_FONT, fill=MUTED)
        y += 42

    image.save(OUT_PATH, quality=95)
    print(OUT_PATH)


if __name__ == "__main__":
    main()
