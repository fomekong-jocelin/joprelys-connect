package db.migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V82__enforce_bed_capacity_and_readiness_integrity extends BaseJavaMigration {

    private static final List<String> STATEMENTS = List.of(
            """
            ALTER TABLE beds
                ADD CONSTRAINT ck_beds_capacity_status
                CHECK (capacity_status IN ('OPEN', 'CLOSED'))
            """,
            """
            ALTER TABLE beds
                ADD CONSTRAINT ck_beds_readiness_status
                CHECK (readiness_status IN ('READY', 'CLEANING', 'MAINTENANCE'))
            """,
            """
            ALTER TABLE beds
                ADD CONSTRAINT ck_beds_legacy_status_projection
                CHECK (
                    (status = 'FREE' AND capacity_status = 'OPEN' AND readiness_status = 'READY')
                    OR (status = 'OCCUPIED' AND capacity_status = 'OPEN' AND readiness_status = 'READY')
                    OR (status = 'CLEANING' AND readiness_status = 'CLEANING')
                    OR (
                        status = 'MAINTENANCE'
                        AND (readiness_status = 'MAINTENANCE' OR capacity_status = 'CLOSED')
                    )
                )
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
