from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


out = Path("tmp/render_chapter5_revised_word")
pages = sorted(out.glob("page-*.png"))
font_path = Path(r"C:\Windows\Fonts\msyhbd.ttc")
font = ImageFont.truetype(str(font_path), 24) if font_path.exists() else ImageFont.load_default()
thumb_w, thumb_h = 300, 400
cols, rows = 4, 3

for sheet_index in range((len(pages) + cols * rows - 1) // (cols * rows)):
    subset = pages[sheet_index * cols * rows : (sheet_index + 1) * cols * rows]
    sheet = Image.new("RGB", (cols * thumb_w, rows * thumb_h), "white")
    draw = ImageDraw.Draw(sheet)
    for item_index, page in enumerate(subset):
        image = Image.open(page).convert("RGB")
        image.thumbnail((thumb_w - 20, thumb_h - 45), Image.LANCZOS)
        x = (item_index % cols) * thumb_w + (thumb_w - image.width) // 2
        y = (item_index // cols) * thumb_h + 35
        sheet.paste(image, (x, y))
        label = page.stem.replace("page-", "Page ")
        draw.text(((item_index % cols) * thumb_w + 12, (item_index // cols) * thumb_h + 6), label, fill="black", font=font)
    sheet.save(out / f"contact-{sheet_index + 1:02d}.png")

print(f"created {len(list(out.glob('contact-*.png')))}")
