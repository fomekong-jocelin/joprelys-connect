import importlib.util, unittest
from pathlib import Path
SCRIPT = Path(__file__).parents[1] / "diagnose_joprelys_server.py"
SPEC = importlib.util.spec_from_file_location("diagnose_joprelys_server", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC); SPEC.loader.exec_module(MODULE)
class DiagnoseJoprelysServerTest(unittest.TestCase):
    def test_redact_masks_secret_values(self):
        result = MODULE.redact("SPRING_DATASOURCE_PASSWORD=database-password\nSAFE_NAME=value\nJOPRELYS_JWT_SECRET=jwt")
        self.assertIn("SPRING_DATASOURCE_PASSWORD=<masqué>", result); self.assertIn("JOPRELYS_JWT_SECRET=<masqué>", result)
        self.assertIn("SAFE_NAME=value", result); self.assertNotIn("database-password", result)
    def test_commands_use_non_interactive_sudo(self):
        privileged = [command for _, command in MODULE.diagnostic_commands(MODULE.DEFAULT_SERVICE) if "sudo" in command]
        self.assertTrue(privileged); self.assertTrue(all("sudo -n" in command for command in privileged))
    def test_default_ssh_port_is_22(self): self.assertEqual(22, MODULE.DEFAULT_PORT)
if __name__ == "__main__": unittest.main()
