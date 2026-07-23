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

public class V90__enforce_unit_space_assignment_non_overlap extends BaseJavaMigration {

    static final String CONSTRAINT_NAME = "ex_unit_space_assignments_valid_period_no_overlap";

    private static final String OVERLAP_COUNT_SQL = """
            SELECT COUNT(*)
            FROM organizational_unit_space_assignments left_assignment
            JOIN organizational_unit_space_assignments right_assignment
              ON left_assignment.organization_id = right_assignment.organization_id
             AND left_assignment.organizational_unit_id = right_assignment.organizational_unit_id
             AND left_assignment.space_id = right_assignment.space_id
             AND left_assignment.id < right_assignment.id
            WHERE tstzrange(
                    left_assignment.valid_from,
                    COALESCE(left_assignment.valid_to, 'infinity'::timestamptz),
                    '[)')
                  &&
                  tstzrange(
                    right_assignment.valid_from,
                    COALESCE(right_assignment.valid_to, 'infinity'::timestamptz),
                    '[)')
            """;

    private static final String CREATE_EXTENSION_SQL =
            "CREATE EXTENSION IF NOT EXISTS btree_gist";

    private static final String CREATE_CONSTRAINT_SQL = """
            ALTER TABLE organizational_unit_space_assignments
                ADD CONSTRAINT %s
                EXCLUDE USING gist (
                    organization_id WITH =,
                    organizational_unit_id WITH =,
                    space_id WITH =,
                    tstzrange(
                        valid_from,
                        COALESCE(valid_to, 'infinity'::timestamptz),
                        '[)') WITH &&
                )
            """.formatted(CONSTRAINT_NAME);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgresql(connection)) {
            return;
        }

        long overlapCount = countOverlaps(connection);
        if (overlapCount > 0) {
            throw new FlywayException(
                    "HOS-LOC V90 blocked: " + overlapCount
                            + " overlapping organizational unit / space assignment pair(s) detected. "
                            + "Resolve the dated assignments explicitly before retrying Flyway.");
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

    private long countOverlaps(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(OVERLAP_COUNT_SQL);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new FlywayException("HOS-LOC V90 preflight returned no result.");
            }
            return resultSet.getLong(1);
        }
    }
}
