import os

# Fix TemplateGalleryScreen
file1 = "android-app/app/src/main/java/com/magazineforge/app/ui/TemplateGalleryScreen.kt"
with open(file1, "r", encoding="utf-8") as f:
    data1 = f.read()

data1 = data1.replace(
    "val context = LocalContext.current\n    val templates = remember { loadTemplates(context) }",
    "val context = LocalContext.current\n    val tokens = LocalThemeTokens.current\n    val templates = remember { loadTemplates(context) }"
)

with open(file1, "w", encoding="utf-8") as f:
    f.write(data1)

# Fix OnboardingScreen
file2 = "android-app/app/src/main/java/com/magazineforge/app/ui/OnboardingScreen.kt"
with open(file2, "r", encoding="utf-8") as f:
    data2 = f.read()

if "import androidx.compose.foundation.shape.RoundedCornerShape" not in data2:
    data2 = data2.replace(
        "import androidx.compose.ui.Alignment",
        "import androidx.compose.ui.Alignment\nimport androidx.compose.foundation.shape.RoundedCornerShape"
    )

with open(file2, "w", encoding="utf-8") as f:
    f.write(data2)

# Fix MyMagazinesScreen
file3 = "android-app/app/src/main/java/com/magazineforge/app/ui/MyMagazinesScreen.kt"
with open(file3, "r", encoding="utf-8") as f:
    data3 = f.read()

data3 = data3.replace("CoverArtImage(", "CoverImage(")

with open(file3, "w", encoding="utf-8") as f:
    f.write(data3)

print("Done")
