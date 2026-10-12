import base64
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("provision", Path(__file__).with_name("provision-production-secrets.py"))
provision = importlib.util.module_from_spec(spec)
spec.loader.exec_module(provision)


class FakeClient:
    class exceptions:
        class ResourceNotFoundException(Exception):
            pass
        class ResourceExistsException(Exception):
            pass
    def __init__(self, existing=None):
        self.existing = existing or {}
        self.created = []
    def describe_secret(self, SecretId):
        if SecretId not in self.existing:
            raise self.exceptions.ResourceNotFoundException()
        return self.existing[SecretId]
    def create_secret(self, **kwargs):
        if kwargs["Name"] in self.existing:
            raise self.exceptions.ResourceExistsException()
        self.created.append(kwargs)
        self.existing[kwargs["Name"]] = {}


class ProvisionTests(unittest.TestCase):
    def test_formats_match_application_parsers(self):
        values = [provision.material(k) for k in ("reissue", "phone", "recovery")]
        self.assertTrue(values[0].startswith("prod-v1="))
        self.assertTrue(values[1].startswith("prod-v1,ACTIVE_WRITE,"))
        self.assertTrue(values[2].startswith("prod-v1:"))
        raw = [values[0].split("=", 1)[1].strip(), values[1].split(",")[2], values[2].split(":")[1]]
        self.assertTrue(all(len(base64.b64decode(v, validate=True)) == 32 for v in raw))
        self.assertEqual(len(set(raw)), 3)
    def test_phone_status_matches_java_contract(self):
        root = Path(__file__).resolve().parents[1]
        enum = (root / "src/main/java/web/tosunsaeng/identity/domain/auth/domain/enums/PhoneFingerprintKeyStatus.java").read_text()
        status = provision.material("phone").split(",")[1]
        self.assertEqual(status, "ACTIVE_WRITE")
        self.assertIn(status + ",", enum)
    def test_directional_tokens_are_distinct_and_printable(self):
        left, right = provision.material("token"), provision.material("token")
        self.assertNotEqual(left, right)
        self.assertEqual(len(left), 43)
        self.assertTrue(left.isascii() and left.isprintable())
    def test_plan_does_not_create(self):
        client = FakeClient()
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(len(provision.inventory(client)), 6)
        self.assertEqual(client.created, [])
    def test_resume_never_overwrites_or_prints_material(self):
        client = FakeClient()
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            provision.create_missing(client, list(provision.TARGETS))
            provision.create_missing(client, list(provision.TARGETS))
        self.assertEqual(len(client.created), 6)
        self.assertTrue(all(x["SecretString"] not in output.getvalue() for x in client.created))
    def test_pending_deletion_blocks(self):
        client = FakeClient({next(iter(provision.TARGETS)): {"DeletedDate": "pending"}})
        with self.assertRaises(RuntimeError):
            provision.inventory(client)


if __name__ == "__main__":
    unittest.main()
