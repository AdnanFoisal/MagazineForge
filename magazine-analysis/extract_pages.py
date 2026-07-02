"""
MagazineForge PDF Analyzer
Extracts key pages from source PDFs and renders them as images for design DNA analysis.
Samples: cover (page 1), TOC area (pages 3-5), and a feature spread (pages ~20-30).
"""
import os
import sys
import subprocess

def check_and_install(package_name, import_name=None):
    if import_name is None:
        import_name = package_name
    try:
        __import__(import_name)
    except ImportError:
        subprocess.check_call([sys.executable, "-m", "pip", "install", package_name])

check_and_install("pymupdf", "fitz")
import fitz  # PyMuPDF

SOURCE_DIR = r"c:\Users\adnan\Downloads\MagBoy\magazine-analysis\source-pdfs"
OUTPUT_DIR = r"c:\Users\adnan\Downloads\MagBoy\magazine-analysis\extracted-pages"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Define which pages to sample from each PDF
# We sample: cover(0), TOC area(2,3,4), a feature spread mid-magazine(~20-25), one more spread(~50-60)
SAMPLE_PAGES = {
    "Food-1.pdf": [0, 2, 3, 20, 21, 50],
    "Food-2.pdf": [0, 2, 3, 25, 26, 60],
    "Travel-1.pdf": [0, 2, 3, 20, 21, 55],
    "Travel-2.pdf": [0, 2, 3, 22, 23, 50],
    "Travel-3.pdf": [0, 2, 3, 18, 19, 40],
}

for pdf_name, pages in SAMPLE_PAGES.items():
    pdf_path = os.path.join(SOURCE_DIR, pdf_name)
    if not os.path.exists(pdf_path):
        print(f"SKIP: {pdf_name} not found")
        continue
    
    print(f"\nProcessing: {pdf_name}")
    doc = fitz.open(pdf_path)
    total_pages = doc.page_count
    print(f"  Total pages: {total_pages}")
    
    prefix = pdf_name.replace(".pdf", "")
    
    for page_num in pages:
        if page_num >= total_pages:
            print(f"  Page {page_num} exceeds total ({total_pages}), skipping")
            continue
        
        page = doc[page_num]
        # Render at 2x zoom for clarity (144 DPI)
        mat = fitz.Matrix(2, 2)
        pix = page.get_pixmap(matrix=mat)
        
        out_path = os.path.join(OUTPUT_DIR, f"{prefix}_page{page_num:03d}.png")
        pix.save(out_path)
        print(f"  Extracted page {page_num} -> {out_path}")
    
    doc.close()

print(f"\nDone! All extracted pages saved to: {OUTPUT_DIR}")
