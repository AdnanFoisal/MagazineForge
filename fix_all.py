import os
import re

# 1. EditorViewModel.kt
evm_path = "android-app/app/src/main/java/com/magazineforge/app/ui/EditorViewModel.kt"
with open(evm_path, "r", encoding="utf-8") as f:
    evm_data = f.read()

evm_data = evm_data.replace("import com.magazineforge.app.models.GenerateRawLatexRequest\n", "")
evm_data = evm_data.replace("import kotlinx.coroutines.flow.MutableStateFlow\n", "import kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n")
evm_data = evm_data.replace("kotlinx.coroutines.flow.asStateFlow(_briefState)", "_briefState.asStateFlow()")
evm_data = evm_data.replace("kotlinx.coroutines.flow.asStateFlow(_schemaState)", "_schemaState.asStateFlow()")
evm_data = evm_data.replace("kotlinx.coroutines.flow.asStateFlow(_latexState)", "_latexState.asStateFlow()")
evm_data = evm_data.replace("kotlinx.coroutines.flow.asStateFlow(_aiRawLatexState)", "_aiRawLatexState.asStateFlow()")

with open(evm_path, "w", encoding="utf-8") as f:
    f.write(evm_data)

# 2. EditorScreen.kt
es_path = "android-app/app/src/main/java/com/magazineforge/app/ui/EditorScreen.kt"
with open(es_path, "r", encoding="utf-8") as f:
    es_data = f.read()

es_data = es_data.replace("import com.magazineforge.app.models.GenerateBriefResponse\n", "")
es_data = es_data.replace(
    "import androidx.compose.ui.Modifier\n",
    "import androidx.compose.ui.Modifier\nimport com.magazineforge.app.models.GenerateBriefResponse\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n"
)
es_data = es_data.replace("okhttp3.MediaType.parse(", "okhttp3.MediaType.Companion.parse(")

with open(es_path, "w", encoding="utf-8") as f:
    f.write(es_data)

# 3. CoAuthorScreen.kt
cas_path = "android-app/app/src/main/java/com/magazineforge/app/ui/CoAuthorScreen.kt"
with open(cas_path, "r", encoding="utf-8") as f:
    cas_data = f.read()

cas_data = cas_data.replace("val isSchemaValid = schema.title.isNotBlank() && schema.topic.isNotBlank()", "val isSchemaValid = schema.cover.mainTitle.isNotBlank()")

with open(cas_path, "w", encoding="utf-8") as f:
    f.write(cas_data)

# 4. LatexNotebookScreen.kt
lns_path = "android-app/app/src/main/java/com/magazineforge/app/ui/LatexNotebookScreen.kt"
with open(lns_path, "r", encoding="utf-8") as f:
    lns_data = f.read()

lns_data = lns_data.replace("                }\n            }\n        },\n        containerColor = bgCream", "                }\n            }\n        }\n    },\n    containerColor = bgCream")

with open(lns_path, "w", encoding="utf-8") as f:
    f.write(lns_data)

print("ALL FIXES APPLIED!")
