package com.joprelys.backend.eco;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;

/** Valide une ressource contre les schémas eco-contracts v1 copiés dans les ressources de test. */
public final class EcoContractSchemas {

    private static final String ID_PREFIX = "https://contracts.eco.example/v1";
    private static final SchemaRegistry REGISTRY = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
            builder -> builder.schemaIdResolvers(resolvers -> resolvers
                    .mapPrefix(ID_PREFIX, "classpath:eco-contracts/v1/schemas")));

    private EcoContractSchemas() {
    }

    /** @return les erreurs de validation (vide si la ressource respecte le contrat) */
    public static List<String> validate(String schemaFile, String json) {
        Schema schema = REGISTRY.getSchema(SchemaLocation.of(ID_PREFIX + "/" + schemaFile));
        schema.initializeValidators();
        List<Error> errors = schema.validate(json, InputFormat.JSON,
                context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
        return errors.stream().map(Error::toString).toList();
    }
}
