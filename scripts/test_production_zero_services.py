import importlib.util
import unittest
from pathlib import Path

s = importlib.util.spec_from_file_location("zero", Path(__file__).with_name("production-zero-services.py"))
m = importlib.util.module_from_spec(s)
s.loader.exec_module(m)


class ZeroServiceTests(unittest.TestCase):
    def test_zero_private_no_exec(self):
        spec = dict(containerName="api", name="new-prod", securityGroup="sg-prod", port=8080)
        topology = dict(cluster="cluster", subnets=["subnet-private"])
        r = m.service_request("identity", spec, topology, "task", "target")
        self.assertEqual(r["desiredCount"], 0)
        self.assertFalse(r["enableExecuteCommand"])
        self.assertEqual(r["healthCheckGracePeriodSeconds"], 300)
        self.assertEqual(r["networkConfiguration"]["awsvpcConfiguration"]["assignPublicIp"], "DISABLED")
    def test_exam_internal_alias(self):
        r = m.service_request("examAi", dict(containerName="ai-api", name="ai-prod", securityGroup="sg-prod", port=8000), dict(cluster="c", subnets=[]), "task", None)
        self.assertNotIn("loadBalancers", r)
        self.assertEqual(r["serviceConnectConfiguration"]["services"][0]["clientAliases"][0]["dnsName"], "tosunsaeng-ai-prod")
    def test_test_secret_rejected(self):
        t = dict(family="identity-prod", networkMode="awsvpc", containerDefinitions=[dict(image="repo@sha256:abc", secrets=[dict(valueFrom="arn/test/key")], environment=[])])
        with self.assertRaises(ValueError):
            m.validate_task("identity", t)
    def test_publisher_rejected(self):
        t = dict(family="identity-prod", networkMode="awsvpc", containerDefinitions=[dict(image="repo@sha256:abc", secrets=[], environment=[dict(name="USER_WITHDRAWN_PUBLISHER_ENABLED", value="true")])])
        with self.assertRaises(ValueError):
            m.validate_task("identity", t)
    def test_build_uses_operating_not_test_environment(self):
        plan = dict(family="identity-prod", cpu="512", memory="1024", executionRole="prod-exec", runtimeRole=None, image="repo@sha256:abc")
        config = dict(environment={"JWT_ISSUER": "operating"}, launchTargetFlags={"USER_WITHDRAWN_PUBLISHER_ENABLED": "true"}, holdOffFlags={})
        source = dict(networkMode="awsvpc", containerDefinitions=[dict(name="api", environment=[dict(name="MONGODB_DATABASE", value="test")])])
        old = dict(containerDefinitions=[dict(environment=[dict(name="MONGODB_DATABASE", value="operating")])])
        t = m.build_task("identity", plan, config, source, old, {"api": {"MONGODB_URI": "arn:operating"}})
        values = m.env(t["containerDefinitions"][0])
        self.assertEqual(values["MONGODB_DATABASE"], "operating")
        self.assertEqual(values["USER_WITHDRAWN_PUBLISHER_ENABLED"], "false")
        self.assertEqual(values["APP_IOS_MINIMUM_VERSION"], "")


if __name__ == "__main__":
    unittest.main()
