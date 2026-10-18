import os
from PIL import Image, ImageDraw, ImageFont

WIDTH, HEIGHT = 1920, 1080
img = Image.new("RGB", (WIDTH, HEIGHT), (248, 250, 252))
draw = ImageDraw.Draw(img)

# Soft gradient background
for y in range(HEIGHT):
    r = int(255 - (255 - 241) * (y / HEIGHT))
    g = int(255 - (255 - 245) * (y / HEIGHT))
    b = int(255 - (255 - 249) * (y / HEIGHT))
    draw.line([(0, y), (WIDTH, y)], fill=(r, g, b))

# 1. Official Logo
logo = Image.open("web/src/assets/branding/logo_principal.png").convert("RGBA")
target_w = 520
target_h = int(logo.height * (target_w / logo.width))
logo_resized = logo.resize((target_w, target_h), Image.Resampling.LANCZOS)
logo_x = (WIDTH - target_w) // 2
logo_y = 100
img.paste(logo_resized, (logo_x, logo_y), logo_resized)

# Fonts
font_path_b = os.path.join(os.environ["WINDIR"], "Fonts", "segoeuib.ttf")
font_path_r = os.path.join(os.environ["WINDIR"], "Fonts", "segoeui.ttf")

font_slogan = ImageFont.truetype(font_path_b, 32)
font_thanks = ImageFont.truetype(font_path_b, 48)
font_sub = ImageFont.truetype(font_path_r, 30)
font_btn = ImageFont.truetype(font_path_b, 36)
font_url = ImageFont.truetype(font_path_b, 28)
font_footer = ImageFont.truetype(font_path_r, 22)

# Slogan
slogan_text = "« Parce qu’elle est précieuse, nous innovons pour la protéger. »"
bbox_s = draw.textbbox((0, 0), slogan_text, font=font_slogan)
draw.text(((WIDTH - (bbox_s[2] - bbox_s[0])) // 2, logo_y + target_h + 20), slogan_text, fill=(71, 85, 105), font=font_slogan)

# Card Container for YouTube CTA
card_w = 960
card_h = 420
card_x = (WIDTH - card_w) // 2
card_y = 400

# White card with subtle border
draw.rounded_rectangle([(card_x, card_y), (card_x + card_w, card_y + card_h)], radius=24, fill=(255, 255, 255), outline=(226, 232, 240), width=2)
# Emerald top accent strip
draw.rounded_rectangle([(card_x, card_y), (card_x + card_w, card_y + 10)], radius=5, fill=(0, 151, 48))

# Big Thank You title
thanks_text = "Merci de nous avoir suivis !"
bbox_th = draw.textbbox((0, 0), thanks_text, font=font_thanks)
draw.text(((WIDTH - (bbox_th[2] - bbox_th[0])) // 2, card_y + 40), thanks_text, fill=(0, 29, 74), font=font_thanks)

# Subtitle Subscribe
sub_text = "Abonnez-vous pour ne rien rater"
bbox_sub = draw.textbbox((0, 0), sub_text, font=font_sub)
draw.text(((WIDTH - (bbox_sub[2] - bbox_sub[0])) // 2, card_y + 115), sub_text, fill=(100, 116, 139), font=font_sub)

# Red YouTube Subscribe Button
btn_w = 460
btn_h = 75
btn_x = (WIDTH - btn_w) // 2
btn_y = card_y + 185
draw.rounded_rectangle([(btn_x, btn_y), (btn_x + btn_w, btn_y + btn_h)], radius=16, fill=(225, 29, 72)) # YouTube crimson red

btn_text = "S'ABONNER À LA CHAÎNE"
bbox_btn = draw.textbbox((0, 0), btn_text, font=font_btn)
draw.text(((WIDTH - (bbox_btn[2] - bbox_btn[0])) // 2, btn_y + 18), btn_text, fill=(255, 255, 255), font=font_btn)

# Separator & Website link
line_y = card_y + 295
draw.line([(card_x + 100, line_y), (card_x + card_w - 100, line_y)], fill=(241, 245, 249), width=2)

url_text = "Démonstration personnalisée sur  joprelys.com"
bbox_url = draw.textbbox((0, 0), url_text, font=font_url)
draw.text(((WIDTH - (bbox_url[2] - bbox_url[0])) // 2, card_y + 325), url_text, fill=(0, 151, 48), font=font_url)

# Footer credits
footer_text = "Joprelys Connect  •  Suite Hospitalière Unifiée  •  Cameroun & Afrique"
bbox_f = draw.textbbox((0, 0), footer_text, font=font_footer)
draw.text(((WIDTH - (bbox_f[2] - bbox_f[0])) // 2, 875), footer_text, fill=(148, 163, 184), font=font_footer)

out_file = "docs/teaser/clean_1080_no_header/07_outro.png"
img.save(out_file)
print("Saved new outro slide:", out_file)
