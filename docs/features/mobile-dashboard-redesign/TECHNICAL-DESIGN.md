# Technical Design — Mobile Dashboard UI Overhaul

## 1. Technical Components Impacted
- `mobile/lib/core/theme/app_design_tokens.dart`: Refine dark background/surface tokens, shadows, and subtle borders.
- `mobile/lib/core/theme/app_theme.dart`: Enhance card themes, outline colors, and input background contrast.
- `mobile/lib/features/dashboard/presentation/dashboard_localizations.dart`: Clean up DPU string formatting to avoid duplicate `DPU DPU-` prefixes.
- `mobile/lib/features/foundation/presentation/pages/foundation_page.dart`: Clean header top bar layout, workspace badge, greeting hierarchy, and background gradient removal.
- `mobile/lib/features/dashboard/presentation/widgets/active_queue_section.dart`: Overhaul `_QueueSummary`, `_MetricCard`, and `_ActiveVisitCard` layouts with micro-iconography, pill badges, and structured spacing.

## 2. Testing Strategy
- Run `flutter analyze` to ensure zero static errors or warnings.
- Run `flutter test --update-goldens` / `flutter test` to ensure all widget and layout tests pass cleanly.
