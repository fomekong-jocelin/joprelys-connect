from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"Expected {label} not found")
    return text.replace(old, new, 1)


changelog_path = Path("docs/ai/CHANGELOG.md")
changelog = changelog_path.read_text(encoding="utf-8")
marker = "## [Unreleased]\n\n"
entry = """## [Unreleased]

- **MOB-2804 — Client API Flutter, erreurs, corrélation et résilience réseau** :
  - **Configuration** : ajout de `APP_ENV` / `API_BASE_URL`, validation stricte de la base URL et HTTPS obligatoire en recette/production ; aucune valeur sensible n’est placée dans `--dart-define`.
  - **Client central** : Dio est encapsulé par `ApiClient` et injecté via Riverpod ; headers JSON, locale, bearer conditionnel, idempotence et `X-Trace-Id` sont gérés hors des widgets et des features métier.
  - **Sessions** : port `ApiSessionAccess`, distinction professionnel/patient, coordination d’un seul refresh concurrent et replay borné ; un `403` ne déclenche jamais de refresh/logout et le patient n’utilise jamais le refresh professionnel.
  - **Résilience / erreurs** : retry unique limité aux requêtes sûres ou explicitement idempotentes, mapping de l’enveloppe Joprelys, de `ProblemDetail` et des erreurs Dio, sans log de token, cookie, PII ou body clinique.
  - **Frontière thème préservée** : aucun changement de `AppTheme`, `AppDesignTokens` ou widget partagé ; `core/network` reste non visuel et les DTO métier restent feature-scoped.
  - **Validation runtime** : run #2046 (`30522369209`) vert sur `0c2670a3545bedc60d2f241e9452bc7a6a594de8` : pub get, format, analyze, tests Flutter et APK debug. Gate final exact-HEAD requis après clôture documentaire.

"""
changelog = replace_once(changelog, marker, entry, "Unreleased marker")
changelog = replace_once(
    changelog,
    "  - **Validation runtime** : run #2019 (`30516924571`) vert sur `b242cbcd0c21f48b153fdfe28cabd52de5250a8e` : pub get, format, analyze, tests Flutter et APK debug. Gate final exact-HEAD requis après clôture documentaire.",
    "  - **Validation finale** : run #2030 (`30517927222`) vert sur le HEAD exact `5363c491585b0f4944f941a2576730c28c9c9775`, puis fusion squash de la PR #251 dans `main` au commit `43e3d053`.",
    "MOB-2803 validation line",
)
changelog_path.write_text(changelog, encoding="utf-8")

tracking_path = Path("docs/ai/PROJECT-TRACKING.md")
tracking = tracking_path.read_text(encoding="utf-8")
replacements = [
    (
        "| Dernière mise à jour | 2026-07-30 — MOB-2803 : i18n Flutter FR/EN, résolution de locale système et formats date/heure/nombre implémentés dans la PR #251. Gate runtime #2019 vert ; clôture documentaire terminée, gate final exact-HEAD requis avant fusion. |",
        "| Dernière mise à jour | 2026-07-30 — MOB-2804 : client API Flutter, corrélation, erreurs, refresh coordonné et retry borné implémentés dans la PR #253. Gate runtime #2046 vert ; clôture documentaire terminée, gate final exact-HEAD requis avant fusion. |",
        "last update",
    ),
    (
        "| État global | MOB-2801 et MOB-2802 sont DONE et fusionnés. MOB-2803 fournit la fondation i18n native FR/EN sans élargir `AppTheme` ni `AppDesignTokens` ; la composition visuelle métier reste feature-scoped. MOB-2804 réseau/API est la prochaine story après validation finale de #251. |",
        "| État global | MOB-2801, MOB-2802 et MOB-2803 sont DONE et fusionnés. MOB-2804 fournit la fondation réseau native sans élargir le thème ni déplacer les DTO métier hors des features. MOB-2805 auth/secure storage est la prochaine story après validation finale de #253. |",
        "global state",
    ),
    (
        "| Risques majeurs | P0 mobile : le contrat backend audio segmenté/idempotent reste à construire avant le pilote longue durée et la rétention locale chiffrée doit être validée DPO. La persistance de préférence de langue reste volontairement différée jusqu’à une couche de préférences non sensibles ; la frontière du thème doit rester limitée comme sur le web. |",
        "| Risques majeurs | P0 mobile : le contrat backend audio segmenté/idempotent reste à construire avant le pilote longue durée et la rétention locale chiffrée doit être validée DPO. Le CookieJar de MOB-2804 est volontairement volatile ; MOB-2805 doit fournir la persistance sécurisée, le nettoyage de session et la recette réelle du refresh professionnel. |",
        "risks",
    ),
    (
        "| Prochaine priorité | Obtenir le gate final vert de la PR #251 puis engager MOB-2804 (client réseau/API) et MOB-2805 (auth/secure storage). En parallèle, préparer MOB-2814 avant le pilote audio natif longue durée. |",
        "| Prochaine priorité | Obtenir le gate final vert de la PR #253 puis engager MOB-2805 (auth, secure storage, refresh réel et biométrie). En parallèle, préparer MOB-2814 avant le pilote audio natif longue durée. |",
        "next priority",
    ),
    (
        "| Capacité sprint | MOB-2800 : 3 SP livré ; MOB-2801 : 5 SP livré ; MOB-2802 : 5 SP livré ; MOB-2803 : 3 SP en review ; EPIC-0028 : 99 SP multi-sprint. Les engagements historiques restent séparés tant qu’un sprint mobile dédié n’est pas validé. |",
        "| Capacité sprint | MOB-2800 : 3 SP livré ; MOB-2801 : 5 SP livré ; MOB-2802 : 5 SP livré ; MOB-2803 : 3 SP livré ; MOB-2804 : 5 SP en review ; EPIC-0028 : 99 SP multi-sprint. Les engagements historiques restent séparés tant qu’un sprint mobile dédié n’est pas validé. |",
        "capacity",
    ),
    (
        "| Charge engagée | MOB-2803 implémenté et validé techniquement par le gate mobile #2019 ; documentation et suivi sont à jour, seul le gate final exact-HEAD reste à obtenir. MOB-2804+ et MOB-2814 ne sont pas encore démarrés. |",
        "| Charge engagée | MOB-2804 implémenté et validé techniquement par le gate mobile #2046 ; documentation et suivi sont à jour, seul le gate final exact-HEAD reste à obtenir. MOB-2805+ et MOB-2814 ne sont pas encore démarrés. |",
        "committed load",
    ),
    (
        "| Dérive globale | MOB-2803 reste dans son enveloppe de 3 SP. Les itérations CI ont détecté le formatage Dart puis un lockfile mal retranscrit ; le lockfile a été régénéré avec Flutter 3.44.6, sans suppression de test ni baisse de couverture. Gate runtime #2019 vert. |",
        "| Dérive globale | MOB-2804 reste dans son enveloppe de 5 SP. Les gates ont détecté le formatage puis des règles d’analyse liées à Dio 5.11 (`transformTimeout`, initialisation et null-safety) ; elles ont été corrigées sans désactiver le lint ni supprimer de test. Gate runtime #2046 vert. |",
        "drift",
    ),
]
for old, new, label in replacements:
    tracking = replace_once(tracking, old, new, label)

tracking = replace_once(
    tracking,
    "| MOB-2803 | EPIC-0028 / MOBILE_NATIVE | Feature / i18n | Internationalisation FR/EN, locale et formats | Flutter / Dart / Riverpod / gen_l10n / intl | IN_REVIEW — issue #250 / PR #251 ; gate runtime #2019 vert, gate final exact-HEAD pending | P0 | 3 | Senior Flutter + i18n | 1–2j | 2–3j | Non recommandé seul | GPT-5.6 Thinking | Tech Lead + QA mobile + UX/i18n | Hors sprint / fondation | ~1j implémentation | Gate final exact-HEAD puis fusion ; MOB-2804 ensuite | Faible | 2026-07-30 |",
    "| MOB-2804 | EPIC-0028 / MOBILE_NATIVE | Feature / Réseau | Client API, erreurs, corrélation, refresh et résilience | Flutter / Dart / Riverpod / Dio | IN_REVIEW — issue #252 / PR #253 ; gate runtime #2046 vert, gate final exact-HEAD pending | P0 | 5 | Senior Flutter + sécurité/auth | 2–3j | 3–4j | Non recommandé seul | GPT-5.6 Thinking | Tech Lead + QA mobile + backend/auth + sécurité | Hors sprint / fondation | ~1j implémentation | Gate final exact-HEAD puis fusion ; MOB-2805 ensuite | Faible/Moyen jusqu’à recette auth | 2026-07-30 |\n| MOB-2803 | EPIC-0028 / MOBILE_NATIVE | Feature / i18n | Internationalisation FR/EN, locale et formats | Flutter / Dart / Riverpod / gen_l10n / intl | DONE — issue #250 / PR #251 fusionnée par squash dans `main` au commit `43e3d053` ; gate final #2030 vert | P0 | 3 | Senior Flutter + i18n | 1–2j | 2–3j | Non recommandé seul | GPT-5.6 Thinking | Tech Lead + QA mobile + UX/i18n | Hors sprint / fondation | ~1j implémentation | Suivi post-fusion ; i18n consommée par MOB-2804+ | Faible | 2026-07-30 |",
    "MOB-2803 row",
)
tracking = replace_once(
    tracking,
    "| EPIC-0028 | MOBILE_NATIVE | Epic | Application mobile native Joprelys Connect | Flutter / Android / iOS / Spring Boot | IN_PROGRESS — architecture, MOB-2801 et MOB-2802 fusionnés ; MOB-2803 en review, MOB-2804 prochaine story | P0/P1 | 99 | Senior Flutter + Android/Kotlin + backend + QA mobile | Multi-sprint | Multi-sprint | Non recommandé | Équipe mobile | Tech Lead + sécurité/DPO + clinique | À planifier | Fondation + design system + i18n partiels | Finaliser MOB-2803 puis réseau/auth et audio natif P0 | Élevé jusqu’au pilote audio | 2026-07-30 |",
    "| EPIC-0028 | MOBILE_NATIVE | Epic | Application mobile native Joprelys Connect | Flutter / Android / iOS / Spring Boot | IN_PROGRESS — architecture et MOB-2801 à MOB-2803 fusionnés ; MOB-2804 en review, MOB-2805 prochaine story | P0/P1 | 99 | Senior Flutter + Android/Kotlin + backend + QA mobile | Multi-sprint | Multi-sprint | Non recommandé | Équipe mobile | Tech Lead + sécurité/DPO + clinique | À planifier | Fondation + design system + i18n + réseau partiels | Finaliser MOB-2804 puis auth et audio natif P0 | Élevé jusqu’au pilote audio | 2026-07-30 |",
    "EPIC-0028 row",
)
tracking_path.write_text(tracking, encoding="utf-8")
