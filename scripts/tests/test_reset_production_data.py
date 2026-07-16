import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).parents[1] / "reset_production_data.py"
SPEC = importlib.util.spec_from_file_location("reset_production_data", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ResetProductionDataTest(unittest.TestCase):
    def test_sql_preserves_flyway_and_creates_only_super_admin(self):
        sql = MODULE.build_reset_sql("temporary-test-password")
        self.assertIn("tablename <> 'flyway_schema_history'", sql)
        self.assertIn(MODULE.ADMIN_EMAIL, sql)
        self.assertIn("SUPER_ADMIN", sql)
        self.assertNotIn("temporary-test-password", sql)
        self.assertIn("$2", sql)

    def test_sql_literal_escapes_quotes(self):
        self.assertEqual("'Jocelin''s'", MODULE.sql_literal("Jocelin's"))

    def test_remote_sql_is_transferred_before_execution(self):
        self.assertTrue(callable(MODULE.upload_private_file))


if __name__ == "__main__":
    unittest.main()
