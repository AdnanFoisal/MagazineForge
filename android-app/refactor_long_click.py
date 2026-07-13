import re

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\ui\MyMagazinesScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports for combinedClickable
if "androidx.compose.foundation.combinedClickable" not in content:
    content = content.replace("import androidx.compose.foundation.clickable", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\nimport androidx.compose.foundation.ExperimentalFoundationApi")

# Replace clickable with combinedClickable
target_clickable = """.clickable { onMagazineSelected(item.file) },"""
replacement_clickable = """.combinedClickable(
                                onClick = { onMagazineSelected(item.file) },
                                onLongClick = { itemToDelete = item }
                            ),"""

if "combinedClickable" not in content:
    content = content.replace(target_clickable, replacement_clickable)

# We need to add @OptIn(ExperimentalFoundationApi::class) to the function if not present.
# But inside a Composable, we can just add it above the function.
if "@OptIn(ExperimentalFoundationApi::class)" not in content:
    content = content.replace("@Composable\nfun MyMagazinesScreen(", "@OptIn(ExperimentalFoundationApi::class)\n@Composable\nfun MyMagazinesScreen(")

# Remove the IconButton Trash
target_icon = """                                IconButton(
                                    onClick = { itemToDelete = item },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.8f))
                                }"""

content = content.replace(target_icon, "")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
