package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** PostgreSQL-only exclusion constraints for HOS-STAFF-001-A. */
public class V94__enforce_structured_staff_assignment_periods extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgresql(connection)) {
            return;
        }

        execute(connection, "CREATE EXTENSION IF NOT EXISTS btree_gist");

        addConstraintIfMissing(connection, "ex_staff_specialty_period_no_overlap", """
                ALTER TABLE staff_specialty_assignments
                ADD CONSTRAINT ex_staff_specialty_period_no_overlap
                EXCLUDE USING gist (
                    organization_id WITH =,
                    staff_id WITH =,
                    specialty_code WITH =,
                    tsrange(valid_from, COALESCE(valid_to, 'infinity'::timestamp), '[)') WITH &&
                )
                """);

        addConstraintIfMissing(connection, "ex_staff_primary_specialty_period_no_overlap", """
                ALTER TABLE staff_specialty_assignments
                ADD CONSTRAINT ex_staff_primary_specialty_period_no_overlap
                EXCLUDE USING gist (
                    organization_id WITH =,
                    staff_id WITH =,
                    tsrange(valid_from, COALESCE(valid_to, 'infinity'::timestamp), '[)') WITH &&
                ) WHERE (is_primary)
                """);

        addConstraintIfMissing(connection, "ex_staff_unit_period_no_overlap", """
                ALTER TABLE staff_organizational_unit_assignments
                ADD CONSTRAINT ex_staff_unit_period_no_overlap
                EXCLUDE USING gist (
                    organization_id WITH =,
                    staff_id WITH =,
                    organizational_unit_id WITH =,
                    tsrange(valid_from, COALESCE(valid_to, 'infinity'::timestamp), '[)') WITH &&
                )
                """);

        addConstraintIfMissing(connection, "ex_staff_primary_unit_period_no_overlap", """
                ALTER TABLE staff_organizational_unit_assignments
                ADD CONSTRAINT ex_staff_primary_unit_period_no_overlap
                EXCLUDE USING gist (
                    organization_id WITH =,
                    staff_id WITH =,
                    tsrange(valid_from, COALESCE(valid_to, 'infinity'::timestamp), '[)') WITH &&
                ) WHERE (is_primary)
                """);
    }

    private void addConstraintIfMissing(Connection connection, String name, String ddl) throws SQLException {
        if (!constraintExists(connection, name)) {
            execute(connection, ddl);
        }
    }

    private boolean constraintExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = ?")) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private boolean isPostgresql(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }
}
