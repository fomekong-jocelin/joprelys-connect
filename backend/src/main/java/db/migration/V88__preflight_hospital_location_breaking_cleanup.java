package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * HOS-LOC-001-A / #131.
 *
 * <p>Preflight intentionally performs no mutation. The location/space cleanup removes the legacy
 * Ward/Room identity model and cannot safely infer structured UUIDs from historical free-text
 * names. Any legacy spatial or hospitalization data therefore blocks the breaking schema change
 * until an explicit reset or an approved remapping procedure has been performed.</p>
 */
public class V88__preflight_hospital_location_breaking_cleanup extends BaseJavaMigration {

    private static final Map<String, String> LEGACY_TABLES = Map.ofEntries(
            Map.entry("wards", "wards"),
            Map.entry("rooms", "rooms"),
            Map.entry("beds", "beds"),
            Map.entry("bedAssignments", "bed_assignments"),
            Map.entry("hospitalizations", "hospitalizations"));

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, Long> counts = legacyCounts(context.getConnection());
        boolean hasLegacyData = counts.values().stream().anyMatch(count -> count > 0);
        if (!hasLegacyData) {
            return;
        }

        StringJoiner details = new StringJoiner(" ");
        counts.forEach((label, count) -> details.add(label + "=" + count));

        throw new FlywayException(
                "HOS-LOC V88 blocked: legacy spatial/hospitalization data detected. "
                        + details
                        + ". No automatic mapping is allowed. Reset the development database or execute an approved "
                        + "versioned remapping procedure before retrying Flyway.");
    }

    @Override
    public Integer getChecksum() {
        return Objects.hash(LEGACY_TABLES);
    }

    private Map<String, Long> legacyCounts(Connection connection) throws SQLException {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : LEGACY_TABLES.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            counts.put(entry.getKey(), countRows(connection, entry.getValue()));
        }
        return counts;
    }

    private long countRows(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new FlywayException("HOS-LOC V88 preflight returned no result for table " + tableName + ".");
            }
            return resultSet.getLong(1);
        }
    }
}
