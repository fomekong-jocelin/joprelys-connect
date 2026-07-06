package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public class V34__lab_orders_cdc_alignment extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();

		// 1. Add source_organization_id column
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("ALTER TABLE lab_orders ADD COLUMN source_organization_id UUID");
		}

		// 2. Set source_organization_id to organization_id
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("UPDATE lab_orders SET source_organization_id = organization_id");
		}

		// 3. Create lab_order_items table
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("CREATE TABLE lab_order_items (id UUID PRIMARY KEY, lab_order_id UUID NOT NULL REFERENCES lab_orders(id) ON DELETE CASCADE, exam_name VARCHAR(255) NOT NULL)");
		}

		// 4. Migrate CSV exams to lab_order_items
		try (Statement stmt = connection.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT id, exams FROM lab_orders WHERE exams IS NOT NULL AND exams <> ''")) {
			
			String insertSql = "INSERT INTO lab_order_items (id, lab_order_id, exam_name) VALUES (?, ?, ?)";
			try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
				while (rs.next()) {
					UUID orderId = (UUID) rs.getObject("id");
					String examsCsv = rs.getString("exams");
					if (examsCsv != null) {
						String[] exams = examsCsv.split(",");
						for (String exam : exams) {
							String trimmed = exam.trim();
							if (!trimmed.isEmpty()) {
								pstmt.setObject(1, UUID.randomUUID());
								pstmt.setObject(2, orderId);
								pstmt.setString(3, trimmed);
								pstmt.addBatch();
							}
						}
					}
				}
				pstmt.executeBatch();
			}
		}

		// 5. Drop exams column
		try (Statement stmt = connection.createStatement()) {
			stmt.execute("ALTER TABLE lab_orders DROP COLUMN exams");
		}
	}
}
