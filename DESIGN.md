# MagazineForge Design System (Stitch MCP Inspired)

## Design Philosophy
An AI-native premium publishing tool. The UI should resemble a high-end editorial desk: clean layouts, sophisticated serif headers, dark mode matte backgrounds, and warm metal accents (copper/bronze).

## Color Palette
| Token | Hex Code | Compose Value | Purpose |
|---|---|---|---|
| **Background** | `#0F0F10` | `Color(0xFF0F0F10)` | Main app background (Deep Obsidian) |
| **Surface** | `#18181B` | `Color(0xFF18181B)` | Card and screen component backgrounds |
| **Primary** | `#C5A059` | `Color(0xFFC5A059)` | Primary highlight, main buttons (Warm Gold) |
| **Secondary** | `#B87333` | `Color(0xFFB87333)` | Accent details, active states (Warm Copper) |
| **Text Primary** | `#F5F5F7` | `Color(0xFFF5F5F7)` | High-contrast headers, input texts (Crisp Ivory) |
| **Text Secondary** | `#A1A1AA` | `Color(0xFFA1A1AA)` | Subtitles, helper text (Warm Muted Gray) |
| **Border / Divider** | `#2E2A24` | `Color(0xFF2E2A24)` | Outline fields, dividers, card borders |

## Typography
- **Headers & Titles**: `FontFamily.Serif`, `FontWeight.Bold`, letter spacing default or slightly tracking. E.g. "Template Gallery" and screen titles.
- **Body & Controls**: `FontFamily.SansSerif`, readable, clean, modern weight.

## Layout & Components

### 1. TemplateGalleryScreen
- **Theme**: Dark background with ivory text and gold accents.
- **Search Bar**: Minimalist search input field with `#2E2A24` borders, white/ivory text, and a gold magnifying glass icon.
- **Tabs**: Scrollable tab row using transparent background, with gold underline for the active tab and bold serif text.
- **Grid Layout**: A 2-column grid of cards.
- **Template Card**:
  - Dark surface color (`#18181B`) with 12dp rounded corners and a subtle border (`#2E2A24`).
  - High-contrast typography: Title in bold serif, description in muted sans-serif.
  - Instead of placeholder gray boxes, use a styled gold/copper outline or abstract design representation.

### 2. EditorScreen
- **Theme**: Deep obsidian theme.
- **Header**: Shows selected template in serif font with gold accent.
- **Overall Topic**: Outlined text field with gold label and custom borders.
- **Page Sequencer**:
  - Vertical timeline-style sequence.
  - Add native Photo Picker integration. Instead of entering image URL, click "Select Cover Photo" or "Select Article Photo" which launches the Android System Photo Picker and shows a thumbnail/filename of the selected image.
  - Custom buttons: Rounded, gold-bordered button rows (`+ Article`, `+ Cover`, `+ TOC`).
- **Submission Action**: Large gold-filled button ("Craft Magazine") at the bottom, taking full width with 56dp height.

### 3. MyMagazinesScreen
- **Theme**: Unified editorial desk styling.
- **Content**: Displays a grid/list of previously generated magazines (PDFs). Reads local directory to find PDFs and displays their name, compile date, and size. Clicking a magazine opens the PDF viewer.

### 4. Photo Picker Integration
- Use the modern Activity Result contract `PickVisualMedia` to natively prompt the user for images.
- Store the returned URI locally or pass it to the backend (or convert to temporary mock file or base64 if needed).
