from __future__ import annotations

import json
import shutil
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1].resolve()
BATCH_DIR = (ROOT / "studyplatform-vue" / "src" / "assets" / "pet" / "pixel_pet_batch_20260709").resolve()
SELECTED_DIR = (BATCH_DIR / "selected_candidates").resolve()
SELECTED_IDS = ["07", "27", "20", "29", "44", "75", "49", "39"]
SOURCE_SHADOW_PIXEL = (77, 95, 128, 48)


def ensure_inside_workspace(path: Path) -> None:
    path.resolve().relative_to(ROOT)


def find_pet_file(pet_id: str) -> Path:
    matches = sorted(BATCH_DIR.glob(f"pet_{pet_id}_*.png"))
    if not matches:
        raise FileNotFoundError(f"Cannot find pet_{pet_id}_*.png in {BATCH_DIR}")
    return matches[0]


def remove_source_shadow(path: Path) -> None:
    image = Image.open(path).convert("RGBA")
    pixels = image.load()
    changed = False
    for y in range(image.height):
        for x in range(image.width):
            if pixels[x, y] == SOURCE_SHADOW_PIXEL:
                pixels[x, y] = (0, 0, 0, 0)
                changed = True
    if changed:
        image.save(path)


def make_catalog(files: list[Path]) -> None:
    tile = 168
    columns = len(files)
    width = columns * tile
    height = tile
    sheet = Image.new("RGBA", (width, height), "#f6fbff")
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("arial.ttf", 14)
    except OSError:
        font = ImageFont.load_default()

    for index, file in enumerate(files):
        image = Image.open(file).convert("RGBA")
        x = index * tile
        card = Image.new("RGBA", (tile - 12, tile - 12), "#ffffff")
        card_draw = ImageDraw.Draw(card)
        card_draw.rounded_rectangle((0, 0, tile - 13, tile - 13), radius=14, fill="#ffffff", outline="#d8e8f2", width=2)
        card_draw.rounded_rectangle((20, 16, tile - 33, tile - 47), radius=12, fill="#fff7fb")
        card.alpha_composite(image, ((tile - 12 - image.width) // 2, 14))
        label = file.stem.replace("_", " ")
        card_draw.text((12, tile - 40), label[:24], fill="#425466", font=font)
        sheet.alpha_composite(card, (x + 6, 6))

    sheet.convert("RGB").save(SELECTED_DIR / "selected_pixel_pet_catalog.png", quality=95)


def main() -> None:
    ensure_inside_workspace(BATCH_DIR)
    ensure_inside_workspace(SELECTED_DIR)
    SELECTED_DIR.mkdir(parents=True, exist_ok=True)

    selected_files = []
    manifest_items = []
    manifest_path = BATCH_DIR / "pixel_pet_manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest_by_id = {item["id"].replace("pet_", ""): item for item in manifest}

    for pet_id in SELECTED_IDS:
        source = find_pet_file(pet_id)
        target = SELECTED_DIR / source.name
        shutil.copy2(source, target)
        remove_source_shadow(target)
        selected_files.append(target)
        manifest_items.append(manifest_by_id.get(pet_id, {"id": f"pet_{pet_id}", "filename": source.name}))

    make_catalog(selected_files)
    (SELECTED_DIR / "selected_pixel_pet_manifest.json").write_text(
        json.dumps(manifest_items, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"Selected {len(selected_files)} pets")
    print(SELECTED_DIR)


if __name__ == "__main__":
    main()
