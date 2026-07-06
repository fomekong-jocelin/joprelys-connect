package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class V39__fix_medical_documents_h2_unique_constraint extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		String databaseProductName = connection.getMetaData().getDatabaseProductName();

		if ("H2".equalsIgnoreCase(databaseProductName)) {
			String uniqueConstraintName = null;
			String fkConstraintName = null;

			// Find UNIQUE constraint name on VISIT_ID
			String findUniqueSql = "SELECT tc.CONSTRAINT_NAME " +
					"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
					"JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu " +
					"ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME " +
					"WHERE UPPER(tc.TABLE_NAME) = 'MEDICAL_DOCUMENTS' " +
					"AND UPPER(tc.CONSTRAINT_TYPE) = 'UNIQUE' " +
					"AND UPPER(ccu.COLUMN_NAME) = 'VISIT_ID'";

			try (Statement stmt = connection.createStatement();
				 ResultSet rs = stmt.executeQuery(findUniqueSql)) {
				if (rs.next()) {
					uniqueConstraintName = rs.getString("CONSTRAINT_NAME");
				}
			}

			// Find FOREIGN KEY constraint name on VISIT_ID
			String findFkSql = "SELECT tc.CONSTRAINT_NAME " +
					"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
					"JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu " +
					"ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME " +
					"WHERE UPPER(tc.TABLE_NAME) = 'MEDICAL_DOCUMENTS' " +
					"AND UPPER(tc.CONSTRAINT_TYPE) IN ('REFERENTIAL', 'FOREIGN KEY') " +
					"AND UPPER(ccu.COLUMN_NAME) = 'VISIT_ID'";

			try (Statement stmt = connection.createStatement();
				 ResultSet rs = stmt.executeQuery(findFkSql)) {
				if (rs.next()) {
					fkConstraintName = rs.getString("CONSTRAINT_NAME");
				}
			}

			System.out.println("=== H2: Unique Constraint = " + uniqueConstraintName + ", FK Constraint = " + fkConstraintName + " ===");

			try (Statement stmt = connection.createStatement()) {
				// 1. Drop FOREIGN KEY first so it releases the index dependency
				if (fkConstraintName != null) {
					stmt.execute("ALTER TABLE medical_documents DROP CONSTRAINT " + fkConstraintName);
					System.out.println("=== H2 FK CONSTRAINT DROPPED SUCCESSFULLY ===");
				}

				// 2. Drop UNIQUE constraint
				if (uniqueConstraintName != null) {
					stmt.execute("ALTER TABLE medical_documents DROP CONSTRAINT " + uniqueConstraintName);
					System.out.println("=== H2 UNIQUE CONSTRAINT DROPPED SUCCESSFULLY ===");
				}

				// 3. Redefine the column to remove any column-level UNIQUE attribute
				stmt.execute("ALTER TABLE medical_documents ALTER COLUMN visit_id UUID NOT NULL");
				System.out.println("=== H2 COLUMN REDEFINED TO REMOVE UNIQUE ATTRIBUTE ===");

				// 4. Drop INDEX if it still exists
				if (uniqueConstraintName != null) {
					stmt.execute("DROP INDEX IF EXISTS " + uniqueConstraintName + "_INDEX_C");
					System.out.println("=== H2 INDEX DROPPED SUCCESSFULLY ===");
				}
			} catch (Exception e) {
				System.out.println("=== ERROR RUNNING H2 MIGRATION WORKAROUND: " + e.getMessage() + " ===");
				throw e;
			}
		}
	}
}
