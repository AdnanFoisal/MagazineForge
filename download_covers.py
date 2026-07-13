import urllib.request
import os

images = {
    "food_classic_cover.jpg": "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?q=80&w=800&h=1200&fit=crop",
    "food_modern_cover.jpg": "https://images.unsplash.com/photo-1555939594-58d7cb561ad1?q=80&w=800&h=1200&fit=crop",
    "food_rustic_cover.jpg": "https://images.unsplash.com/photo-1504674900247-0877df9cc836?q=80&w=800&h=1200&fit=crop",
    "travel_landscape_cover.jpg": "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?q=80&w=800&h=1200&fit=crop",
    "travel_journal_cover.jpg": "https://images.unsplash.com/photo-1488646953014-85cb44e25828?q=80&w=800&h=1200&fit=crop",
    "travel_guide_cover.jpg": "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?q=80&w=800&h=1200&fit=crop",
    "tech_cyber_cover.jpg": "https://images.unsplash.com/photo-1518770660439-4636190af475?q=80&w=800&h=1200&fit=crop",
    "tech_minimal_cover.jpg": "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?q=80&w=800&h=1200&fit=crop",
    "tech_spec_cover.jpg": "https://images.unsplash.com/photo-1517433670267-08bbd4be890f?q=80&w=800&h=1200&fit=crop",
    "lifestyle_vogue_cover.jpg": "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=800&h=1200&fit=crop",
    "lifestyle_indie_cover.jpg": "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=800&h=1200&fit=crop",
    "lifestyle_wellness_cover.jpg": "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?q=80&w=800&h=1200&fit=crop",
    "science_cosmos_cover.jpg": "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=800&h=1200&fit=crop",
    "science_nature_cover.jpg": "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?q=80&w=800&h=1200&fit=crop",
    "science_journal_cover.jpg": "https://images.unsplash.com/photo-1532094349884-543bc11b234d?q=80&w=800&h=1200&fit=crop",
    "custom_corporate_cover.jpg": "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=800&h=1200&fit=crop",
    "custom_bold_cover.jpg": "https://images.unsplash.com/photo-1557672172-298e090bd0f1?q=80&w=800&h=1200&fit=crop",
    "custom_blank_cover.jpg": "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?q=80&w=800&h=1200&fit=crop"
}

output_dir = r"c:\Users\adnan\Downloads\MagBoy\backend\static\samples"
os.makedirs(output_dir, exist_ok=True)

for filename, url in images.items():
    filepath = os.path.join(output_dir, filename)
    print(f"Downloading {filename}...")
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response, open(filepath, 'wb') as out_file:
        out_file.write(response.read())

print("All beautiful images downloaded successfully!")
