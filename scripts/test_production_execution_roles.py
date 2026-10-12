import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("roles", Path(__file__).with_name("production-execution-roles.py"))
roles = importlib.util.module_from_spec(spec)
spec.loader.exec_module(roles)


class ExecutionRoleTests(unittest.TestCase):
    def policy(self, plan):
        refs = {name: f"arn:aws:secretsmanager:{roles.REGION}:{roles.ACCOUNT}:secret:{name}-abc123" for name in plan["secretNames"]}
        return roles.permission_policy(plan, refs)
    def test_only_four_separate_prod_roles(self):
        self.assertEqual(len(roles.PLANS), 4)
        self.assertEqual(len({p["role"] for p in roles.PLANS.values()}), 4)
        self.assertTrue(all("-prod-" in p["role"] for p in roles.PLANS.values()))
    def test_no_test_secrets(self):
        for plan in roles.PLANS.values():
            self.assertTrue(all("/test/" not in n for n in plan["secretNames"]))
    def test_only_registry_authorization_has_global_resource(self):
        for plan in roles.PLANS.values():
            statements = self.policy(plan)["Statement"]
            self.assertEqual([s["Action"] for s in statements if s["Resource"] == "*"], [["ecr:GetAuthorizationToken"]])
    def test_secret_values_read_only_exact_resources(self):
        for plan in roles.PLANS.values():
            statement = self.policy(plan)["Statement"][0]
            self.assertEqual(statement["Action"], ["secretsmanager:GetSecretValue"])
            self.assertTrue(all("*" not in r for r in statement["Resource"]))
    def test_repository_and_log_groups_scoped(self):
        for plan in roles.PLANS.values():
            policy = self.policy(plan)["Statement"]
            self.assertTrue(policy[2]["Resource"].endswith("/" + plan["repository"]))
            self.assertTrue(policy[3]["Resource"].endswith(plan["logGroup"] + ":log-stream:*"))
    def test_trust_only_same_account_region_ecs(self):
        t = roles.trust_policy()["Statement"][0]
        self.assertEqual(t["Principal"], {"Service": "ecs-tasks.amazonaws.com"})
        self.assertEqual(t["Condition"]["StringEquals"]["aws:SourceAccount"], roles.ACCOUNT)
    def test_missing_or_cross_account_refs_rejected(self):
        plan = roles.PLANS["examAi"]
        with self.assertRaises(ValueError):
            roles.permission_policy(plan, {})
        with self.assertRaises(ValueError):
            roles.permission_policy(plan, {plan["secretNames"][0]: "arn:aws:secretsmanager:ap-northeast-2:000000000000:secret:tosunsaeng/staging/ai-abc123"})


if __name__ == "__main__":
    unittest.main()
