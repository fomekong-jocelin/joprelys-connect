package com.joprelys.backend.eco.api;

import com.joprelys.backend.eco.application.EcoBusinessException;
import com.joprelys.backend.eco.application.EcoBusinessException.EcoError;
import com.joprelys.backend.eco.application.EcoCaller;
import com.joprelys.backend.eco.config.EcoProperties;
import java.util.Optional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Traduit un jeton Joprelys ID validé en appelant de l'écosystème ; refuse les clients non déclarés. */
@Component
public class EcoCallerResolver {

    private final EcoProperties properties;

    public EcoCallerResolver(EcoProperties properties) {
        this.properties = properties;
    }

    public EcoCaller resolve(Jwt jwt) {
        String clientId = jwt.getClaimAsString("azp");
        String application = properties.applicationFor(clientId)
                .orElseThrow(() -> new EcoBusinessException(EcoError.UNKNOWN_CLIENT));
        Optional<String> dpu = Optional.ofNullable(jwt.getClaimAsString("dpu")).filter(value -> !value.isBlank());
        return new EcoCaller(jwt.getSubject(), clientId, application, dpu);
    }
}
