from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "studyplatform-vue" / "src" / "assets" / "pet" / "pixel-candidates"
SPRITE_SIZE = 32
SCALE = 4
PREVIEW_SIZE = SPRITE_SIZE * SCALE


@dataclass(frozen=True)
class Candidate:
    code: str
    category: str
    name: str
    template: str
    body: str
    accent: str
    detail: str
    mood: str = "happy"


ANIMALS = [
    Candidate("A01", "cute-animal", "peach-bunny", "bunny", "#ffd1df", "#ff7fb0", "#fff5b8"),
    Candidate("A02", "cute-animal", "nebula-kitten", "cat", "#bca7ff", "#6e5bff", "#ffe680"),
    Candidate("A03", "cute-animal", "mint-panda", "panda", "#d7fff2", "#72d6b4", "#2d365f"),
    Candidate("A04", "cute-animal", "pudding-puppy", "dog", "#ffd887", "#e58b45", "#fff2c4"),
    Candidate("A05", "cute-animal", "cloud-sheep", "sheep", "#f7fbff", "#a9d9ff", "#ffc6dd"),
    Candidate("A06", "cute-animal", "honey-bear", "bear", "#f6bf63", "#bd763d", "#fff4a8"),
    Candidate("A07", "cute-animal", "star-penguin", "penguin", "#8ed7ff", "#324b7a", "#fff3a5"),
    Candidate("A08", "cute-animal", "strawberry-fox", "fox", "#ff9d8f", "#ff5d6c", "#fff1cf"),
    Candidate("A09", "cute-animal", "milk-cow", "cow", "#fff8ee", "#463d59", "#ffc9dd"),
    Candidate("A10", "cute-animal", "tiny-owl", "owl", "#d2b48c", "#8b6b52", "#ffeaa0"),
    Candidate("A11", "cute-animal", "blue-whale", "whale", "#91dcff", "#4d8bff", "#fff8d6"),
    Candidate("A12", "cute-animal", "cocoa-squirrel", "squirrel", "#c98758", "#8a5636", "#ffe0a3"),
    Candidate("A13", "cute-animal", "lemon-duck", "duck", "#ffe879", "#ffb347", "#fff7c7"),
    Candidate("A14", "cute-animal", "snow-ferret", "ferret", "#f4f5ff", "#9aa7ff", "#ffd0e8"),
    Candidate("A15", "cute-animal", "bubble-axolotl", "axolotl", "#ffc0d8", "#ff7cae", "#a7f1ff"),
    Candidate("A16", "cute-animal", "matcha-frog", "frog", "#a7e87b", "#64b85c", "#fff8a8"),
]


SPRITES = [
    Candidate("S01", "anime-sprite", "leaf-pixie", "pixie", "#bff2a1", "#58c777", "#ffe88a"),
    Candidate("S02", "anime-sprite", "moon-fairy", "fairy", "#d9d4ff", "#8d7bff", "#fff3a6"),
    Candidate("S03", "anime-sprite", "star-mage", "mage", "#8cc8ff", "#5554c9", "#ffe66d"),
    Candidate("S04", "anime-sprite", "jelly-sprite", "jelly", "#9df7ff", "#54b8ff", "#ffd1ef"),
    Candidate("S05", "anime-sprite", "flower-elf", "elf", "#ffc5df", "#ff7fb2", "#fff097"),
    Candidate("S06", "anime-sprite", "snow-spirit", "spirit", "#e8fbff", "#80cfff", "#d0e4ff"),
    Candidate("S07", "anime-sprite", "flame-buddy", "flame", "#ffb05c", "#ff5c5c", "#fff4a3"),
    Candidate("S08", "anime-sprite", "water-wisp", "wisp", "#8ddcff", "#477dff", "#d7fff8"),
    Candidate("S09", "anime-sprite", "book-charm", "book", "#d4b4ff", "#8467d7", "#fff0a8"),
    Candidate("S10", "anime-sprite", "dream-ghost", "ghost", "#f6f0ff", "#b99cff", "#ffd7ec"),
    Candidate("S11", "anime-sprite", "mushroom-kid", "mushroom", "#ffd1b0", "#ff776e", "#fff4ca"),
    Candidate("S12", "anime-sprite", "sparkle-slime", "slime", "#b7f7df", "#58d7bd", "#fff28d"),
    Candidate("S13", "anime-sprite", "cloud-nymph", "nymph", "#d9f3ff", "#8cc9ff", "#ffe2f0"),
    Candidate("S14", "anime-sprite", "crystal-mini", "crystal", "#c7edff", "#7a8cff", "#fff5a8"),
    Candidate("S15", "anime-sprite", "pumpkin-fae", "pumpkin", "#ffc16e", "#ff8a42", "#baffb0"),
    Candidate("S16", "anime-sprite", "cosmic-baby", "cosmic", "#b8a9ff", "#5951d6", "#ffed92"),
]


ROBOTS = [
    Candidate("R01", "cute-robot", "capsule-bot", "bot", "#dff8ff", "#65b9ff", "#ffd76d"),
    Candidate("R02", "cute-robot", "cat-ear-bot", "catbot", "#e8e3ff", "#8f7cff", "#ffcfeb"),
    Candidate("R03", "cute-robot", "study-cube", "cube", "#cdefff", "#5aa6e8", "#ffe06c"),
    Candidate("R04", "cute-robot", "round-droid", "roundbot", "#f1fbff", "#83c7ff", "#a6ffd8"),
    Candidate("R05", "cute-robot", "heart-bot", "heartbot", "#ffe2eb", "#ff8ab3", "#fff1a8"),
    Candidate("R06", "cute-robot", "book-bot", "bookbot", "#d6fff2", "#52c7a9", "#fff0a3"),
    Candidate("R07", "cute-robot", "star-antenna", "antenna", "#e7eeff", "#7087ff", "#ffe576"),
    Candidate("R08", "cute-robot", "cloud-mech", "mech", "#eef8ff", "#9bc9ff", "#ffd7f2"),
    Candidate("R09", "cute-robot", "tiny-rover", "rover", "#d6e8ff", "#7196de", "#fff09a"),
    Candidate("R10", "cute-robot", "sleepy-screen", "screen", "#222943", "#8de7ff", "#ffd1e7"),
    Candidate("R11", "cute-robot", "music-bot", "musicbot", "#f1e1ff", "#b47cff", "#fff184"),
    Candidate("R12", "cute-robot", "pencil-bot", "pencilbot", "#ffe098", "#ff9c4f", "#cdefff"),
    Candidate("R13", "cute-robot", "mochi-drone", "drone", "#f7fbff", "#9ad7ff", "#ffbeda"),
    Candidate("R14", "cute-robot", "bubble-terminal", "terminal", "#cdf7e7", "#2f9f88", "#fff2a4"),
    Candidate("R15", "cute-robot", "sprout-bot", "sproutbot", "#e9ffe0", "#70c768", "#ffe68a"),
    Candidate("R16", "cute-robot", "planet-bot", "planetbot", "#d8d1ff", "#6958d9", "#ffde7b"),
]


def hex_to_rgba(value: str) -> tuple[int, int, int, int]:
    value = value.lstrip("#")
    return tuple(int(value[index:index + 2], 16) for index in (0, 2, 4)) + (255,)


def shade(color: str, amount: int) -> tuple[int, int, int, int]:
    r, g, b, _ = hex_to_rgba(color)
    return (
        max(0, min(255, r + amount)),
        max(0, min(255, g + amount)),
        max(0, min(255, b + amount)),
        255,
    )


def rect(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], fill: str | tuple[int, int, int, int]) -> None:
    draw.rectangle(xy, fill=hex_to_rgba(fill) if isinstance(fill, str) else fill)


def ellipse(draw: ImageDraw.ImageDraw, xy: tuple[int, int, int, int], fill: str | tuple[int, int, int, int]) -> None:
    draw.ellipse(xy, fill=hex_to_rgba(fill) if isinstance(fill, str) else fill)


def poly(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: str | tuple[int, int, int, int]) -> None:
    draw.polygon(points, fill=hex_to_rgba(fill) if isinstance(fill, str) else fill)


def eye(draw: ImageDraw.ImageDraw, x: int, y: int, mood: str = "happy") -> None:
    dark = "#25233f"
    if mood == "sleepy":
        rect(draw, (x, y + 1, x + 2, y + 1), dark)
    else:
        rect(draw, (x, y, x + 1, y + 2), dark)
        rect(draw, (x + 1, y, x + 1, y), "#ffffff")


def blush(draw: ImageDraw.ImageDraw, x: int, y: int) -> None:
    rect(draw, (x, y, x + 2, y), "#ff8fb8")


def sparkle(draw: ImageDraw.ImageDraw, x: int, y: int, color: str) -> None:
    rect(draw, (x + 1, y, x + 1, y + 2), color)
    rect(draw, (x, y + 1, x + 2, y + 1), color)


def draw_animal(draw: ImageDraw.ImageDraw, pet: Candidate) -> None:
    body = pet.body
    accent = pet.accent
    detail = pet.detail
    outline = "#3a2d4d"

    ellipse(draw, (8, 10, 24, 26), outline)
    ellipse(draw, (9, 9, 23, 25), body)
    ellipse(draw, (11, 20, 21, 28), shade(body, 22))

    if pet.template == "bunny":
        ellipse(draw, (8, 2, 12, 13), outline)
        ellipse(draw, (20, 2, 24, 13), outline)
        ellipse(draw, (9, 3, 11, 13), body)
        ellipse(draw, (21, 3, 23, 13), body)
        rect(draw, (10, 5, 10, 10), accent)
        rect(draw, (22, 5, 22, 10), accent)
    elif pet.template == "cat":
        poly(draw, [(9, 11), (12, 4), (15, 12)], outline)
        poly(draw, [(17, 12), (20, 4), (23, 11)], outline)
        poly(draw, [(10, 10), (12, 5), (14, 11)], body)
        poly(draw, [(18, 11), (20, 5), (22, 10)], body)
        sparkle(draw, 24, 6, detail)
    elif pet.template == "panda":
        ellipse(draw, (7, 9, 13, 15), outline)
        ellipse(draw, (19, 9, 25, 15), outline)
        ellipse(draw, (11, 14, 15, 19), "#2d365f")
        ellipse(draw, (17, 14, 21, 19), "#2d365f")
    elif pet.template == "dog":
        ellipse(draw, (6, 11, 11, 18), outline)
        ellipse(draw, (21, 11, 26, 18), outline)
        ellipse(draw, (7, 11, 10, 18), accent)
        ellipse(draw, (22, 11, 25, 18), accent)
        rect(draw, (14, 22, 18, 24), "#fff4da")
    elif pet.template == "sheep":
        for x, y in [(7, 10), (10, 7), (14, 6), (18, 7), (22, 10)]:
            ellipse(draw, (x, y, x + 5, y + 5), body)
        ellipse(draw, (12, 13, 20, 23), "#f3d4be")
    elif pet.template == "bear":
        ellipse(draw, (7, 8, 13, 14), outline)
        ellipse(draw, (19, 8, 25, 14), outline)
        ellipse(draw, (8, 9, 12, 13), body)
        ellipse(draw, (20, 9, 24, 13), body)
        ellipse(draw, (13, 19, 19, 24), detail)
    elif pet.template == "penguin":
        ellipse(draw, (8, 8, 24, 27), outline)
        ellipse(draw, (11, 12, 21, 26), "#fff8ee")
        poly(draw, [(15, 18), (17, 18), (16, 20)], "#ffb347")
    elif pet.template == "fox":
        poly(draw, [(8, 11), (10, 4), (15, 12)], outline)
        poly(draw, [(17, 12), (22, 4), (24, 11)], outline)
        poly(draw, [(9, 10), (11, 5), (14, 12)], body)
        poly(draw, [(18, 12), (21, 5), (23, 10)], body)
        ellipse(draw, (12, 19, 20, 25), "#fff2d2")
        poly(draw, [(23, 22), (30, 20), (27, 27)], accent)
        rect(draw, (27, 24, 30, 27), "#fff2d2")
    elif pet.template == "cow":
        ellipse(draw, (7, 9, 13, 15), "#463d59")
        ellipse(draw, (19, 9, 25, 15), "#463d59")
        rect(draw, (11, 10, 15, 13), "#463d59")
        rect(draw, (19, 18, 22, 21), "#463d59")
        ellipse(draw, (13, 20, 19, 25), "#ffc9dd")
    elif pet.template == "owl":
        poly(draw, [(9, 10), (11, 5), (14, 11)], outline)
        poly(draw, [(18, 11), (21, 5), (23, 10)], outline)
        ellipse(draw, (10, 13, 15, 19), detail)
        ellipse(draw, (17, 13, 22, 19), detail)
        poly(draw, [(15, 19), (17, 19), (16, 21)], "#ffb347")
    elif pet.template == "whale":
        ellipse(draw, (6, 14, 26, 25), body)
        poly(draw, [(23, 17), (30, 12), (28, 21)], accent)
        rect(draw, (10, 23, 20, 25), "#f7fbff")
        sparkle(draw, 14, 6, detail)
    elif pet.template == "squirrel":
        ellipse(draw, (22, 10, 31, 24), accent)
        ellipse(draw, (24, 12, 29, 20), body)
        ellipse(draw, (12, 19, 20, 25), "#ffe0a3")
    elif pet.template == "duck":
        ellipse(draw, (8, 12, 23, 25), body)
        ellipse(draw, (11, 8, 22, 19), body)
        poly(draw, [(14, 17), (20, 17), (17, 20)], "#ff9c42")
    elif pet.template == "ferret":
        ellipse(draw, (5, 14, 27, 24), body)
        ellipse(draw, (10, 10, 22, 21), body)
        rect(draw, (9, 14, 22, 16), accent)
    elif pet.template == "axolotl":
        for x in [5, 22]:
            rect(draw, (x, 12, x + 4, 13), accent)
            rect(draw, (x, 16, x + 5, 17), accent)
            rect(draw, (x, 20, x + 4, 21), accent)
        ellipse(draw, (9, 10, 23, 24), body)
    elif pet.template == "frog":
        ellipse(draw, (8, 8, 14, 14), body)
        ellipse(draw, (18, 8, 24, 14), body)
        ellipse(draw, (8, 11, 24, 25), body)
        rect(draw, (12, 22, 20, 24), detail)

    eye(draw, 12, 15, pet.mood)
    eye(draw, 19, 15, pet.mood)
    rect(draw, (15, 20, 17, 20), "#3a2d4d")
    blush(draw, 10, 19)
    blush(draw, 21, 19)


def draw_sprite(draw: ImageDraw.ImageDraw, pet: Candidate) -> None:
    body = pet.body
    accent = pet.accent
    detail = pet.detail
    outline = "#35304f"

    if pet.template in {"flame", "wisp", "spirit", "ghost", "cosmic"}:
        poly(draw, [(16, 4), (23, 14), (21, 26), (16, 30), (10, 26), (8, 14)], outline)
        poly(draw, [(16, 5), (22, 14), (20, 25), (16, 28), (11, 25), (9, 14)], body)
    elif pet.template in {"book"}:
        rect(draw, (8, 9, 24, 26), outline)
        rect(draw, (9, 10, 15, 25), body)
        rect(draw, (17, 10, 23, 25), shade(body, 18))
        rect(draw, (16, 11, 16, 25), accent)
    elif pet.template in {"mushroom"}:
        ellipse(draw, (6, 7, 26, 18), accent)
        rect(draw, (11, 15, 21, 27), body)
        rect(draw, (9, 11, 12, 13), detail)
        rect(draw, (19, 10, 22, 12), detail)
    elif pet.template in {"crystal"}:
        poly(draw, [(16, 4), (24, 12), (22, 25), (16, 30), (10, 25), (8, 12)], outline)
        poly(draw, [(16, 5), (23, 13), (21, 24), (16, 28), (11, 24), (9, 13)], body)
        rect(draw, (16, 7, 18, 25), shade(body, 28))
    elif pet.template in {"pumpkin"}:
        ellipse(draw, (7, 11, 25, 27), outline)
        ellipse(draw, (8, 12, 24, 26), body)
        rect(draw, (15, 7, 17, 12), detail)
    else:
        ellipse(draw, (8, 9, 24, 26), outline)
        ellipse(draw, (9, 8, 23, 25), body)

    if pet.template in {"pixie", "fairy", "elf", "nymph"}:
        ellipse(draw, (3, 12, 10, 21), shade(detail, -8))
        ellipse(draw, (22, 12, 29, 21), shade(detail, -8))
        ellipse(draw, (10, 4, 14, 10), accent)
        ellipse(draw, (18, 4, 22, 10), accent)
    if pet.template == "mage":
        poly(draw, [(9, 10), (16, 2), (23, 10)], accent)
        rect(draw, (12, 8, 20, 10), outline)
        sparkle(draw, 24, 5, detail)
    if pet.template == "jelly":
        for x in [10, 14, 18, 22]:
            rect(draw, (x, 24, x, 28), accent)
    if pet.template == "slime":
        ellipse(draw, (7, 13, 25, 27), outline)
        ellipse(draw, (8, 12, 24, 26), body)
        sparkle(draw, 20, 8, detail)
    if pet.template == "flower":
        pass

    eye(draw, 12, 16)
    eye(draw, 19, 16)
    rect(draw, (15, 21, 17, 21), "#35304f")
    blush(draw, 10, 20)
    blush(draw, 21, 20)
    sparkle(draw, 5, 6, detail)
    sparkle(draw, 25, 25, detail)


def draw_robot(draw: ImageDraw.ImageDraw, pet: Candidate) -> None:
    body = pet.body
    accent = pet.accent
    detail = pet.detail
    outline = "#26324f"

    if pet.template in {"catbot"}:
        poly(draw, [(9, 10), (12, 4), (15, 10)], outline)
        poly(draw, [(17, 10), (20, 4), (23, 10)], outline)
    if pet.template in {"cube", "screen", "terminal"}:
        rect(draw, (8, 9, 24, 25), outline)
        rect(draw, (9, 10, 23, 24), body)
    elif pet.template in {"drone"}:
        rect(draw, (10, 10, 22, 21), outline)
        rect(draw, (11, 11, 21, 20), body)
        ellipse(draw, (3, 8, 9, 14), accent)
        ellipse(draw, (23, 8, 29, 14), accent)
    elif pet.template in {"rover"}:
        rect(draw, (8, 11, 24, 22), outline)
        rect(draw, (9, 12, 23, 21), body)
        ellipse(draw, (8, 22, 13, 27), outline)
        ellipse(draw, (19, 22, 24, 27), outline)
    else:
        ellipse(draw, (8, 8, 24, 25), outline)
        ellipse(draw, (9, 9, 23, 24), body)

    if pet.template in {"antenna", "musicbot", "sproutbot", "planetbot"}:
        rect(draw, (16, 4, 16, 8), outline)
        if pet.template == "sproutbot":
            ellipse(draw, (12, 3, 16, 7), "#76d875")
            ellipse(draw, (17, 3, 21, 7), "#76d875")
        else:
            sparkle(draw, 15, 2, detail)
    if pet.template in {"heartbot"}:
        rect(draw, (14, 19, 18, 22), accent)
        rect(draw, (13, 18, 14, 19), accent)
        rect(draw, (18, 18, 19, 19), accent)
    if pet.template in {"bookbot"}:
        rect(draw, (9, 23, 23, 27), accent)
        rect(draw, (16, 23, 16, 27), "#ffffff")
    if pet.template in {"pencilbot"}:
        poly(draw, [(23, 7), (29, 10), (24, 14)], "#ffe098")
        rect(draw, (23, 10, 26, 13), "#ff9c4f")
    if pet.template in {"cloud-mech"}:
        ellipse(draw, (5, 21, 13, 27), "#ffffff")
        ellipse(draw, (11, 20, 22, 28), "#ffffff")
        ellipse(draw, (20, 21, 28, 27), "#ffffff")

    eye(draw, 12, 15, "sleepy" if pet.template == "sleepy-screen" else "happy")
    eye(draw, 19, 15, "sleepy" if pet.template == "sleepy-screen" else "happy")
    rect(draw, (14, 20, 18, 20), outline)
    rect(draw, (5, 17, 8, 20), accent)
    rect(draw, (24, 17, 27, 20), accent)
    blush(draw, 10, 19)
    blush(draw, 21, 19)


def draw_candidate(pet: Candidate) -> Image.Image:
    image = Image.new("RGBA", (SPRITE_SIZE, SPRITE_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)

    ellipse(draw, (9, 27, 23, 30), (0, 0, 0, 36))
    if pet.category == "cute-animal":
        draw_animal(draw, pet)
    elif pet.category == "anime-sprite":
        draw_sprite(draw, pet)
    else:
        draw_robot(draw, pet)

    return image


def save_candidate(pet: Candidate) -> dict[str, str]:
    image = draw_candidate(pet)
    file_name = f"{pet.code.lower()}-{pet.name}.png"
    scaled = image.resize((PREVIEW_SIZE, PREVIEW_SIZE), Image.Resampling.NEAREST)
    scaled.save(OUT_DIR / file_name)
    return {
        "code": pet.code,
        "category": pet.category,
        "name": pet.name,
        "file": file_name,
    }


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for candidate in [
        Path("C:/Windows/Fonts/arial.ttf"),
        Path("C:/Windows/Fonts/segoeui.ttf"),
        Path("C:/Windows/Fonts/calibri.ttf"),
    ]:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def make_contact_sheet(entries: list[dict[str, str]]) -> None:
    columns = 8
    cell_width = 166
    cell_height = 178
    padding = 18
    title_height = 54
    rows = (len(entries) + columns - 1) // columns
    sheet = Image.new(
        "RGBA",
        (padding * 2 + columns * cell_width, title_height + padding + rows * cell_height),
        "#fff8fb",
    )
    draw = ImageDraw.Draw(sheet)
    title_font = load_font(24)
    label_font = load_font(13)
    small_font = load_font(11)

    draw.text((padding, 16), "Cute Pixel AI Pet Candidates · 48 concepts", fill="#3a2d4d", font=title_font)
    draw.text((padding + 560, 23), "A=Animals  S=Sprites  R=Robots", fill="#8a6b92", font=small_font)

    for index, entry in enumerate(entries):
        col = index % columns
        row = index // columns
        x = padding + col * cell_width
        y = title_height + padding + row * cell_height
        draw.rounded_rectangle((x, y, x + cell_width - 12, y + cell_height - 12), radius=18, fill="#ffffff", outline="#f0dceb", width=2)
        sprite = Image.open(OUT_DIR / entry["file"]).convert("RGBA")
        sheet.alpha_composite(sprite, (x + 19, y + 10))
        draw.text((x + 12, y + 136), f'{entry["code"]}  {entry["name"]}', fill="#3a2d4d", font=label_font)
        draw.text((x + 12, y + 154), entry["category"], fill="#9b7b9f", font=small_font)

    sheet.save(OUT_DIR / "pixel-pet-candidates-contact-sheet.png")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    candidates = ANIMALS + SPRITES + ROBOTS
    entries = [save_candidate(candidate) for candidate in candidates]
    make_contact_sheet(entries)
    (OUT_DIR / "manifest.json").write_text(
        json.dumps(entries, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Generated {len(entries)} pixel pet candidates in {OUT_DIR}")


if __name__ == "__main__":
    main()
