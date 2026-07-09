from __future__ import annotations

import json
import math
import random
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError as exc:  # pragma: no cover
    raise SystemExit("Pillow is required. Please install it with: python -m pip install pillow") from exc


ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "studyplatform-vue" / "src" / "assets" / "pet"
SPRITE_SIZE = 32
SCALE = 4
OUTPUT_SIZE = SPRITE_SIZE * SCALE


PALETTES = {
    "pink": {"main": "#ffb7d5", "dark": "#e879a6", "light": "#ffe3ef", "accent": "#fff4a8"},
    "mint": {"main": "#8ee6c6", "dark": "#45b89b", "light": "#ddfff3", "accent": "#a7d8ff"},
    "blue": {"main": "#8fc7ff", "dark": "#4c86d9", "light": "#e2f2ff", "accent": "#ffd6ea"},
    "violet": {"main": "#c6a5ff", "dark": "#8661d6", "light": "#efe6ff", "accent": "#b9f7e5"},
    "cream": {"main": "#ffdca8", "dark": "#d89445", "light": "#fff3db", "accent": "#ffb7d5"},
    "orange": {"main": "#ffb36f", "dark": "#d76a35", "light": "#ffe4c9", "accent": "#fff4a8"},
    "gray": {"main": "#c7d0df", "dark": "#6d7b95", "light": "#f0f5ff", "accent": "#9df0ff"},
    "green": {"main": "#afe67d", "dark": "#67ad48", "light": "#efffdc", "accent": "#ffd6ea"},
}


def px(draw: ImageDraw.ImageDraw, x: int, y: int, color: str, size: int = 1) -> None:
    draw.rectangle((x, y, x + size - 1, y + size - 1), fill=color)


def rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: str) -> None:
    draw.rectangle((x, y, x + w - 1, y + h - 1), fill=color)


def ellipse(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, color: str) -> None:
    draw.ellipse((x, y, x + w - 1, y + h - 1), fill=color)


def poly(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], color: str) -> None:
    draw.polygon(points, fill=color)


def outline_ellipse(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, fill: str, outline: str) -> None:
    ellipse(draw, x - 1, y, w + 2, h, outline)
    ellipse(draw, x, y - 1, w, h + 2, outline)
    ellipse(draw, x, y, w, h, fill)


def outline_rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, fill: str, outline: str) -> None:
    rect(draw, x - 1, y, w + 2, h, outline)
    rect(draw, x, y - 1, w, h + 2, outline)
    rect(draw, x, y, w, h, fill)


def blush(draw: ImageDraw.ImageDraw, y: int = 17) -> None:
    rect(draw, 8, y, 3, 1, "#ff8fb1")
    rect(draw, 21, y, 3, 1, "#ff8fb1")


def sparkle(draw: ImageDraw.ImageDraw, x: int, y: int, color: str = "#ffffff") -> None:
    px(draw, x, y - 1, color)
    px(draw, x - 1, y, color)
    px(draw, x, y, color)
    px(draw, x + 1, y, color)
    px(draw, x, y + 1, color)


def face(draw: ImageDraw.ImageDraw, mood: str = "smile") -> None:
    eye = "#263047"
    px(draw, 11, 14, eye, 2)
    px(draw, 20, 14, eye, 2)
    px(draw, 12, 14, "#ffffff")
    px(draw, 21, 14, "#ffffff")
    if mood == "smile":
        px(draw, 15, 18, eye)
        px(draw, 16, 19, eye)
        px(draw, 17, 19, eye)
        px(draw, 18, 18, eye)
    elif mood == "cat":
        px(draw, 15, 18, eye)
        px(draw, 17, 18, eye)
        px(draw, 16, 19, eye)
    elif mood == "sleepy":
        rect(draw, 11, 14, 3, 1, eye)
        rect(draw, 20, 14, 3, 1, eye)
        rect(draw, 15, 18, 4, 1, eye)
    blush(draw)


def add_shadow(draw: ImageDraw.ImageDraw) -> None:
    ellipse(draw, 8, 26, 17, 3, "#4d5f8030")


def draw_animal(draw: ImageDraw.ImageDraw, kind: str, palette: dict[str, str], accessory: str) -> None:
    main, dark, light, accent = palette["main"], palette["dark"], palette["light"], palette["accent"]
    add_shadow(draw)

    if kind in {"cat", "fox"}:
        poly(draw, [(8, 9), (11, 3), (14, 10)], dark)
        poly(draw, [(18, 10), (21, 3), (24, 9)], dark)
        poly(draw, [(10, 8), (11, 5), (13, 10)], light)
        poly(draw, [(19, 10), (21, 5), (22, 8)], light)
    elif kind == "bunny":
        outline_ellipse(draw, 9, 1, 5, 12, main, dark)
        outline_ellipse(draw, 18, 1, 5, 12, main, dark)
        ellipse(draw, 11, 4, 2, 7, light)
        ellipse(draw, 20, 4, 2, 7, light)
    elif kind == "bear":
        outline_ellipse(draw, 7, 7, 6, 6, main, dark)
        outline_ellipse(draw, 19, 7, 6, 6, main, dark)
    elif kind == "panda":
        outline_ellipse(draw, 7, 7, 6, 6, "#1f2937", "#1f2937")
        outline_ellipse(draw, 19, 7, 6, 6, "#1f2937", "#1f2937")

    if kind == "fox":
        poly(draw, [(5, 20), (2, 15), (8, 16), (10, 21)], dark)
        poly(draw, [(3, 16), (6, 17), (4, 18)], light)
    elif kind == "dog":
        outline_ellipse(draw, 5, 10, 6, 10, dark, dark)
        outline_ellipse(draw, 21, 10, 6, 10, dark, dark)
    else:
        ellipse(draw, 5, 18, 5, 4, main)
        ellipse(draw, 22, 18, 5, 4, main)

    outline_ellipse(draw, 7, 8, 18, 17, main if kind != "panda" else "#f8fafc", dark)
    ellipse(draw, 11, 17, 10, 6, light)

    if kind == "panda":
        ellipse(draw, 9, 12, 6, 5, "#1f2937")
        ellipse(draw, 18, 12, 6, 5, "#1f2937")
        px(draw, 11, 14, "#ffffff", 2)
        px(draw, 20, 14, "#ffffff", 2)
        rect(draw, 15, 18, 3, 1, "#1f2937")
        px(draw, 16, 19, "#1f2937")
        blush(draw)
    else:
        face(draw, "cat" if kind in {"cat", "fox"} else "smile")

    if kind == "fox":
        ellipse(draw, 13, 18, 6, 4, "#fff2dc")
    if accessory == "bow":
        rect(draw, 14, 6, 4, 3, accent)
        poly(draw, [(13, 6), (10, 4), (10, 8)], accent)
        poly(draw, [(18, 6), (21, 4), (21, 8)], accent)
    elif accessory == "star":
        sparkle(draw, 24, 7, accent)
    elif accessory == "leaf":
        poly(draw, [(15, 5), (19, 2), (20, 6)], "#78c96d")
        px(draw, 17, 5, dark)


def draw_spirit(draw: ImageDraw.ImageDraw, kind: str, palette: dict[str, str], accessory: str) -> None:
    main, dark, light, accent = palette["main"], palette["dark"], palette["light"], palette["accent"]
    add_shadow(draw)

    if kind == "fairy":
        ellipse(draw, 4, 9, 8, 10, "#dff8ff80")
        ellipse(draw, 20, 9, 8, 10, "#dff8ff80")
        outline_ellipse(draw, 9, 6, 14, 18, main, dark)
        ellipse(draw, 11, 7, 10, 6, light)
        rect(draw, 13, 23, 2, 4, dark)
        rect(draw, 18, 23, 2, 4, dark)
        sparkle(draw, 25, 5, accent)
    elif kind == "slime":
        outline_ellipse(draw, 7, 10, 19, 15, main, dark)
        ellipse(draw, 10, 8, 8, 6, main)
        ellipse(draw, 13, 11, 7, 3, light)
        sparkle(draw, 23, 8, accent)
    elif kind == "dragon":
        poly(draw, [(8, 10), (9, 4), (13, 10)], dark)
        poly(draw, [(19, 10), (23, 4), (24, 10)], dark)
        outline_ellipse(draw, 7, 8, 18, 16, main, dark)
        poly(draw, [(4, 18), (1, 13), (7, 14)], accent)
        poly(draw, [(25, 14), (31, 13), (28, 18)], accent)
        rect(draw, 14, 5, 2, 3, accent)
        rect(draw, 17, 5, 2, 3, accent)
    elif kind == "mushroom":
        outline_rect(draw, 12, 15, 8, 10, light, dark)
        outline_ellipse(draw, 7, 7, 18, 11, main, dark)
        ellipse(draw, 10, 9, 3, 3, accent)
        ellipse(draw, 18, 9, 4, 3, accent)
    else:  # star sprite
        poly(draw, [(16, 4), (19, 12), (27, 12), (21, 17), (23, 25), (16, 20), (9, 25), (11, 17), (5, 12), (13, 12)], main)
        poly(draw, [(16, 6), (18, 13), (24, 13), (19, 17), (21, 22), (16, 19), (11, 22), (13, 17), (8, 13), (14, 13)], light)

    face(draw, "smile")
    if accessory == "halo":
        rect(draw, 11, 3, 10, 1, accent)
        px(draw, 10, 4, accent)
        px(draw, 21, 4, accent)
    elif accessory == "gem":
        poly(draw, [(16, 3), (19, 6), (16, 9), (13, 6)], accent)
        px(draw, 16, 5, "#ffffff")


def draw_robot(draw: ImageDraw.ImageDraw, kind: str, palette: dict[str, str], accessory: str) -> None:
    main, dark, light, accent = palette["main"], palette["dark"], palette["light"], palette["accent"]
    add_shadow(draw)
    rect(draw, 15, 4, 2, 4, dark)
    px(draw, 14, 3, accent, 4)

    if kind == "roundbot":
        outline_ellipse(draw, 7, 8, 18, 17, main, dark)
    elif kind == "cube":
        outline_rect(draw, 7, 8, 18, 17, main, dark)
        px(draw, 7, 8, light)
        px(draw, 24, 8, light)
    elif kind == "screenbot":
        outline_rect(draw, 6, 8, 20, 16, dark, dark)
        rect(draw, 8, 10, 16, 10, "#1e293b")
        rect(draw, 10, 12, 12, 6, main)
    else:  # capsule
        outline_ellipse(draw, 8, 8, 16, 17, main, dark)
        rect(draw, 8, 14, 16, 6, main)

    rect(draw, 4, 15, 4, 2, dark)
    rect(draw, 24, 15, 4, 2, dark)
    rect(draw, 10, 25, 3, 3, dark)
    rect(draw, 19, 25, 3, 3, dark)

    if kind == "screenbot":
        px(draw, 11, 14, "#ffffff", 2)
        px(draw, 20, 14, "#ffffff", 2)
        rect(draw, 15, 18, 4, 1, "#ffffff")
    else:
        px(draw, 11, 14, "#253047", 2)
        px(draw, 20, 14, "#253047", 2)
        px(draw, 12, 14, "#ffffff")
        px(draw, 21, 14, "#ffffff")
        rect(draw, 15, 19, 4, 1, "#253047")
    rect(draw, 11, 10, 10, 2, light)

    if accessory == "heart":
        px(draw, 15, 6, "#ff7aa8")
        px(draw, 17, 6, "#ff7aa8")
        rect(draw, 15, 7, 4, 1, "#ff7aa8")
        rect(draw, 16, 8, 2, 1, "#ff7aa8")
    elif accessory == "bolt":
        poly(draw, [(22, 5), (18, 12), (22, 11), (19, 18), (25, 9), (21, 10)], accent)
    elif accessory == "spark":
        sparkle(draw, 25, 6, accent)


def render_pet(pet: dict[str, str]) -> Image.Image:
    img = Image.new("RGBA", (SPRITE_SIZE, SPRITE_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img, "RGBA")
    palette = PALETTES[pet["palette"]]
    if pet["family"] == "animal":
        draw_animal(draw, pet["kind"], palette, pet["accessory"])
    elif pet["family"] == "spirit":
        draw_spirit(draw, pet["kind"], palette, pet["accessory"])
    else:
        draw_robot(draw, pet["kind"], palette, pet["accessory"])

    return img.resize((OUTPUT_SIZE, OUTPUT_SIZE), Image.Resampling.NEAREST)


def build_pets() -> list[dict[str, str]]:
    animals = ["cat", "dog", "bunny", "bear", "panda", "fox"]
    spirits = ["fairy", "slime", "dragon", "mushroom", "star"]
    robots = ["roundbot", "cube", "screenbot", "capsule"]
    palette_names = list(PALETTES)
    family_accessories = {
        "animal": ["bow", "star", "leaf"],
        "spirit": ["halo", "gem", "star", "spark"],
        "robot": ["heart", "bolt", "spark"],
    }

    pets: list[dict[str, str]] = []
    index = 1
    for family, kinds, count in (
        ("animal", animals, 36),
        ("spirit", spirits, 32),
        ("robot", robots, 28),
    ):
        for offset in range(count):
            kind_index = offset % len(kinds)
            kind = kinds[kind_index]
            palette = palette_names[(offset + kind_index * 3 + len(family)) % len(palette_names)]
            accessories = family_accessories[family]
            accessory = accessories[(offset // len(kinds) + kind_index) % len(accessories)]
            pets.append(
                {
                    "id": f"pet_{index:02d}",
                    "family": family,
                    "kind": kind,
                    "palette": palette,
                    "accessory": accessory,
                    "filename": f"pet_{index:02d}_{family}_{kind}_{palette}.png",
                }
            )
            index += 1
    return pets


def make_contact_sheet(pets: list[dict[str, str]], images: list[Image.Image]) -> Image.Image:
    columns = 8
    tile = 164
    label_height = 24
    rows = math.ceil(len(pets) / columns)
    sheet = Image.new("RGBA", (columns * tile, rows * tile), "#f6fbff")
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 14)
    except OSError:
        font = ImageFont.load_default()

    for idx, (pet, img) in enumerate(zip(pets, images)):
        col = idx % columns
        row = idx // columns
        x = col * tile
        y = row * tile
        card = Image.new("RGBA", (tile - 12, tile - 12), "#ffffff")
        card_draw = ImageDraw.Draw(card)
        card_draw.rounded_rectangle((0, 0, tile - 13, tile - 13), radius=14, fill="#ffffff", outline="#d8e8f2", width=2)
        bg_color = random.choice(["#fff3fb", "#eefcff", "#f5f0ff", "#fff8e8", "#efffed"])
        card_draw.rounded_rectangle((18, 16, tile - 31, tile - 47), radius=12, fill=bg_color)
        card.alpha_composite(img, ((tile - 12 - OUTPUT_SIZE) // 2, 14))
        label = f'{pet["id"]}  {pet["family"]}-{pet["kind"]}'
        card_draw.text((14, tile - label_height - 16), label, fill="#425466", font=font)
        sheet.alpha_composite(card, (x + 6, y + 6))
    return sheet.convert("RGB")


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    pets = build_pets()
    images = []
    for pet in pets:
        image = render_pet(pet)
        image.save(OUT_DIR / pet["filename"])
        images.append(image)

    sheet = make_contact_sheet(pets, images)
    sheet.save(OUT_DIR / "pixel_pet_catalog.png", quality=95)

    (OUT_DIR / "pixel_pet_manifest.json").write_text(
        json.dumps(pets, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"Generated {len(pets)} pixel pets")
    print(f"Output: {OUT_DIR}")
    print(f"Catalog: {OUT_DIR / 'pixel_pet_catalog.png'}")


if __name__ == "__main__":
    main()
