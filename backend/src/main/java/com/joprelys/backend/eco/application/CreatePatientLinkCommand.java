package com.joprelys.backend.eco.application;

import com.joprelys.backend.eco.domain.ConsentScope;
import com.joprelys.backend.eco.domain.LinkRelationship;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record CreatePatientLinkCommand(
        EcoCaller caller,
        UUID externalProfileId,
        LinkRelationship relationship,
        Set<ConsentScope> scopes,
        Optional<Instant> periodEnd) {

    public CreatePatientLinkCommand {
        scopes = Set.copyOf(scopes);
    }
}
