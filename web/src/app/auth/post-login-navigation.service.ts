import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

const LOGIN_PATHS = new Set(['/login', '/patient/login']);

/**
 * Ouvre la page de destination après une authentification réussie.
 *
 * Le code OTP est consommé dès sa vérification : si la navigation qui suit échoue (fichier JavaScript
 * introuvable, garde refusée, réseau coupé), l'utilisateur ne doit pas se retrouver devant un formulaire
 * apparemment intact dont le second envoi échouera. Le résultat permet donc à l'écran de connexion
 * d'afficher un message utile au lieu de revenir silencieusement à son état initial.
 */
@Injectable({ providedIn: 'root' })
export class PostLoginNavigationService {
  private readonly router = inject(Router);
  private pending: Promise<boolean> | null = null;

  /**
   * Résout `true` quand la page de connexion a été quittée, `false` quand la navigation a échoué
   * ou a été annulée en laissant l'utilisateur sur la page de connexion.
   * Des appels simultanés partagent la même navigation (pas de double navigation concurrente).
   */
  open(url: string): Promise<boolean> {
    if (this.pending) {
      return this.pending;
    }
    const attempt = this.router.navigateByUrl(url)
      .then((navigated) => navigated || !this.isOnLoginPage(), () => false)
      .finally(() => {
        this.pending = null;
      });
    this.pending = attempt;
    return attempt;
  }

  private isOnLoginPage(): boolean {
    return LOGIN_PATHS.has(this.router.url.split(/[?#]/)[0]);
  }
}
