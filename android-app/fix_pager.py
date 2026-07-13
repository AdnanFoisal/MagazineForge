import re

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\MainActivity.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add BackHandler import
imports_to_add = """
import androidx.activity.compose.BackHandler
"""
if "import androidx.activity.compose.BackHandler" not in content:
    content = content.replace("import androidx.activity.ComponentActivity", "import androidx.activity.ComponentActivity\n" + imports_to_add.strip())

# 2. Add BackHandler and beyondBoundsPageCount
target = """                                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->"""

replacement = """                                        BackHandler(enabled = pagerState.currentPage != 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }
                                        
                                        HorizontalPager(
                                            state = pagerState, 
                                            modifier = Modifier.fillMaxSize(),
                                            beyondBoundsPageCount = 4 // Keeps all tabs in memory to completely eliminate lag!
                                        ) { page ->"""

if "beyondBoundsPageCount = 4" not in content:
    content = content.replace(target, replacement)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
