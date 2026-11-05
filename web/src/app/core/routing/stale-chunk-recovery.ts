import { NavigationError } from '@angular/router';

const RELOAD_MARKER_KEY = 'joprelys.staleChunkReload';
/** Un second échec sur la même URL dans ce délai est considéré comme persistant : pas de boucle de rechargement. */
const RELOAD_GUARD_MS = 30_000;

/** Messages des navigateurs quand un module chargé à la demande est introuvable ou n'est pas du JavaScript. */
const STALE_CHUNK_MESSAGES = [
  'Failed to fetch dynamically imported module', // Chrome, Edge
  'error loading dynamically imported module', // Firefox
  'Importing a module script failed', // Safari
  'is not a valid JavaScript MIME type',
];

interface ReloadMarker {
  url: string;
  at: number;
}

/** Vrai si l'erreur vient d'un fichier JavaScript chargé à la demande devenu introuvable. */
export function isStaleChunkError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error ?? '');
  return STALE_CHUNK_MESSAGES.some((fragment) => message.includes(fragment));
}

/**
 * Un onglet ouvert avant un déploiement référence des fichiers JavaScript qui n'existent plus :
 * toute navigation vers un écran chargé à la demande échoue alors sans rien afficher.
 * Recharger l'URL cible récupère la nouvelle version (la session de l'onglet est conservée).
 * Un seul rechargement par URL dans un délai de 30 s, pour ne jamais boucler.
 *
 * @returns vrai si un rechargement a été déclenché.
 */
export function reloadOnStaleChunk(event: NavigationError, win: Window | null): boolean {
  if (!win || !isStaleChunkError(event.error)) {
    return false;
  }
  const now = Date.now();
  const previous = readMarker(win);
  if (previous && previous.url === event.url && now - previous.at < RELOAD_GUARD_MS) {
    return false;
  }
  if (!writeMarker(win, { url: event.url, at: now })) {
    // Sans mémoire du rechargement précédent, une ressource durablement absente ferait boucler la page.
    return false;
  }
  win.location.assign(event.url);
  return true;
}

function readMarker(win: Window): ReloadMarker | null {
  try {
    const raw = win.sessionStorage.getItem(RELOAD_MARKER_KEY);
    return raw ? (JSON.parse(raw) as ReloadMarker) : null;
  } catch {
    return null;
  }
}

function writeMarker(win: Window, marker: ReloadMarker): boolean {
  try {
    win.sessionStorage.setItem(RELOAD_MARKER_KEY, JSON.stringify(marker));
    return true;
  } catch {
    return false;
  }
}
