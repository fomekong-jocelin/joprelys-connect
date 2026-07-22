package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V85__enforce_bed_state_history_integrity extends BaseJavaMigration {

    private static final List<String> STATEMENTS = List.of(
            """
            ALTER TABLE bed_state_changes
                ADD CONSTRAINT ck_bed_state_changes_axis
                CHECK (state_axis IN ('CAPACITY', 'READINESS'))
            """,
            """
            ALTER TABLE bed_state_changes
                ADD CONSTRAINT ck_bed_state_changes_source
                CHECK (source IN ('MANUAL', 'SYSTEM_TRANSFER', 'SYSTEM_PHYSICAL_DEPARTURE', 'LEGACY_SUPERVISION'))
            """);

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!isPostgresql(connection)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            for (String sql : STATEMENTS) {
                statement.execute(sql);
            }
        }
    }

    @Override
    public Integer getChecksum() {
        return Objects.hash(STATEMENTS);
    }

    private boolean isPostgresql(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null
                && productName.toLowerCase(Locale.ROOT).contains("postgresql");
    }
}
