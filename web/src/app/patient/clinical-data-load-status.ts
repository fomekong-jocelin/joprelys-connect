/**
 * État de chargement d'une donnée clinique de sécurité (allergies, antécédents…).
 * `error` doit toujours être affiché comme « non vérifié », jamais comme une liste vide.
 */
export type ClinicalDataLoadStatus = 'idle' | 'loading' | 'loaded' | 'error';
