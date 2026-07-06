package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V40__fix_lab_results_result_number_uniqueness extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		String databaseProductName = connection.getMetaData().getDatabaseProductName();

		if ("H2".equalsIgnoreCase(databaseProductName)) {
			// Find UNIQUE constraint name on RESULT_NUMBER in LAB_RESULTS (case-insensitive)
			String findUniqueSql = "SELECT tc.CONSTRAINT_NAME " +
					"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
					"JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu " +
					"ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME " +
					"WHERE UPPER(tc.TABLE_NAME) = 'LAB_RESULTS' " +
					"AND UPPER(tc.CONSTRAINT_TYPE) = 'UNIQUE' " +
					"AND UPPER(ccu.COLUMN_NAME) = 'RESULT_NUMBER'";

			try (Statement stmt = connection.createStatement();
				 ResultSet rs = stmt.executeQuery(findUniqueSql)) {
				while (rs.next()) {
					String constraintName = rs.getString("CONSTRAINT_NAME");
					try (Statement dropStmt = connection.createStatement()) {
						dropStmt.execute("ALTER TABLE lab_results DROP CONSTRAINT " + constraintName);
						System.out.println("=== H2 LAB RESULTS CONSTRAINT " + constraintName + " DROPPED SUCCESSFULLY ===");
					} catch (Exception e) {
						System.out.println("=== FAILED TO DROP H2 CONSTRAINT " + constraintName + ": " + e.getMessage() + " ===");
					}
				}
			}

			try (Statement stmt = connection.createStatement()) {
				// 1. Redefine the column to remove any column-level UNIQUE attribute
				stmt.execute("ALTER TABLE lab_results ALTER COLUMN result_number VARCHAR(50) NOT NULL");
				System.out.println("=== H2 LAB RESULTS COLUMN REDEFINED TO REMOVE UNIQUE ATTRIBUTE ===");

				// 2. Drop INDEX if it still exists
				stmt.execute("DROP INDEX IF EXISTS uk_lab_results_result_number");
				System.out.println("=== H2 LAB RESULTS INDEX DROPPED SUCCESSFULLY ===");

				// 3. Create the composite unique index
				stmt.execute("CREATE UNIQUE INDEX uk_lab_results_number_version_analyte ON lab_results (result_number, version, analyte_name)");
				System.out.println("=== H2 LAB RESULTS COMPOSITE UNIQUE INDEX CREATED SUCCESSFULLY ===");
			} catch (Exception e) {
				System.out.println("=== ERROR RUNNING H2 LAB RESULTS MIGRATION WORKAROUND: " + e.getMessage() + " ===");
				throw e;
			}
		} else {
			// For PostgreSQL and other databases
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("ALTER TABLE lab_results DROP CONSTRAINT IF EXISTS lab_results_result_number_key");
				stmt.execute("DROP INDEX IF EXISTS uk_lab_results_result_number");
				stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_lab_results_number_version_analyte ON lab_results (result_number, version, analyte_name)");
				System.out.println("=== POSTGRES/DEFAULT LAB RESULTS COMPOSITE UNIQUE INDEX CREATED SUCCESSFULLY ===");
			}
		}
	}
}
