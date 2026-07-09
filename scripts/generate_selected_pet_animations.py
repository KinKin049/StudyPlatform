from __future__ import annotations

import json
import math
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1].resolve()
SELECTED_DIR = (
    ROOT
    / "studyplatform-vue"
    / "src"
    / "assets"
    / "pet"
    / "pixel_pet_batch_20260709"
    / "selected_candidates"
).resolve()
ANIM_DIR = (SELECTED_DIR / "animations").resolve()
FRAME_DIR = (ANIM_DIR / "frames").resolve()
GIF_DIR = (ANIM_DIR / "gif").resolve()
SHEET_DIR = (ANIM_DIR / "spritesheets").resolve()
CANVAS = 192
FRAME_COUNT = 12
FRAME_DURATION = 95


ACTION_LABELS = {
    "idle": "待机",
    "happy": "开心",
    "study": "学习",
    "sleep": "睡觉",
    "levelup": "升级",
}


def ensure_inside_workspace(path: Path) -> None:
    path.resolve().relative_to(ROOT)


def clear_dir(path: Path) -> None:
    ensure_inside_workspace(path)
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True, exist_ok=True)


def nearest_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    return image.resize(size, Image.Resampling.NEAREST)


def transform_sprite(sprite: Image.Image, scale_x: float, scale_y: float, rotate: float = 0) -> Image.Image:
    width = max(1, int(round(sprite.width * scale_x)))
    height = max(1, int(round(sprite.height * scale_y)))
    result = nearest_resize(sprite, (width, height))
    if rotate:
        result = result.rotate(rotate, resample=Image.Resampling.NEAREST, expand=True)
    return result


def paste_center(canvas: Image.Image, sprite: Image.Image, x: int, y: int) -> None:
    canvas.alpha_composite(sprite, (int(x - sprite.width / 2), int(y - sprite.height / 2)))


def draw_shadow(draw: ImageDraw.ImageDraw, cx: int, cy: int, width: int, alpha: int) -> None:
    draw.ellipse(
        (cx - width // 2, cy - 9, cx + width // 2, cy + 8),
        fill=(60, 80, 110, max(20, min(95, alpha))),
    )


def draw_book(draw: ImageDraw.ImageDraw, frame: int) -> None:
    page_flip = 3 if frame % 4 in (1, 2) else 0
    draw.rounded_rectangle((47, 126, 95, 153), radius=4, fill="#5b7cff", outline="#2f4275", width=3)
    draw.rounded_rectangle((97, 126, 145, 153), radius=4, fill="#6de1c8", outline="#2f6f67", width=3)
    draw.line((96, 128, 96, 153), fill="#ffffff", width=2)
    draw.line((58, 136, 85 + page_flip, 136), fill="#e8edff", width=2)
    draw.line((108 - page_flip, 137, 134, 137), fill="#e8fff9", width=2)


def draw_sleep_marks(draw: ImageDraw.ImageDraw, frame: int) -> None:
    rise = (frame % FRAME_COUNT) * 4
    opacity = 255 - frame * 12
    for offset, size in ((0, 16), (18, 12), (31, 9)):
        x = 124 + offset
        y = 72 - rise // 2 - offset // 2
        color = (91, 124, 255, max(70, opacity - offset * 4))
        draw.text((x, y), "Z", fill=color, font=get_font(size))


def draw_levelup_effect(draw: ImageDraw.ImageDraw, frame: int, cx: int, cy: int) -> None:
    radius = 28 + frame * 5
    pulse = abs(6 - frame)
    for i in range(12):
        angle = math.tau * i / 12 + frame * 0.18
        inner = radius - 13 - pulse
        outer = radius + 14
        x1 = cx + math.cos(angle) * inner
        y1 = cy + math.sin(angle) * inner
        x2 = cx + math.cos(angle) * outer
        y2 = cy + math.sin(angle) * outer
        draw.line((x1, y1, x2, y2), fill="#ffdd63", width=4)
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline="#8ff7ff", width=4)
    draw.ellipse((cx - radius // 2, cy - radius // 2, cx + radius // 2, cy + radius // 2), outline="#ff9bd2", width=3)


def draw_sparkles(draw: ImageDraw.ImageDraw, frame: int) -> None:
    points = [(38, 48), (151, 54), (44, 112), (151, 121), (96, 34)]
    for index, (x, y) in enumerate(points):
        phase = (frame + index * 2) % FRAME_COUNT
        size = 3 + (phase % 4)
        color = "#fff27d" if index % 2 else "#8ff7ff"
        draw.line((x - size, y, x + size, y), fill=color, width=2)
        draw.line((x, y - size, x, y + size), fill=color, width=2)


def get_font(size: int) -> ImageFont.ImageFont:
    for font_name in ("arial.ttf", "seguisym.ttf"):
        try:
            return ImageFont.truetype(font_name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def frame_idle(sprite: Image.Image, frame: int) -> Image.Image:
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas, "RGBA")
    angle = math.tau * frame / FRAME_COUNT
    x = 96 + round(math.sin(angle) * 14)
    y = 102 + round(math.cos(angle) * 8)
    tilt = math.sin(angle) * 12
    scale_y = 1.0 + math.cos(angle) * 0.08
    scale_x = 1.0 - math.cos(angle) * 0.05
    draw_shadow(draw, 96, 166, 68 + int(math.cos(angle) * 10), 70)
    paste_center(canvas, transform_sprite(sprite, scale_x, scale_y, tilt), x, y)
    return canvas


def frame_happy(sprite: Image.Image, frame: int) -> Image.Image:
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas, "RGBA")
    progress = frame / (FRAME_COUNT - 1)
    jump = abs(math.sin(progress * math.pi * 2))
    x = 96 + round(math.sin(progress * math.pi * 4) * 18)
    y = 116 - round(jump * 42)
    tilt = math.sin(progress * math.pi * 4) * 22
    squash = 0.20 if frame in (0, 6) else 0
    scale_x = 1.0 + jump * 0.14 + squash
    scale_y = 1.0 + jump * 0.12 - squash * 0.55
    draw_shadow(draw, 96, 170, 88 - int(jump * 36), 80 - int(jump * 35))
    draw_sparkles(draw, frame)
    paste_center(canvas, transform_sprite(sprite, scale_x, scale_y, tilt), x, y)
    return canvas


def frame_study(sprite: Image.Image, frame: int) -> Image.Image:
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas, "RGBA")
    angle = math.tau * frame / FRAME_COUNT
    nod = math.sin(angle * 2)
    x = 96 + round(math.sin(angle) * 8)
    y = 88 + round(nod * 12)
    tilt = nod * 14
    draw_shadow(draw, 96, 169, 74, 66)
    draw_book(draw, frame)
    draw.arc((58, 36, 134, 104), start=200, end=340, fill="#7df4e5", width=3)
    draw.line((50, 69, 60, 77), fill="#7df4e5", width=3)
    paste_center(canvas, transform_sprite(sprite, 0.92, 0.92, tilt), x, y)
    return canvas


def frame_sleep(sprite: Image.Image, frame: int) -> Image.Image:
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas, "RGBA")
    angle = math.tau * frame / FRAME_COUNT
    sway = math.sin(angle)
    x = 91 + round(sway * 11)
    y = 113 + round(math.cos(angle) * 6)
    tilt = -18 + sway * 12
    draw_shadow(draw, 96, 169, 82, 70)
    draw_sleep_marks(draw, frame)
    paste_center(canvas, transform_sprite(sprite, 0.96, 0.86, tilt), x, y)
    return canvas


def frame_levelup(sprite: Image.Image, frame: int) -> Image.Image:
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas, "RGBA")
    progress = frame / (FRAME_COUNT - 1)
    burst = math.sin(progress * math.pi)
    x = 96
    y = 108 - round(burst * 34)
    spin = frame * 22
    scale = 0.88 + burst * 0.42
    draw_levelup_effect(draw, frame, 96, 100)
    draw_shadow(draw, 96, 172, 92 - int(burst * 44), 78 - int(burst * 34))
    paste_center(canvas, transform_sprite(sprite, scale, scale, spin), x, y)
    draw.text((74, 14), "UP!", fill="#ff77b7", font=get_font(22))
    return canvas


FRAME_BUILDERS = {
    "idle": frame_idle,
    "happy": frame_happy,
    "study": frame_study,
    "sleep": frame_sleep,
    "levelup": frame_levelup,
}


def save_gif(frames: list[Image.Image], path: Path) -> None:
    frames[0].save(
        path,
        save_all=True,
        append_images=frames[1:],
        duration=FRAME_DURATION,
        loop=0,
        disposal=2,
        transparency=0,
    )


def save_spritesheet(frames: list[Image.Image], path: Path) -> None:
    sheet = Image.new("RGBA", (CANVAS * len(frames), CANVAS), (0, 0, 0, 0))
    for index, frame in enumerate(frames):
        sheet.alpha_composite(frame, (index * CANVAS, 0))
    sheet.save(path)


def load_selected_files() -> list[Path]:
    manifest_path = SELECTED_DIR / "selected_pixel_pet_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    files = []
    for item in manifest:
        file = SELECTED_DIR / item["filename"]
        if file.exists():
            files.append(file)
    return files


def make_preview(gif_first_frames: list[tuple[str, Image.Image]]) -> None:
    columns = 5
    tile_w = 190
    tile_h = 222
    rows = math.ceil(len(gif_first_frames) / columns)
    preview = Image.new("RGBA", (columns * tile_w, rows * tile_h), "#f6fbff")
    draw = ImageDraw.Draw(preview)
    font = get_font(13)
    for index, (label, image) in enumerate(gif_first_frames):
        x = (index % columns) * tile_w
        y = (index // columns) * tile_h
        draw.rounded_rectangle((x + 8, y + 8, x + tile_w - 8, y + tile_h - 8), radius=16, fill="#ffffff", outline="#d8e8f2", width=2)
        draw.rounded_rectangle((x + 25, y + 22, x + tile_w - 25, y + 164), radius=14, fill="#fff7fb")
        preview.alpha_composite(image, (x + (tile_w - image.width) // 2, y + 4))
        draw.text((x + 18, y + 180), label, fill="#425466", font=font)
    preview.convert("RGB").save(ANIM_DIR / "selected_pet_animation_preview.png", quality=95)


def make_combo_gif(pet_files: list[Path]) -> None:
    label_font = get_font(12)
    columns = len(pet_files)
    tile_w = 172
    tile_h = 204
    combo_frames = []
    happy_sequences = []
    for pet_file in pet_files:
        sprite = Image.open(pet_file).convert("RGBA")
        happy_sequences.append([frame_happy(sprite, frame) for frame in range(FRAME_COUNT)])

    for frame_index in range(FRAME_COUNT):
        canvas = Image.new("RGBA", (columns * tile_w, tile_h), "#f6fbff")
        draw = ImageDraw.Draw(canvas)
        for index, pet_file in enumerate(pet_files):
            x = index * tile_w
            draw.rounded_rectangle((x + 8, 8, x + tile_w - 8, tile_h - 8), radius=16, fill="#ffffff", outline="#d8e8f2", width=2)
            canvas.alpha_composite(happy_sequences[index][frame_index], (x + (tile_w - CANVAS) // 2, 0))
            draw.text((x + 14, tile_h - 26), pet_file.stem[:22], fill="#425466", font=label_font)
        combo_frames.append(canvas)
    save_gif(combo_frames, ANIM_DIR / "selected_pet_happy_combo_preview.gif")


def main() -> None:
    ensure_inside_workspace(SELECTED_DIR)
    clear_dir(ANIM_DIR)
    FRAME_DIR.mkdir(parents=True, exist_ok=True)
    GIF_DIR.mkdir(parents=True, exist_ok=True)
    SHEET_DIR.mkdir(parents=True, exist_ok=True)

    pet_files = load_selected_files()
    preview_frames: list[tuple[str, Image.Image]] = []
    generated = []
    for pet_file in pet_files:
        sprite = Image.open(pet_file).convert("RGBA")
        for action, builder in FRAME_BUILDERS.items():
            frames = [builder(sprite, frame) for frame in range(FRAME_COUNT)]
            pet_action_name = f"{pet_file.stem}_{action}"
            pet_frame_dir = FRAME_DIR / pet_action_name
            pet_frame_dir.mkdir(parents=True, exist_ok=True)
            for frame_index, frame in enumerate(frames):
                frame.save(pet_frame_dir / f"{pet_action_name}_{frame_index:02d}.png")
            save_gif(frames, GIF_DIR / f"{pet_action_name}.gif")
            save_spritesheet(frames, SHEET_DIR / f"{pet_action_name}_sheet.png")
            preview_frames.append((f"{pet_file.stem} / {ACTION_LABELS[action]}", frames[3]))
            generated.append(
                {
                    "pet": pet_file.name,
                    "action": action,
                    "label": ACTION_LABELS[action],
                    "gif": f"gif/{pet_action_name}.gif",
                    "spritesheet": f"spritesheets/{pet_action_name}_sheet.png",
                    "frames": f"frames/{pet_action_name}",
                    "frame_count": FRAME_COUNT,
                    "duration_ms": FRAME_DURATION,
                    "canvas": CANVAS,
                }
            )

    make_preview(preview_frames)
    make_combo_gif(pet_files)
    (ANIM_DIR / "selected_pet_animation_manifest.json").write_text(
        json.dumps(generated, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"Generated {len(generated)} animations for {len(pet_files)} pets")
    print(ANIM_DIR)


if __name__ == "__main__":
    main()
