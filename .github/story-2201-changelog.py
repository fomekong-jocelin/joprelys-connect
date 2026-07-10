from pathlib import Path

path = Path("docs/ai/CHANGELOG.md")
text = path.read_text(encoding="utf-8")
entry = """### Changed

- **Contrat d’état financier patient/assurance (STORY-2201)** : définition unique de `PAID` comme part patient soldée avec assurance encore due et de `SETTLED` comme facture totalement soldée ; centralisation des créances et transitions dans `InvoiceFinancialStateService`, synchronisation des paiements patient et assurance, protection de l’immuabilité après validation, adaptation de l’export Sage 100 et extension des contrats Angular.

"""
marker = "## [Unreleased]\n\n### Fixed\n"
if "Contrat d’état financier patient/assurance (STORY-2201)" not in text:
    if marker not in text:
        raise SystemExit("Unreleased/Fixed marker missing")
    text = text.replace(marker, "## [Unreleased]\n\n" + entry + "### Fixed\n", 1)
    path.write_text(text, encoding="utf-8")
