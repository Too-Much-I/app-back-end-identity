import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("network", Path(__file__).with_name("production-runtime-network.py"))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class RuntimeNetworkTests(unittest.TestCase):
    def setUp(self):
        self.ids = {k: "sg-" + k for k in m.GROUPS}
    def test_exact_two_s3_roles(self):
        self.assertEqual(len(m.ROLES), 2)
        for role in m.ROLES:
            self.assertTrue(role.endswith("-prod-task-role"))
            self.assertTrue(all(x.startswith("s3:") for x in m.ROLES[role][0]))
    def test_delete_only_challenge_prefix(self):
        for actions, resource in m.ROLES.values():
            if "s3:DeleteObject" in actions:
                self.assertEqual(resource, "arn:aws:s3:::to-teacher-exam/challenge/*")
    def test_no_public_ingress(self):
        for incoming, outgoing in m.rules(self.ids).values():
            self.assertTrue(all(a[3] == "group" for a in incoming))
            self.assertTrue(all(a[0] == "tcp" for a in outgoing))
    def test_only_lc_reaches_exam_ai(self):
        plan = m.rules(self.ids)
        self.assertEqual(plan["examAi"][0], {m.tcp(8000, "group", self.ids["learningCore"])})
        self.assertIn(m.tcp(8000, "group", self.ids["examAi"]), plan["learningCore"][1])
    def test_identity_no_redis(self):
        self.assertNotIn(m.tcp(6379, "group", m.REDIS), m.rules(self.ids)["identity"][1])
    def test_normalization_roundtrip(self):
        for pair in m.rules(self.ids).values():
            for rules in pair:
                self.assertEqual(m.atoms([m.permission(a) for a in rules]), rules)
    def test_trust_not_human_or_cross_account(self):
        s = m.trust()["Statement"][0]
        self.assertEqual(s["Principal"], {"Service": "ecs-tasks.amazonaws.com"})
        self.assertEqual(s["Condition"]["StringEquals"]["aws:SourceAccount"], m.ACCOUNT)


if __name__ == "__main__":
    unittest.main()
