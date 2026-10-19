import importlib.util
import json
from pathlib import Path
import unittest

s = importlib.util.spec_from_file_location('fcm_task', Path(__file__).with_name('production-fcm-task.py'))
m = importlib.util.module_from_spec(s)
s.loader.exec_module(m)


class FcmTaskTests(unittest.TestCase):
    def source(self):
        return {'family': 'tosunsaeng-learning-core-prod', 'revision': 2,
                'containerDefinitions': [{'name': 'app', 'image': 'repo@sha256:pinned',
                    'portMappings': [{'containerPort': 8080}],
                    'environment': [{'name': 'SPRING_APPLICATION_JSON', 'value': json.dumps({
                        'app': {'notifications': {'api-enabled': True, 'tracking-enabled': True}}})}],
                    'secrets': [{'name': 'EXISTING', 'valueFrom': 'unchanged'}]}]}

    def test_preserves_source_and_injects_only_dedicated_secret(self):
        source = self.source()
        out = m.build_task(source, 'arn:aws:secretsmanager:region:account:secret:tosunsaeng/prod/learning-core/fcm-example', '2026-10-08T09:32:00Z')
        self.assertEqual(source['revision'], 2)
        self.assertNotIn('revision', out)
        app = out['containerDefinitions'][0]
        self.assertEqual(app['image'], source['containerDefinitions'][0]['image'])
        self.assertEqual(app['secrets'][0], source['containerDefinitions'][0]['secrets'][0])
        n = json.loads(app['environment'][0]['value'])['app']['notifications']
        self.assertTrue(n['sending-enabled'])
        self.assertFalse(n['dry-run'])
        self.assertIn('umask 077', app['command'][0])
        self.assertIn('unset FCM_SERVICE_ACCOUNT_JSON', app['command'][0])
        self.assertIn('exec java', app['command'][0])

    def test_rejects_existing_service(self):
        source = self.source()
        source['family'] = 'tosunsaeng-learning-core'
        with self.assertRaises(ValueError):
            m.build_task(source, 'irrelevant', '2026-10-08T09:32:00Z')

    def test_flat_json_has_no_conflicting_override(self):
        source = self.source()
        source['containerDefinitions'][0]['environment'][0]['value'] = json.dumps({
            'app.notifications.api-enabled': True, 'app.notifications.tracking-enabled': True,
            'app.notifications.sending-enabled': False, 'app.notifications.dry-run': True})
        out = m.build_task(source, 'arn:aws:secretsmanager:region:account:secret:tosunsaeng/prod/learning-core/fcm-example', '2026-10-08T09:32:00Z')
        config = json.loads(out['containerDefinitions'][0]['environment'][0]['value'])
        self.assertNotIn('app.notifications.sending-enabled', config)
        self.assertTrue(config['app']['notifications']['sending-enabled'])

    def test_rejects_unrelated_secret(self):
        with self.assertRaises(ValueError):
            m.build_task(self.source(), 'arn:identity-secret', '2026-10-08T09:32:00Z')


if __name__ == '__main__':
    unittest.main()
