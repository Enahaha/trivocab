#!/usr/bin/env python3
"""Generate the TriVocab home-screen icons (PNG) used by the PWA manifest.

Only Pillow is required.  Icons are written next to this script's output
directory so the Spring Boot static resources can serve them.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


STATIC_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/static"
ICON_DIR = STATIC_DIR / "icons"
BRAND_GREEN = (31, 111, 92, 255)
WHITE = (255, 255, 255, 255)


def font_at(size: int) -> ImageFont.FreeTypeFont:
    candidates = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    ]
    for path in candidates:
        if Path(path).is_file():
            try:
                return ImageFont.truetype(path, size=size)
            except OSError:
                continue
    return ImageFont.load_default()


def rounded_rectangle_mask(size: int, radius: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return mask


def render_icon(size: int) -> Image.Image:
    radius = max(8, int(size * 0.22))
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    icon.paste(BRAND_GREEN, (0, 0), rounded_rectangle_mask(size, radius))

    draw = ImageDraw.Draw(icon)
    font = font_at(int(size * 0.56))
    text = "T"
    box = draw.textbbox((0, 0), text, font=font)
    width = box[2] - box[0]
    height = box[3] - box[1]
    x = (size - width) / 2 - box[0]
    y = (size - height) / 2 - box[1]
    draw.text((x, y), text, font=font, fill=WHITE)
    return icon


def main() -> None:
    ICON_DIR.mkdir(parents=True, exist_ok=True)
    for size, name in ((192, "icon-192.png"), (512, "icon-512.png")):
        render_icon(size).save(ICON_DIR / name, format="PNG")
    render_icon(180).save(STATIC_DIR / "apple-touch-icon.png", format="PNG")
    packaging_dir = Path(__file__).resolve().parents[1] / "packaging"
    packaging_dir.mkdir(parents=True, exist_ok=True)
    render_icon(256).save(
        packaging_dir / "TrVocab.ico",
        format="ICO",
        sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)],
    )
    print(f"Icons written to {ICON_DIR} and apple-touch-icon.png")


if __name__ == "__main__":
    main()
