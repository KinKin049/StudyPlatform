from pathlib import Path
import math
import random

from PIL import Image, ImageDraw, ImageFilter


WIDTH = 1536
HEIGHT = 1024
OUTPUT_DIR = Path(__file__).resolve().parents[1] / "src" / "assets" / "home"


def blend(start, end, ratio):
    return tuple(int(start[index] + (end[index] - start[index]) * ratio) for index in range(3))


def vertical_gradient(top, bottom):
    image = Image.new("RGB", (WIDTH, HEIGHT), top)
    draw = ImageDraw.Draw(image)
    for y_position in range(HEIGHT):
        ratio = y_position / max(HEIGHT - 1, 1)
        draw.line([(0, y_position), (WIDTH, y_position)], fill=blend(top, bottom, ratio))
    return image.convert("RGBA")


def add_glow(image, xy, color, radius):
    glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow)
    draw.ellipse(xy, fill=color)
    glow = glow.filter(ImageFilter.GaussianBlur(radius))
    image.alpha_composite(glow)


def rounded_rect(draw, xy, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(xy, radius=radius, fill=fill, outline=outline, width=width)


def draw_grid(draw, color, step=64, perspective=False):
    for x_position in range(-WIDTH, WIDTH * 2, step):
        if perspective:
            draw.line([(WIDTH // 2, HEIGHT * 0.58), (x_position, HEIGHT)], fill=color, width=2)
        else:
            draw.line([(x_position, 0), (x_position, HEIGHT)], fill=color, width=1)
    for y_position in range(0, HEIGHT, step):
        draw.line([(0, y_position), (WIDTH, y_position)], fill=color, width=1)


def save(image, name):
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    image.convert("RGB").save(OUTPUT_DIR / name, quality=95, optimize=True)


def academy():
    image = vertical_gradient((176, 231, 222), (230, 255, 235))
    draw = ImageDraw.Draw(image, "RGBA")
    add_glow(image, (880, -120, 1660, 520), (255, 255, 255, 170), 70)
    add_glow(image, (-220, 540, 520, 1220), (74, 214, 159, 100), 90)
    draw_grid(draw, (255, 255, 255, 34), 72)

    for index, x_position in enumerate([150, 340, 560, 870, 1080, 1270]):
        height = [500, 640, 570, 700, 520, 620][index]
        y_position = HEIGHT - height - 70
        fill = (25, 78, 86, 145) if index % 2 else (32, 104, 99, 132)
        rounded_rect(draw, (x_position, y_position, x_position + 150, HEIGHT - 72), 28, fill, (255, 255, 255, 70), 2)
        for row in range(y_position + 38, HEIGHT - 128, 58):
            draw.rounded_rectangle((x_position + 26, row, x_position + 124, row + 18), radius=8, fill=(210, 255, 246, 92))
        draw.ellipse((x_position + 88, y_position + 80, x_position + 190, y_position + 182), fill=(117, 224, 90, 130))

    for index, (x_position, y_position) in enumerate([(330, 250), (720, 160), (1000, 320), (560, 430)]):
        card = Image.new("RGBA", image.size, (0, 0, 0, 0))
        card_draw = ImageDraw.Draw(card, "RGBA")
        rounded_rect(card_draw, (x_position, y_position, x_position + 250, y_position + 132), 30, (255, 255, 255, 118), (255, 255, 255, 190), 2)
        card_draw.rounded_rectangle((x_position + 28, y_position + 30, x_position + 206, y_position + 48), radius=8, fill=(41, 154, 136, 132))
        card_draw.rounded_rectangle((x_position + 28, y_position + 68, x_position + 170, y_position + 84), radius=8, fill=(76, 176, 255, 110))
        card_draw.ellipse((x_position + 190, y_position + 64, x_position + 226, y_position + 100), fill=(198, 255, 42, 180))
        card = card.rotate([-7, 4, 8, -3][index], resample=Image.Resampling.BICUBIC, center=(x_position + 125, y_position + 66))
        image.alpha_composite(card)

    save(image, "academy-future.png")


def practice():
    image = vertical_gradient((15, 128, 209), (221, 248, 255))
    draw = ImageDraw.Draw(image, "RGBA")
    add_glow(image, (780, -160, 1600, 560), (255, 255, 255, 150), 82)
    add_glow(image, (-260, 300, 620, 1180), (64, 206, 255, 96), 92)
    draw_grid(draw, (220, 255, 255, 58), 58, perspective=True)

    for band in range(8):
        y_position = 150 + band * 88
        draw.arc((-120, y_position, WIDTH + 160, y_position + 520), 185, 355, fill=(150, 235, 255, 70), width=3)

    random.seed(8)
    for row in range(4):
        for column in range(5):
            x_position = 210 + column * 230 + random.randint(-18, 18)
            y_position = 210 + row * 145 + random.randint(-16, 16)
            alpha = 120 if row < 3 else 86
            rounded_rect(draw, (x_position, y_position, x_position + 170, y_position + 96), 22, (255, 255, 255, alpha), (255, 255, 255, 165), 2)
            draw.rounded_rectangle((x_position + 22, y_position + 26, x_position + 132, y_position + 38), radius=6, fill=(20, 120, 220, 124))
            draw.rounded_rectangle((x_position + 22, y_position + 56, x_position + 104, y_position + 68), radius=6, fill=(54, 211, 255, 150))

    points = [(210, 790), (460, 650), (760, 700), (1010, 520), (1260, 610)]
    for start, end in zip(points, points[1:]):
        draw.line([start, end], fill=(141, 238, 255, 210), width=8)
    for point in points:
        draw.ellipse((point[0] - 20, point[1] - 20, point[0] + 20, point[1] + 20), fill=(255, 255, 255, 230))
        draw.ellipse((point[0] - 10, point[1] - 10, point[0] + 10, point[1] + 10), fill=(0, 151, 255, 230))

    save(image, "practice-future.png")


def lab():
    image = vertical_gradient((32, 21, 84), (219, 214, 255))
    draw = ImageDraw.Draw(image, "RGBA")
    add_glow(image, (760, -150, 1560, 580), (190, 172, 255, 132), 80)
    add_glow(image, (-180, 520, 560, 1240), (82, 221, 255, 88), 90)
    draw_grid(draw, (255, 255, 255, 34), 64)

    chamber = Image.new("RGBA", image.size, (0, 0, 0, 0))
    chamber_draw = ImageDraw.Draw(chamber, "RGBA")
    chamber_draw.rounded_rectangle((470, 150, 1130, 880), radius=96, fill=(255, 255, 255, 36), outline=(255, 255, 255, 95), width=4)
    chamber_draw.ellipse((530, 210, 1070, 750), fill=(105, 83, 224, 72), outline=(255, 255, 255, 88), width=3)
    chamber_draw.ellipse((640, 320, 960, 640), fill=(16, 14, 54, 110), outline=(167, 236, 255, 130), width=5)
    image.alpha_composite(chamber)

    for index in range(18):
        angle = math.tau * index / 18
        x_center = 800 + math.cos(angle) * 210
        y_center = 480 + math.sin(angle) * 150
        draw.line([(800, 480), (x_center, y_center)], fill=(176, 239, 255, 48), width=2)
        draw.ellipse((x_center - 9, y_center - 9, x_center + 9, y_center + 9), fill=(188, 238, 255, 170))

    for x_position, y_position, width_value, height_value in [(120, 170, 290, 235), (1160, 220, 270, 260), (190, 650, 330, 210)]:
        rounded_rect(draw, (x_position, y_position, x_position + width_value, y_position + height_value), 28, (16, 15, 52, 132), (210, 226, 255, 110), 2)
        for row in range(32, height_value - 30, 34):
            line_width = random.randint(110, width_value - 54)
            color = (150, 235, 255, 130) if row % 68 else (198, 172, 255, 155)
            draw.rounded_rectangle((x_position + 28, y_position + row, x_position + line_width, y_position + row + 10), radius=5, fill=color)

    model_points = [(770, 430), (880, 480), (820, 600), (700, 560)]
    draw.polygon(model_points, fill=(255, 255, 255, 70), outline=(203, 236, 255, 180))
    draw.line([(770, 430), (820, 600), (880, 480), (700, 560), (770, 430)], fill=(208, 246, 255, 180), width=3)

    save(image, "lab-future.png")


def games():
    image = vertical_gradient((255, 149, 34), (255, 237, 197))
    draw = ImageDraw.Draw(image, "RGBA")
    add_glow(image, (760, -160, 1580, 590), (255, 255, 255, 120), 76)
    add_glow(image, (-160, 390, 650, 1190), (255, 72, 122, 90), 92)
    draw_grid(draw, (255, 255, 255, 36), 68, perspective=True)

    path = [(170, 780), (360, 640), (550, 690), (770, 520), (1030, 590), (1300, 390)]
    for index in range(len(path) - 1):
        draw.line([path[index], path[index + 1]], fill=(60, 28, 7, 120), width=26)
        draw.line([path[index], path[index + 1]], fill=(255, 229, 99, 220), width=9)
    for index, point in enumerate(path):
        draw.ellipse((point[0] - 36, point[1] - 36, point[0] + 36, point[1] + 36), fill=(255, 210, 56, 235), outline=(90, 46, 5, 110), width=4)
        draw.ellipse((point[0] - 15, point[1] - 15, point[0] + 15, point[1] + 15), fill=(255, 255, 225, 210))

    for x_position, y_position, width_value, height_value in [(210, 220, 340, 170), (980, 160, 330, 190), (640, 690, 420, 150)]:
        rounded_rect(draw, (x_position, y_position, x_position + width_value, y_position + height_value), 30, (35, 18, 8, 128), (255, 235, 173, 120), 2)
        for column in range(5):
            key_x = x_position + 34 + column * 54
            key_y = y_position + height_value - 62
            draw.rounded_rectangle((key_x, key_y, key_x + 38, key_y + 28), radius=8, fill=(255, 255, 255, 142))
        draw.rounded_rectangle((x_position + 34, y_position + 36, x_position + width_value - 38, y_position + 58), radius=8, fill=(255, 132, 64, 180))

    random.seed(12)
    for _ in range(38):
        x_position = random.randint(80, WIDTH - 100)
        y_position = random.randint(80, HEIGHT - 160)
        radius = random.randint(9, 24)
        draw.ellipse((x_position - radius, y_position - radius, x_position + radius, y_position + radius), fill=(255, 214, 63, random.randint(110, 210)), outline=(255, 255, 220, 110), width=2)

    save(image, "games-future.png")


if __name__ == "__main__":
    academy()
    practice()
    lab()
    games()
    print(f"generated images in {OUTPUT_DIR}")
