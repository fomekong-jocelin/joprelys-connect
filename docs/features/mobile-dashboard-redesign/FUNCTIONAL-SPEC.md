# Functional Spec — Mobile Dashboard UI Overhaul

## 1. Context & User Need
The Flutter clinical mobile dashboard had visual feedback regarding dark theme colors, header layout, metric cards alignment, and visit card formatting (such as repetitive `DPU DPU-` text and harsh blue radial top gradients).
The objective is to provide a sleek, medical-grade, highly readable mobile UI that respects `DESIGN.md` and `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md`.

## 2. Requirements
- Remove artificial radial smudges in background; replace with smooth dark theme background.
- Refine workspace badge, greeting header, section titles and icons.
- Enhance active patient queue metric cards (`En attente`, `Prêts`, `À évaluer`) with clear borders, semantic micro-icons, and clean number hierarchy.
- Re-architect patient queue visit card (`_ActiveVisitCard`):
  - Fix DPU prefix repetition (`DPU DPU-` -> `DPU-`).
  - Top header row with patient initials avatar, patient name, and top-right status pill badge.
  - Clear section for chief complaint and chips (Arrival time, Care unit).
- Guarantee `fr` and `en` internationalization compliance.
- Ensure light and dark mode compliance.
