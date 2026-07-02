import os
from PIL import Image, ImageDraw

source_image_path = r"C:\Users\adnan\.gemini\antigravity\brain\3292b7c2-ebe1-4a9c-9f3e-9551c4ed6c8f\mag_forge_icon_1782972114154.png"
base_res_dir = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\res"

sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

def make_round(img):
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + img.size, fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result

img = Image.open(source_image_path).convert("RGBA")

# Crop to square if it isn't
width, height = img.size
min_dim = min(width, height)
left = (width - min_dim) / 2
top = (height - min_dim) / 2
right = (width + min_dim) / 2
bottom = (height + min_dim) / 2
img = img.crop((left, top, right, bottom))

for bucket, size in sizes.items():
    folder = os.path.join(base_res_dir, f"mipmap-{bucket}")
    os.makedirs(folder, exist_ok=True)
    
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    
    # Square icon
    resized.save(os.path.join(folder, "ic_launcher.png"))
    
    # Round icon
    round_img = make_round(resized)
    round_img.save(os.path.join(folder, "ic_launcher_round.png"))

print("Icons generated successfully.")
