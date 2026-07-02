---
name: Copper & Ink
colors:
  surface: '#fbf9f5'
  surface-dim: '#dbdad6'
  surface-bright: '#fbf9f5'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ef'
  surface-container: '#efeeea'
  surface-container-high: '#eae8e4'
  surface-container-highest: '#e4e2de'
  on-surface: '#1b1c1a'
  on-surface-variant: '#524439'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f0ed'
  outline: '#857467'
  outline-variant: '#d8c3b4'
  surface-tint: '#8c4f10'
  primary: '#894d0d'
  on-primary: '#ffffff'
  primary-container: '#a76526'
  on-primary-container: '#fffbff'
  inverse-primary: '#ffb77b'
  secondary: '#5d5e61'
  on-secondary: '#ffffff'
  secondary-container: '#e2e2e5'
  on-secondary-container: '#636467'
  tertiary: '#615c49'
  on-tertiary: '#ffffff'
  tertiary-container: '#7b7460'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdcc2'
  primary-fixed-dim: '#ffb77b'
  on-primary-fixed: '#2e1500'
  on-primary-fixed-variant: '#6d3a00'
  secondary-fixed: '#e2e2e5'
  secondary-fixed-dim: '#c6c6c9'
  on-secondary-fixed: '#1a1c1e'
  on-secondary-fixed-variant: '#454749'
  tertiary-fixed: '#ebe2c9'
  tertiary-fixed-dim: '#cec6ae'
  on-tertiary-fixed: '#1f1b0c'
  on-tertiary-fixed-variant: '#4c4735'
  background: '#fbf9f5'
  on-background: '#1b1c1a'
  surface-variant: '#e4e2de'
typography:
  display-lg:
    fontFamily: Libre Caslon Text
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Libre Caslon Text
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Libre Caslon Text
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  headline-md:
    fontFamily: Libre Caslon Text
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  title-lg:
    fontFamily: Hanken Grotesk
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 16px
  container-padding: 12px
---

## Brand & Style

This design system is crafted for a premium magazine editor application, blending the tactile heritage of high-end print journalism with the efficiency of modern Material 3 architecture. The aesthetic is "New Editorial"—a fusion of **Minimalism** and **Modern Corporate** styles that prioritizes content legibility and sophisticated creative control.

## Colors

The palette is anchored by **Copper (#B87333)**, a warm metallic that serves as the primary brand signifier and call-to-action color. This is balanced by **Deep Charcoal (#1A1C1E)**, providing the necessary weight and sophistication for navigation and structural elements.

The background uses an **Off-white/Cream (#FDFBF7)** to reduce eye strain and simulate high-quality paper stock. This "Paper" surface is critical for the editorial experience. Secondary surfaces and containers use a slightly darker variant to create subtle tonal separation without relying on heavy borders. Success, error, and warning states should be desaturated to maintain the premium, understated feel of the system.

## Typography

The typographic strategy employs a classic "Serif for Voice, Sans for Utility" approach.

- **Libre Caslon Text** is used for headlines and display roles. It brings an authoritative, literary character to the app, mirroring the masthead of a traditional magazine.
- **Hanken Grotesk** is used for all UI elements, body copy, and labels. Its clean, sharp metrics ensure high legibility in dense editing interfaces and toolbars.

## Layout & Spacing

This design system utilizes a **Fluid Grid** model based on an 8px square baseline. 

- **Mobile:** A 4-column layout with 16px side margins. Elements should snap to the grid to maintain the "columnar" feel of a magazine.
- **Tablet/Foldable:** An 8-column or 12-column layout with 24px margins, allowing for a side-rail navigation or property inspector.

Spacing between components follows a strict hierarchy: use 32px+ for section breaks to emphasize the premium "whitespace" feel, while utility bars and tool palettes use tighter 8px/12px spacing to maximize screen real estate during the editing process.

## Elevation & Depth

Consistent with Material 3, elevation is primarily communicated through **Tonal Layering** rather than heavy shadows.

- **Level 0 (Surface):** The cream background.
- **Level 1 (Cards/Lists):** A subtle tint overlay using the Primary Copper color at 5% opacity or a soft neutral grey. Use a 1px "ghost" border in a slightly darker cream for definition.
- **Level 2 (Modals/Floating Action Buttons):** Use ambient, highly diffused shadows (Blur: 12px, Y: 4px, Opacity: 8%) with a subtle Copper tint to maintain warmth.
- **Backdrop Blurs:** Used sparingly behind navigation bars (Glassmorphism) to maintain context of the underlying "paper" while ensuring text legibility.

## Shapes

The shape language is refined and consistent. We use **Rounded (0.5rem)** as the default for most components.

- **Buttons & Chips:** Use the `rounded-xl` (1.5rem) setting for a soft, inviting touch that contrasts against the sharp serif typography.
- **Cards & Sheets:** Use `rounded-lg` (1.0rem) to provide clear containment while feeling modern.
- **Input Fields:** Use 8px (0.5rem) corners to maintain a professional, architectural feel in the editor.
- **Images/Media:** Use 12px corners to distinguish content from the surrounding UI.
