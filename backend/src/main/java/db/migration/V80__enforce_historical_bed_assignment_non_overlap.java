package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V80__enforce_historical_bed_assignment_non_overlap extends BaseJavaMigration {

    static final String CONSTRAINT_NAME = "ex_bed_assignments_valid_period_no_overlap";

    private static final String OVERLAP_COUNT_SQL = """
            SELECT COUNT(*)
            FROM bed_assignments left_assignment
            JOIN bed_assignments right_assignment
              ON left_assignment.organization_id = right_assignment.organization_id
             AND left_assignment.bed_id = right_assignment.bed_id
             AND left_assignment.id < right_assignment.id
            WHERE left_assignment.integrity_status = 'VALID'
              AND right_assignment.integrity_status = 'VALID'
              AND tsrange(
                    left_assignment.assigned_at,
                    COALESCE(left_assignment.released_at, 'infinity'::timestamp),
                    '[)')
                  &&
                  tsrange(
                    right_assignment.assigned_at,
                    COALESCE(right_assignment.released_at, 'infinity'::timestamp),
                    '[)')
            """;

    private static final String CREATE_EXTENSION_SQL =
            "CREATE EXTENSION IF NOT EXISTS btree_gist";

    private static final String CREATE_CONSTRAINT_SQL = """
            ALTER TABLE bed_assignments
                ADD CONSTRAINT ex_bed_assignments_valid_period_no_overlap
                EXCLUDE USING gist (
                    organization_id WITH =,
                    bed_id WITH =,
                    tsrange(
                        assigned_at,
                        COALESCE(released_at, 'infinity'::timestamp),
                        '[)') WITH &&
                )
                WHERE (integrity_status = 'VALID')
                DEFERRABLE INITIALLY IMMEDIATE
            """;

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgresql(connection)) {
            return;
        }

        long overlapCount = countUnresolvedOverlaps(connection);
        if (overlapCount > 0) {
            throw new FlywayException(
                    "HOS-BED-001-D bloque V80 : " + overlapCount
                            + " paire(s) d'affectations VALID se chevauchent. "
                            + "Exécuter le préflight, faire arbitrer les périodes par le DBA et le bed manager, "
                            + "puis marquer uniquement les lignes approuvées comme QUARANTINED avant de relancer Flyway.");
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_EXTENSION_SQL);
            statement.execute(CREATE_CONSTRAINT_SQL);
        }
    }

    @Override
    public Integer getChecksum() {
        return Objects.hash(OVERLAP_COUNT_SQL, CREATE_EXTENSION_SQL, CREATE_CONSTRAINT_SQL);
    }

    private boolean isPostgresql(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null
                && productName.toLowerCase(Locale.ROOT).contains("postgresql");
    }

    private long countUnresolvedOverlaps(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OVERLAP_COUNT_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new FlywayException("Le préflight HOS-BED-001-D n'a retourné aucun résultat.");
            }
            return resultSet.getLong(1);
        }
    }
}
