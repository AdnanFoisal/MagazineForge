import re

file_path = r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\ui\MyMagazinesScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add Delete Icon import
imports_to_add = """
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
"""
if "import androidx.compose.material.icons.filled.Delete" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.ArrowBack", "import androidx.compose.material.icons.filled.ArrowBack\nimport androidx.compose.material.icons.filled.Delete")

# 2. Add state for deletion and refresh trigger
target_state = """fun MyMagazinesScreen(
    onBack: () -> Unit,
    onMagazineSelected: (File) -> Unit
) {
    val context = LocalContext.current"""

replacement_state = """fun MyMagazinesScreen(
    onBack: () -> Unit,
    onMagazineSelected: (File) -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }
    var itemToDelete by remember { mutableStateOf<MagazinePdfItem?>(null) }"""

if "var refreshTrigger" not in content:
    content = content.replace(target_state, replacement_state)

# 3. Change magazines to remember(refreshTrigger)
content = content.replace("val magazines = remember {", "val magazines = remember(refreshTrigger) {")

# 4. Add the delete button inside the Card
target_card_content = """                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(obsidian, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {"""

replacement_card_content = """                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(obsidian, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {"""

if "Box(modifier = Modifier.fillMaxSize().padding(8.dp)" not in content:
    content = content.replace(target_card_content, replacement_card_content)

# We need to add the icon button to the top right of the cover image wrapper.
# Find where the cover image Box ends:
target_cover_end = """                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))"""

replacement_cover_end = """                                        }
                                    }
                                }
                                
                                IconButton(
                                    onClick = { itemToDelete = item },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))"""

if "Icon(Icons.Default.Delete" not in content:
    content = content.replace(target_cover_end, replacement_cover_end)

# 5. Add AlertDialog at the end of the file
alert_dialog_code = """
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Magazine") },
                text = { Text("Are you sure you want to delete '${itemToDelete?.name}'? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        itemToDelete?.file?.delete()
                        itemToDelete?.coverFile?.delete()
                        itemToDelete = null
                        refreshTrigger++
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                },
                containerColor = darkSurface,
                titleContentColor = ivory,
                textContentColor = mutedGray
            )
        }
"""

if "AlertDialog(" not in content and "Delete Magazine" not in content:
    target_end = """    }
}"""
    content = content.replace(target_end, alert_dialog_code + "\n    }\n}")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
