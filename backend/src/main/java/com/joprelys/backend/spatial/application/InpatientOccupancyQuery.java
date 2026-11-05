package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.api.SpaceOccupancyResponse;
import java.util.List;

/** Vue d'ensemble de l'occupation des lits, chambre par chambre, pour l'établissement de l'utilisateur. */
public interface InpatientOccupancyQuery {

    /**
     * Occupation de toutes les chambres d'hébergement actives de l'établissement courant,
     * triées par code de chambre, avec leurs lits triés par numéro.
     */
    List<SpaceOccupancyResponse> listInpatientOccupancy();
}
