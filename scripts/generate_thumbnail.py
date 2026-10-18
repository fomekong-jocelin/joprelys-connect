import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

W, H = 1280, 720
img = Image.new("RGB", (W, H), (248, 250, 252))
draw = ImageDraw.Draw(img)

# Soft gradient background
for y in range(H):
    r = int(255 - (255 - 240) * (y / H))
    g = int(255 - (255 - 245) * (y / H))
    b = int(255 - (255 - 249) * (y / H))
    draw.line([(0, y), (W, y)], fill=(r, g, b))

font_path_b = os.path.join(os.environ["WINDIR"], "Fonts", "segoeuib.ttf")
font_path_r = os.path.join(os.environ["WINDIR"], "Fonts", "segoeui.ttf")

font_badge = ImageFont.truetype(font_path_b, 18)
font_title1 = ImageFont.truetype(font_path_b, 46)
font_title2 = ImageFont.truetype(font_path_b, 46)
font_sub = ImageFont.truetype(font_path_b, 22)
font_slogan = ImageFont.truetype(font_path_b, 19)
font_pill = ImageFont.truetype(font_path_b, 20)

# Top Badge
draw.rounded_rectangle([(60, 50), (330, 92)], radius=8, fill=(0, 151, 48))
draw.text((75, 59), "DÉMO OFFICIELLE 2026", fill=(255, 255, 255), font=font_badge)

# Main Titles
draw.text((60, 118), "JOPRELYS CONNECT", fill=(0, 29, 74), font=font_title1)
draw.text((60, 178), "La Suite Hospitalière Unifiée", fill=(0, 151, 48), font=font_title2)

# Subtitle points with drawn green bullet dots
points = [
    "Dossier Patient Unique (DPU 360°)",
    "Dictée & Consultation par IA Vocale",
    "Admissions, Urgences & Gestion des Lits",
    "Passerelle Géolocalisée AllôPharma"
]
y_pt = 270
for pt in points:
    draw.ellipse([(60, y_pt + 6), (72, y_pt + 18)], fill=(0, 151, 48))
    draw.text((85, y_pt), pt, fill=(15, 23, 42), font=font_sub)
    y_pt += 48

# Slogan at bottom left
draw.line([(60, 535), (220, 535)], fill=(0, 151, 48), width=4)
draw.text((60, 555), "« Parce qu'elle est précieuse, nous innovons pour la protéger. »", fill=(71, 85, 105), font=font_slogan)
draw.text((60, 600), "joprelys.com  •  Douala & Yaoundé, Cameroun", fill=(0, 151, 48), font=font_pill)

# Logo
logo = Image.open("web/src/assets/branding/logo_principal.png").convert("RGBA")
lw = 380
lh = int(logo.height * (lw / logo.width))
logo_r = logo.resize((lw, lh), Image.Resampling.LANCZOS)
img.paste(logo_r, (830, 55), logo_r)

# Preview snippet from clean hero
hero_im = Image.open("docs/teaser/clean_1080_no_header/01_hero.png")
card_crop = hero_im.crop((1020, 220, 1520, 620))
card_crop = card_crop.resize((430, 344), Image.Resampling.LANCZOS)

draw.rounded_rectangle([(800, 220), (1240, 580)], radius=16, fill=(255, 255, 255), outline=(226, 232, 240), width=3)
img.paste(card_crop, (805, 225))

# Pill on card: IA SOUVERAINE
draw.rounded_rectangle([(820, 595), (1080, 642)], radius=8, fill=(0, 29, 74))
draw.text((835, 608), "IA MÉDICALE SOUVERAINE", fill=(255, 255, 255), font=font_badge)

p_thumb = Path("docs/teaser/miniature_youtube.png").resolve()
img.save(str(p_thumb))
desktop_thumb = Path(os.path.expanduser("~/Desktop/miniature_youtube_joprelys.png")).resolve()
img.save(str(desktop_thumb))
print("Thumbnail generated successfully!")
