---
name: Luxe Editorial Noir
colors:
  surface: '#121414'
  surface-dim: '#121414'
  surface-bright: '#383939'
  surface-container-lowest: '#0d0e0f'
  surface-container-low: '#1b1c1c'
  surface-container: '#1f2020'
  surface-container-high: '#292a2a'
  surface-container-highest: '#343535'
  on-surface: '#e3e2e2'
  on-surface-variant: '#d0c5af'
  inverse-surface: '#e3e2e2'
  inverse-on-surface: '#303031'
  outline: '#99907c'
  outline-variant: '#4d4635'
  surface-tint: '#e9c349'
  primary: '#f2ca50'
  on-primary: '#3c2f00'
  primary-container: '#d4af37'
  on-primary-container: '#554300'
  inverse-primary: '#735c00'
  secondary: '#c6c6c7'
  on-secondary: '#2f3131'
  secondary-container: '#454747'
  on-secondary-container: '#b4b5b5'
  tertiary: '#d0cdcd'
  on-tertiary: '#313030'
  tertiary-container: '#b4b2b2'
  on-tertiary-container: '#454544'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffe088'
  primary-fixed-dim: '#e9c349'
  on-primary-fixed: '#241a00'
  on-primary-fixed-variant: '#574500'
  secondary-fixed: '#e2e2e2'
  secondary-fixed-dim: '#c6c6c7'
  on-secondary-fixed: '#1a1c1c'
  on-secondary-fixed-variant: '#454747'
  tertiary-fixed: '#e5e2e1'
  tertiary-fixed-dim: '#c8c6c5'
  on-tertiary-fixed: '#1c1b1b'
  on-tertiary-fixed-variant: '#474746'
  background: '#121414'
  on-background: '#e3e2e2'
  surface-variant: '#343535'
typography:
  display-lg:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 52px
    letterSpacing: -0.04em
  display-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 36px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.03em
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 38px
    letterSpacing: -0.02em
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 30px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 30px
    letterSpacing: 0em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: 0em
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-page: 24px
  gutter: 16px
  stack-xl: 64px
  stack-lg: 40px
  stack-md: 24px
  stack-sm: 12px
---

## Brand & Style
The design system embodies a "High-End Digital Editorial" aesthetic. It merges the prestigious, high-contrast visual language of luxury fashion magazines with the functional clarity of modern digital publishing. The target audience is discerning readers and creators who value intellectual depth and aesthetic precision.

The style is **Neo-Minimalist with a Bold Editorial edge**. It utilizes pitch-black backgrounds to create an infinite canvas where content—especially photography and typography—takes center stage. The emotional response is one of exclusivity, authority, and cinematic focus. We avoid "visual noise" in favor of aggressive whitespace, strict geometric alignment, and a sophisticated dark mode execution.

## Colors
The palette is rooted in absolute contrast. **Pitch Black (#000000)** serves as the primary atmospheric base, ensuring OLED screens achieve perfect depth. **Graphite (#1A1A1A)** is used exclusively for surface elevation and card containers to provide subtle separation from the void.

**Editorial Gold (#D4AF37)** is our high-value accent. It must be used with extreme restraint—reserved only for primary calls-to-action, active states of high importance, or premium indicators. **Ghost White (#F3F3F3)** provides maximum legibility for long-form reading, while **Ash Grey (#888888)** handles secondary metadata and UI chrome to maintain a clear visual hierarchy.

## Typography
The typographic strategy relies on the tension between the technical, geometric personality of **Space Grotesk** and the neutral, utilitarian perfection of **Inter**. 

Headlines utilize tight tracking and bold weights to create high-impact "visual hooks" reminiscent of magazine mastheads. Per the design constraints, no serif fonts are permitted; instead, we achieve elegance through oversized scale and deliberate line-height. Body text is optimized for deep reading with generous leading (1.6x) to ensure comfort against the dark background. Labels and metadata should often use uppercase styling with slight letter spacing to differentiate them from prose.

## Layout & Spacing
This design system utilizes a **Fluid-Fixed Hybrid** model. The layout is anchored by an aggressive 24px minimum page margin, ensuring content never feels cramped against the bezel. 

The vertical rhythm is defined by large "breathing rooms" (stack-xl/lg) between major sections to prevent information density from overwhelming the user. We use a 12-column grid for tablet and desktop, collapsing to a single or dual-column view on mobile. Components should rely on internal padding that mirrors the external margins to maintain a consistent "frame" effect throughout the application.

## Elevation & Depth
In a pitch-black environment, traditional shadows are ineffective. Instead, we use **Tonal Layering** and **Low-Contrast Outlines**.

Level 0 is the #000000 background. Level 1 (Cards, Sheets) uses the #1A1A1A surface. To define edges without adding bulk, use a subtle 1px border of #2A2A2A on elevated elements. For "Floating" elements like Bottom Navigation Bars, use a heavy backdrop blur (20px+) with a 60% opaque #1A1A1A fill. This creates a "Glassmorphism" effect that feels premium and maintains a sense of spatial awareness without relying on drop shadows.

## Shapes
The shape language is "Subtle Precision." We avoid the playfulness of fully rounded "pill" shapes in favor of **8px (Standard)** and **12px (Large)** corner radii. This maintains a structured, architectural feel while softening the harshness of the high-contrast color palette. All interactive containers—from input fields to article thumbnails—must adhere to these specific radii to ensure a unified visual signature.

## Components

**Buttons**
- **Primary:** Solid Editorial Gold (#D4AF37) with Black text. 8px radius. No shadow.
- **Secondary:** Ghost White outline (1px) with transparent background.
- **Tertiary:** Text-only, Ghost White, uppercase label-md styling.

**Cards**
- Background: Graphite (#1A1A1A).
- Padding: 24px (consistent with page margins).
- Border: 1px subtle stroke (#2A2A2A) to define edges against the black void.

**Input Fields**
- Background: Transparent with a bottom-only 1px border (#888888). 
- Active State: Border transitions to Editorial Gold (#D4AF37).
- Label: Inter, 12px, Uppercase, Ash Grey.

**Progress & Indicators**
- Always use Editorial Gold (#D4AF37). Thin 2px lines for progress bars to maintain the minimalist aesthetic.

**Lists & Dividers**
- Dividers should be hairline (0.5px) and colored #2A2A2A. 
- List items should have generous vertical padding (20px+) to honor the editorial whitespace requirement.

**Featured Article Hero**
- Full-bleed imagery with a 40% black-to-transparent gradient overlay at the bottom. 
- Headline (Space Grotesk) sits directly on the image in Ghost White.