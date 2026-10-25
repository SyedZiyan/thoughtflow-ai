import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

REPO_ROOT = Path(__file__).resolve().parent.parent
DOCS_ASSETS = REPO_ROOT / "docs" / "assets"
DOCS_ASSETS.mkdir(parents=True, exist_ok=True)

APP_BACKGROUNDS = REPO_ROOT / "apps" / "android" / "app" / "src" / "main" / "assets" / "backgrounds"

def get_font(size: int, bold: bool = False):
    font_names = [
        "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\arial.ttf",
        "C:\\Windows\\Fonts\\calibri.ttf"
    ]
    if bold:
        font_names = [
            "C:\\Windows\\Fonts\\segoeuib.ttf",
            "C:\\Windows\\Fonts\\arialbd.ttf",
            "C:\\Windows\\Fonts\\calibrib.ttf"
        ]
    for fn in font_names:
        if os.path.exists(fn):
            try:
                return ImageFont.truetype(fn, size)
            except Exception:
                pass
    return ImageFont.load_default()

def create_hero_banner():
    width, height = 1200, 420
    img = Image.new("RGBA", (width, height), (9, 10, 13, 255))
    draw = ImageDraw.Draw(img)

    # Ambient Glow
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse([width//2 - 280, -120, width//2 + 280, 260], fill=(37, 99, 235, 45))
    glow = glow.filter(ImageFilter.GaussianBlur(70))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Border
    draw.rounded_rectangle([1, 1, width-2, height-2], radius=16, outline=(38, 40, 48), width=2)

    # Title & Tagline
    f_title = get_font(46, bold=True)
    f_subtitle = get_font(20, bold=False)
    f_tag = get_font(13, bold=True)
    f_badge = get_font(12, bold=True)

    # Top Category Pill
    pill_text = "✦  AUTONOMOUS CREATOR PLATFORM  ✦"
    draw.rounded_rectangle([width//2 - 170, 48, width//2 + 170, 78], radius=15, fill=(19, 21, 26), outline=(38, 40, 48), width=1)
    draw.text((width//2, 63), pill_text, fill=(147, 197, 253), font=f_tag, anchor="mm")

    # Main Title
    draw.text((width//2, 135), "ThoughtFlow AI", fill=(248, 250, 252), font=f_title, anchor="mm")

    # Subtitle
    sub = "Daily Motivational Wisdom • Procedural 9:16 Video Studio • In-App OTA • 24/7 Cloud Automation"
    draw.text((width//2, 190), sub, fill=(148, 163, 184), font=f_subtitle, anchor="mm")

    # Feature Badges
    badges = [
        "📱 Native Android (Kotlin + Jetpack Compose)",
        "⚡ 24/7 Cloud Backend (FastAPI + Render)",
        "🎬 Automated 9:16 Shorts (Pillow + FFmpeg)",
        "🎲 Infinite Style Randomizer (>11,700 Styles)",
        "🚀 1-Click In-App OTA Updater"
    ]

    bx = 80
    by = 260
    badge_w = 205
    spacing = 15
    for i, b in enumerate(badges):
        x = 55 + i * 220
        draw.rounded_rectangle([x, by, x + 208, by + 85], radius=12, fill=(19, 21, 26), outline=(38, 40, 48), width=1)
        # Wrap badge text
        words = b.split(" ", 1)
        emoji = words[0]
        label = words[1] if len(words) > 1 else ""
        draw.text((x + 104, by + 26), emoji, font=get_font(20, bold=True), anchor="mm")
        draw.text((x + 104, by + 58), label, fill=(248, 250, 252), font=get_font(11, bold=True), anchor="mm")

    # Footer Status Line
    draw.line([40, 375, width - 40, 375], fill=(30, 33, 40), width=1)
    f_footer = get_font(11.5, bold=False)
    draw.text((width//2, 395), "Open Source • Zero Muddy Purple (Obsidian Dark Studio) • YouTube Data API v3 Integrated", fill=(100, 116, 139), font=f_footer, anchor="mm")

    output_path = DOCS_ASSETS / "hero_banner.png"
    img.convert("RGB").save(output_path, quality=95)
    print(f"Generated: {output_path}")

def create_showcase_features():
    width, height = 1200, 560
    img = Image.new("RGBA", (width, height), (9, 10, 13, 255))
    draw = ImageDraw.Draw(img)

    # Border
    draw.rounded_rectangle([1, 1, width-2, height-2], radius=16, outline=(38, 40, 48), width=2)

    f_title = get_font(26, bold=True)
    f_desc = get_font(14, bold=False)
    f_badge = get_font(12, bold=True)

    draw.text((width//2, 45), "Core Architectural Highlights", fill=(248, 250, 252), font=f_title, anchor="mm")
    draw.text((width//2, 80), "Designed from the ground up for autonomous creators, live photographic rendering, and zero-downtime distribution", fill=(148, 163, 184), font=f_desc, anchor="mm")

    cards = [
        ("user_mountain_hiker.jpg", "Photo Quote Generator", "Procedural Dynamic Styles", "16 photographic scenic backdrops combined with torn sticker notes, marker highlighters, and 1080p Gallery export."),
        ("user_balcony_sunset.jpg", "9:16 Video Studio", "FFmpeg Short Generator", "Live short preview, word-by-word highlighted text placement, ambient audio blending, and smooth Ken-Burns zooms."),
        ("mountain_ridge_sunrise.jpg", "In-App OTA Updater", "1-Click App Updates", "Over-the-air binary distribution. The phone automatically alerts users and updates in-place without Play Store friction.")
    ]

    card_w = 345
    card_h = 410
    top_y = 120
    for i, (bg_name, title, subtitle, desc) in enumerate(cards):
        cx = 40 + i * (card_w + 42)
        card_img = Image.new("RGBA", (card_w, card_h), (19, 21, 26, 255))
        cdraw = ImageDraw.Draw(card_img)

        # Background preview (top half)
        bg_path = APP_BACKGROUNDS / bg_name
        if bg_path.exists():
            thumb = Image.open(bg_path).convert("RGBA")
            thumb = thumb.resize((card_w, 200), Image.Resampling.LANCZOS)
            card_img.paste(thumb, (0, 0))

        # Bottom content
        cdraw.rounded_rectangle([0, 0, card_w-1, card_h-1], radius=14, outline=(38, 40, 48), width=1)
        cdraw.rectangle([0, 190, card_w, card_h], fill=(19, 21, 26))

        # Badge
        cdraw.rounded_rectangle([18, 210, 18 + len(subtitle)*8, 234], radius=6, fill=(37, 99, 235), outline=None)
        cdraw.text((24, 214), subtitle, fill=(255, 255, 255), font=get_font(10, bold=True))

        # Title
        cdraw.text((18, 250), title, fill=(248, 250, 252), font=get_font(18, bold=True))

        # Description
        words = desc.split(" ")
        lines = []
        cur = ""
        for w in words:
            if len(cur + " " + w) < 36:
                cur += (" " if cur else "") + w
            else:
                lines.append(cur)
                cur = w
        if cur:
            lines.append(cur)

        for li, line in enumerate(lines):
            cdraw.text((18, 285 + li * 20), line, fill=(148, 163, 184), font=get_font(12, bold=False))

        # Bottom subtle link
        cdraw.text((18, 380), "Live Feature  ➔", fill=(96, 165, 250), font=get_font(12, bold=True))

        img.paste(card_img, (cx, top_y))

    output_path = DOCS_ASSETS / "feature_gallery.png"
    img.convert("RGB").save(output_path, quality=95)
    print(f"Generated: {output_path}")

def create_randomizer_showcase():
    width, height = 1200, 380
    img = Image.new("RGBA", (width, height), (9, 10, 13, 255))
    draw = ImageDraw.Draw(img)

    # Border
    draw.rounded_rectangle([1, 1, width-2, height-2], radius=16, outline=(38, 40, 48), width=2)

    f_title = get_font(24, bold=True)
    f_desc = get_font(14, bold=False)

    draw.text((width//2, 40), "Procedural Dynamic Visual Randomizer", fill=(248, 250, 252), font=f_title, anchor="mm")
    draw.text((width//2, 70), "Generates over 11,700+ unique visual identities procedurally on every generation tap", fill=(148, 163, 184), font=f_desc, anchor="mm")

    templates = [
        ("Torn Sticker Note", "Yellow highlight marker on keywords, craft paper shadow, realistic paper tilt.", (253, 224, 71)),
        ("Vintage Storybook", "Classic serif typography, gold floral flourishes ('— ❧ —'), warm evening gradient.", (245, 158, 11)),
        ("Retro Polaroid", "Authentic white Polaroid frame with handwritten cursive quote and date stamp.", (244, 114, 182)),
        ("Swiss Editorial", "Minimalist high-fashion Swiss typography with stark geometry and clean rules.", (56, 189, 248)),
        ("Glassmorphic Frost", "Translucent frosted acrylic panel with specular borders and cyan accents.", (168, 85, 247))
    ]

    tw = 212
    th = 230
    for i, (name, d, accent_col) in enumerate(templates):
        tx = 35 + i * (tw + 22)
        ty = 110
        timg = Image.new("RGBA", (tw, th), (19, 21, 26, 255))
        tdraw = ImageDraw.Draw(timg)

        tdraw.rounded_rectangle([0, 0, tw-1, th-1], radius=12, fill=(19, 21, 26), outline=(38, 40, 48), width=1)

        # Accent top bar
        tdraw.rounded_rectangle([0, 0, tw, 6], radius=3, fill=accent_col)

        # Style number
        tdraw.text((16, 22), f"STYLE 0{i+1}", fill=(100, 116, 139), font=get_font(10, bold=True))

        # Title
        tdraw.text((16, 42), name, fill=(248, 250, 252), font=get_font(14, bold=True))

        # Description
        words = d.split(" ")
        lines = []
        cur = ""
        for w in words:
            if len(cur + " " + w) < 22:
                cur += (" " if cur else "") + w
            else:
                lines.append(cur)
                cur = w
        if cur:
            lines.append(cur)

        for li, line in enumerate(lines):
            tdraw.text((16, 85 + li * 18), line, fill=(148, 163, 184), font=get_font(11, bold=False))

        # Randomize indicator
        tdraw.rounded_rectangle([16, 180, tw - 16, 210], radius=8, fill=(28, 32, 42), outline=(45, 50, 62), width=1)
        tdraw.text((tw//2, 195), "🎲 Rolled Live", fill=accent_col, font=get_font(11, bold=True), anchor="mm")

        img.paste(timg, (tx, ty))

    output_path = DOCS_ASSETS / "procedural_styles.png"
    img.convert("RGB").save(output_path, quality=95)
    print(f"Generated: {output_path}")

if __name__ == "__main__":
    create_hero_banner()
    create_showcase_features()
    create_randomizer_showcase()
