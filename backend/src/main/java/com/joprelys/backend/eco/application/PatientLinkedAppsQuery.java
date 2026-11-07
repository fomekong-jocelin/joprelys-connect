package com.joprelys.backend.eco.application;

import java.util.List;
import java.util.UUID;

/** Lecture, pour le portail patient Joprelys, des applications actuellement reliées au dossier. */
public interface PatientLinkedAppsQuery {

    /** Liaisons actives du patient, les plus récentes d'abord. */
    List<LinkedAppView> activeLinks(UUID patientId);
}
