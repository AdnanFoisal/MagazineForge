import re

with open(r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\ui\CoAuthorScreen.kt", "r") as f:
    code = f.read()

# Replace articles -> pages in iteration
loop_replacement = """
            schema.pages.forEachIndexed { index, page ->
                when (page) {
                    is ArticleSchema -> {
                        ExpandableSection("Article: ${page.headline}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = page.bodyCopy,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(bodyCopy = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Body Copy") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 5
                            )
                        }
                    }
                    is AdSchema -> {
                        ExpandableSection("Ad: ${page.fakeCompanyName}") {
                            OutlinedTextField(
                                value = page.headline,
                                onValueChange = {
                                    val newPages = schema.pages.toMutableList()
                                    newPages[index] = page.copy(headline = it)
                                    schema = schema.copy(pages = newPages)
                                },
                                label = { Text("Ad Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        ExpandableSection("Page: ${page.type}") {
                            Text("Editing not fully supported for this page type yet.")
                        }
                    }
                }
            }
"""

start_str = "            schema.articles.forEachIndexed { index, article ->"
end_str = "            schema.backCover?.let { backCover ->"

start_idx = code.find(start_str)
end_idx = code.find(end_str)

if start_idx != -1 and end_idx != -1:
    code = code[:start_idx] + loop_replacement + "\n" + code[end_idx:]
    with open(r"c:\Users\adnan\Downloads\MagBoy\android-app\app\src\main\java\com\magazineforge\app\ui\CoAuthorScreen.kt", "w") as f:
        f.write(code)
    print("CoAuthorScreen patched successfully.")
else:
    print("Failed to find CoAuthorScreen markers.")
