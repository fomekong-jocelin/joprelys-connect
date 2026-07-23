package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Fail-fast guard for HOS-STAFF-001-A.
 *
 * <p>No department/specialty text is mapped automatically to structured hospital
 * organization data. Existing non-empty legacy values must be handled by an
 * explicit, reviewed migration decision before V93/V95 can run.</p>
 */
public class V92__preflight_structured_staff_assignments extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        if (!tableExists(connection, "users")) {
            return;
        }

        long departmentCount = countNonBlank(connection, "department");
        long specialtyCount = countNonBlank(connection, "specialty");
        if (departmentCount == 0 && specialtyCount == 0) {
            return;
        }

        throw new FlywayException(
                "HOS-STAFF V92 blocked: legacy free-text staff data requires an explicit reviewed migration decision "
                        + "before structured assignments can be enabled. "
                        + "department=" + departmentCount + ", specialty=" + specialtyCount + ". "
                        + "No automatic mapping by name or similarity is allowed.");
    }

    private long countNonBlank(Connection connection, String column) throws SQLException {
        if (!columnExists(connection, "users", column)) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM users WHERE " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (tables.next()) {
                if (tableName.equalsIgnoreCase(tables.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "%", "%")) {
            while (columns.next()) {
                if (tableName.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                        && columnName.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
