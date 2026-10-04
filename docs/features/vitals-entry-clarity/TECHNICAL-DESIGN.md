# Conception — constantes

Ancrage : capture utilisateur, DESIGN.md, VisitVitalsFormModalComponent et SmartVitalsAssistantComponent. Extraire un petit composant de choix de mode avec radios et output ; état de sélection distinct de realtimeEnabled/recording. Conserver les services de capture/analyse existants et la validation explicite des propositions.

Modale flex en-tête/corps/footer : seul le corps défile ; max-width 3xl desktop, plein écran mobile, tokens de rayon/ombre. Libellés et unités n'utilisent pas de nouveaux tokens arbitraires. Saisie textuelle optionnelle en textarea, séparée de la capture ; pas de troisième contrôle comprimé dans la rangée du microphone. Activity output transmet l'initialisation/capture/analyse au parent pour verrouiller temporairement la sauvegarde.

Pendant permission microphone : garde contre double clic, invalidation si fermeture/réduction et arrêt des tracks tardifs ; pas d'analyse après destruction. Labels et focus clavier de la modale vérifiés par DOM. Pas de nouveau contrat, migration, stockage, règle clinique ni configuration. Tests Angular et build ; recette visuelle et microphone réel séparés et encore ouverts si accès navigateur refusé.
