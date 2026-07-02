import json

config_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\assets\template_config.json"

with open(config_path, "r", encoding="utf-8") as f:
    templates = json.load(f)

img_urls = [
    "https://lh3.googleusercontent.com/aida/AP1WRLtjlayrFoqOHRuL5UGcypprMD9jbZwoEwsIgCdhfdhtmPMzQNA9UdU6QFIPJSksCVNU0Fehv4amN4ndM12h6IppxAXTSHf0Ri1CxWj_herewxAx0TG30vSnuRSeDZUqQIEop4WAykUIG7ze52fnetYgwGvpw_rWtWAPoYcOMtP3iyh7d3SgNQrSIqGc3T_IPPyB98CaleEbgRE68hCcEVKQB_98jhlipaJfTw-k7aDvgmRSi0i-UWIZ1bY",
    "https://lh3.googleusercontent.com/aida/AP1WRLsi-Xn5FjpUhsOHvSYXBk8o3cTCe4MW3Ab_3e71bkt_RgbnpUITvCJIfaZc1548BMhf_cBTykisGs1qgCBAs2WPi0WTrLiMQliAlSSU2j01_lJjFI0MxsfdoqZ2b6HU1zBHjMGDEY4KEaEm8ZfWqU2PEbIhsa7_2caguBppDwSEbHszh1Jn92nO9gf4wtquaa2C-FcX4uxwevg8EWJaatIyGppu68nQ0EYWTvVT7re7QJPSaOGdWjP22w",
    "https://lh3.googleusercontent.com/aida/AP1WRLuZGZTl5S9M6Sw2c0C8FvWKTIG2VNFaPOFvcIaNmDit9ShP9YjZz8k9dYlj32n5QthwfDVid88k2iNRUD8IY8rnLQbRJdP9_Xhh9fbvbTZqbp4AOrpHt4PqN4bV56uB4H3cjXvkDmx-UzF_HthmZG45mDT5hIoB2QLWkXrJo7sJVRGwnvO65MhYTSJ4EMWE9KKL1ZbM6NImyHuwhpIVQLsqq6sFXqkTTnm7zw0AvBKiBsyPMCBR5cCuKIg"
]

pdf_url = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf" # Dummy PDF for preview

for i, t in enumerate(templates):
    t["thumbnailUrl"] = img_urls[i % len(img_urls)]
    t["samplePdfUrl"] = pdf_url

with open(config_path, "w", encoding="utf-8") as f:
    json.dump(templates, f, indent=2)

print("Updated template_config.json")
