import copy
import importlib.util
import re
from pathlib import Path
import unittest

s = importlib.util.spec_from_file_location('withdrawal_task', Path(__file__).with_name('production-withdrawal-task.py'))
m = importlib.util.module_from_spec(s)
s.loader.exec_module(m)


class WithdrawalTaskTests(unittest.TestCase):
    def source(self):
        values = dict.fromkeys(m.FLAGS, 'false')
        values.update(FIREBASE_AUTH_ENABLED='true', WORKLOAD_JWT_ENABLED='true',
                      USER_WITHDRAWN_PUBLISHER_ENDPOINT='https://api-prod.to-teacher.com/internal/v1/events/user-withdrawn',
                      GUEST_MERGE_ENABLED='false', USER_WITHDRAWN_BACKFILL_ENABLED='false')
        return {'family': 'tosunsaeng-identity-prod', 'revision': 2,
                'containerDefinitions': [{'image': 'repo@sha256:pinned', 'portMappings': [{'containerPort': 8081}],
                    'secrets': [{'name': 'EXISTING', 'valueFrom': 'unchanged'}],
                    'environment': [{'name': k, 'value': v} for k, v in values.items()]}]}

    def test_only_four_flags_and_incorrect_endpoint_change(self):
        source = self.source()
        expected = copy.deepcopy(source)
        expected.pop('revision')
        for item in expected['containerDefinitions'][0]['environment']:
            if item['name'] in m.FLAGS:
                item['value'] = 'true'
            elif item['name'] == 'USER_WITHDRAWN_PUBLISHER_ENDPOINT':
                item['value'] = m.ENDPOINT
        self.assertEqual(m.build_task(source), expected)
        self.assertEqual(source, self.source())

    def test_endpoint_matches_java_publisher_contract(self):
        path = Path(__file__).resolve().parents[1] / 'src/main/java/web/tosunsaeng/identity/domain/user/withdrawalevent/infrastructure/UserWithdrawnPublisherProperties.java'
        required = re.search(r'REQUIRED_PATH = "([^"]+)"', path.read_text()).group(1)
        self.assertEqual(m.ENDPOINT, 'https://api-prod.to-teacher.com' + required)

    def test_rejects_old_service(self):
        source = self.source()
        source['family'] = 'tosunsaeng-identity'
        with self.assertRaises(ValueError):
            m.build_task(source)

    def test_rejects_old_callback(self):
        source = self.source()
        for item in source['containerDefinitions'][0]['environment']:
            if item['name'] == 'USER_WITHDRAWN_PUBLISHER_ENDPOINT':
                item['value'] = 'https://api-staging.to-teacher.com/internal/v1/events/user-withdrawn'
        with self.assertRaises(ValueError):
            m.build_task(source)

    def test_rejects_json_flag_override(self):
        source = self.source()
        source['containerDefinitions'][0]['environment'].append({'name': 'SPRING_APPLICATION_JSON',
            'value': '{"app.firebase-withdrawal.enabled":false}'})
        with self.assertRaises(ValueError):
            m.build_task(source)


if __name__ == '__main__':
    unittest.main()
