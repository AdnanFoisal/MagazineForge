import re

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\MainActivity.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add OptIn and SplashScreen import
imports_to_add = """
import androidx.compose.foundation.ExperimentalFoundationApi
import com.magazineforge.app.ui.SplashScreen
"""
if "import com.magazineforge.app.ui.SplashScreen" not in content:
    content = content.replace("import com.magazineforge.app.ui.theme.MagazineForgeTheme", imports_to_add.strip() + "\nimport com.magazineforge.app.ui.theme.MagazineForgeTheme")

# 2. Add OptIn to MainActivity class
if "@OptIn(ExperimentalFoundationApi::class)" not in content:
    content = content.replace("class MainActivity : ComponentActivity() {", "@OptIn(ExperimentalFoundationApi::class)\nclass MainActivity : ComponentActivity() {")

# 3. Fix the missing brace. Wait, if it's missing a brace at EOF, let's just append one.
# But wait, looking at the previous diff, `after_when_idx` was the `// Floating Progress Card Overlay`.
# Did I accidentally delete a brace when merging? 
# The `Scaffold` has `{ innerPadding -> Box(..) { ... } }`.
# Let's count braces in MainActivity.kt.
lines = content.split('\n')
open_braces = sum(line.count('{') for line in lines)
close_braces = sum(line.count('}') for line in lines)
print(f"Open: {open_braces}, Close: {close_braces}")
if open_braces > close_braces:
    content += "\n" + "}\n" * (open_braces - close_braces)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
