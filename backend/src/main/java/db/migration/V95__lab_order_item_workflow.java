package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Adds per-exam lifecycle tracking while preserving legacy order-level data. */
public class V95__lab_order_item_workflow extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        addColumnIfMissing(connection, "lab_order_items", "status", "VARCHAR(30) DEFAULT 'REQUESTED' NOT NULL");
        addColumnIfMissing(connection, "lab_order_items", "sample_collected_at", "TIMESTAMP WITH TIME ZONE");
        addColumnIfMissing(connection, "lab_order_items", "result_at", "TIMESTAMP WITH TIME ZONE");
        addColumnIfMissing(connection, "lab_order_items", "validated_at", "TIMESTAMP WITH TIME ZONE");
        addColumnIfMissing(connection, "lab_results", "lab_order_item_id", "UUID");

        backfillItemStatuses(connection);
        addForeignKeyIfMissing(connection);
        execute(connection, "CREATE INDEX IF NOT EXISTS idx_lab_results_order_item ON lab_results(lab_order_item_id)");
    }

    private void backfillItemStatuses(Connection connection) throws SQLException {
        List<ItemStatus> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT i.id, o.status
                FROM lab_order_items i
                JOIN lab_orders o ON o.id = i.lab_order_id
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ItemStatus(resultSet.getObject(1), resultSet.getString(2)));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE lab_order_items SET status = ? WHERE id = ?")) {
            for (ItemStatus row : rows) {
                statement.setString(1, normalizeItemStatus(row.status()));
                statement.setObject(2, row.id());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private String normalizeItemStatus(String status) {
        if (status == null) {
            return "REQUESTED";
        }
        return switch (status) {
            case "SAMPLE_COLLECTED", "IN_PROGRESS", "RESULT_AVAILABLE", "VALIDATED", "CANCELLED" -> status;
            default -> "REQUESTED";
        };
    }

    private void addColumnIfMissing(Connection connection, String table, String column, String definition)
            throws SQLException {
        if (!columnExists(connection, table, column)) {
            execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, table, column)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
            return resultSet.next();
        }
    }

    private void addForeignKeyIfMissing(Connection connection) throws SQLException {
        if (foreignKeyExists(connection, "lab_results", "lab_order_item_id")) {
            return;
        }
        execute(connection, """
                ALTER TABLE lab_results
                ADD CONSTRAINT fk_lab_results_order_item
                FOREIGN KEY (lab_order_item_id) REFERENCES lab_order_items(id)
                """);
    }

    private boolean foreignKeyExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(null, null, table)) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("FKCOLUMN_NAME"))) {
                    return true;
                }
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(null, null, table.toUpperCase())) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("FKCOLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private record ItemStatus(Object id, String status) {}
}
