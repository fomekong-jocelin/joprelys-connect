import { Component, inject, OnInit, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../shared/ui/button.component';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ConsultationApiService } from './consultation-api.service';
import { VisitApiService } from '../visit/visit-api.service';
import { Consultation } from './consultation.models';
import { Vitals } from '../visit/visit.models';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, AppShellComponent],
  template: `
    <app-shell>
      <div class="app-container py-8 max-w-5xl mx-auto space-y-6">

        <!-- Page Header -->
        <div class="flex items-center gap-4">
          <button (click)="goBack()" class="p-2 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-500 hover:text-slate-700 dark:hover:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors cursor-pointer">
            <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <div class="flex items-center gap-2 mb-1">
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-md text-[11px] font-extrabold bg-violet-100 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300 uppercase tracking-wider">
                Consultation Médicale
              </span>
              @if (visitNumber()) {
                <span class="text-xs font-bold text-slate-500 dark:text-slate-400">{{ visitNumber() }}</span>
              }
            </div>
            <h1 class="text-2xl font-extrabold tracking-tight" style="color: var(--text-primary)">
              Saisie de la consultation
            </h1>
          </div>
        </div>

        <!-- Success / Error messages -->
        @if (successMessage()) {
          <div class="flex items-center gap-3 p-4 bg-emerald-50 dark:bg-emerald-950/30 border border-emerald-200 dark:border-emerald-800/50 rounded-xl text-sm font-semibold text-emerald-700 dark:text-emerald-300 animate-fade-in">
            <svg class="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ successMessage() }}
          </div>
        }

        @if (errorMessage()) {
          <div class="flex items-center gap-3 p-4 bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800/50 rounded-xl text-sm font-semibold text-red-700 dark:text-red-300 animate-fade-in">
            <svg class="w-5 h-5 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {{ errorMessage() }}
          </div>
        }

        <!-- Loading state -->
        @if (isLoading()) {
          <div class="py-16 text-center">
            <div class="inline-block w-8 h-8 rounded-full border-4 border-violet-200 border-t-violet-600 animate-spin"></div>
            <p class="mt-3 text-sm font-semibold" style="color: var(--text-muted)">Chargement des données de la visite...</p>
          </div>
        } @else {

          <!-- Card: Constantes Vitales -->
          <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-2xl shadow-xs overflow-hidden">
            <div class="px-6 py-4 border-b border-slate-100 dark:border-slate-800/80 bg-gradient-to-r from-cyan-50 to-blue-50 dark:from-cyan-950/20 dark:to-blue-950/20 flex items-center gap-3">
              <div class="w-9 h-9 rounded-xl bg-cyan-500/10 dark:bg-cyan-500/20 flex items-center justify-center text-cyan-600 dark:text-cyan-400">
                <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
              </div>
              <div>
                <h2 class="font-display font-bold text-base text-brand-night dark:text-white">Constantes Vitales</h2>
                <p class="text-xs text-slate-500 dark:text-slate-400">Données de tri initial — lecture seule</p>
              </div>
            </div>
            <div class="p-6">
              @if (vitals()) {
                <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <!-- Température -->
                  <div class="p-3.5 rounded-xl bg-orange-50 dark:bg-orange-950/20 border border-orange-100 dark:border-orange-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-orange-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-orange-600 dark:text-orange-400">Température</span>
                    </div>
                    <span class="text-xl font-extrabold text-orange-700 dark:text-orange-300">
                      {{ vitals()?.temperature ?? '—' }}<span class="text-sm ml-0.5">°C</span>
                    </span>
                  </div>
                  <!-- Poids -->
                  <div class="p-3.5 rounded-xl bg-indigo-50 dark:bg-indigo-950/20 border border-indigo-100 dark:border-indigo-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">Poids</span>
                    </div>
                    <span class="text-xl font-extrabold text-indigo-700 dark:text-indigo-300">
                      {{ vitals()?.weight ?? '—' }}<span class="text-sm ml-0.5">kg</span>
                    </span>
                  </div>
                  <!-- Pouls -->
                  <div class="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/20 border border-rose-100 dark:border-rose-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-rose-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-rose-600 dark:text-rose-400">Pouls</span>
                    </div>
                    <span class="text-xl font-extrabold text-rose-700 dark:text-rose-300">
                      {{ vitals()?.pulse ?? '—' }}<span class="text-sm ml-0.5">bpm</span>
                    </span>
                  </div>
                  <!-- Tension -->
                  <div class="p-3.5 rounded-xl bg-purple-50 dark:bg-purple-950/20 border border-purple-100 dark:border-purple-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-purple-600 dark:text-purple-400">Tension</span>
                    </div>
                    <span class="text-xl font-extrabold text-purple-700 dark:text-purple-300">
                      {{ vitals()?.systolic ?? '—' }}/{{ vitals()?.diastolic ?? '—' }}<span class="text-sm ml-0.5">mmHg</span>
                    </span>
                  </div>
                  <!-- SpO2 -->
                  <div class="p-3.5 rounded-xl bg-sky-50 dark:bg-sky-950/20 border border-sky-100 dark:border-sky-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-sky-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-sky-600 dark:text-sky-400">SpO2</span>
                    </div>
                    <span class="text-xl font-extrabold text-sky-700 dark:text-sky-300">
                      {{ vitals()?.spo2 ?? '—' }}<span class="text-sm ml-0.5">%</span>
                    </span>
                  </div>
                  <!-- Glycémie -->
                  <div class="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/20 border border-amber-100 dark:border-amber-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-amber-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 9.172V5L8 4z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-amber-600 dark:text-amber-400">Glycémie</span>
                    </div>
                    <span class="text-xl font-extrabold text-amber-700 dark:text-amber-300">
                      {{ vitals()?.glycemia ?? '—' }}<span class="text-sm ml-0.5">g/L</span>
                    </span>
                  </div>
                  <!-- Fréquence respiratoire -->
                  <div class="p-3.5 rounded-xl bg-teal-50 dark:bg-teal-950/20 border border-teal-100 dark:border-teal-900/30 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-teal-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-teal-600 dark:text-teal-400">Fréq. Resp.</span>
                    </div>
                    <span class="text-xl font-extrabold text-teal-700 dark:text-teal-300">
                      {{ vitals()?.respiratoryRate ?? '—' }}<span class="text-sm ml-0.5">/min</span>
                    </span>
                  </div>
                  <!-- IMC -->
                  <div class="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-700/50 flex flex-col gap-1">
                    <div class="flex items-center gap-1.5">
                      <svg class="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8v8m-4-5v5m-4-2v2m-2 4h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                      <span class="text-[10px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">IMC</span>
                    </div>
                    <span class="text-xl font-extrabold text-slate-700 dark:text-slate-200">
                      {{ vitals()?.bmi ?? '—' }}<span class="text-sm ml-0.5">kg/m²</span>
                    </span>
                  </div>
                </div>
              } @else {
                <div class="py-6 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl">
                  <p class="text-sm font-semibold" style="color: var(--text-muted)">Aucune constante vitale enregistrée pour cette visite.</p>
                </div>
              }
            </div>
          </div>

          <!-- Card: Saisie de la Consultation -->
          <form [formGroup]="form" (ngSubmit)="onSave()">
            <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-2xl shadow-xs overflow-hidden">
              <div class="px-6 py-4 border-b border-slate-100 dark:border-slate-800/80 bg-gradient-to-r from-violet-50 to-purple-50 dark:from-violet-950/20 dark:to-purple-950/20 flex items-center gap-3">
                <div class="w-9 h-9 rounded-xl bg-violet-500/10 dark:bg-violet-500/20 flex items-center justify-center text-violet-600 dark:text-violet-400">
                  <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <div>
                  <h2 class="font-display font-bold text-base text-brand-night dark:text-white">Saisie de la Consultation</h2>
                  <p class="text-xs text-slate-500 dark:text-slate-400">Remplir les champs obligatoires (*) avant de sauvegarder</p>
                </div>
              </div>
              <div class="p-6 space-y-5">

                <!-- Symptômes -->
                <div class="space-y-1.5">
                  <label class="ui-label">
                    Symptômes <span class="text-red-500">*</span>
                  </label>
                  <textarea
                    formControlName="symptoms"
                    placeholder="Ex: Fièvre à 38.5°C depuis 3 jours, toux sèche persistante, fatigue intense, maux de tête..."
                    rows="3"
                    class="ui-textarea w-full p-3 text-sm rounded-xl transition-colors resize-none"
                    [class.border-red-400]="form.get('symptoms')?.invalid && form.get('symptoms')?.touched"
                  ></textarea>
                  @if (form.get('symptoms')?.invalid && form.get('symptoms')?.touched) {
                    <p class="text-xs text-red-500 font-semibold">Les symptômes sont obligatoires.</p>
                  }
                </div>

                <!-- Examen clinique -->
                <div class="space-y-1.5">
                  <label class="ui-label">Examen clinique</label>
                  <textarea
                    formControlName="clinicalExam"
                    placeholder="Ex: Auscultation pulmonaire normale, abdomen souple non douloureux, ganglions non palpables..."
                    rows="3"
                    class="ui-textarea w-full p-3 text-sm rounded-xl transition-colors resize-none"
                  ></textarea>
                </div>

                <!-- Diagnostic -->
                <div class="space-y-1.5">
                  <label class="ui-label">
                    Diagnostic <span class="text-red-500">*</span>
                  </label>
                  <textarea
                    formControlName="diagnosis"
                    placeholder="Ex: Syndrome grippal, infection virale des voies respiratoires supérieures..."
                    rows="3"
                    class="ui-textarea w-full p-3 text-sm rounded-xl transition-colors resize-none"
                    [class.border-red-400]="form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched"
                  ></textarea>
                  @if (form.get('diagnosis')?.invalid && form.get('diagnosis')?.touched) {
                    <p class="text-xs text-red-500 font-semibold">Le diagnostic est obligatoire.</p>
                  }
                </div>

                <!-- Conseils patient -->
                <div class="space-y-1.5">
                  <label class="ui-label">Conseils au patient</label>
                  <textarea
                    formControlName="advice"
                    placeholder="Ex: Repos complet, hydratation abondante, éviter la sortie par temps froid..."
                    rows="2"
                    class="ui-textarea w-full p-3 text-sm rounded-xl transition-colors resize-none"
                  ></textarea>
                </div>

                <!-- Suivi recommandé -->
                <div class="space-y-1.5">
                  <label class="ui-label">Suivi recommandé</label>
                  <input
                    type="text"
                    formControlName="followUp"
                    placeholder="Ex: Contrôle dans 7 jours, bilan sanguin NFS à J+10..."
                    class="ui-input w-full p-3 text-sm rounded-xl transition-colors"
                  />
                </div>
              </div>
            </div>

            <!-- Card: Prescription Médicale -->
            <div class="bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-2xl shadow-xs overflow-hidden mt-6">
              <div class="px-6 py-4 border-b border-slate-100 dark:border-slate-800/80 bg-gradient-to-r from-emerald-50 to-teal-50 dark:from-emerald-950/20 dark:to-teal-950/20 flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-emerald-500/10 dark:bg-emerald-500/20 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                    <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 9.172V5L8 4z" />
                    </svg>
                  </div>
                  <div>
                    <h2 class="font-display font-bold text-base text-brand-night dark:text-white">Prescription Médicale</h2>
                    <p class="text-xs text-slate-500 dark:text-slate-400">{{ prescriptionItems.length }} médicament(s) ajouté(s)</p>
                  </div>
                </div>
                <button
                  type="button"
                  (click)="addPrescriptionLine()"
                  class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 transition-colors cursor-pointer"
                >
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                  Ajouter un médicament
                </button>
              </div>

              <div class="p-6">
                @if (prescriptionItems.length === 0) {
                  <div class="py-8 text-center border border-dashed border-slate-200 dark:border-slate-700 rounded-xl">
                    <svg class="w-10 h-10 mx-auto text-slate-300 dark:text-slate-600 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 9.172V5L8 4z" />
                    </svg>
                    <p class="text-sm font-semibold" style="color: var(--text-muted)">Aucun médicament prescrit.</p>
                    <p class="text-xs mt-1" style="color: var(--text-muted)">Cliquez sur "+ Ajouter un médicament" pour commencer.</p>
                  </div>
                } @else {
                  <!-- Desktop table -->
                  <div class="hidden md:block overflow-x-auto">
                    <table class="w-full border-collapse text-sm" formArrayName="prescription">
                      <thead>
                        <tr class="border-b" style="border-color: var(--border-color)">
                          <th class="py-2 px-3 text-left text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Médicament *</th>
                          <th class="py-2 px-3 text-left text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Dosage *</th>
                          <th class="py-2 px-3 text-left text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Posologie</th>
                          <th class="py-2 px-3 text-left text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Durée</th>
                          <th class="py-2 px-3 text-left text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Quantité</th>
                          <th class="py-2 px-3 text-right text-[10px] font-extrabold uppercase tracking-wider" style="color: var(--text-muted)">Action</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y" style="border-color: var(--border-color)">
                        @for (item of prescriptionItems.controls; track $index; let i = $index) {
                          <tr [formGroupName]="i" class="hover:bg-slate-50/50 dark:hover:bg-slate-800/20 transition-colors">
                            <td class="py-2 px-3">
                              <input type="text" formControlName="drugName" placeholder="Ex: Paracétamol" class="ui-input w-full text-xs p-2 rounded-lg" />
                            </td>
                            <td class="py-2 px-3">
                              <input type="text" formControlName="dosage" placeholder="Ex: 500 mg" class="ui-input w-full text-xs p-2 rounded-lg" />
                            </td>
                            <td class="py-2 px-3">
                              <input type="text" formControlName="posology" placeholder="Ex: 3x/jour" class="ui-input w-full text-xs p-2 rounded-lg" />
                            </td>
                            <td class="py-2 px-3">
                              <input type="text" formControlName="duration" placeholder="Ex: 5 jours" class="ui-input w-full text-xs p-2 rounded-lg" />
                            </td>
                            <td class="py-2 px-3">
                              <input type="text" formControlName="quantity" placeholder="Ex: 1 boîte" class="ui-input w-full text-xs p-2 rounded-lg" />
                            </td>
                            <td class="py-2 px-3 text-right">
                              <button
                                type="button"
                                (click)="removePrescriptionLine(i)"
                                title="Supprimer ce médicament"
                                class="inline-flex items-center justify-center w-7 h-7 rounded-lg text-red-500 bg-red-50 hover:bg-red-100 dark:bg-red-950/20 dark:hover:bg-red-900/30 transition-colors cursor-pointer"
                              >
                                <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                              </button>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>

                  <!-- Mobile card view -->
                  <div class="md:hidden space-y-3" formArrayName="prescription">
                    @for (item of prescriptionItems.controls; track $index; let i = $index) {
                      <div [formGroupName]="i" class="p-4 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-700/50 space-y-3">
                        <div class="flex items-center justify-between">
                          <span class="text-xs font-bold text-emerald-700 dark:text-emerald-400">Médicament {{ i + 1 }}</span>
                          <button type="button" (click)="removePrescriptionLine(i)" class="text-red-500 hover:text-red-700 cursor-pointer">
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                          </button>
                        </div>
                        <div class="grid grid-cols-2 gap-2">
                          <div class="col-span-2 space-y-1">
                            <label class="ui-label text-[10px]">Médicament *</label>
                            <input type="text" formControlName="drugName" placeholder="Nom du médicament" class="ui-input w-full text-xs p-2 rounded-lg" />
                          </div>
                          <div class="space-y-1">
                            <label class="ui-label text-[10px]">Dosage *</label>
                            <input type="text" formControlName="dosage" placeholder="Ex: 500mg" class="ui-input w-full text-xs p-2 rounded-lg" />
                          </div>
                          <div class="space-y-1">
                            <label class="ui-label text-[10px]">Posologie</label>
                            <input type="text" formControlName="posology" placeholder="Ex: 3x/jour" class="ui-input w-full text-xs p-2 rounded-lg" />
                          </div>
                          <div class="space-y-1">
                            <label class="ui-label text-[10px]">Durée</label>
                            <input type="text" formControlName="duration" placeholder="Ex: 5 jours" class="ui-input w-full text-xs p-2 rounded-lg" />
                          </div>
                          <div class="space-y-1">
                            <label class="ui-label text-[10px]">Quantité</label>
                            <input type="text" formControlName="quantity" placeholder="Ex: 1 boîte" class="ui-input w-full text-xs p-2 rounded-lg" />
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                }
              </div>
            </div>            <!-- Action Bar -->
            <div class="mt-6 flex flex-col sm:flex-row items-center justify-between gap-4 p-5 bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/60 rounded-2xl shadow-xs">
              <app-ui-button variant="secondary" (pressed)="goBack()" [disabled]="isSaving() || isClosing()">
                Annuler
              </app-ui-button>
              <div class="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
                <app-ui-button variant="secondary" type="button" (pressed)="onSave(false)" [disabled]="form.invalid || isSaving() || isClosing()">
                  @if (isSaving() && !shouldCloseAfterSave) {
                    <span class="flex items-center gap-2">
                      <span class="inline-block w-4 h-4 rounded-full border-2 border-slate-300 border-t-slate-600 animate-spin"></span>
                      Enregistrement...
                    </span>
                  } @else {
                    <span class="flex items-center gap-2">
                      Sauvegarder (Brouillon)
                    </span>
                  }
                </app-ui-button>
                <app-ui-button variant="primary" type="button" (pressed)="onSave(true)" [disabled]="form.invalid || isSaving() || isClosing()">
                  @if (isSaving() && shouldCloseAfterSave) {
                    <span class="flex items-center gap-2">
                      <span class="inline-block w-4 h-4 rounded-full border-2 border-white/40 border-t-white animate-spin"></span>
                      Clôture en cours...
                    </span>
                  } @else {
                    <span class="flex items-center gap-2">
                      <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      Valider & Clôturer la visite
                    </span>
                  }
                </app-ui-button>
              </div>
            </div>
          </form>
        }
      </div>
    </app-shell>
  `,
})
export class ConsultationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly visitApi = inject(VisitApiService);

  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly isClosing = signal(false);
  readonly successMessage = signal('');
  readonly errorMessage = signal('');
  readonly vitals = signal<Vitals | null>(null);
  readonly consultation = signal<Consultation | null>(null);
  readonly visitNumber = signal('');

  private visitId = '';
  shouldCloseAfterSave = false;
  readonly form: FormGroup = this.fb.group({
    symptoms: ['', Validators.required],
    clinicalExam: [''],
    diagnosis: ['', Validators.required],
    advice: [''],
    followUp: [''],
    prescription: this.fb.array([]),
  });

  get prescriptionItems(): FormArray {
    return this.form.get('prescription') as FormArray;
  }

  ngOnInit(): void {
    this.visitId = this.route.snapshot.paramMap.get('visitId') ?? '';
    this.loadData();
  }

  private loadData(): void {
    if (!this.visitId) return;
    this.isLoading.set(true);

    // Load vitals
    this.http.get<{ visitNumber?: string } & Vitals>(`/api/visits/${this.visitId}/vitals`).subscribe({
      next: (data) => {
        this.vitals.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });

    // Load visit info for visit number
    this.http.get<{ visitNumber: string }>(`/api/visits/${this.visitId}`).subscribe({
      next: (visit) => {
        if (visit?.visitNumber) {
          this.visitNumber.set(visit.visitNumber);
        }
      },
      error: () => {}
    });

    // Load existing consultation (ignore 404 silently)
    this.consultationApi.getConsultation(this.visitId).subscribe({
      next: (existing) => {
        this.consultation.set(existing);
        this.form.patchValue({
          symptoms: existing.symptoms,
          clinicalExam: existing.clinicalExam ?? '',
          diagnosis: existing.diagnosis,
          advice: existing.advice ?? '',
          followUp: existing.followUp ?? '',
        });
      },
      error: () => {}
    });
  }

  addPrescriptionLine(): void {
    const line = this.fb.group({
      drugName: ['', Validators.required],
      dosage: ['', Validators.required],
      posology: [''],
      duration: [''],
      quantity: [''],
    });
    this.prescriptionItems.push(line);
  }

  removePrescriptionLine(index: number): void {
    this.prescriptionItems.removeAt(index);
  }

  onSave(closeVisitAfter: boolean = false): void {
    if (this.form.invalid || this.isSaving() || this.isClosing()) return;

    this.shouldCloseAfterSave = closeVisitAfter;
    this.isSaving.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    const { symptoms, clinicalExam, diagnosis, advice, followUp } = this.form.value;

    this.consultationApi.saveConsultation(this.visitId, {
      symptoms,
      clinicalExam: clinicalExam || undefined,
      diagnosis,
      advice: advice || undefined,
      followUp: followUp || undefined,
    }).subscribe({
      next: (savedConsultation) => {
        this.consultation.set(savedConsultation);

        const prescriptionLines = this.prescriptionItems.value;
        if (prescriptionLines.length > 0) {
          this.consultationApi.savePrescription(savedConsultation.id, {
            items: prescriptionLines,
          }).subscribe({
            next: () => {
              this.handleAfterSaveSuccess(closeVisitAfter, 'Consultation et prescription enregistrées avec succès !');
            },
            error: (err) => {
              this.isSaving.set(false);
              this.errorMessage.set('Consultation enregistrée mais erreur de prescription : ' + (err.error?.detail || err.message || 'Erreur inconnue'));
            }
          });
        } else {
          this.handleAfterSaveSuccess(closeVisitAfter, 'Consultation enregistrée avec succès !');
        }
      },
      error: (err) => {
        this.isSaving.set(false);
        this.errorMessage.set(err.error?.detail || err.error?.title || 'Une erreur est survenue lors de l\'enregistrement.');
      }
    });
  }

  private handleAfterSaveSuccess(closeVisitAfter: boolean, successMsg: string): void {
    if (closeVisitAfter) {
      this.isClosing.set(true);
      this.visitApi.closeVisit(this.visitId).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.isClosing.set(false);
          this.successMessage.set('Consultation enregistrée et visite clôturée avec succès !');
          setTimeout(() => this.goBack(), 1500);
        },
        error: (err) => {
          this.isSaving.set(false);
          this.isClosing.set(false);
          this.errorMessage.set('Consultation enregistrée mais impossible de clôturer la visite : ' + (err.error?.detail || err.message || 'Erreur inconnue'));
        }
      });
    } else {
      this.isSaving.set(false);
      this.successMessage.set(successMsg);
    }
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}
