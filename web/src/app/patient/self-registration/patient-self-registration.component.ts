import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { InputComponent } from '../../shared/ui/input.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientPreRegistrationRequest } from '../patient.models';

@Component({
  selector: 'app-patient-self-registration',
  standalone: true,
  imports: [AlertComponent, ButtonComponent, CardComponent, InputComponent],
  templateUrl: './patient-self-registration.component.html',
})
export class PatientSelfRegistrationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly patientApiService = inject(PatientApiService);
  readonly i18n = inject(I18nService);

  // Identifiant d'organisation extrait de l'URL
  organizationId = signal<string | null>(null);
  organizationIdMissing = signal(false);

  // État du cycle de vie du captcha
  captchaId = signal<string | null>(null);
  captchaQuestion = signal<string | null>(null);
  captchaLoading = signal(false);

  // État de soumission
  submitting = signal(false);
  registrationSuccess = signal(false);
  errorMessage = signal<string | null>(null);

  // Type d'admission
  isNewAdmission = signal(true);

  // Formulaire d'identité
  firstName = signal('');
  lastName = signal('');
  gender = signal('');
  birthDate = signal('');
  bloodGroup = signal('');

  // Coordonnées de contact
  phone = signal('');
  email = signal('');
  address = signal('');

  // Contact d'urgence
  emergencyContactName = signal('');
  emergencyContactPhone = signal('');
  emergencyContactRelation = signal('');

  // Captcha de sécurité
  captchaAnswer = signal('');

  ngOnInit(): void {
    // Tenter de lire organizationId depuis les query params (?orgId={uuid} ou ?organizationId={uuid})
    this.route.queryParams.subscribe((params) => {
      const orgId = params['orgId'] || params['organizationId'];
      if (orgId && this.isValidUuid(orgId)) {
        this.organizationId.set(orgId);
        this.organizationIdMissing.set(false);
        this.loadCaptcha();
      } else {
        this.organizationIdMissing.set(true);
      }
    });
  }

  isValidUuid(uuid: string): boolean {
    const regex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    return regex.test(uuid);
  }

  loadCaptcha(): void {
    this.captchaLoading.set(true);
    this.captchaAnswer.set('');
    this.patientApiService.getPublicCaptcha().subscribe({
      next: (res) => {
        this.captchaId.set(res.captchaId);
        this.captchaQuestion.set(res.question);
        this.captchaLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.i18n.t('common.error.server'));
        this.captchaLoading.set(false);
      }
    });
  }

  toggleLanguage(): void {
    this.i18n.toggle();
  }

  setAdmissionType(isNew: boolean): void {
    this.isNewAdmission.set(isNew);
  }

  submitForm(): void {
    this.errorMessage.set(null);

    // Validation des champs requis
    if (
      !this.firstName().trim() ||
      !this.lastName().trim() ||
      !this.gender() ||
      !this.birthDate()
    ) {
      this.errorMessage.set(this.i18n.t('selfRegistration.errorRequired'));
      return;
    }

    if (!this.captchaId() || !this.captchaAnswer().trim()) {
      this.errorMessage.set(this.i18n.t('selfRegistration.errorCaptcha'));
      return;
    }

    const orgId = this.organizationId();
    if (!orgId) {
      this.errorMessage.set(this.i18n.t('selfRegistration.errorOrgMissing'));
      return;
    }

    this.submitting.set(true);

    const request: PatientPreRegistrationRequest = {
      organizationId: orgId,
      firstName: this.firstName().trim(),
      lastName: this.lastName().trim(),
      gender: this.gender(),
      birthDate: this.birthDate(),
      bloodGroup: this.bloodGroup() || undefined,
      phone: this.phone().trim() || undefined,
      email: this.email().trim() || undefined,
      address: this.address().trim() || undefined,
      emergencyContactName: this.emergencyContactName().trim() || undefined,
      emergencyContactPhone: this.emergencyContactPhone().trim() || undefined,
      emergencyContactRelation: this.emergencyContactRelation() || undefined,
      captchaId: this.captchaId()!,
      captchaAnswer: this.captchaAnswer().trim()
    };

    this.patientApiService.submitPublicPreRegistration(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.registrationSuccess.set(true);
      },
      error: (err) => {
        this.submitting.set(false);
        if (err.status === 400 && err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else if (err.error?.message) {
          this.errorMessage.set(err.error.message);
        } else {
          this.errorMessage.set(this.i18n.t('common.error.server'));
        }
        // Recharger le captcha après échec (requis car le captcha expiré/invalide est supprimé du cache serveur)
        this.loadCaptcha();
      }
    });
  }

  resetForm(): void {
    this.firstName.set('');
    this.lastName.set('');
    this.gender.set('');
    this.birthDate.set('');
    this.bloodGroup.set('');
    this.phone.set('');
    this.email.set('');
    this.address.set('');
    this.emergencyContactName.set('');
    this.emergencyContactPhone.set('');
    this.emergencyContactRelation.set('');
    this.captchaAnswer.set('');
    this.registrationSuccess.set(false);
    this.errorMessage.set(null);
    this.loadCaptcha();
  }
}
